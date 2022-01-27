package org.recap.service.accession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.commons.collections.CollectionUtils;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.accession.AccessionModelRequest;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.accession.AccessionResponse;
import org.recap.model.accession.AccessionSummary;
import org.recap.model.jpa.AccessionEntity;
import org.recap.model.jpa.ReportDataEntity;
import org.recap.repository.jpa.AccessionDetailsRepository;
import org.recap.service.accession.callable.BibDataCallable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Created by sheiks on 26/05/17.
 */
@Slf4j
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class BulkAccessionService extends AccessionService{



    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AccessionValidationService accessionValidationService;

    @Autowired
    private AccessionDetailsRepository accessionDetailsRepository;

    /**
     * The batch accession thread size.
     */
    @Value("${" + PropertyKeyConstants.BATCH_ACCESSION_THREAD_SIZE + "}")
    int batchAccessionThreadSize;

    /**
     * This method saves the accession request in database and returns the status message.
     *
     * @param accessionModelRequest
     * @return
     */
    @Transactional
    public String saveRequest(AccessionModelRequest accessionModelRequest) {
        List<AccessionRequest> trimmedAccessionRequests = getTrimmedAccessionRequests(accessionModelRequest.getAccessionRequests());
        String status;
        try {
            AccessionEntity accessionEntity = new AccessionEntity();
            accessionModelRequest.setAccessionRequests(trimmedAccessionRequests);
            accessionEntity.setAccessionRequest(convertJsonToString(accessionModelRequest));
            accessionEntity.setCreatedDate(new Date());
            accessionEntity.setAccessionStatus(ScsbConstants.PENDING);
            accessionDetailsRepository.save(accessionEntity);
            status = ScsbConstants.ACCESSION_SAVE_SUCCESS_STATUS;
        } catch (Exception ex) {
            log.error(ScsbCommonConstants.LOG_ERROR, ex);
            status = ScsbConstants.ACCESSION_SAVE_FAILURE_STATUS + ScsbCommonConstants.EXCEPTION_MSG + " : " + ex.getMessage();
        }
        return status;
    }

    /**
     * This method is used to find the list of accession entity based on the accession status.
     *
     * @param accessionStatus
     * @return
     */
    public List<AccessionEntity> getAccessionEntities(String accessionStatus) {
        return accessionDetailsRepository.findByAccessionStatus(accessionStatus);
    }

    /**
     * This method is used to get the accession request for the given accession list.
     *
     * @param accessionModelRequestList
     * @return
     */
    public List<AccessionRequest> getAccessionRequest( List<AccessionModelRequest> accessionModelRequestList) {
        List<AccessionRequest> accessionRequestList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(accessionModelRequestList)) {
            try {
                accessionModelRequestList.forEach(accessionModelRequest -> accessionRequestList.addAll(accessionModelRequest.getAccessionRequests()));
            } catch(Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR, e);
            }
        }
        return accessionRequestList;
    }


    public void updateStatusForAccessionEntities(List<AccessionEntity> accessionEntities, String status) {
        accessionEntities.forEach(accessionEntity -> accessionEntity.setAccessionStatus(status));
        accessionDetailsRepository.saveAll(accessionEntities);
    }

    @Override
    public List<AccessionResponse> doAccession(AccessionModelRequest accessionModelRequest, AccessionSummary accessionSummary, Exchange exhange) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        List<AccessionRequest> trimmedAccessionRequests = getTrimmedAccessionRequests(accessionModelRequest.getAccessionRequests());
        trimmedAccessionRequests = accessionProcessService.removeDuplicateRecord(trimmedAccessionRequests);
        int requestedCount = accessionModelRequest.getAccessionRequests().size();
        int duplicateCount = requestedCount - trimmedAccessionRequests.size();
        if(duplicateCount>0){
            saveReportForDuplicateBarcodes(accessionSummary, accessionModelRequest.getAccessionRequests(), accessionModelRequest.getImsLocationCode());
        }
        accessionSummary.addDuplicateRecords(duplicateCount);

        ExecutorService executorService = Executors.newFixedThreadPool(batchAccessionThreadSize);
        List<List<AccessionRequest>> partitions = Lists.partition(trimmedAccessionRequests, batchAccessionThreadSize);

        //validate ims_location
        AccessionValidationService.AccessionValidationResponse imsValidation = accessionValidationService.validateImsLocationCode(accessionModelRequest.getImsLocationCode());
        if(imsValidation.isValid()) {
            for (Iterator<List<AccessionRequest>> iterator = partitions.iterator(); iterator.hasNext(); ) {
                List<AccessionRequest> accessionRequests = iterator.next();
                List<Future> futures = new ArrayList<>();
                List<AccessionRequest> failedRequests = new ArrayList<>();
                for (Iterator<AccessionRequest> accessionRequestIterator = accessionRequests.iterator(); accessionRequestIterator.hasNext(); ) {
                    AccessionRequest accessionRequest = accessionRequestIterator.next();
                    log.info("Processing accession for item barcode----->{}", accessionRequest.getItemBarcode());
                    // validate empty barcode ,customer code and owning institution
                    String itemBarcode = accessionRequest.getItemBarcode();
                    String customerCode = accessionRequest.getCustomerCode();
                    AccessionValidationService.AccessionValidationResponse accessionValidationResponse = accessionValidationService.validateBarcodeOrCustomerCode(itemBarcode, customerCode, accessionModelRequest.getImsLocationCode());

                    String owningInstitution = accessionValidationResponse.getOwningInstitution();

                    if (!accessionValidationResponse.isValid()) {
                        String message = accessionValidationResponse.getMessage();
                        List<ReportDataEntity> reportDataEntityList = new ArrayList<>(accessionUtil.createReportDataEntityList(accessionRequest, message));
                        accessionUtil.saveReportEntity(owningInstitution, reportDataEntityList);
                        addCountToSummary(accessionSummary, message);
                        continue;
                    }

                    BibDataCallable bibDataCallable = applicationContext.getBean(BibDataCallable.class);
                    bibDataCallable.setAccessionRequest(accessionRequest);
                    bibDataCallable.setOwningInstitution(owningInstitution);
                    bibDataCallable.setImsLocationEntity(imsValidation.getImsLocationEntity());
                    futures.add(executorService.submit(bibDataCallable));

                }
                for (Iterator<Future> futureIterator = futures.iterator(); futureIterator.hasNext(); ) {
                    Future bibDataFuture = futureIterator.next();
                    try {
                        Object object = bibDataFuture.get();
                        if (object instanceof Set) {
                            prepareSummary(accessionSummary, object);
                        } else if (object instanceof AccessionRequest) {
                            failedRequests.add((AccessionRequest) object);
                        }

                    } catch (Exception e) {
                        log.error(ScsbCommonConstants.LOG_ERROR, e);
                        exhange.setException(e);
                    }
                }

                // Processed failed barcodes one by one
                for (Iterator<AccessionRequest> accessionRequestIterator = failedRequests.iterator(); accessionRequestIterator.hasNext(); ) {
                    AccessionRequest accessionRequest = accessionRequestIterator.next();
                    BibDataCallable bibDataCallable = applicationContext.getBean(BibDataCallable.class);
                    bibDataCallable.setAccessionRequest(accessionRequest);
                    bibDataCallable.setWriteToReport(true);
                    bibDataCallable.setImsLocationEntity(imsValidation.getImsLocationEntity());
                    String owningInstitution = accessionUtil.getOwningInstitution(accessionRequest.getCustomerCode(), accessionModelRequest.getImsLocationCode());
                    bibDataCallable.setOwningInstitution(owningInstitution);
                    Future submit = executorService.submit(bibDataCallable);
                    try {
                        Object o = submit.get();
                        prepareSummary(accessionSummary, o);
                    } catch (Exception e) {
                        log.error(ScsbCommonConstants.LOG_ERROR, e);
                        exhange.setException(e);
                        accessionSummary.addException(1);
                    }
                }
            }
        }
        else {
            partitions.forEach(partitionList->{
                partitionList.forEach(accessionRequest -> {
                    String message = imsValidation.getMessage();
                    List<ReportDataEntity> reportDataEntityList = new ArrayList<>(accessionUtil.createReportDataEntityList(accessionRequest, message));
                    String owningInstitution = imsValidation.getOwningInstitution();
                    accessionUtil.saveReportEntity(owningInstitution, reportDataEntityList);
                    addCountToSummary(accessionSummary, message);
                });
            });
        }
        executorService.shutdown();
        stopWatch.stop();
        log.info("Total time taken to accession for all barcode -> {} sec",stopWatch.getTotalTimeSeconds());
        return null;
    }


    /**
     * This method converts the json object to string.
     * @param objJson
     * @return
     */
    private String convertJsonToString(Object objJson) {
        String strJson = "";
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            strJson = objectMapper.writeValueAsString(objJson);
        } catch (JsonProcessingException ex) {
            log.error(ScsbCommonConstants.LOG_ERROR, ex);
        }
        return strJson;
    }

    private void saveReportForDuplicateBarcodes(AccessionSummary accessionSummary, List<AccessionRequest> accessionRequestList, String imsLocationCode) {
        accessionRequestList.stream()
                .collect(Collectors.groupingBy(AccessionRequest::getItemBarcode))
                .entrySet()
                .stream()
                .filter(e -> e.getValue().size() > 1)
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toSet())
                .forEach(accessionRequest -> {
                    String owningInstitution = accessionUtil.getOwningInstitution(accessionRequest.getCustomerCode(),imsLocationCode);
                    List<ReportDataEntity> reportDataEntityList = accessionUtil.createReportDataEntityList(accessionRequest, ScsbConstants.DUPLICATE_BARCODE_ENTRY);
                    accessionUtil.saveReportEntity(owningInstitution, reportDataEntityList);
                });
    }



}
