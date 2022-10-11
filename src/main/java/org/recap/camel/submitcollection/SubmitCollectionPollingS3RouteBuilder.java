package org.recap.camel.submitcollection;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.camel.submitcollection.processor.SubmitCollectionProcessor;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.util.CommonUtil;
import org.recap.util.PropertyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


/**
 * Created by premkb on 19/3/17.
 */

@Slf4j
@Component
public class SubmitCollectionPollingS3RouteBuilder {


    @Autowired
    ProducerTemplate producer;

    @Autowired
    CamelContext camelContext;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    PropertyUtil propertyUtil;

    @Autowired
    InstitutionDetailsRepository institutionDetailsRepository;

    @Autowired
    CommonUtil commonUtil;

    @Value("${" + PropertyKeyConstants.S3_SUBMIT_COLLECTION_DIR + "}")
    private String submitCollectionS3BasePath;

    @Autowired
    AmazonS3 awsS3Client;

    @Value("${" + PropertyKeyConstants.SCSB_BUCKET_NAME + "}")
    private String scsbBucketName;

    @Value("${" + PropertyKeyConstants.SUBMIT_COLLECTION_LOCAL_DIR + "}")
    private String submitCollectionLocalWorkingDir;

    /**
     * Predicate to identify is the input file is gz
     */
    Predicate gzipFile = exchange -> {
        if (exchange.getIn().getHeader(ScsbConstants.CAMEL_FILE_NAME_ONLY) != null) {
            String fileName = exchange.getIn().getHeader(ScsbConstants.CAMEL_FILE_NAME_ONLY).toString();
            return StringUtils.equalsIgnoreCase("gz", FilenameUtils.getExtension(fileName));
        } else {
            return false;
        }
    };

    public SubmitCollectionPollingS3RouteBuilder(CamelContext camelContext, ApplicationContext applicationContext) {
        try {
            //This route is used to send message to queue which is used in controller to identify the completion of submit collection process
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ScsbConstants.SUBMIT_COLLECTION_COMPLETION_QUEUE_FROM)
                            .routeId(ScsbConstants.SUBMIT_COLLECTION_COMPLETED_ROUTE)
                            .log("Completed Submit Collection Process")
                            .process(exchange -> exchange.getIn().setBody("Submit collection process completed sucessfully in sequential order"))
                            .to(ScsbConstants.SUBMIT_COLLECTION_COMPLETION_QUEUE_TO)
                            .end();
                }
            });

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ScsbCommonConstants.DIRECT_ROUTE_FOR_EXCEPTION)
                            .log("Calling direct route for exception")
                            .bean(applicationContext.getBean(SubmitCollectionProcessor.class), ScsbConstants.SUBMIT_COLLECTION_CAUGHT_EXCEPTION_METHOD);

                }
            });
        } catch (Exception e) {
            log.info("Exception occurred while instantiating submit collection route definitions : {}", e.getMessage());
        }
    }

    public void createRoutesForSubmitCollection() {
        List<String> protectedAndNotProtected = Arrays.asList(ScsbConstants.PROTECTED, ScsbConstants.NOT_PROTECTED);
        String nextInstitution = null;
        List<String> allInstitutionCodesExceptSupportInstitution = commonUtil.findAllInstitutionCodesExceptSupportInstitution();
        for (int i = 0; i < allInstitutionCodesExceptSupportInstitution.size(); i++) {
            String currentInstitution = allInstitutionCodesExceptSupportInstitution.get(i);
            nextInstitution = (i < allInstitutionCodesExceptSupportInstitution.size() - 1) ? allInstitutionCodesExceptSupportInstitution.get(i + 1) : null;
            for (String cdgType : protectedAndNotProtected) {
                String nextRouteId = getNextRouteId(currentInstitution, nextInstitution, cdgType);
                if (ScsbConstants.PROTECTED.equalsIgnoreCase(cdgType))
                    addRoutesToCamelContext(currentInstitution, cdgType, currentInstitution + ScsbConstants.CGD_PROTECTED_ROUTE_ID, nextRouteId, true);
                else {
                    addRoutesToCamelContext(currentInstitution, cdgType, currentInstitution + ScsbConstants.CGD_NOT_PROTECTED_ROUTE_ID, nextRouteId, false);
                }
            }
        }
    }

    private String getNextRouteId(String currentInstitution, String nextInstitution, String cdgType) {
        if (ScsbConstants.PROTECTED.equalsIgnoreCase(cdgType)) {
            return currentInstitution + ScsbConstants.CGD_NOT_PROTECTED_ROUTE_ID;
        } else if (StringUtils.isNotBlank(nextInstitution)) {
            return nextInstitution + ScsbConstants.CGD_PROTECTED_ROUTE_ID;
        } else {
            return ScsbConstants.SUBMIT_COLLECTION_COMPLETED_ROUTE;
        }
    }

    public void addRoutesToCamelContext(String currentInstitution, String cgdType, String currentInstitutionRouteId, String nextInstitutionRouteId, Boolean isCGDProtected) {
        try {
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
            listObjectsRequest.setBucketName(scsbBucketName);
            listObjectsRequest.setPrefix(submitCollectionS3BasePath + currentInstitution + "/cgd_" + cgdType + "/" + "scsb");
            ObjectListing objectListing = awsS3Client.listObjects(listObjectsRequest);

            for (S3ObjectSummary os : objectListing.getObjectSummaries()) {
                getObjectContentToDrive(os.getKey(), currentInstitution, cgdType);
                log.info("File with the key --> {} Size --> {} Last Modified -->{} " , os.getKey() , os.getSize() , os.getLastModified());
            }

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    onCompletion()
                            .choice()
                            .when(exchangeProperty(ScsbCommonConstants.CAMEL_BATCH_COMPLETE))
                            .process(exchange -> clearDirectory(currentInstitution, cgdType))
                            .log("OnCompletion executing for :" + currentInstitution + cgdType)
                            .to("controlbus:route?routeId=" + currentInstitutionRouteId + "&action=stop&async=true")
                            .delay(10)
                            .process(exchange -> startNextRouteInNewThread(exchange, nextInstitutionRouteId));
                    onException(Exception.class)
                            .log("Exception caught during submit collection process - " + currentInstitution + cgdType)
                            .handled(true)
                            .setHeader(ScsbCommonConstants.INSTITUTION, constant(currentInstitution))
                            .setHeader(ScsbCommonConstants.IS_CGD_PROTECTED, constant(isCGDProtected))
                            .setHeader(ScsbConstants.CGG_TYPE, constant(cgdType))
                            .to(ScsbCommonConstants.DIRECT_ROUTE_FOR_EXCEPTION);
                    from("file://"+ submitCollectionLocalWorkingDir + currentInstitution + ScsbCommonConstants.PATH_SEPARATOR + "cgd_" + cgdType + "?sendEmptyMessageWhenIdle=true&delete=true")
                            .routeId(currentInstitutionRouteId)
                            .noAutoStartup()
                            .choice()
                            .when(gzipFile)
                            .unmarshal()
                            .gzipDeflater()
                            .log(currentInstitution + "Submit Collection S3 Route Unzip Complete")
                            .bean(applicationContext.getBean(SubmitCollectionProcessor.class, currentInstitution, isCGDProtected, cgdType), ScsbConstants.PROCESS_INPUT)
                            .when(body().isNull())//This condition is satisfied when there are no files in the S3 directory(parameter-sendEmptyMessageWhenIdle=true)
                            .bean(applicationContext.getBean(SubmitCollectionProcessor.class, currentInstitution, isCGDProtected, cgdType), ScsbConstants.SEND_EMAIL_FOR_EMPTY_DIRECTORY)
                            .log(currentInstitution + "-" + cgdType + " Directory is empty")
                            .otherwise()
                            .log("submit collection for " + currentInstitution + "-" + cgdType + " started")
                            .bean(applicationContext.getBean(SubmitCollectionProcessor.class, currentInstitution, isCGDProtected, cgdType), ScsbConstants.PROCESS_INPUT)
                            .log(currentInstitution + " Submit Collection " + cgdType + " S3 Route Record Processing completed")
                            .end();
                }
            });

        } catch (Exception e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e.getMessage());
        }
    }

    private void getObjectContentToDrive(String fileName, String currentInstitution, String cgdType) {
        String finalFileName = null;
        try {
            S3Object s3Object = awsS3Client.getObject(scsbBucketName, fileName);
            S3ObjectInputStream inputStream = s3Object.getObjectContent();
            finalFileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            if (inputStream != null) {
                IOUtils.copy(inputStream, new FileOutputStream(new File(submitCollectionLocalWorkingDir + currentInstitution + ScsbCommonConstants.PATH_SEPARATOR + "cgd_" + cgdType + ScsbCommonConstants.PATH_SEPARATOR + finalFileName)));
            }
        } catch (Exception e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e.getMessage());
        }
    }

    public void startNextRouteInNewThread(Exchange exchange, String nextInstitutionRouteId) {
        Thread startThread;
        startThread = new Thread(() -> {
            try {
                if (nextInstitutionRouteId.contains("Complete")) {
                    producer.sendBody(ScsbConstants.SUBMIT_COLLECTION_COMPLETION_QUEUE_FROM, exchange);
                } else {
                    camelContext.getRouteController().startRoute(nextInstitutionRouteId);
                }
            } catch (Exception e) {
                log.info("Exception occured while starting next route : {}", e.getMessage());
                exchange.setException(e);
            }
        });
        startThread.start();
    }

    public void removeRoutesForSubmitCollection() throws Exception {
        log.info(" Total routes before removing : {}", camelContext.getRoutesSize());
        List<String> protectedAndNotProtected = Arrays.asList(ScsbConstants.PROTECTED, ScsbConstants.NOT_PROTECTED);
        List<String> allInstitutionCodesExceptSupportInstitution = commonUtil.findAllInstitutionCodesExceptSupportInstitution();
        for (String institution : allInstitutionCodesExceptSupportInstitution) {
            for (String cdgType : protectedAndNotProtected) {
                if (ScsbConstants.PROTECTED.equalsIgnoreCase(cdgType)) {
                    camelContext.getRouteController().stopRoute(institution + ScsbConstants.CGD_PROTECTED_ROUTE_ID);
                    camelContext.removeRoute(institution + ScsbConstants.CGD_PROTECTED_ROUTE_ID);
                } else {
                    camelContext.getRouteController().stopRoute(institution + ScsbConstants.CGD_NOT_PROTECTED_ROUTE_ID);
                    camelContext.removeRoute(institution + ScsbConstants.CGD_NOT_PROTECTED_ROUTE_ID);
                }
            }
        }
        log.info(" Total routes after removing : {}", camelContext.getRoutesSize());
    }

    public void clearDirectory(String institutionCode, String cgdType) {
        File destDirFile = new File(submitCollectionLocalWorkingDir + institutionCode + "/cgd_"+ cgdType);
        try {
            FileUtils.cleanDirectory(destDirFile);
        } catch (IOException e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e.getMessage());
        }
    }
}
