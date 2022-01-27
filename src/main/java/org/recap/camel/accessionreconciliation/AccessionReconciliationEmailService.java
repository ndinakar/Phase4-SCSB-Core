package org.recap.camel.accessionreconciliation;

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
 * Created by akulak on 22/5/17.
 */

@Slf4j
@Service
@Scope("prototype")
public class AccessionReconciliationEmailService {


    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    PropertyUtil propertyUtil;

    private final String institutionCode;
    private final String imsLocationCode;

    /**
     * Instantiates a new Accession reconciliation email service.
     *
     * @param institutionCode the institution code
     */
    public AccessionReconciliationEmailService(String institutionCode, String imsLocationCode) {
        this.institutionCode = institutionCode;
        this.imsLocationCode = imsLocationCode;
    }

    /**
     * Process input for accession reconciliation email service.
     *
     * @param exchange the exchange
     */
    public void processInput(Exchange exchange) {
        log.info("accession email started for {} - {}", imsLocationCode, institutionCode);
        producerTemplate.sendBodyAndHeader(ScsbConstants.EMAIL_Q, getEmailPayLoad(exchange), ScsbConstants.EMAIL_BODY_FOR, ScsbConstants.REQUEST_ACCESSION_RECONCILIATION_MAIL_QUEUE);
    }

    /**
     * Get email pay load for accession reconciliation email service.
     *
     * @return the email pay load
     */
    public EmailPayLoad getEmailPayLoad(Exchange exchange) {
        EmailPayLoad emailPayLoad = new EmailPayLoad();
        String fileNameWithPath = (String) exchange.getIn().getHeader(ScsbConstants.CAMEL_AWS_KEY);
        emailPayLoad.setTo(propertyUtil.getPropertyByInstitutionAndKey(institutionCode, PropertyKeyConstants.ILS.ILS_EMAIL_ACCESSION_RECONCILIATION_TO));
        emailPayLoad.setCc(propertyUtil.getPropertyByInstitutionAndKey(institutionCode, PropertyKeyConstants.ILS.ILS_EMAIL_ACCESSION_RECONCILIATION_CC));
        log.info("Accession Reconciliation email sent to : {} and cc : {} ", emailPayLoad.getTo(), emailPayLoad.getCc());
        emailPayLoad.setMessageDisplay("Barcode Reconciliation has been completed for " + institutionCode.toUpperCase() + " at " + imsLocationCode + ". The report is at the S3 location " + fileNameWithPath);
        return emailPayLoad;
    }

}
