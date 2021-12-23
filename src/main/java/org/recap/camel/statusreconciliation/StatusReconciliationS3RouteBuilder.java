package org.recap.camel.statusreconciliation;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.s3.S3Constants;
import org.apache.camel.model.dataformat.BindyType;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbConstants;
import org.recap.ScsbCommonConstants;
import org.recap.model.csv.StatusReconciliationCSVRecord;
import org.recap.model.csv.StatusReconciliationErrorCSVRecord;
import org.recap.service.statusreconciliation.StatusReconciliationEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Created by hemalathas on 22/5/17.
 */
@Component
public class StatusReconciliationS3RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(StatusReconciliationS3RouteBuilder.class);

    /**
     * Instantiates a new Status reconciliation s3 route builder.
     *
     * @param camelContext         the camel context
     * @param applicationContext   the application context
     * @param statusReconciliation the status reconciliation
     */
    @Autowired
    public StatusReconciliationS3RouteBuilder(CamelContext camelContext, ApplicationContext applicationContext, @Value("${" + PropertyKeyConstants.S3_ADD_S3_ROUTES_ON_STARTUP + "}") boolean addS3RoutesOnStartup, @Value("${" + PropertyKeyConstants.STATUS_RECONCILIATION + "}") String statusReconciliation) {
        try {
            if (addS3RoutesOnStartup) {
                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        from(ScsbConstants.STATUS_RECONCILIATION_REPORT)
                                .routeId(ScsbConstants.STATUS_RECONCILIATION_REPORT_ID)
                                .onCompletion()
                                    .log("status reconciliation process finished files generated in S3")
                                    .bean(applicationContext.getBean(StatusReconciliationEmailService.class), ScsbConstants.PROCESS_INPUT)
                                .end()
                                .marshal().bindy(BindyType.Csv, StatusReconciliationCSVRecord.class)
                                .setHeader(S3Constants.KEY, simple(statusReconciliation + "${in.headers.ImsLocation}/StatusReconciliation_${in.headers.ImsLocation}_${date:now:yyyyMMdd_HHmmss}.csv"))
                                .to(ScsbConstants.SCSB_CAMEL_S3_TO_ENDPOINT)
                                .log("status reconciliation report generated in s3");
                    }
                });

                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        from(ScsbConstants.STATUS_RECONCILIATION_FAILURE_REPORT)
                                .routeId(ScsbConstants.STATUS_RECONCILIATION_FAILURE_REPORT_ID)
                                .onCompletion()
                                    .log("status reconciliation process finished failure files generated in S3")
                                    .bean(applicationContext.getBean(StatusReconciliationEmailService.class), ScsbConstants.PROCESS_INPUT_FAILURE)
                                .end()
                                .marshal().bindy(BindyType.Csv, StatusReconciliationErrorCSVRecord.class)
                                .setHeader(S3Constants.KEY, simple(statusReconciliation + "${in.headers.ImsLocation}/StatusReconciliationFailure_${in.headers.ImsLocation}_${date:now:yyyyMMdd_HHmmss}.csv"))
                                .to(ScsbConstants.SCSB_CAMEL_S3_TO_ENDPOINT)
                                .log("status reconciliation failure report generated in s3");
                    }
                });
            }
        } catch (Exception e) {
            logger.error(ScsbCommonConstants.LOG_ERROR, e);
        }
    }
}
