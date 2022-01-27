package org.recap.service.accession;

import lombok.extern.slf4j.Slf4j;
import org.recap.ScsbConstants;
import org.recap.model.ILSConfigProperties;
import org.recap.service.authorization.OauthTokenApiService;
import org.recap.util.PropertyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@Service
public class OAuthRestCallForBibdata extends BibDataAbstract{



    @Autowired
    PropertyUtil propertyUtil;

    @Autowired
    OauthTokenApiService oauthTokenApiService;


    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }
    public  RestTemplate getRestTmp() {
        RestTemplate restTmp = getRestTemplate();
        ((SimpleClientHttpRequestFactory)restTmp.getRequestFactory()).setConnectTimeout(connectionTimeout);
        ((SimpleClientHttpRequestFactory)restTmp.getRequestFactory()).setReadTimeout(readTimeout);
        return restTmp;
    }




    @Override
    public String getBibData(String itemBarcode, String customerCode, String institution,String url) {

        String bibDataResponse;
        String response;
        try {
            log.info("BIBDATA URL = {}",url);
            Map<String, String> params = getParamsMap(itemBarcode, customerCode);

            RestTemplate restTmp = getRestTmp();
            ILSConfigProperties ilsConfigProperties = propertyUtil.getILSConfigProperties(institution);
            String oAuthTokenApiUrl = ilsConfigProperties.getOauthTokenApiUrl();
            String operatorUserId = ilsConfigProperties.getOperatorUserId();
            String operatorPassword = ilsConfigProperties.getOperatorPassword();
            HttpEntity requestEntity = new HttpEntity(getHttpHeaders(oAuthTokenApiUrl,operatorUserId,operatorPassword));
            ResponseEntity<String> responseEntity = restTmp.exchange(url, HttpMethod.GET, requestEntity, String.class, params);
            bibDataResponse = responseEntity.getBody();

        } catch (Exception e) {
            response = String.format("[%s : %s] %s. (%s : %s)", itemBarcode, customerCode, ScsbConstants.ITEM_BARCODE_NOT_FOUND, url, e.getMessage());
            log.error(response);
            throw new RuntimeException(response);
        }
        return bibDataResponse;
    }



    public HttpHeaders getHttpHeaders(String oauthTokenApiUrl,String operatorUserId,String operatorPassword) throws Exception {
        String authorization = "Bearer " + oauthTokenApiService.generateAccessToken(oauthTokenApiUrl,operatorUserId,operatorPassword);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
        headers.set("Authorization", authorization);
        return headers;
    }

    public Map<String, String> getParamsMap(String itemBarcode, String customerCode) {
        Map<String, String> params  = new HashMap<>();
        params.put("barcode", itemBarcode);
        params.put("customercode", customerCode);
        return params;
    }

    @Override
    public boolean isAuth(String auth) {
        return "OAuth".equalsIgnoreCase(auth);
    }
}
