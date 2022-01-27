package org.recap.camel.route;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.recap.ScsbCommonConstants;


/**
 * The type Stop route processor.
 */

@Slf4j
public class StopRouteProcessor implements Processor {

    private String routeId;

    /**
     * Instantiates a new Stop route processor.
     *
     * @param routeId the route id
     */
    public StopRouteProcessor(String routeId) {
        this.routeId = routeId;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Thread stopThread;
        stopThread = new Thread() {
            @Override
            public void run() {
                try {
                    stopRouteWithTimeOutOption();
                    log.info("Stop Route {}" , routeId);
                } catch (Exception e) {
                    log.error("Exception while stop route : {}" , routeId);
                    log.error(ScsbCommonConstants.LOG_ERROR , e);
                }
            }

            private void stopRouteWithTimeOutOption() throws Exception {
                exchange.getContext().getShutdownStrategy().setTimeout(1);
                exchange.getContext().getRouteController().stopRoute(routeId);
            }
        };
        stopThread.start();
    }
}

