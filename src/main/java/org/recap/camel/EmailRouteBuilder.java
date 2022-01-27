package org.recap.camel;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.io.FileUtils;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Created by chenchulakshmig on 13/9/16.
 */

@Slf4j
@Component
public class EmailRouteBuilder {



    private String emailBodyDeletedRecords;
    private String emailPassword;
    private String emailBodyForSubmitCollection;
    private String emailBodyForSubmitCollectionEmptyDirectory;
    private String emailBodyForExceptionInSubmitColletion;
    private  String subjectHeader = "subject";
    private  String smtps = "smtps://";
    private  String emailPayLoadSubject = "${header.emailPayLoad.subject}";
    private  String emailPayLoadTo = "${header.emailPayLoad.to}";
    private String password = "&password=";
    private String userName = "?username=";
    private String emailPayLoadcc = "${header.emailPayLoad.cc}";
    private String emailPayLoadMessage = "${header.emailPayLoad.messageDisplay}";

    /**
     * Instantiates a new Email route builder.
     *
     * @param context           the context
     * @param username          the username
     * @param passwordDirectory the password directory
     * @param from              the from
     * @param subject           the subject
     * @param smtpServer        the smtp server
     */
    @Autowired
    public EmailRouteBuilder(CamelContext context, CommonUtil commonUtil, @Value("${" + PropertyKeyConstants.EMAIL_SMTP_SERVER_USERNAME + "}") String username, @Value("${" + PropertyKeyConstants.EMAIL_SMTP_SERVER_PASSWORD_FILE + "}") String passwordDirectory,
                             @Value("${" + PropertyKeyConstants.EMAIL_SMTP_SERVER_ADDRESS_FROM + "}") String from, @Value("${" + PropertyKeyConstants.EMAIL_REQUEST_RECALL_SUBJECT + "}") String subject,
                             @Value("${" + PropertyKeyConstants.EMAIL_SMTP_SERVER + "}") String smtpServer) {
        try {
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    loadEmailPassword();
                    loadEmailBodyTemplateForNoData();
                    loadEmailBodyTemplateForSubmitCollectionEmptyDirectory();
                    loadEmailBodyTemplateForExceptionInSubmitCollection();
                    emailBodyDeletedRecords=loadEmailStatus(ScsbConstants.DELETED_RECORDS_EMAIL_TEMPLATE);

                    from(ScsbConstants.EMAIL_Q)
                            .routeId(ScsbConstants.EMAIL_ROUTE_ID)
                            .setHeader("emailPayLoad").body(EmailPayLoad.class)
                            .onCompletion().log("Email has been sent successfully.")
                            .end()
                            .choice()
                                .when(header(ScsbConstants.EMAIL_BODY_FOR).isEqualTo(ScsbConstants.SUBMIT_COLLECTION))
                                    .setHeader(subjectHeader, simple(emailPayLoadSubject))
                                    .setBody(simple(emailBodyForSubmitCollection))
                                    .setHeader("from", simple(from))
                                    .setHeader("to", simple(emailPayLoadTo))
                                    .setHeader("cc", simple(emailPayLoadcc))
                                    .log("email sent for submit collection")
                                    .to(smtps + smtpServer + userName + username + password + emailPassword)
                                .when(header(ScsbConstants.EMAIL_BODY_FOR).isEqualTo(ScsbConstants.SUBMIT_COLLECTION_FOR_NO_FILES))
                                    .setHeader(subjectHeader, simple(emailPayLoadSubject))
                                    .setBody(simple(emailBodyForSubmitCollectionEmptyDirectory))
                                    .setHeader("from", simple(from))
                                    .setHeader("to", simple(emailPayLoadTo))
                                    .log("email sent for submit collection for no files")
                                    .to(smtps + smtpServer + userName + username + password + emailPassword)
                                .when(header(ScsbConstants.EMAIL_BODY_FOR).isEqualTo(ScsbConstants.REQUEST_ACCESSION_RECONCILIATION_MAIL_QUEUE))
                                    .log("email for accession Reconciliation")
                                    .setHeader(subjectHeader, simple("Barcode Reconciliation Report"))
                                    .setBody(simple(emailPayLoadMessage))
                                    .setHeader("from", simple(from))
                                    .setHeader("to", simple(emailPayLoadTo))
                                    .setHeader("cc", simple(emailPayLoadcc))
                                    .to(smtps + smtpServer + userName + username + password + emailPassword)
                                .when(header(ScsbConstants.EMAIL_BODY_FOR).isEqualTo(ScsbConstants.DELETED_MAIL_QUEUE))
                                    .setHeader(subjectHeader, simple(emailPayLoadSubject))
                                    .setBody(simple(emailBodyDeletedRecords))
                                    .setHeader("from", simple(from))
                                    .setHeader("to", simple(emailPayLoadTo))
                                    .log("Email Send for Deleted Records")
                                    .to(smtps + smtpServer + userName + username + password + emailPassword)
                                .when(header(ScsbConstants.EMAIL_BODY_FOR).isEqualTo(ScsbConstants.STATUS_RECONCILIATION))
                                    .log("email for status Reconciliation")
                                    .setHeader(subjectHeader, simple("\"Out\" Status Reconciliation Report"))
                                    .setBody(simple(emailPayLoadMessage))
                                    .setHeader("from", simple(from))
                                    .setHeader("to", simple(emailPayLoadTo))
                                    .setHeader("cc", simple(emailPayLoadcc))
                                    .to(smtps + smtpServer + userName + username + password + emailPassword)
                                .when(header(ScsbConstants.EMAIL_BODY_FOR).isEqualTo(ScsbConstants.STATUS_RECONCILIATION_FAILURE))
                                    .log("email for status Reconciliation failure report")
                                    .setHeader(subjectHeader, simple("\"Out\" Status Reconciliation Failure Report"))
                                    .setBody(simple(emailPayLoadMessage))
                                    .setHeader("from", simple(from))
                                    .setHeader("to", simple(emailPayLoadTo))
                                    .setHeader("cc", simple(emailPayLoadcc))
                                    .to(smtps + smtpServer + userName + username + password + emailPassword)
                               .when(header(ScsbConstants.EMAIL_BODY_FOR).isEqualTo(ScsbConstants.DAILY_RECONCILIATION))
                                    .log("email for Daily Reconciliation")
                                    .setHeader(subjectHeader, simple("Daily Reconciliation Report"))
                                    .setBody(simple(emailPayLoadMessage))
                                    .setHeader("from", simple(from))
                                    .setHeader("to", simple(emailPayLoadTo))
                                    .to(smtps + smtpServer + userName + username + password + emailPassword)
                                .when(header(ScsbConstants.EMAIL_BODY_FOR).isEqualTo(ScsbConstants.REQUEST_INITIAL_DATA_LOAD))
                                    .setHeader(subjectHeader, simple(emailPayLoadSubject))
                                    .setBody(simple(emailPayLoadMessage))
                                    .setHeader("from", simple(from))
                                    .setHeader("to", simple(emailPayLoadTo))
                                    .log("Email for request initial data load")
                                    .to(smtps + smtpServer + userName + username + password + emailPassword)
                                .when(header(ScsbConstants.EMAIL_BODY_FOR).isEqualTo(ScsbConstants.SUBMIT_COLLECTION_EXCEPTION))
                                    .setHeader(subjectHeader, simple(emailPayLoadSubject))
                                    .setBody(simple(emailBodyForExceptionInSubmitColletion))
                                    .setHeader("from", simple(from))
                                    .setHeader("to", simple(emailPayLoadTo))
                                    .setHeader("cc", simple(emailPayLoadcc))
                                    .log("Email sent for exception in submit collection")
                                    .to(smtps + smtpServer + userName + username + password + emailPassword);
                }

                private String loadEmailStatus(String emailTemplate) {
                    return getEmailBodyString(emailTemplate).toString();
                }

                private void loadEmailBodyTemplateForNoData() {
                    emailBodyForSubmitCollection = getEmailBodyString(ScsbConstants.SUBMIT_COLLECTION_EMAIL_BODY_VM).toString();
                }

                private void loadEmailBodyTemplateForSubmitCollectionEmptyDirectory() {
                    emailBodyForSubmitCollectionEmptyDirectory = getEmailBodyString(ScsbConstants.SUBMIT_COLLECTION_EMAIL_BODY_FOR_EMPTY_DIRECTORY_VM).toString();
                }

                private void loadEmailBodyTemplateForExceptionInSubmitCollection() {
                    emailBodyForExceptionInSubmitColletion = getEmailBodyString(ScsbConstants.SUBMIT_COLLECTION_EXCEPTION_BODY_VM).toString();
                }

                private void loadEmailPassword() {
                    File file = new File(passwordDirectory);
                    if (file.exists()) {
                        try {
                            emailPassword = FileUtils.readFileToString(file, StandardCharsets.UTF_8).trim();
                        } catch (IOException e) {
                            log.error(ScsbCommonConstants.LOG_ERROR,e);
                        }
                    }
                }

                private StringBuilder getEmailBodyString(String vmFileName) {
                    return commonUtil.getContentByFileName(vmFileName);
                }
            });
        } catch (Exception e) {
            log.error(ScsbCommonConstants.REQUEST_EXCEPTION,e);
        }
    }
}
