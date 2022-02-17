package org.recap.camel.submitcollection.processor;

import com.amazonaws.services.s3.AmazonS3;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.camel.EmailPayLoad;
import org.recap.model.reports.ReportDataRequest;
import org.recap.service.common.SetupDataService;
import org.recap.service.submitcollection.SubmitCollectionBatchService;
import org.recap.service.submitcollection.SubmitCollectionReportGenerator;
import org.recap.service.submitcollection.SubmitCollectionService;
import org.recap.util.CommonUtil;
import org.recap.util.PropertyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by premkb on 19/3/17.
 */
@Slf4j
@Service
@Scope("prototype")
public class SubmitCollectionProcessor {


    @Autowired
    private SubmitCollectionService submitCollectionService;

    @Autowired
    private SubmitCollectionBatchService submitCollectionBatchService;

    @Autowired
    private SubmitCollectionReportGenerator submitCollectionReportGenerator;

    @Autowired
    private ProducerTemplate producer;

    @Autowired
    PropertyUtil propertyUtil;

    @Value("${" + PropertyKeyConstants.EMAIL_SUBMIT_COLLECTION_SUBJECT + "}")
    private String submitCollectionEmailSubject;

    @Value("${" + PropertyKeyConstants.EMAIL_SUBMIT_COLLECTION_SUBJECT_FOR_EMPTY_DIRECTORY + "}")
    private String subjectForEmptyDirectory;

    @Value("${" + PropertyKeyConstants.S3_SUBMIT_COLLECTION_REPORT_DIR + "}")
    private String submitCollectionReportS3Dir;

    private String institutionCode;
    private boolean isCGDProtection;
    private String cgdType;

    @Autowired
    private SetupDataService setupDataService;

    @Autowired
    AmazonS3 awsS3Client;

    @Value("${" + PropertyKeyConstants.S3_SUBMIT_COLLECTION_DIR + "}")
    private String submitCollectionS3BasePath;

    @Value("${" + PropertyKeyConstants.SCSB_BUCKET_NAME + "}")
    private String bucketName;

    @Value("${" + PropertyKeyConstants.SUBMIT_COLLECTION_USE_SOLR_PARTIAL_INDEX_TOTAL_DOCS_SIZE + ":1000}")
    private int solrMaxDocSizeToUsePartialIndex;

    @Autowired
    private CommonUtil commonUtil;

    public SubmitCollectionProcessor() {
    }

    public SubmitCollectionProcessor(String inputInstitutionCode, boolean isCGDProtection, String cgdType) {
        this.institutionCode = inputInstitutionCode;
        this.isCGDProtection = isCGDProtection;
        this.cgdType=cgdType;
    }

    /**
     * Process input.
     *
     * @param exchange the exchange
     * @throws Exception the exception
     */
    public void processInput(Exchange exchange) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Set<Integer> processedBibIds = new HashSet<>();
        Set<String> updatedBoundWithDummyRecordOwnInstBibIdSet = new HashSet<>();
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        List<Map<String, String>> bibIdMapToRemoveIndexList = new ArrayList<>();
        List<Integer> reportRecordNumList = new ArrayList<>();
        String xmlFileName = null;
        ExecutorService executorService = null;
        try {
            log.info("Submit Collection : Route started and started processing the records from s3 for submitcollection");
            String inputXml = exchange.getIn().getBody(String.class);
            xmlFileName = exchange.getIn().getHeader(ScsbConstants.CAMEL_FILE_NAME_ONLY).toString();
            xmlFileName = submitCollectionS3BasePath+ institutionCode+ ScsbCommonConstants.PATH_SEPARATOR + "cgd_" + cgdType + ScsbCommonConstants.PATH_SEPARATOR + xmlFileName;
            log.info("Processing xmlFileName----->{}", xmlFileName);
            Integer institutionId = setupDataService.getInstitutionCodeIdMap().get(institutionCode);
            executorService = Executors.newFixedThreadPool(10);
            List<Future> futures = new ArrayList<>();
            submitCollectionBatchService.process(institutionCode, inputXml, processedBibIds, idMapToRemoveIndexList, bibIdMapToRemoveIndexList, xmlFileName, reportRecordNumList, false, isCGDProtection, updatedBoundWithDummyRecordOwnInstBibIdSet, exchange, executorService, futures);
            log.info("Submit Collection : Solr indexing started for {} records", processedBibIds.size());
            performIndexing(processedBibIds);
            performIndexingByOwningInstitutionBibIds(updatedBoundWithDummyRecordOwnInstBibIdSet, institutionId);
            performIndexingToRemoveBibs(idMapToRemoveIndexList, bibIdMapToRemoveIndexList);
            collectFuturesAndProcess(futures);
            executorService.shutdown();
            ReportDataRequest reportRequest = getReportDataRequest(xmlFileName);
            String generatedReportFileName = submitCollectionReportGenerator.generateReport(reportRequest);
            producer.sendBodyAndHeader(ScsbConstants.EMAIL_Q, getEmailPayLoad(xmlFileName, generatedReportFileName), ScsbConstants.EMAIL_BODY_FOR, ScsbConstants.SUBMIT_COLLECTION);
            if (awsS3Client.doesObjectExist(bucketName, xmlFileName) && (inputXml != null && !inputXml.equals(""))) {
                String basepath = xmlFileName.substring(0, xmlFileName.lastIndexOf('/'));
                String fileName = xmlFileName.substring(xmlFileName.lastIndexOf('/'));
                awsS3Client.copyObject(bucketName, xmlFileName, bucketName, basepath + "/.done-" + institutionCode + "-cgd_" + cgdType + fileName);
                awsS3Client.deleteObject(bucketName, xmlFileName);
            }
            stopWatch.stop();
            log.info("Submit Collection : Total time taken for processing through s3---> {} sec", stopWatch.getTotalTimeSeconds());
        } catch (Exception e) {
            log.info("Caught for institution inside catch block {} ",institutionCode);
            log.error(ScsbCommonConstants.LOG_ERROR, e);
            exchange.setException(e);
            if (executorService != null) {
                executorService.shutdown();
            }
        }
    }

    private void performIndexingToRemoveBibs(List<Map<String, String>> idMapToRemoveIndexList, List<Map<String, String>> bibIdMapToRemoveIndexList) {
        log.info("Submit Collection : Solr indexing completed and remove the incomplete record from solr index for {} records", idMapToRemoveIndexList.size());
        if (!idMapToRemoveIndexList.isEmpty() || !bibIdMapToRemoveIndexList.isEmpty()) {//remove the incomplete record from solr index
            StopWatch stopWatchRemovingDummy = new StopWatch();
            stopWatchRemovingDummy.start();
            log.info("Calling indexing to remove dummy records");
            new Thread(() -> {
                try {
                    submitCollectionBatchService.removeBibFromSolrIndex(bibIdMapToRemoveIndexList);
                    submitCollectionBatchService.removeSolrIndex(idMapToRemoveIndexList);
                    log.info("Removed dummy records from solr");
                } catch (Exception e) {
                    log.error(ScsbCommonConstants.LOG_ERROR, e);
                }
            }).start();
            stopWatchRemovingDummy.stop();
            log.info("Time take to call and execute solr call to remove dummy-->{} sec", stopWatchRemovingDummy.getTotalTimeSeconds());
        }
    }

    private void performIndexingByOwningInstitutionBibIds(Set<String> updatedBoundWithDummyRecordOwnInstBibIdSet, Integer institutionId) {
        if (!updatedBoundWithDummyRecordOwnInstBibIdSet.isEmpty()) {
            log.info("Updated boundwith dummy record own inst bib id size-->{}", updatedBoundWithDummyRecordOwnInstBibIdSet.size());
            submitCollectionService.indexDataUsingOwningInstBibId(new ArrayList<>(updatedBoundWithDummyRecordOwnInstBibIdSet), institutionId);
        }
    }

    private void performIndexing(Set<Integer> processedBibIds) {
        if (!processedBibIds.isEmpty()) {
            StopWatch stopWatchSolrIndexing = new StopWatch();
            stopWatchSolrIndexing.start();
            String indexingStatus = null;
            if (processedBibIds.size() > solrMaxDocSizeToUsePartialIndex) { // If the number of bib Ids is greater than configured value, default is 1000, index data with multi-threading using partial index api
                indexingStatus = submitCollectionBatchService.partialIndexData(processedBibIds);
            } else { // If the number of bib Ids is less than configured value, default is 1000, index data without multi-threading
                indexingStatus = submitCollectionBatchService.indexData(processedBibIds);
            }
            log.info("Submit Collection : Solr indexing Status - {}", indexingStatus);
            stopWatchSolrIndexing.stop();
            log.info("Submit Collection : Total Time taken to do solr indexing : {} sec", stopWatchSolrIndexing.getTotalTimeSeconds());
        }
    }

    public void caughtException(Exchange exchange) {
        log.info("inside caught exception..........");
        Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        log.info("Headers - institution - {}, isCgdProtected - {}, cgdType - {} ", exchange.getIn().getHeader(ScsbCommonConstants.INSTITUTION),
                exchange.getIn().getHeader(ScsbCommonConstants.IS_CGD_PROTECTED), exchange.getIn().getHeader(ScsbConstants.CGG_TYPE));
        if (exception != null) {
            String fileName = (String) exchange.getIn().getHeader(Exchange.FILE_NAME);
            String filePath = (String) exchange.getIn().getHeader(Exchange.FILE_PARENT);
            String institutionCode1 = (String) exchange.getIn().getHeader(ScsbCommonConstants.INSTITUTION);
            log.info("Institution inside caught  - {}", institutionCode1);
            log.info("Exception occured is - {}", exception.getMessage());
            producer.sendBodyAndHeader(ScsbConstants.EMAIL_Q, getEmailPayLoadForExcepion(institutionCode1, fileName, filePath, exception, exception.getMessage()), ScsbConstants.EMAIL_BODY_FOR, ScsbConstants.SUBMIT_COLLECTION_EXCEPTION);
        }
    }

    private void collectFuturesAndProcess(List<Future> futures) {
        log.info("Before Collecting Futures - Number of Futures for Match Point Checks: {}", futures.size());
        Set<Integer> bibIds = commonUtil.collectFuturesAndUpdateMAQualifier(futures);
        if (!bibIds.isEmpty()) {
            log.info("Submit Collection : Solr indexing started for MA Qualifier Update. Total Bib Records : {}", bibIds.size());
            performIndexing(bibIds);
        }
    }

    private EmailPayLoad getEmailPayLoadForExcepion(String institutionCode, String name, String filePath, Exception exception, String exceptionMessage) {
        EmailPayLoad emailPayLoad = new EmailPayLoad();
        emailPayLoad.setSubject(ScsbConstants.SUBJECT_FOR_SUBMIT_COL_EXCEPTION);
        emailPayLoad.setXmlFileName(name);
        log.info("Institution inside email payload for exception- {}", institutionCode);
        emailPayLoad.setTo(propertyUtil.getPropertyByInstitutionAndKey(institutionCode, PropertyKeyConstants.ILS.ILS_EMAIL_SUBMIT_COLLECTION_TO));
        emailPayLoad.setCc(propertyUtil.getPropertyByInstitutionAndKey(institutionCode, PropertyKeyConstants.ILS.ILS_EMAIL_SUBMIT_COLLECTION_CC));
        emailPayLoad.setLocation(submitCollectionReportS3Dir);
        emailPayLoad.setLocation(filePath);
        emailPayLoad.setInstitution(institutionCode.toUpperCase());
        emailPayLoad.setException(exception);
        emailPayLoad.setExceptionMessage(exceptionMessage);
        return emailPayLoad;

    }

    private ReportDataRequest getReportDataRequest(String xmlFileName) {
        ReportDataRequest reportRequest = new ReportDataRequest();
        log.info("filename--->{}-{}", ScsbCommonConstants.SUBMIT_COLLECTION_REPORT, xmlFileName);
        reportRequest.setFileName(ScsbCommonConstants.SUBMIT_COLLECTION_REPORT + "-" + xmlFileName);
        reportRequest.setInstitutionCode(institutionCode.toUpperCase());
        reportRequest.setReportType(ScsbCommonConstants.SUBMIT_COLLECTION_SUMMARY);
        reportRequest.setTransmissionType(ScsbCommonConstants.FTP);
        return reportRequest;
    }

    private EmailPayLoad getEmailPayLoad(String xmlFileName, String reportFileName) {
        EmailPayLoad emailPayLoad = new EmailPayLoad();
        emailPayLoad.setSubject(submitCollectionEmailSubject);
        emailPayLoad.setReportFileName(reportFileName);
        emailPayLoad.setXmlFileName(xmlFileName);
        log.info("Institution inside email payload - {}", institutionCode);
        emailPayLoad.setTo(propertyUtil.getPropertyByInstitutionAndKey(institutionCode, PropertyKeyConstants.ILS.ILS_EMAIL_SUBMIT_COLLECTION_TO));
        emailPayLoad.setCc(propertyUtil.getPropertyByInstitutionAndKey(institutionCode, PropertyKeyConstants.ILS.ILS_EMAIL_SUBMIT_COLLECTION_CC));
        emailPayLoad.setLocation(submitCollectionReportS3Dir);
        emailPayLoad.setInstitution(institutionCode.toUpperCase());
        return emailPayLoad;
    }

    /**
     * This method is used to send email when there are no files in the respective directory
     */
    public void sendEmailForEmptyDirectory() {
        String s3Path = submitCollectionS3BasePath+ institutionCode + "/cgd_" + cgdType;
        producer.sendBodyAndHeader(ScsbConstants.EMAIL_Q, getEmailPayLoadForNoFiles(institutionCode,s3Path), ScsbConstants.EMAIL_BODY_FOR, ScsbConstants.SUBMIT_COLLECTION_FOR_NO_FILES);
    }

    private EmailPayLoad getEmailPayLoadForNoFiles(String institutionCode, String ftpLocationPath) {
        EmailPayLoad emailPayLoad = new EmailPayLoad();
        emailPayLoad.setSubject(subjectForEmptyDirectory+" - "+institutionCode+" - "+cgdType);
        emailPayLoad.setLocation(ftpLocationPath);
        emailPayLoad.setTo(propertyUtil.getPropertyByInstitutionAndKey(institutionCode, PropertyKeyConstants.ILS.ILS_EMAIL_SUBMIT_COLLECTION_NOFILES_TO));
        return  emailPayLoad;
    }
}
