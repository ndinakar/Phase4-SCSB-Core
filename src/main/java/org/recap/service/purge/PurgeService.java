package org.recap.service.purge;

import lombok.extern.slf4j.Slf4j;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbConstants;
import org.recap.ScsbCommonConstants;
import org.recap.model.jpa.RequestTypeEntity;
import org.recap.repository.jpa.AccessionDetailsRepository;
import org.recap.repository.jpa.RequestItemDetailsRepository;
import org.recap.repository.jpa.RequestTypeDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hemalathas on 13/4/17.
 */
@Slf4j
@Service
public class PurgeService {



    @Value("${" + PropertyKeyConstants.PURGE_EMAIL_ADDRESS_EDD_REQUEST_DAY_LIMIT + "}")
    private Integer purgeEmailEddRequestDayLimit;

    @Value("${" + PropertyKeyConstants.PURGE_EMAIL_ADDRESS_PHYSICAL_REQUEST_DAY_LIMIT + "}")
    private Integer purgeEmailPhysicalRequestDayLimit;

    @Value("${" + PropertyKeyConstants.PURGE_EXCEPTION_REQUEST_DAY_LIMIT + "}")
    private Integer purgeExceptionRequestDayLimit;

    @Value("${" + PropertyKeyConstants.PURGE_ACCESSION_REQUEST_DAY_LIMIT + "}")
    private Integer purgeAccessionRequestDayLimit;

    @Autowired
    private RequestItemDetailsRepository requestItemDetailsRepository;

    @Autowired
    private RequestTypeDetailsRepository requestTypeDetailsRepository;

    @Autowired
    private AccessionDetailsRepository accessionDetailsRepository;

    /**
     * Purge email address map.
     *
     * @return the map
     */
    public Map<String, String> purgeEmailAddress() {
        Map<String, String> responseMap = new HashMap<>();
        try {
            List<RequestTypeEntity> requestTypeEntityList = requestTypeDetailsRepository.findAll();
            List<Integer> physicalRequestTypeIdList = new ArrayList<>();
            List<Integer> eddRequestTypeIdList = new ArrayList<>();
            for (RequestTypeEntity requestTypeEntity : requestTypeEntityList) {
                if (requestTypeEntity.getRequestTypeCode().equals(ScsbConstants.EDD_REQUEST)) {
                    eddRequestTypeIdList.add(requestTypeEntity.getId());
                } else {
                    physicalRequestTypeIdList.add(requestTypeEntity.getId());
                }
            }
            int noOfUpdatedRecordsForEddRequest = requestItemDetailsRepository.purgeEmailId(eddRequestTypeIdList, new Date(), purgeEmailEddRequestDayLimit, ScsbConstants.REFILED_REQUEST);
            int noOfUpdatedRecordsForPhysicalRequest = requestItemDetailsRepository.purgeEmailId(physicalRequestTypeIdList, new Date(), purgeEmailPhysicalRequestDayLimit, ScsbConstants.REFILED_REQUEST);
            responseMap.put(ScsbCommonConstants.STATUS, ScsbCommonConstants.SUCCESS);
            responseMap.put(ScsbCommonConstants.PURGE_EDD_REQUEST, String.valueOf(noOfUpdatedRecordsForEddRequest));
            responseMap.put(ScsbCommonConstants.PURGE_PHYSICAL_REQUEST, String.valueOf(noOfUpdatedRecordsForPhysicalRequest));
        } catch (Exception exception) {
            log.error(ScsbCommonConstants.LOG_ERROR, exception);
            responseMap.put(ScsbCommonConstants.STATUS, ScsbCommonConstants.FAILURE);
            responseMap.put(ScsbCommonConstants.MESSAGE, exception.getMessage());
        }
        return responseMap;
    }

    /**
     * Purge exception Request from Request_t table after certain period.
     *
     * @return the map
     */
    public Map<String, String> purgeExceptionRequests() {
        Map<String, String> responseMap = new HashMap<>();
        try {
            Integer countOfPurgedExceptionRequests = requestItemDetailsRepository.purgeExceptionRequests(ScsbConstants.REQUEST_STATUS_EXCEPTION, new Date(), purgeExceptionRequestDayLimit);
            log.info("Total number of exception requests purged : {}", countOfPurgedExceptionRequests);
            responseMap.put(ScsbCommonConstants.STATUS, ScsbCommonConstants.SUCCESS);
            responseMap.put(ScsbCommonConstants.MESSAGE, ScsbConstants.COUNT_OF_PURGED_EXCEPTION_REQUESTS + " : " + countOfPurgedExceptionRequests);
        } catch (Exception exception) {
            log.error(ScsbCommonConstants.LOG_ERROR, exception);
            responseMap.put(ScsbCommonConstants.STATUS, ScsbCommonConstants.FAILURE);
            responseMap.put(ScsbCommonConstants.MESSAGE, exception.getMessage());
        }
        return responseMap;
    }

    /**
     * Purge accession requests map.
     *
     * @return the map
     */
    public Map<String, String> purgeAccessionRequests() {
        Map<String, String> responseMap = new HashMap<>();
        try {
            Integer countOfPurgedAccessionRequests = accessionDetailsRepository.purgeAccessionRequests(ScsbConstants.COMPLETE, new Date(), purgeAccessionRequestDayLimit);
            log.info("Total number of accession requests purged : {}", countOfPurgedAccessionRequests);
            responseMap.put(ScsbCommonConstants.STATUS, ScsbCommonConstants.SUCCESS);
            responseMap.put(ScsbCommonConstants.MESSAGE, ScsbConstants.COUNT_OF_PURGED_ACCESSION_REQUESTS + " : " + countOfPurgedAccessionRequests);
        } catch (Exception exception) {
            log.error(ScsbCommonConstants.LOG_ERROR, exception);
            responseMap.put(ScsbCommonConstants.STATUS, ScsbCommonConstants.FAILURE);
            responseMap.put(ScsbCommonConstants.MESSAGE, exception.getMessage());
        }
        return responseMap;
    }
}
