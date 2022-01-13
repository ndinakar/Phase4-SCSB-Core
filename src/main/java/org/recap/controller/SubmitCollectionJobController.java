package org.recap.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.camel.submitcollection.SubmitCollectionPollingS3RouteBuilder;
import org.recap.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Created by harikrishnanv on 20/6/17.
 */
@Slf4j
@RestController
@RequestMapping("/submitCollectionJob")
public class SubmitCollectionJobController {


    @Autowired
    private CamelContext camelContext;

    @Autowired
    SubmitCollectionPollingS3RouteBuilder submitCollectionPollingFtpRouteBuilder;

    @Autowired
    CommonUtil commonUtil;

    /**
     * This method is initiated from the scheduler to start the submit collection process in sequence
     * if the file exists in the respective folders.
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/startSubmitCollection")
    public String startSubmitCollection() throws Exception{
        List<String> allInstitutionCodesExceptSupportInstitution = commonUtil.findAllInstitutionCodesExceptSupportInstitution();
        Optional<String> institution = allInstitutionCodesExceptSupportInstitution.stream().findFirst();
        submitCollectionPollingFtpRouteBuilder.createRoutesForSubmitCollection();
        camelContext.getRouteController().startRoute((institution.isPresent() ? institution.get() : "") + ScsbConstants.CGD_PROTECTED_ROUTE_ID);
        Endpoint endpoint = camelContext.getEndpoint(ScsbConstants.SUBMIT_COLLECTION_COMPLETION_QUEUE_TO);
        PollingConsumer consumer = null;
        try {
            consumer = endpoint.createPollingConsumer();
            Exchange exchange = consumer.receive();
            log.info("Message Received : {}", exchange.getIn().getBody());
            Thread.sleep(500);
            submitCollectionPollingFtpRouteBuilder.removeRoutesForSubmitCollection();
        }
        catch (Exception e){
            log.error(ScsbCommonConstants.LOG_ERROR, e);
        }
        finally {
            if(consumer != null) {
                consumer.close();
            }
        }
        log.info("Submit Collection Job ends");
        return ScsbCommonConstants.SUCCESS;
    }
}
