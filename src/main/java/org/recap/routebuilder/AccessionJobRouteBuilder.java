package org.recap.routebuilder;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.service.accession.AccessionJobProcessor;
import org.recap.controller.SharedCollectionRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Created by rajeshbabuk on 21/8/17.
 */
@Slf4j
@Component
public class AccessionJobRouteBuilder {



    /**
     * Instantiates a new Accession job route builder.
     *
     * @param camelContext                   the camel context
     * @param sharedCollectionRestController the shared collection rest controller
     */
    @Autowired
    public AccessionJobRouteBuilder(CamelContext camelContext, ApplicationContext applicationContext, SharedCollectionRestController sharedCollectionRestController) {
        try {
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    onException(Exception.class)
                            .log("Exception caught during ongoing Accession Job")
                            .handled(true)
                            .to(ScsbConstants.ACCESSION_DIRECT_ROUTE_FOR_EXCEPTION);
                    from(ScsbCommonConstants.ACCESSION_JOB_INITIATE_QUEUE)
                            .routeId(ScsbConstants.ACCESSION_JOB_INITIATE_ROUTE_ID)
                            .process(new Processor() {
                                @Override
                                public void process(Exchange exchange) {
                                    String jobId = null;
                                    try {
                                        jobId = (String) exchange.getIn().getBody();
                                        log.info("Accession Job initiated for Job Id : {}", jobId);
                                        String accessionJobStatus = sharedCollectionRestController.ongoingAccessionJob(exchange);

                                        log.info("Job Id : {} Accession Job Status : {}", jobId, accessionJobStatus);
                                        exchange.getIn().setBody("JobId:" + jobId + "|" + accessionJobStatus);
                                    } catch (Exception ex) {
                                        exchange.getIn().setBody("JobId:" + jobId + "|" + ex.getMessage());
                                        log.info(ScsbCommonConstants.LOG_ERROR, ex);
                                    }
                                }
                            })
                            .onCompletion()
                            .to(ScsbCommonConstants.ACCESSION_JOB_COMPLETION_OUTGOING_QUEUE)
                            .end();
                }
            });

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ScsbConstants.ACCESSION_DIRECT_ROUTE_FOR_EXCEPTION)
                            .log("Calling direct route for exception")
                            .bean(applicationContext.getBean(AccessionJobProcessor.class), ScsbConstants.ACCESSION_CAUGHT_EXCEPTION_METHOD)
                            .onCompletion()
                            .to(ScsbCommonConstants.ACCESSION_JOB_COMPLETION_OUTGOING_QUEUE);
                }
            });
        } catch (Exception ex) {
            log.error(ScsbConstants.EXCEPTION, ex);
        }
    }
}
