package org.recap.activemq;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.broker.jmx.DestinationViewMBean;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;

/**
 * Created by angelind on 22/8/17.
 */

@Slf4j
@Component
public class JmxHelper {

    @Value("${" + PropertyKeyConstants.ACTIVEMQ_JMX_SERVICE_URL + "}")
    private String serviceUrl;
    private MBeanServerConnection connection;

    /**
     * This method is used to get detailed information about the queue.
     *
     * @param queueName the queue name
     * @return the bean for queue name
     */
    public DestinationViewMBean getBeanForQueueName(String queueName) {
        try {
            ObjectName nameConsumers = new ObjectName("org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName="+ queueName);
            return MBeanServerInvocationHandler.newProxyInstance(getConnection(), nameConsumers, DestinationViewMBean.class, true);
        } catch (MalformedObjectNameException e) {
            log.error(ScsbCommonConstants.LOG_ERROR,e);
        }
        return null;
    }

    /**
     * Gets message bean server connection.
     *
     * @return the connection
     */
    public MBeanServerConnection getConnection() {
        if (null == connection) {
            JMXConnector connector = null;
            try {
                connector = JMXConnectorFactory.connect(new JMXServiceURL(serviceUrl));
                connection = connector.getMBeanServerConnection();
            } catch (IOException e) {
                log.error(ScsbCommonConstants.LOG_ERROR,e);
            }
        }
        return connection;
    }
}
