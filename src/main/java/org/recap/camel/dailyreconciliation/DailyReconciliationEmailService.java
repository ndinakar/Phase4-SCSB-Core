package org.recap.camel.dailyreconciliation;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbConstants;
import org.recap.camel.EmailPayLoad;
import org.recap.util.PropertyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Created by akulak on 12/7/17.
 */

@Slf4j
@Service
@Scope("prototype")
public class DailyReconciliationEmailService {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private PropertyUtil propertyUtil;

    private String fileLocation;

    private final String imsLocationCode;

    public DailyReconciliationEmailService(String imsLocationCode) {
        this.imsLocationCode = imsLocationCode;
    }

    public void process(Exchange exchange) throws Exception {
        fileLocation = (String) exchange.getIn().getHeader(ScsbConstants.CAMEL_AWS_KEY);
        log.info("Daily Reconciliation file created in the path {}", fileLocation);
        log.info("Daily Reconciliation email started for {}", imsLocationCode);
        producerTemplate.sendBodyAndHeader(ScsbConstants.EMAIL_Q, getEmailPayLoad(), ScsbConstants.EMAIL_BODY_FOR, ScsbConstants.DAILY_RECONCILIATION);
    }

    private EmailPayLoad getEmailPayLoad() {
        EmailPayLoad emailPayLoad = new EmailPayLoad();
        emailPayLoad.setTo(propertyUtil.getPropertyByImsLocationAndKey(imsLocationCode, PropertyKeyConstants.IMS.IMS_EMAIL_DAILY_RECONCILIATION_TO));
        log.info("Daily Reconciliation email sent to {}", emailPayLoad.getTo());
        emailPayLoad.setMessageDisplay("Daily reconciliation report is available at the S3 location " + fileLocation);
        return emailPayLoad;
    }
}
