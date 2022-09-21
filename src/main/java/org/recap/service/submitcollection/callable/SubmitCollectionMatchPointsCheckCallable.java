package org.recap.service.submitcollection.callable;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.jpa.ItemEntity;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by rajeshbabuk on 17/Sep/2021
 */
@Slf4j
@Component
@Scope("prototype")
@Data
public class SubmitCollectionMatchPointsCheckCallable implements Callable<Map<Integer, Set<Integer>>> {

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private BibliographicDetailsRepository bibliographicDetailsRepository;

    Integer fetchedBibId;
    Integer fetchedBibMAQualifier;
    String incomingMarcXml;
    String existingMarcXml;
    String matchingIdentifier;
    String institutionCode;
    boolean isCGDProtected;
    Map<String, ItemEntity> fetchedBarcodeItemEntityMap;
    Map<String, ItemEntity> incomingBarcodeItemEntityMap;
    Map<Integer, String> collectionGroupIdCodeMap;
    Map<Integer, String> itemStatusIdCodeMap;

    @Override
    public Map<Integer, Set<Integer>> call() throws Exception {
        Map<Integer, Set<Integer>> responseMap = new HashMap<>();
        try {
            boolean isMatchPointsChanged = commonUtil.checkIfMatchPointsChanged(incomingMarcXml, existingMarcXml, institutionCode);
            boolean isCgdChangedToShared = !isCGDProtected && commonUtil.isCgdChangedToShared(fetchedBarcodeItemEntityMap, incomingBarcodeItemEntityMap, collectionGroupIdCodeMap, itemStatusIdCodeMap, false);
            boolean isCgdAlreadyShared = commonUtil.isCgdAlreadyShared(fetchedBarcodeItemEntityMap, incomingBarcodeItemEntityMap, collectionGroupIdCodeMap, itemStatusIdCodeMap);
            int maQualifier = 0;
            if (isMatchPointsChanged && (isCgdAlreadyShared || isCgdChangedToShared)) {
                maQualifier = ScsbCommonConstants.MA_QUALIFIER_3;
            } else if (isCgdChangedToShared) {
                maQualifier = ScsbCommonConstants.MA_QUALIFIER_2;
            } else if (isMatchPointsChanged) {
                maQualifier = ScsbCommonConstants.MA_QUALIFIER_1;
            }
            if (maQualifier > 0) {
                getAndPutBibIdsBasedOnMatchingIdValue(maQualifier, responseMap);
            }
        } catch (Exception e) {
            log.info("Exception while processing SC Match Points Check in thread for Fetched Bib Id: {} - {}", fetchedBibId, e.getMessage());
            e.printStackTrace();
        }
        return responseMap;
    }

    private void getAndPutBibIdsBasedOnMatchingIdValue(int maQualifier, Map<Integer, Set<Integer>> responseMap) {
        Set<Integer> bibIds = new HashSet<>();
        if (StringUtils.isBlank(matchingIdentifier)) {
            setFetchedBibIdOnlyToMap(maQualifier, responseMap, bibIds);
        } else {
            setBibIdsByMatchingIdToMap(maQualifier, responseMap, bibIds);
        }
    }

    private void setBibIdsByMatchingIdToMap(int maQualifier, Map<Integer, Set<Integer>> responseMap, Set<Integer> bibIds) {
        maQualifier = checkExistingBibMAQualifier(fetchedBibMAQualifier, maQualifier, bibIds);
        if (maQualifier == ScsbCommonConstants.MA_QUALIFIER_1) {
            bibIds = bibliographicDetailsRepository.findIdByMatchingIdentity(matchingIdentifier);
            putToResponseMap(maQualifier, bibIds, responseMap);
            log.debug(ScsbConstants.LOG_MATCH_ID_QUALIFIER_UPDATE, matchingIdentifier, maQualifier, bibIds.size(), bibIds);
        } else if (maQualifier == ScsbCommonConstants.MA_QUALIFIER_2) {
            setFetchedBibIdOnlyToMap(maQualifier, responseMap, bibIds);
        } else if (maQualifier == ScsbCommonConstants.MA_QUALIFIER_3) {
            Set<Integer> sharedBibIds = new HashSet<>();
            Set<Integer> nonSharedBibIds = new HashSet<>();
            commonUtil.collectSharedAndNonSharedBibIdsForMatchingId(sharedBibIds, nonSharedBibIds, matchingIdentifier, collectionGroupIdCodeMap);
            if (!sharedBibIds.isEmpty()) {
                putToResponseMap(ScsbCommonConstants.MA_QUALIFIER_3, sharedBibIds, responseMap);
                log.debug(ScsbConstants.LOG_MATCH_ID_QUALIFIER_UPDATE, matchingIdentifier, maQualifier, sharedBibIds.size(), sharedBibIds);
            }
            if (!nonSharedBibIds.isEmpty()) {
                putToResponseMap(ScsbCommonConstants.MA_QUALIFIER_1, nonSharedBibIds, responseMap);
                log.debug(ScsbConstants.LOG_MATCH_ID_QUALIFIER_UPDATE, matchingIdentifier, ScsbCommonConstants.MA_QUALIFIER_1, nonSharedBibIds.size(), nonSharedBibIds);
            }
        }
    }

    private Integer checkExistingBibMAQualifier(int fetchedBibMAQualifier, int currentMaQualifier, Set<Integer> bibIds) {
        if (fetchedBibMAQualifier > 0 && currentMaQualifier > 0 && fetchedBibMAQualifier != currentMaQualifier) {
            log.debug("Matching Id - {}, MA Qualifier Existing - {}, Current - {}, Changed To - {} for {} Bib Ids: {}", matchingIdentifier, fetchedBibMAQualifier, currentMaQualifier, ScsbCommonConstants.MA_QUALIFIER_3, bibIds.size(), bibIds);
            return ScsbCommonConstants.MA_QUALIFIER_3;
        } else {
            return currentMaQualifier;
        }
    }

    private void setFetchedBibIdOnlyToMap(int maQualifier, Map<Integer, Set<Integer>> responseMap, Set<Integer> bibIds) {
        bibIds.add(fetchedBibId);
        maQualifier = checkExistingBibMAQualifier(fetchedBibMAQualifier, maQualifier, bibIds);
        putToResponseMap(maQualifier, bibIds, responseMap);
        log.debug(ScsbConstants.LOG_MATCH_ID_QUALIFIER_UPDATE, matchingIdentifier, maQualifier, bibIds.size(), bibIds);
    }

    private static void putToResponseMap(Integer maQualifier, Set<Integer> bibIds, Map<Integer, Set<Integer>> responseMap) {
        if (responseMap.containsKey(maQualifier)) {
            responseMap.get(maQualifier).addAll(bibIds);
        } else {
            responseMap.put(maQualifier, bibIds);
        }
    }
}
