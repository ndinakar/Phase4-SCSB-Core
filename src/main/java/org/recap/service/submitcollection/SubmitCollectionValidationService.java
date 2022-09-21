package org.recap.service.submitcollection;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.map.HashedMap;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbConstants;
import org.recap.ScsbCommonConstants;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.report.SubmitCollectionReportInfo;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.jpa.ItemDetailsRepository;
import org.recap.service.common.SetupDataService;
import org.recap.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by premkb on 11/7/17.
 */
@Slf4j
@Service
public class SubmitCollectionValidationService {



    @Autowired
    private InstitutionDetailsRepository institutionDetailsRepository;

    @Autowired
    private ItemDetailsRepository itemDetailsRepository;

    @Autowired
    private SetupDataService setupDataService;

    @Autowired
    private SubmitCollectionReportHelperService submitCollectionReportHelperService;

    @Autowired
    private SubmitCollectionHelperService submitCollectionHelperService;

    @Autowired
    private CommonUtil commonUtil;

    @Value("${" + PropertyKeyConstants.NONHOLDINGID_INSTITUTION + "}")
    private String nonHoldingIdInstitution;

    /**
     * Validate institution boolean.
     *
     * @param institutionCode the institution code
     * @return the boolean
     */
    public boolean validateInstitution(String institutionCode){
         return (institutionDetailsRepository.findByInstitutionCode(institutionCode) != null);
    }

    /**
     * Validate incoming entities boolean.
     *
     * @param submitCollectionReportInfoMap the submit collection report info map
     * @param fetchedBibliographicEntity    the fetched bibliographic entity
     * @param incomingBibliographicEntity   the incoming bibliographic entity
     * @return the boolean
     */
    public boolean validateIncomingEntities(Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap, BibliographicEntity fetchedBibliographicEntity,
                                            BibliographicEntity incomingBibliographicEntity){
        Boolean isValidToProcess = true;
        Boolean isValid;

        List<SubmitCollectionReportInfo> successSubmitCollectionReportInfoList = submitCollectionReportInfoMap.get(ScsbConstants.SUBMIT_COLLECTION_SUCCESS_LIST);
        List<SubmitCollectionReportInfo> rejectedSubmitCollectionReportInfoList = submitCollectionReportInfoMap.get(ScsbConstants.SUBMIT_COLLECTION_REJECTION_LIST);
        List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList = submitCollectionReportInfoMap.get(ScsbConstants.SUBMIT_COLLECTION_FAILURE_LIST);
        Map<String,Map<String,ItemEntity>> fetchedHoldingItemMap = submitCollectionHelperService.getHoldingItemIdMap(fetchedBibliographicEntity);
        Map<String,Map<String,ItemEntity>> incomingHoldingItemMap = submitCollectionHelperService.getHoldingItemIdMap(incomingBibliographicEntity);
        String owningInstitution = setupDataService.getInstitutionIdCodeMap().get(fetchedBibliographicEntity.getOwningInstitutionId());
        String[] nonHoldingIdInstitutionArray = getNonHoldingIdInstitutionArray();
        String institutionCode = setupDataService.getInstitutionIdCodeMap().get(incomingBibliographicEntity.getOwningInstitutionId());

        for (Map.Entry<String,Map<String,ItemEntity>> incomingHoldingItemMapEntry : incomingHoldingItemMap.entrySet()) {
            Map<String,ItemEntity> incomingOwningItemIdEntityMap = incomingHoldingItemMapEntry.getValue();
            Map<String,ItemEntity> fetchedOwningItemIdEntityMap = fetchedHoldingItemMap.get(incomingHoldingItemMapEntry.getKey());
            if(Arrays.asList(nonHoldingIdInstitutionArray).contains(institutionCode)) {//Report for non holding id institution
                Map<String,ItemEntity> incomingItemEntityMap = getItemIdEntityMap(incomingBibliographicEntity);
                Map<String,ItemEntity> fetchedItemEntityMap = getItemIdEntityMap(fetchedBibliographicEntity);
                for(Map.Entry<String,ItemEntity> incomingItemEntityMapEntry:incomingItemEntityMap.entrySet()){
                    isValid = validateMatchedAndUnmatchedRecords(submitCollectionReportInfoMap, failureSubmitCollectionReportInfoList, owningInstitution, fetchedItemEntityMap, incomingItemEntityMapEntry,incomingHoldingItemMapEntry.getKey());
                    isValidToProcess &= isValid;
                }
            } else if (fetchedOwningItemIdEntityMap != null && !fetchedHoldingItemMap.isEmpty()) {
                for(Map.Entry<String,ItemEntity> incomingOwningItemIdEntityMapEntry:incomingOwningItemIdEntityMap.entrySet()){
                    isValid = validateMatchedAndUnmatchedRecords(submitCollectionReportInfoMap, failureSubmitCollectionReportInfoList, owningInstitution, fetchedOwningItemIdEntityMap, incomingOwningItemIdEntityMapEntry,incomingHoldingItemMapEntry.getKey());
                    isValidToProcess &= isValid;
                }
            } else {//Failure report - holding id mismatch and for dummy record not having CGD in the incoming data
                for(Map.Entry<String,ItemEntity> incomingOwningItemIdBarcodeMapEntry:incomingOwningItemIdEntityMap.entrySet()) {
                    ItemEntity incomingItemEntity = incomingOwningItemIdBarcodeMapEntry.getValue();
                    if (incomingItemEntity.getCollectionGroupId()==null) {
                        commonUtil.buildSubmitCollectionReportInfoWhenNoGroupIdAndAddFailures(incomingBibliographicEntity, failureSubmitCollectionReportInfoList, owningInstitution, incomingItemEntity);
                        isValid = false;
                        isValidToProcess &= isValid;
                    } else {
                        commonUtil.buildSubmitCollectionReportInfoAndAddFailures(fetchedBibliographicEntity, failureSubmitCollectionReportInfoList, owningInstitution, incomingHoldingItemMapEntry, incomingItemEntity);
                        isValid = false;
                        isValidToProcess &= isValid;
                    }
                }
            }
        }
        submitCollectionReportInfoMap.put(ScsbConstants.SUBMIT_COLLECTION_SUCCESS_LIST,successSubmitCollectionReportInfoList);
        submitCollectionReportInfoMap.put(ScsbConstants.SUBMIT_COLLECTION_FAILURE_LIST,failureSubmitCollectionReportInfoList);
        submitCollectionReportInfoMap.put(ScsbConstants.SUBMIT_COLLECTION_REJECTION_LIST,rejectedSubmitCollectionReportInfoList);
        return isValidToProcess;

    }

    private Boolean validateMatchedAndUnmatchedRecords(Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap, List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList, String owningInstitution, Map<String, ItemEntity> fetchedOwningItemIdEntityMap, Map.Entry<String,
            ItemEntity> incomingOwningItemIdEntityMapEntry, String incomingOwningInstHoldingsId) {
        Boolean isValid;
        ItemEntity incomingItemEntity = incomingOwningItemIdEntityMapEntry.getValue();
        ItemEntity fetchedItemEntity = fetchedOwningItemIdEntityMap.get(incomingOwningItemIdEntityMapEntry.getKey());
        if (fetchedItemEntity != null && incomingItemEntity.getBarcode().equals(fetchedItemEntity.getBarcode()) && incomingItemEntity.getOwningInstitutionItemId().equals(fetchedItemEntity.getOwningInstitutionItemId())) {
            isValid = true;
        } else {//Failure report - item id mismatch
            reportFailureRecords(incomingItemEntity, fetchedItemEntity, fetchedOwningItemIdEntityMap, incomingOwningInstHoldingsId, failureSubmitCollectionReportInfoList,owningInstitution);
            isValid = false;
        }
        return isValid;
    }

    public Map<String,String> getOwningBibIdOwnInstHoldingsIdIfAnyHoldingMismatch(List<BibliographicEntity> bibliographicEntityList,List<String> holdingsIdUniqueList){
        Map<String,String> owningInstBibIdOwningInstHoldingsIdMap = new HashedMap();
        List<String> holdingsIdList = new ArrayList<>();
        for(BibliographicEntity bibliographicEntity:bibliographicEntityList){
            owningInstBibIdOwningInstHoldingsIdMap.put(bibliographicEntity.getOwningInstitutionBibId(),bibliographicEntity.getHoldingsEntities().get(0).getOwningInstitutionHoldingsId());
            holdingsIdList.add(bibliographicEntity.getHoldingsEntities().get(0).getOwningInstitutionHoldingsId());
            log.info("hold id--->{}",bibliographicEntity.getHoldingsEntities().get(0).getOwningInstitutionHoldingsId());
        }
        holdingsIdUniqueList.addAll(holdingsIdList.stream().distinct().collect(Collectors.toCollection(ArrayList::new)));
        log.info("holdingsIdUniqueList size--->{}",holdingsIdUniqueList.size());
        log.info("holdingsIdUniqueList --->{}",holdingsIdUniqueList.stream().collect(Collectors.joining(",")));
        if(!holdingsIdUniqueList.isEmpty()){
            return owningInstBibIdOwningInstHoldingsIdMap;
        }
        return null;
    }

    /**
     * Is available item boolean.
     *
     * @param itemAvailabilityStatusId the item availability status id
     * @return the boolean
     */
    public boolean isAvailableItem(Integer itemAvailabilityStatusId){
        String itemStatusCode = setupDataService.getItemStatusIdCodeMap().get(itemAvailabilityStatusId);
        return (itemStatusCode.equalsIgnoreCase(ScsbConstants.ITEM_STATUS_AVAILABLE));
    }

    private static Map<String,ItemEntity> getItemIdEntityMap(BibliographicEntity bibliographicEntity){
        Map<String,ItemEntity> itemEntityMap = new HashedMap();
        for(ItemEntity itemEntity:bibliographicEntity.getItemEntities()){
            itemEntityMap.put(itemEntity.getOwningInstitutionItemId(),itemEntity);
        }
        return itemEntityMap;
    }

    public boolean validateIncomingItemHavingBibCountIsSameAsExistingItem(Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap, Map<String, ItemEntity> fetchedBarcodeItemEntityMap,
                                                                           List<BibliographicEntity> incomingBibliographicEntityList) {
        boolean isValidRecordToProcess = true;
        for (BibliographicEntity incomingBibliographicEntity : incomingBibliographicEntityList) {
            for (ItemEntity incomingItemEntity : incomingBibliographicEntity.getItemEntities()) {
                ItemEntity fetchedItemEntity = fetchedBarcodeItemEntityMap.get(incomingItemEntity.getBarcode());
                if (fetchedItemEntity != null) {
                    List<BibliographicEntity> fetchedBibliographicEntityList = fetchedItemEntity.getBibliographicEntities();
                    List<String> notMatchedIncomingOwnInstBibId = new ArrayList<>();
                    List<String> notMatchedFetchedOwnInstBibId = new ArrayList<>();
                    verifyAndSetMisMatchBoundWithOwnInstBibIdIfAny(incomingBibliographicEntityList,fetchedBibliographicEntityList
                            ,notMatchedIncomingOwnInstBibId,notMatchedFetchedOwnInstBibId);
                    if(notMatchedIncomingOwnInstBibId.isEmpty() && notMatchedFetchedOwnInstBibId.isEmpty()){
                        Map<String,BibliographicEntity> fetchedOwnInstBibIdBibliographicEntityMap = getOwnInstBibIdBibliographicEntityMap(fetchedBibliographicEntityList);
                        BibliographicEntity fetchedBibliographicEntity = fetchedOwnInstBibIdBibliographicEntityMap.get(incomingBibliographicEntity.getOwningInstitutionBibId());
                        Boolean isValid = validateIncomingEntities(submitCollectionReportInfoMap,fetchedBibliographicEntity,incomingBibliographicEntity );
                        isValidRecordToProcess &= isValid;
                    } else if(fetchedBibliographicEntityList.get(0).getOwningInstitutionBibId().substring(0, 1).equals("d")) {//update existing dummy record if any (Removes existing dummy record and creates new record for the same barcode based on the input xml)
                        isValidRecordToProcess &= true;
                    } else { //Owning inst bib id mismatch for non dummy record
                        boolean isBarcodeAlreadyAdded = submitCollectionReportHelperService.isBarcodeAlreadyAdded(incomingItemEntity.getBarcode(),submitCollectionReportInfoMap);
                        if (!isBarcodeAlreadyAdded) {
                            submitCollectionReportHelperService.setSubmitCollectionReportInfoForOwningInstitutionBibIdMismatchForBoundWith(notMatchedIncomingOwnInstBibId, notMatchedFetchedOwnInstBibId,incomingItemEntity,fetchedItemEntity, submitCollectionReportInfoMap.get(ScsbConstants.SUBMIT_COLLECTION_FAILURE_LIST));
                        }
                        isValidRecordToProcess &= false;
                    }
                }
            }
        }
        return isValidRecordToProcess;
    }

    public boolean validateIncomingItemHavingBibCountGreaterThanExistingItem(Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap,
                                                                              List<BibliographicEntity> incomingBibliographicEntityList,List<BibliographicEntity> existingBibliographicEntityList){
        boolean isValidRecordToProcess = true;
        List<String> matchedOwningInstBibIdList = getMatchedOwningInstBibId(incomingBibliographicEntityList,existingBibliographicEntityList);
        Map<String,BibliographicEntity> incomingBibliographicEntityMap = incomingBibliographicEntityList.stream()
                .collect(Collectors.toMap(BibliographicEntity::getOwningInstitutionBibId,bibliographicEntity -> bibliographicEntity));
        Map<String,BibliographicEntity> existingBibliographicEntityMap = existingBibliographicEntityList.stream()
                .collect(Collectors.toMap(BibliographicEntity::getOwningInstitutionBibId,bibliographicEntity -> bibliographicEntity));
        List<String> existingBibsNotInIncomingBibs = getExistingBibsNotInIncomingBibs(incomingBibliographicEntityMap,existingBibliographicEntityMap);
        List<String> holdingsIdUniqueList = new ArrayList<>();
        String owningInstitutionCode = existingBibliographicEntityList.get(0).getItemEntities().get(0).getInstitutionEntity().getInstitutionCode();
        String[] nonHoldingIdInstitutionArray = getNonHoldingIdInstitutionArray();
        Map<String,String> owningBibIdOwnInstHoldingsIdMap = getOwningBibIdOwnInstHoldingsIdIfAnyHoldingMismatch(incomingBibliographicEntityList,holdingsIdUniqueList);
        if(existingBibliographicEntityList.get(0).getOwningInstitutionBibId().substring(0, 1).equals("d")) {//validation to update existing dummy record if any (Removes existing dummy record and creates new record for the same barcode based on the input xml)
            isValidRecordToProcess &= true;
        } else if(!matchedOwningInstBibIdList.isEmpty()  && existingBibsNotInIncomingBibs.isEmpty() && holdingsIdUniqueList.size() == 1){
            for (BibliographicEntity incomingBibliographicEntity : incomingBibliographicEntityList) {
                if(matchedOwningInstBibIdList.contains(incomingBibliographicEntity.getOwningInstitutionBibId())){
                    BibliographicEntity existingBibliographicEntity = existingBibliographicEntityMap.get(incomingBibliographicEntity.getOwningInstitutionBibId());
                    Boolean isValid = validateIncomingEntities(submitCollectionReportInfoMap,existingBibliographicEntity,incomingBibliographicEntity );
                    isValidRecordToProcess &= isValid;
                } else {
                    BibliographicEntity existingBibliographicEntity = existingBibliographicEntityList.get(0);
                    Boolean isValid = validateIncomingEntities(submitCollectionReportInfoMap,existingBibliographicEntity,incomingBibliographicEntity );
                    isValidRecordToProcess &= isValid;
                }
            }
        } else if(!existingBibsNotInIncomingBibs.isEmpty()){//if incoming does not have the existing bibinfo then error message is thrown
            StringBuilder message = new StringBuilder();
            message.append(ScsbConstants.SUBMIT_COLLECTION_FAILED_RECORD).append(ScsbCommonConstants.HYPHEN).append("Incoming bound-with item does not have matching bib that are available in the " +
                    "existing record, bib id(s) that are not linked with incoming item ").append(existingBibsNotInIncomingBibs.stream().collect(Collectors.joining(",")));
            String barcode = existingBibliographicEntityList.get(0).getItemEntities().get(0).getBarcode();
            String customerCode = existingBibliographicEntityList.get(0).getItemEntities().get(0).getCustomerCode();
            String owningInstitution = owningInstitutionCode;
            submitCollectionReportHelperService.setSubmitCollectionReportInfo(submitCollectionReportInfoMap.get(ScsbConstants.SUBMIT_COLLECTION_FAILURE_LIST),
                    barcode,customerCode,owningInstitution,message.toString());
            isValidRecordToProcess &= false;
        } else if(holdingsIdUniqueList.size() > 1 &&
                !Arrays.asList(nonHoldingIdInstitutionArray).contains(owningInstitutionCode)){//Owning inst Holdings id mismatch with in the incoming bound-with record
            isValidRecordToProcess = isValidRecordToProcess(submitCollectionReportInfoMap, incomingBibliographicEntityList, existingBibliographicEntityList, isValidRecordToProcess, holdingsIdUniqueList, owningBibIdOwnInstHoldingsIdMap);
        }
        return isValidRecordToProcess;
    }

    private boolean isValidRecordToProcess(Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap, List<BibliographicEntity> incomingBibliographicEntityList, List<BibliographicEntity> existingBibliographicEntityList, boolean isValidRecordToProcess, List<String> holdingsIdUniqueList, Map<String, String> owningBibIdOwnInstHoldingsIdMap) {
        String multipleHoldingIds = holdingsIdUniqueList.stream().collect(Collectors.joining(","));
        for (Map.Entry<String, String> owningBibIdOwnInstHoldingsIdEntry : owningBibIdOwnInstHoldingsIdMap.entrySet()) {
            StringBuilder message = new StringBuilder();
            message.append(ScsbConstants.SUBMIT_COLLECTION_FAILED_RECORD).append(ScsbCommonConstants.HYPHEN).append("Incoming bound-with item has multiple owning institution holdings id attached to it, " +
                    "multiple owning institution holdings are ").append(multipleHoldingIds).append(" - incoming owning institution holdings id ")
                    .append(owningBibIdOwnInstHoldingsIdEntry.getValue()).append(", incoming owning institution item id ")
                    .append(incomingBibliographicEntityList.get(0).getItemEntities().get(0).getOwningInstitutionItemId()).append(", ")
                    .append("incoming owning institution bib id ").append(owningBibIdOwnInstHoldingsIdEntry.getKey());
            setSubmitCollectionReportInfo(submitCollectionReportInfoMap, existingBibliographicEntityList, message);
        }
        isValidRecordToProcess &= false;
        return isValidRecordToProcess;
    }

    public boolean validateIncomingItemHavingBibCountLesserThanExistingItem(Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap,
                                                                            List<BibliographicEntity> incomingBibliographicEntityList,List<BibliographicEntity> existingBibliographicEntityList
    ,List<String> incomingBibsNotInExistingBibs,ItemEntity existingItemEntity){
        boolean isValidRecordToProcess = true;
        List<String> matchedOwningInstBibIdList = getMatchedOwningInstBibId(incomingBibliographicEntityList,existingBibliographicEntityList);
        Map<String,BibliographicEntity> incomingBibliographicEntityMap = incomingBibliographicEntityList.stream()
                .collect(Collectors.toMap(BibliographicEntity::getOwningInstitutionBibId,bibliographicEntity -> bibliographicEntity));
        Map<String,BibliographicEntity> existingBibliographicEntityMap = existingBibliographicEntityList.stream()
                .collect(Collectors.toMap(BibliographicEntity::getOwningInstitutionBibId,bibliographicEntity -> bibliographicEntity));
        incomingBibsNotInExistingBibs.addAll(getIncomingBibsNotInExistingBibs(incomingBibliographicEntityMap,existingBibliographicEntityMap));
        List<String> holdingsIdUniqueList = new ArrayList<>();
        boolean isItemAvailable = isAvailableItem(existingItemEntity.getItemAvailabilityStatusId());
        String owningInstitutionCode = existingBibliographicEntityList.get(0).getItemEntities().get(0).getInstitutionEntity().getInstitutionCode();
        String[] nonHoldingIdInstitutionArray = getNonHoldingIdInstitutionArray();
        Map<String,String> owningBibIdOwnInstHoldingsIdMap = getOwningBibIdOwnInstHoldingsIdIfAnyHoldingMismatch(incomingBibliographicEntityList,holdingsIdUniqueList);
        if(!isItemAvailable) {
            StringBuilder message = new StringBuilder();
            message.append(ScsbConstants.SUBMIT_COLLECTION_FAILED_RECORD).append(ScsbCommonConstants.HYPHEN).append("Incoming record has reduced bib, but the bibs are not unlinked since the item is unavailable ");
            setSubmitCollectionReportInfo(submitCollectionReportInfoMap, existingBibliographicEntityList, message);
            isValidRecordToProcess &= false;
        }else if(!matchedOwningInstBibIdList.isEmpty() && incomingBibsNotInExistingBibs.isEmpty() && holdingsIdUniqueList.size() == 1) {
            for (BibliographicEntity incomingBibliographicEntity : incomingBibliographicEntityList) {
                if(matchedOwningInstBibIdList.contains(incomingBibliographicEntity.getOwningInstitutionBibId())){
                    BibliographicEntity existingBibliographicEntity = existingBibliographicEntityMap.get(incomingBibliographicEntity.getOwningInstitutionBibId());
                    Boolean isValid = validateIncomingEntities(submitCollectionReportInfoMap,existingBibliographicEntity,incomingBibliographicEntity );
                    isValidRecordToProcess &= isValid;
                }
            }
        } else if(!incomingBibsNotInExistingBibs.isEmpty()){//if incoming does not have the existing bibinfo then error message is thrown
            StringBuilder message = new StringBuilder();
            message.append(ScsbConstants.SUBMIT_COLLECTION_FAILED_RECORD).append(ScsbCommonConstants.HYPHEN).append("Incoming bound-with item with less bibs than the existing bibs which does not have matching bib that are available in the " +
                    "existing record, bib id(s) that are not linked with incoming item ").append(incomingBibsNotInExistingBibs.stream().collect(Collectors.joining(",")));
            setSubmitCollectionReportInfo(submitCollectionReportInfoMap, existingBibliographicEntityList, message);
            isValidRecordToProcess &= false;
        } else if(holdingsIdUniqueList.size() > 1 &&
                !Arrays.asList(nonHoldingIdInstitutionArray).contains(owningInstitutionCode)){//Owning inst Holdings id mismatch with in the incoming bound-with record
            isValidRecordToProcess = isValidRecordToProcess(submitCollectionReportInfoMap, incomingBibliographicEntityList, existingBibliographicEntityList, isValidRecordToProcess, holdingsIdUniqueList, owningBibIdOwnInstHoldingsIdMap);
        }
        return isValidRecordToProcess;
    }

    private void setSubmitCollectionReportInfo(Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap, List<BibliographicEntity> existingBibliographicEntityList, StringBuilder message) {
        String barcode = existingBibliographicEntityList.get(0).getItemEntities().get(0).getBarcode();
        String customerCode = existingBibliographicEntityList.get(0).getItemEntities().get(0).getCustomerCode();
        String owningInstitution = existingBibliographicEntityList.get(0).getItemEntities().get(0).getInstitutionEntity().getInstitutionCode();
        submitCollectionReportHelperService.setSubmitCollectionReportInfo(submitCollectionReportInfoMap.get(ScsbConstants.SUBMIT_COLLECTION_FAILURE_LIST),
                barcode, customerCode, owningInstitution, message.toString());
    }

    private static List<String> getMatchedOwningInstBibId(List<BibliographicEntity> incomingBibliographicEntityList,List<BibliographicEntity> existingBibliographicEntityList){
        List<String> incomingOwningInstBibIdList = incomingBibliographicEntityList.stream()
                .map(BibliographicEntity::getOwningInstitutionBibId)
                .collect(Collectors.toCollection(ArrayList::new));
        List<String> existingOwningInstBibIdList = existingBibliographicEntityList.stream()
                .map(BibliographicEntity::getOwningInstitutionBibId)
                .collect(Collectors.toCollection(ArrayList::new));
        List<String> matchedOwningInstBibId = new ArrayList<>();
        for(String incomingOwningInstBibId:incomingOwningInstBibIdList){
            if(existingOwningInstBibIdList.contains(incomingOwningInstBibId)){
                matchedOwningInstBibId.add(incomingOwningInstBibId);
            }
        }
        return matchedOwningInstBibId;
    }

    public List<String> getExistingBibsNotInIncomingBibs(Map<String,BibliographicEntity> incomingBibliographicEntityMap,Map<String,BibliographicEntity> existingBibliographicEntityMap){
        List<String> existingBibsNotInIncomingBibs = new ArrayList<>();
        for(Map.Entry<String,BibliographicEntity> existingBibliographicEntityMapEntry:existingBibliographicEntityMap.entrySet()){
            if(!incomingBibliographicEntityMap.containsKey(existingBibliographicEntityMapEntry.getKey())){
                existingBibsNotInIncomingBibs.add(existingBibliographicEntityMapEntry.getKey());
            }
        }
        return existingBibsNotInIncomingBibs;
    }

    public List<String> getIncomingBibsNotInExistingBibs(Map<String,BibliographicEntity> incomingBibliographicEntityMap,Map<String,BibliographicEntity> existingBibliographicEntityMap){
        List<String> incomingBibsNotInExistingBibs = new ArrayList<>();
        for(Map.Entry<String,BibliographicEntity> incomingBibliographicEntityMapEntry:incomingBibliographicEntityMap.entrySet()){
            if(!existingBibliographicEntityMap.containsKey(incomingBibliographicEntityMapEntry.getKey())){
                incomingBibsNotInExistingBibs.add(incomingBibliographicEntityMapEntry.getKey());
            }
        }
        return incomingBibsNotInExistingBibs;
    }

    public void verifyAndSetMisMatchBoundWithOwnInstBibIdIfAny(List<BibliographicEntity> incomingBibliographicEntityList,List<BibliographicEntity> fetchedBibliographicEntityList
            ,List<String> notMatchedIncomingOwnInstBibId,List<String> notMatchedFetchedOwnInstBibId){
        List<String> incomingOwnInstBibIdList = getOwnInstBibIdList(incomingBibliographicEntityList);
        List<String> fetchedOwnInstBibIdList = getOwnInstBibIdList(fetchedBibliographicEntityList);

        for(String incomingOwnInstBibId:incomingOwnInstBibIdList){
            if(!fetchedOwnInstBibIdList.contains(incomingOwnInstBibId)){
                notMatchedIncomingOwnInstBibId.add(incomingOwnInstBibId);
            }
        }

        for (String fetchedOwnInstBibId:fetchedOwnInstBibIdList){
            if(!incomingOwnInstBibIdList.contains(fetchedOwnInstBibId)){
                notMatchedFetchedOwnInstBibId.add(fetchedOwnInstBibId);
            }
        }
    }

    private static List<String> getOwnInstBibIdList(List<BibliographicEntity> bibliographicEntityList){
        List<String> ownInstBibIdList = new ArrayList<>();
        for(BibliographicEntity bibliographicEntity:bibliographicEntityList){
            ownInstBibIdList.add(bibliographicEntity.getOwningInstitutionBibId());
        }
        return ownInstBibIdList;
    }

    public boolean isExistingBoundWithItem(ItemEntity itemEntity){
        return (itemEntity != null && itemEntity.getBibliographicEntities().size()>1);
    }
    public Map<String,BibliographicEntity> getOwnInstBibIdBibliographicEntityMap(List<BibliographicEntity> bibliographicEntityList){
        return bibliographicEntityList.stream().collect(Collectors.toMap(BibliographicEntity::getOwningInstitutionBibId,bibliographicEntity -> bibliographicEntity));

    }

    private String[] getNonHoldingIdInstitutionArray(){
        return nonHoldingIdInstitution.split(",");
    }

    private void reportFailureRecords(ItemEntity incomingItemEntity, ItemEntity fetchedItemEntity, Map<String, ItemEntity> fetchedOwningItemIdEntityMap, String incomingOwningInstHoldingsId, List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList, String owningInstitution) {
        submitCollectionReportHelperService.setFailureSubmitCollectionReportInfoList(failureSubmitCollectionReportInfoList, owningInstitution, fetchedOwningItemIdEntityMap, incomingOwningInstHoldingsId, incomingItemEntity, fetchedItemEntity);
    }

}
