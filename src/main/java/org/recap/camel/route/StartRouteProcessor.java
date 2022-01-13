package org.recap.camel.route;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.context.annotation.Scope;

/**
 * Created by akulak on 19/7/17.
 */
@Slf4j
@Scope("prototype")
public class StartRouteProcessor implements Processor {


    private String routeId;

    public StartRouteProcessor(String routeId) {
        this.routeId = routeId;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        log.info("Starting next route !!! {}",routeId);
        exchange.getContext().getRouteController().startRoute(routeId);
    }
}
