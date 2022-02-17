package org.recap.routebuilder;

import lombok.extern.slf4j.Slf4j;
import org.recap.ScsbCommonConstants;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.recap.ScsbConstants;
import org.recap.controller.SubmitCollectionJobController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by rajeshbabuk on 14/9/17.
 */
@Slf4j
@Component
public class SubmitCollectionJobRouteBuilder {

    /**
     * Instantiates a new Submit collection job route builder.
     *
     * @param camelContext                  the camel context
     * @param submitCollectionJobController the submit collection job controller
     */
    @Autowired
    public SubmitCollectionJobRouteBuilder(CamelContext camelContext, SubmitCollectionJobController submitCollectionJobController) {
        try {
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ScsbCommonConstants.SUBMIT_COLLECTION_JOB_INITIATE_QUEUE)
                            .routeId(ScsbConstants.SUBMIT_COLLECTION_JOB_INITIATE_ROUTE_ID)
                            .process(new Processor() {
                                @Override
                                public void process(Exchange exchange) throws Exception {
                                    String jobId = (String) exchange.getIn().getBody();
                                    log.info("Submit Collection Job Initiated for Job Id : {}", jobId);
                                    String submitCollectionJobStatus = submitCollectionJobController.startSubmitCollection();
                                    log.info("Job Id : {} Submit Collection Job Status : {}", jobId, submitCollectionJobStatus);
                                    exchange.getIn().setBody("JobId:" + jobId + "|" + submitCollectionJobStatus);
                                }
                            })
                            .onCompletion()
                            .to(ScsbCommonConstants.SUBMIT_COLLECTION_JOB_COMPLETION_OUTGOING_QUEUE)
                            .end();
                }
            });
        } catch (Exception ex) {
            log.error(ScsbCommonConstants.LOG_ERROR, ex);
        }
    }
}
