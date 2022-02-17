package org.recap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.model.queueinfo.QueueSizeInfoJson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Created by akulak on 20/10/17.
 */
@Slf4j
@Service
public class ActiveMqQueuesInfo {



    @Value("${" + PropertyKeyConstants.ACTIVEMQ_JOLOKIA_API_URL + "}")
    private String activeMqApiUrl;

    @Value("${" + PropertyKeyConstants.ACTIVEMQ_JOLOKIA_API_QUEUE_SIZE_ATTRIBUTE + "}")
    private String searchAttribute;

    @Value("${" + PropertyKeyConstants.ACTIVEMQ_WEB_CONSOLE_URL + "}")
    private String serviceUrl;

    @Value("${" + PropertyKeyConstants.ACTIVEMQ_CREDENTIALS + "}")
    private String activemqCredentials;

    public Integer getActivemqQueuesInfo(String queueName){
        Integer queueSizeCount = 0;
        String[] activemqUrls = serviceUrl.split(",");
        for (String activemqUrl : activemqUrls) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Authorization", "Basic " + getEncodedActivemqCredentials());
                HttpEntity<String> stringHttpEntity = new HttpEntity<>(headers);
                String searchUrl = String.format(activemqUrl + activeMqApiUrl + "%s" + searchAttribute, queueName);
                ResponseEntity<String> response = new RestTemplate().exchange(searchUrl, HttpMethod.GET, stringHttpEntity, String.class);
                QueueSizeInfoJson queueInfo = new ObjectMapper().readValue(response.getBody(), QueueSizeInfoJson.class);
                queueSizeCount = Integer.valueOf(queueInfo.getValue());
                break;
            } catch (ResourceAccessException e) {
                log.error("ActiveMq slave url called to get queues info ----> {} ",activemqUrl);
            } catch (Exception e){
                log.error(ScsbCommonConstants.LOG_ERROR,e);
            }
        }
        return queueSizeCount;
    }

    public String getEncodedActivemqCredentials(){
        byte[] activemqCredsBytes = activemqCredentials.getBytes();
        byte[] activemqBase64CredsBytes = Base64.encodeBase64(activemqCredsBytes);
        return new String(activemqBase64CredsBytes);
    }
}
