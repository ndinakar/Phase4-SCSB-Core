package org.recap.camel.accessionreconciliation;

import com.amazonaws.services.s3.AmazonS3;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbConstants;
import org.recap.ScsbCommonConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by akulak on 16/5/17.
 */

@Slf4j
@Service
@Scope("prototype")
public class AccessionReconciliationProcessor {


    @Autowired
    CamelContext camelContext;

    @Autowired
    RestTemplate restTemplate;

    @Value("${" + PropertyKeyConstants.SCSB_SOLR_DOC_URL + "}")
    private String solrSolrClientUrl;

    @Value("${" + PropertyKeyConstants.ACCESSION_RECONCILIATION_FILEPATH + "}")
    private String accessionFilePath;

    @Autowired
    AmazonS3 awsS3Client;

    private String institutionCode;
    private String imsLocationCode;

    int noOfLinesInFile=0;

    /**
     * Instantiates a new Accession reconcilation processor.
     *
     * @param institutionCode the institution code
     */
    public AccessionReconciliationProcessor(String institutionCode, String imsLocationCode) {
        this.institutionCode = institutionCode;
        this.imsLocationCode = imsLocationCode;
    }

    /**
     * Process input for accession reconcilation report.
     *
     * @param exchange the exchange
     */
    public void processInput(Exchange exchange) {
        HashMap<String,String> barcodesAndCustomerCodes=new HashMap<>();
        ArrayList<BarcodeReconcilitaionReport> barcodeReconcilitaionReportArrayList = exchange.getIn().getBody(ArrayList.class);
        for (BarcodeReconcilitaionReport barcodeReconcilitaionReport : barcodeReconcilitaionReportArrayList) {
           barcodesAndCustomerCodes.put(barcodeReconcilitaionReport.getBarcode(),barcodeReconcilitaionReport.getCustomerCode());
        }
        Integer index = (Integer) exchange.getProperty(ScsbConstants.CAMEL_SPLIT_INDEX);
        HttpEntity httpEntity = new HttpEntity(barcodesAndCustomerCodes);
        ResponseEntity<Map> responseEntity = restTemplate.exchange(solrSolrClientUrl+ ScsbConstants.ACCESSION_RECONCILATION_SOLR_CLIENT_URL, HttpMethod.POST, httpEntity,Map.class);
        Map<String,String> body = responseEntity.getBody();
        String barcodesAndCustomerCodesForReportFile = body.entrySet().stream().map(Object::toString).collect(Collectors.joining("\n")).replaceAll("=","\t");
        byte[] barcodesAndCustomerCodesForReportFileBytes =barcodesAndCustomerCodesForReportFile.getBytes(StandardCharsets.UTF_8);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ScsbConstants.BARCODE_RECONCILIATION_FILE_DATE_FORMAT);
        try {
            String line= ScsbConstants.NEW_LINE;
            byte[] newLine=line.getBytes(StandardCharsets.UTF_8);
            Path filePath = Paths.get(accessionFilePath+ScsbCommonConstants.PATH_SEPARATOR+imsLocationCode+ScsbCommonConstants.PATH_SEPARATOR+institutionCode+ScsbCommonConstants.PATH_SEPARATOR+ ScsbConstants.ACCESSION_RECONCILATION_FILE_NAME+imsLocationCode+institutionCode+simpleDateFormat.format(new Date())+".csv");
            if (!filePath.toFile().exists()) {
                Files.createDirectories(filePath.getParent());
                Files.createFile(filePath);
                log.info("Accession Reconciliation File Created {} ",filePath);
            }
            if(filePath.toFile().exists()){
                noOfLinesInFile= Files.readAllLines(filePath).size();
            }
            if(index == 0){
                ArrayList<String> headerSet = new ArrayList<>();
                headerSet.add(ScsbConstants.ACCESSION_RECONCILIATION_HEADER+ ScsbConstants.TAB+ ScsbConstants.CUSTOMER_CODE_HEADER);
                Files.write(filePath,headerSet, StandardOpenOption.APPEND);
            }
            else if (index > 0 && body.size()>0 && noOfLinesInFile>1){
                Files.write(filePath,newLine,StandardOpenOption.APPEND);
            }
            if(body.size()>0) {
                Files.write(filePath,barcodesAndCustomerCodesForReportFileBytes,StandardOpenOption.APPEND);
            }
        }
        catch (Exception e){
            log.error(ScsbCommonConstants.LOG_ERROR ,e);
        }
        startFileSystemRoutesForAccessionReconciliation(exchange,index);
        String xmlFileName = exchange.getIn().getHeader(ScsbConstants.CAMEL_AWS_KEY).toString();
        String bucketName = exchange.getIn().getHeader("CamelAwsS3BucketName").toString();
        if (awsS3Client.doesObjectExist(bucketName, xmlFileName)) {
            String basepath = xmlFileName.substring(0, xmlFileName.lastIndexOf('/'));
            String fileName = xmlFileName.substring(xmlFileName.lastIndexOf('/'));
            awsS3Client.copyObject(bucketName, xmlFileName, bucketName, basepath + "/.done-" + imsLocationCode + "-" + institutionCode + fileName);
            awsS3Client.deleteObject(bucketName, xmlFileName);
        }
    }

    private void startFileSystemRoutesForAccessionReconciliation(Exchange exchange,Integer index) {
        if ((boolean)exchange.getProperty(ScsbConstants.CAMEL_SPLIT_COMPLETE)){
            log.info("split last index-->{}",index);
            try {
                log.info("Starting {}{}{}",imsLocationCode, institutionCode, ScsbConstants.ACCESSION_RECONCILIATION_FS_ROUTE_ID);
                camelContext.getRouteController().startRoute(imsLocationCode+institutionCode+ ScsbConstants.ACCESSION_RECONCILIATION_FS_ROUTE_ID);
            } catch (Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR, e);
            }
        }
    }

}
