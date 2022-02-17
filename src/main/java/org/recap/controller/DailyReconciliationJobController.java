package org.recap.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.camel.dailyreconciliation.DailyReconciliationRouteBuilder;
import org.recap.repository.jpa.ImsLocationDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * Created by akulak on 9/5/17.
 */

@Slf4j
@RestController
@RequestMapping("/dailyReconciliation")
public class DailyReconciliationJobController {

    @Autowired
    CamelContext camelContext;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    ImsLocationDetailsRepository imsLocationDetailsRepository;

    @Value("${" + PropertyKeyConstants.S3_DAILY_RECONCILIATION + "}")
    String dailyReconciliationS3;

    @Value("${" + PropertyKeyConstants.S3_DAILY_RECONCILIATION_PROCESSED + "}")
    String dailyReconciliationFtpProcessed;

    @Value("${" + PropertyKeyConstants.DAILY_RECONCILIATION_FILE + "}")
    String dailyReconciliationFilePath;

    /**
     * This Rest service initiates, transaction comparisons between each LAS and SCSB on day-to-day basis.
     *
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/startDailyReconciliation")
    public String statCamel() throws Exception {
        log.info("Daily Reconciliation Job Starting.....");
        List<String> imsLocationCodes = imsLocationDetailsRepository.findAllImsLocationCodeExceptUN();
        for (String imsLocation : imsLocationCodes) {
            camelContext.addRoutes(new DailyReconciliationRouteBuilder(camelContext, applicationContext, imsLocation, dailyReconciliationS3, dailyReconciliationFtpProcessed, dailyReconciliationFilePath));
        }
        for (String imsLocation : imsLocationCodes) {
            camelContext.getRouteController().startRoute(ScsbConstants.DAILY_RR_S3_ROUTE_ID + imsLocation);
        }
        log.info("After Starting Daily Reconciliation Routes# : {}", camelContext.getRoutes().size());
        return ScsbCommonConstants.SUCCESS;
    }
}
