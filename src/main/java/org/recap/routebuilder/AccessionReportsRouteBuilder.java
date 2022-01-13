package org.recap.routebuilder;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccessionReportsRouteBuilder {



    /**
     * This method instantiates a new reports route builder to save accession reports in database.
     *
     * @param camelContext    the camel context
     * @param reportProcessor the report processor
     */
    @Autowired
    public AccessionReportsRouteBuilder(CamelContext camelContext, ReportProcessor reportProcessor) {
        try {
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ScsbConstants.ACCESSION_REPORT_Q + "?concurrentConsumers=10")
                            .routeId(ScsbConstants.ACCESSION_REPORT_ROUTE_ID).threads(10)
                            .process(reportProcessor);
                }
            });
        } catch (Exception e) {
            log.error(ScsbCommonConstants.LOG_ERROR,e);
        }
    }

}
