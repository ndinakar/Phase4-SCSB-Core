package org.recap.service.submitcollection;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.collections4.ListUtils;
import org.marc4j.marc.Record;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.jaxb.BibRecord;
import org.recap.model.jaxb.marc.BibRecords;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.report.SubmitCollectionReportInfo;
import org.recap.model.submitcollection.BarcodeBibliographicEntityObject;
import org.recap.model.submitcollection.BoundWithBibliographicEntityObject;
import org.recap.model.submitcollection.NonBoundWithBibliographicEntityObject;
import org.recap.service.common.RepositoryService;
import org.recap.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import javax.xml.bind.JAXBException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Created by premkb on 10/10/17.
 */
@Slf4j
@Service
public class SubmitCollectionBatchService extends SubmitCollectionService {



    @Autowired
    private SubmitCollectionReportHelperService submitCollectionReportHelperService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private CommonUtil commonUtil;

    @Value("${" + PropertyKeyConstants.SUBMIT_COLLECTION_INPUT_LIMIT + "}")
    private Integer inputLimit;

    @Value("${" + PropertyKeyConstants.SUBMIT_COLLECTION_PARTITION_SIZE + "}")
    private Integer partitionSize;

    @Override
    public String processMarc(String inputRecords, Set<Integer> processedBibIds, Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap, List<Map<String, String>> idMapToRemoveIndexList, List<Map<String, String>> bibIdMapToRemoveIndexList, boolean checkLimit
            , boolean isCGDProtection, InstitutionEntity institutionEntity, Set<String> updatedDummyRecordOwnInstBibIdSet, ExecutorService executorService, List<Future> futures) {
        log.info("inside SubmitCollectionBatchService");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String format = ScsbConstants.FORMAT_MARC;
        List<Record> recordList = new ArrayList<>();
        String invalidMessage = getMarcUtil().convertAndValidateXml(inputRecords, checkLimit, recordList);
        if (invalidMessage == null) {
            List<BibliographicEntity> validBibliographicEntityList = new ArrayList<>();
            for (Record record : recordList) {
                BibliographicEntity bibliographicEntity = prepareBibliographicEntity(record, format, submitCollectionReportInfoMap, idMapToRemoveIndexList, isCGDProtection, institutionEntity);
                validBibliographicEntityList.add(bibliographicEntity);
            }
            log.info("Total incoming marc records for processing--->{}", recordList.size());
            processConvertedBibliographicEntityFromIncomingRecords(processedBibIds, submitCollectionReportInfoMap, idMapToRemoveIndexList, bibIdMapToRemoveIndexList, institutionEntity, updatedDummyRecordOwnInstBibIdSet, validBibliographicEntityList, executorService, futures);
            stopWatch.stop();
            log.info("Total time take for processMarc--->{}", stopWatch.getTotalTimeSeconds());
            return null;
        } else {
            return invalidMessage;
        }
    }

    private void processConvertedBibliographicEntityFromIncomingRecords(Set<Integer> processedBibIds, Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap, List<Map<String, String>> idMapToRemoveIndexList, List<Map<String, String>> bibIdMapToRemoveIndexList, InstitutionEntity institutionEntity, Set<String> updatedDummyRecordOwnInstBibIdSet, List<BibliographicEntity> validBibliographicEntityList, ExecutorService executorService, List<Future> futures) {
        //TODO need to remove the list - remove the intermediate process
        List<BibliographicEntity> boundwithBibliographicEntityList = new ArrayList<>();
        List<BibliographicEntity> nonBoundWithBibliographicEntityList = new ArrayList<>();
        List<BibliographicEntity> splittedBibliographicEntityList = splitBibWithOneItem(validBibliographicEntityList);
        prepareBoundWithAndNonBoundWithList(splittedBibliographicEntityList, nonBoundWithBibliographicEntityList, boundwithBibliographicEntityList);

        Map<String, List<BibliographicEntity>> groupByOwnInstBibIdBibliographicEntityListMap = groupByOwnInstBibIdBibliographicEntityListMap(nonBoundWithBibliographicEntityList);//Added to avoid data discrepancy during multithreading
        Map<String, List<BibliographicEntity>> groupByBarcodeBibliographicEntityListMap = groupByBarcodeBibliographicEntityListMap(boundwithBibliographicEntityList);//Added to avoid data discrepancy during multithreading
        List<NonBoundWithBibliographicEntityObject> nonBoundWithBibliographicEntityObjectList = prepareNonBoundWithBibliographicEntity(groupByOwnInstBibIdBibliographicEntityListMap);
        List<BoundWithBibliographicEntityObject> boundWithBibliographicEntityObjectList = prepareBoundWithBibliographicEntityObjectList(groupByBarcodeBibliographicEntityListMap);
        log.info("boundwithBibliographicEntityList size--->{}", boundwithBibliographicEntityList.size());
        log.info("boundWithBibliographicEntityObjectList size--->{}", boundWithBibliographicEntityObjectList.size());
        log.info("nonBoundWithBibliographicEntityList size--->{}", nonBoundWithBibliographicEntityList.size());
        if (!nonBoundWithBibliographicEntityObjectList.isEmpty()) {
            processRecordsInBatchesForNonBoundWith(nonBoundWithBibliographicEntityObjectList, institutionEntity.getId(), submitCollectionReportInfoMap, processedBibIds, idMapToRemoveIndexList, executorService, futures);
        }
        if (!boundwithBibliographicEntityList.isEmpty()) {
            processRecordsInBatchesForBoundWith(boundWithBibliographicEntityObjectList, institutionEntity.getId(), submitCollectionReportInfoMap, processedBibIds, idMapToRemoveIndexList, bibIdMapToRemoveIndexList, updatedDummyRecordOwnInstBibIdSet, executorService, futures);//updatedDummyRecordOwnInstBibIdSet is required only for boundwith
        }
    }

    private List<BibliographicEntity> splitBibWithOneItem(List<BibliographicEntity> bibliographicEntityList) {
        List<BibliographicEntity> splitedBibliographicEntityList = new ArrayList<>();
        for (BibliographicEntity bibliographicEntity : bibliographicEntityList) {
            if (null != bibliographicEntity.getItemEntities() && bibliographicEntity.getItemEntities().size() > 1) {
                if (null != bibliographicEntity.getHoldingsEntities()) {
                    for (HoldingsEntity holdingsEntity : bibliographicEntity.getHoldingsEntities()) {
                        if (null != holdingsEntity.getItemEntities()) {
                            for (ItemEntity itemEntity : holdingsEntity.getItemEntities()) {
                                BibliographicEntity splitedBibliographicEntity = new BibliographicEntity();
                                splitedBibliographicEntity.setOwningInstitutionBibId(bibliographicEntity.getOwningInstitutionBibId());
                                splitedBibliographicEntity.setCatalogingStatus(bibliographicEntity.getCatalogingStatus());
                                splitedBibliographicEntity.setContent(bibliographicEntity.getContent());
                                splitedBibliographicEntity.setOwningInstitutionId(bibliographicEntity.getOwningInstitutionId());
                                splitedBibliographicEntity.setCreatedBy(bibliographicEntity.getCreatedBy());
                                splitedBibliographicEntity.setCreatedDate(bibliographicEntity.getCreatedDate());
                                splitedBibliographicEntity.setLastUpdatedBy(bibliographicEntity.getLastUpdatedBy());
                                splitedBibliographicEntity.setLastUpdatedDate(bibliographicEntity.getLastUpdatedDate());
                                HoldingsEntity splitedHoldingsEntity = new HoldingsEntity();
                                splitedHoldingsEntity.setOwningInstitutionId(holdingsEntity.getOwningInstitutionId());
                                splitedHoldingsEntity.setContent(holdingsEntity.getContent());
                                splitedHoldingsEntity.setOwningInstitutionHoldingsId(holdingsEntity.getOwningInstitutionHoldingsId());
                                splitedHoldingsEntity.setCreatedBy(holdingsEntity.getCreatedBy());
                                splitedHoldingsEntity.setCreatedDate(holdingsEntity.getCreatedDate());
                                splitedHoldingsEntity.setLastUpdatedBy(holdingsEntity.getLastUpdatedBy());
                                splitedHoldingsEntity.setLastUpdatedDate(holdingsEntity.getLastUpdatedDate());
                                splitedHoldingsEntity.setItemEntities(Collections.singletonList(itemEntity));
                                splitedBibliographicEntity.setHoldingsEntities(Collections.singletonList(splitedHoldingsEntity));
                                splitedBibliographicEntity.setItemEntities(Collections.singletonList(itemEntity));
                                splitedBibliographicEntityList.add(splitedBibliographicEntity);
                            }
                        }
                    }
                }
            } else {
                splitedBibliographicEntityList.add(bibliographicEntity);
            }
        }
        return splitedBibliographicEntityList;
    }

    @Override
    public String processSCSB(String inputRecords, Set<Integer> processedBibIds, Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap,
                              List<Map<String, String>> idMapToRemoveIndexList, List<Map<String, String>> bibIdMapToRemoveIndexList, boolean checkLimit, boolean isCGDProtected, InstitutionEntity institutionEntity, Set<String> updatedDummyRecordOwnInstBibIdSet,
                              ExecutorService executorService, List<Future> futures) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String format;
        format = ScsbConstants.FORMAT_SCSB;
        BibRecords bibRecords = null;
        try {
            bibRecords = commonUtil.extractBibRecords(inputRecords);
            log.info("bibrecord size {}", bibRecords.getBibRecordList().size());
            if (checkLimit && bibRecords.getBibRecordList().size() > inputLimit) {
                return ScsbConstants.SUBMIT_COLLECTION_LIMIT_EXCEED_MESSAGE + " " + inputLimit;
            }
        } catch (JAXBException e) {
            log.info(String.valueOf(e.getCause()));
            log.error(ScsbCommonConstants.LOG_ERROR, e);
            return ScsbConstants.INVALID_SCSB_XML_FORMAT_MESSAGE;
        }

        List<BibliographicEntity> validBibliographicEntityList = new ArrayList<>();
        for (BibRecord bibRecord : bibRecords.getBibRecordList()) {
            BibliographicEntity bibliographicEntity = prepareBibliographicEntity(bibRecord, format, submitCollectionReportInfoMap, idMapToRemoveIndexList, isCGDProtected, institutionEntity);
            validBibliographicEntityList.add(bibliographicEntity);
        }
        log.info("Total incoming scsb records for processing--->{}", bibRecords.getBibRecordList().size());
        processConvertedBibliographicEntityFromIncomingRecords(processedBibIds, submitCollectionReportInfoMap, idMapToRemoveIndexList, bibIdMapToRemoveIndexList, institutionEntity, updatedDummyRecordOwnInstBibIdSet, validBibliographicEntityList, executorService, futures);
        stopWatch.stop();
        log.info("Total time take for process SCSB--->{}", stopWatch.getTotalTimeSeconds());
        return null;
    }

    private void prepareBoundWithAndNonBoundWithList(List<BibliographicEntity> validBibliographicEntityList, List<BibliographicEntity> nonBoundWithBibliographicEntityList
            , List<BibliographicEntity> boundwithBibliographicEntityList) {
        List<BarcodeBibliographicEntityObject> barcodeBibliographicEntityObjectList = getBarcodeOwningInstitutionBibIdObjectList(validBibliographicEntityList);
        if(!barcodeBibliographicEntityObjectList.isEmpty()) {
            Map<String, List<BarcodeBibliographicEntityObject>> groupByBarcodeBibliographicEntityObjectMap = groupByBarcodeAndGetBarcodeBibliographicEntityObjectMap(barcodeBibliographicEntityObjectList);

            for (Map.Entry<String, List<BarcodeBibliographicEntityObject>> groupByBarcodeBibliographicEntityObjectMapEntry : groupByBarcodeBibliographicEntityObjectMap.entrySet()) {
                if (groupByBarcodeBibliographicEntityObjectMapEntry.getValue().size() > 1) {
                    for (BarcodeBibliographicEntityObject barcodeBibliographicEntityObject : groupByBarcodeBibliographicEntityObjectMapEntry.getValue()) {
                        boundwithBibliographicEntityList.add(barcodeBibliographicEntityObject.getBibliographicEntity());
                        // logger.info("boundwith barcode--->{}", barcodeBibliographicEntityObject.getBarcode());
                    }
                } else {
                    BibliographicEntity bibliographicEntity = groupByBarcodeBibliographicEntityObjectMapEntry.getValue().get(0).getBibliographicEntity();
                    nonBoundWithBibliographicEntityList.add(bibliographicEntity);
                }
            }
        }
    }

    private List<NonBoundWithBibliographicEntityObject> prepareNonBoundWithBibliographicEntity(Map<String, List<BibliographicEntity>> groupByOwnInstBibIdBibliographicEntityListMap) {
        List<NonBoundWithBibliographicEntityObject> nonBoundWithBibliographicEntityObjectList = new ArrayList<>();
        for (Map.Entry<String, List<BibliographicEntity>> groupByOwnInstBibIdBibliographicEntityListMapEntry : groupByOwnInstBibIdBibliographicEntityListMap.entrySet()) {
            NonBoundWithBibliographicEntityObject nonBoundWithBibliographicEntityObject = new NonBoundWithBibliographicEntityObject();
            nonBoundWithBibliographicEntityObject.setOwningInstitutionBibId(groupByOwnInstBibIdBibliographicEntityListMapEntry.getKey());
            nonBoundWithBibliographicEntityObject.setBibliographicEntityList(groupByOwnInstBibIdBibliographicEntityListMapEntry.getValue());
            nonBoundWithBibliographicEntityObjectList.add(nonBoundWithBibliographicEntityObject);
        }
        return nonBoundWithBibliographicEntityObjectList;
    }

    private List<BoundWithBibliographicEntityObject> prepareBoundWithBibliographicEntityObjectList(Map<String, List<BibliographicEntity>> groupByBarcodeBibliographicEntityListMap) {
        List<BoundWithBibliographicEntityObject> boundWithBibliographicEntityObjectList = new ArrayList<>();
        for (Map.Entry<String, List<BibliographicEntity>> groupByBarcodeBibliographicEntityListMapEntry : groupByBarcodeBibliographicEntityListMap.entrySet()) {
            BoundWithBibliographicEntityObject boundWithBibliographicEntityObject = new BoundWithBibliographicEntityObject();
            boundWithBibliographicEntityObject.setBarcode(groupByBarcodeBibliographicEntityListMapEntry.getKey());
            boundWithBibliographicEntityObject.setBibliographicEntityList(groupByBarcodeBibliographicEntityListMapEntry.getValue());
            boundWithBibliographicEntityObjectList.add(boundWithBibliographicEntityObject);
        }
        return boundWithBibliographicEntityObjectList;
    }

    private BibliographicEntity prepareBibliographicEntity(Object record, String format, Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap, List<Map<String, String>> idMapToRemoveIndexList
            , boolean isCGDProtected, InstitutionEntity institutionEntity) {
        BibliographicEntity incomingBibliographicEntity = null;
        try {
            Map responseMap = getConverter(format).convert(record, institutionEntity);
            StringBuilder errorMessage = (StringBuilder) responseMap.get("errorMessage");
            incomingBibliographicEntity = responseMap.get("bibliographicEntity") != null ? (BibliographicEntity) responseMap.get("bibliographicEntity") : null;
            if (errorMessage != null && errorMessage.length() == 0) {//Valid bibliographic entity is returned for further processing
                setCGDProtectionForItems(incomingBibliographicEntity, isCGDProtected);//TODO need to test cgd protected and customer code for dummy
                if (incomingBibliographicEntity != null) {
                    return incomingBibliographicEntity;
                }
            } else {//Invalid bibliographic entity is added to the failure report
                if (errorMessage != null && errorMessage.length() > 0) {
                    log.error("Error while parsing xml for a barcode in submit collection - {} for Owning Institution Bib Id - {}", errorMessage, incomingBibliographicEntity != null ? incomingBibliographicEntity.getOwningInstitutionBibId() : "");
                    submitCollectionReportHelperService.setSubmitCollectionFailureReportForUnexpectedException(incomingBibliographicEntity,
                            submitCollectionReportInfoMap.get(ScsbConstants.SUBMIT_COLLECTION_FAILURE_LIST), "Failed record - Item not updated - " + errorMessage.toString(), institutionEntity);
                } else {
                    log.error("Error while parsing xml for a barcode in submit collection - for Owning Institution Bib Id - {}", incomingBibliographicEntity != null ? incomingBibliographicEntity.getOwningInstitutionBibId() : "");                    submitCollectionReportHelperService.setSubmitCollectionFailureReportForUnexpectedException(incomingBibliographicEntity,
                            submitCollectionReportInfoMap.get(ScsbConstants.SUBMIT_COLLECTION_FAILURE_LIST), "Failed record - Item not updated - ", institutionEntity);

                }
            }
        } catch (Exception e) {
            log.error("Exception while preparing bibliographic entity");
            log.error(ScsbCommonConstants.LOG_ERROR, e);
        }
        return incomingBibliographicEntity;
    }

    private void processRecordsInBatchesForNonBoundWith(List<NonBoundWithBibliographicEntityObject> nonBoundWithBibliographicEntityObjectList, Integer owningInstitutionId, Map<String,
            List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap, Set<Integer> processedBibIds, List<Map<String, String>> idMapToRemoveIndexList, ExecutorService executorService, List<Future> futures) {
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<List<NonBoundWithBibliographicEntityObject>> nonBoundWithBibliographicEntityPartitionList = ListUtils.partition(nonBoundWithBibliographicEntityObjectList, partitionSize);
        log.info("Total non bound-with batch count--->{}", nonBoundWithBibliographicEntityPartitionList.size());
        List<BibliographicEntity> updatedBibliographicEntityToSaveList = new ArrayList<>();
        int batchCounter = 1;
        for (List<NonBoundWithBibliographicEntityObject> nonBoundWithBibliographicEntityObjectListToProces : nonBoundWithBibliographicEntityPartitionList) {
            log.info("nonBoundWithBibliographicEntityObjectListToProces.size---->{}", nonBoundWithBibliographicEntityObjectListToProces.size());
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            log.info("Processing non bound-with batch no. ---->{}", batchCounter);
            List<BibliographicEntity> updatedBibliographicEntityList = null;
            updatedBibliographicEntityList = getSubmitCollectionDAOService().updateBibliographicEntityInBatchForNonBoundWith(nonBoundWithBibliographicEntityObjectListToProces
                    , owningInstitutionId, submitCollectionReportInfoMap, processedBibIds, idMapToRemoveIndexList, processedBarcodeSetForDummyRecords, executorService, futures);
            if (updatedBibliographicEntityList != null && !updatedBibliographicEntityList.isEmpty()) {
                updatedBibliographicEntityToSaveList.addAll(updatedBibliographicEntityList);
            }
            stopWatch.stop();
            log.info("Time taken to process and save {} non bound-with records batch--->{}", partitionSize, stopWatch.getTotalTimeSeconds());
            batchCounter++;
        }
    }

    private void processRecordsInBatchesForBoundWith(List<BoundWithBibliographicEntityObject> boundWithBibliographicEntityObjectList, Integer owningInstitutionId, Map<String,
            List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap, Set<Integer> processedBibIds, List<Map<String, String>> idMapToRemoveIndexList, List<Map<String, String>> bibIdMapToRemoveIndexList, Set<String> updatedDummyRecordOwnInstBibIdSet,
                                                     ExecutorService executorService, List<Future> futures) {

        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<List<BoundWithBibliographicEntityObject>> boundWithBibliographicEntityObjectPartitionList = ListUtils.partition(boundWithBibliographicEntityObjectList, partitionSize);
        log.info("Total bound-with batch count--->{}", boundWithBibliographicEntityObjectPartitionList.size());
        List<BibliographicEntity> updatedBibliographicEntityToSaveList = new ArrayList<>();
        int batchCounter = 1;
        for (List<BoundWithBibliographicEntityObject> boundWithBibliographicEntityObjectToProcess : boundWithBibliographicEntityObjectPartitionList) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            log.info("boundWithBibliographicEntityObjectToProcess.size---->{}", boundWithBibliographicEntityObjectToProcess.size());
            log.info("Processing bound-with batch no. ---->{}", batchCounter);
            List<BibliographicEntity> updatedBibliographicEntityList = null;
            updatedBibliographicEntityList = getSubmitCollectionDAOService().updateBibliographicEntityInBatchForBoundWith(boundWithBibliographicEntityObjectToProcess, owningInstitutionId, submitCollectionReportInfoMap, processedBibIds, idMapToRemoveIndexList, bibIdMapToRemoveIndexList, processedBarcodeSetForDummyRecords, executorService, futures);
            if (updatedBibliographicEntityList != null && !updatedBibliographicEntityList.isEmpty()) {
                updatedBibliographicEntityToSaveList.addAll(updatedBibliographicEntityList);
            }
            setUpdatedDummyRecordOwningInstBibId(updatedBibliographicEntityList, updatedDummyRecordOwnInstBibIdSet);
            stopWatch.stop();
            log.info("Time taken to process and save {} bound-with records batch--->{}", partitionSize, stopWatch.getTotalTimeSeconds());
            log.info("Total updatedDummyRecordOwnInstBibIdSet size--->{}", updatedDummyRecordOwnInstBibIdSet.size());
            batchCounter++;
        }
    }

    private void setUpdatedDummyRecordOwningInstBibId(List<BibliographicEntity> bibliographicEntityList, Set<String> updatedDummyRecordOwnInstBibIdSet) {
        for (BibliographicEntity bibliographicEntity : bibliographicEntityList) {
            if (bibliographicEntity.getId() == null) {
                updatedDummyRecordOwnInstBibIdSet.add(bibliographicEntity.getOwningInstitutionBibId());
            }
        }
    }

    private List<BarcodeBibliographicEntityObject> getBarcodeOwningInstitutionBibIdObjectList(List<BibliographicEntity> bibliographicEntityList) {
        List<BarcodeBibliographicEntityObject> barcodeOwningInstitutionBibIdObjectList = new ArrayList<>();
        for (BibliographicEntity bibliographicEntity : bibliographicEntityList) {
            if (null != bibliographicEntity.getItemEntities() && !bibliographicEntity.getItemEntities().isEmpty()) {
                for (ItemEntity itemEntity : bibliographicEntity.getItemEntities()) {
                    if(itemEntity.getBarcode() != null) {
                        BarcodeBibliographicEntityObject barcodeOwningInstitutionBibIdObject = new BarcodeBibliographicEntityObject();
                        barcodeOwningInstitutionBibIdObject.setBarcode(itemEntity.getBarcode());
                        barcodeOwningInstitutionBibIdObject.setOwningInstitutionBibId(bibliographicEntity.getOwningInstitutionBibId());
                        barcodeOwningInstitutionBibIdObject.setBibliographicEntity(bibliographicEntity);
                        barcodeOwningInstitutionBibIdObjectList.add(barcodeOwningInstitutionBibIdObject);
                    }
                }
            }
        }
        return barcodeOwningInstitutionBibIdObjectList;
    }

    private Map<String, List<BarcodeBibliographicEntityObject>> groupByBarcodeAndGetBarcodeBibliographicEntityObjectMap(List<BarcodeBibliographicEntityObject> barcodeOwningInstitutionBibIdObjectList) {
        return barcodeOwningInstitutionBibIdObjectList.stream()
                .collect(Collectors.groupingBy(BarcodeBibliographicEntityObject::getBarcode));
    }

    private Map<String, List<BibliographicEntity>> groupByOwnInstBibIdBibliographicEntityListMap(List<BibliographicEntity> bibliographicEntityList) {
        return bibliographicEntityList.stream()
                .collect(Collectors.groupingBy(BibliographicEntity::getOwningInstitutionBibId));
    }

    private Map<String, List<BibliographicEntity>> groupByBarcodeBibliographicEntityListMap(List<BibliographicEntity> bibliographicEntityList) {
        Map<String, List<BibliographicEntity>> groupByBarcodeBibliographicEntityListMap = new HashedMap();
        for (BibliographicEntity bibliographicEntity : bibliographicEntityList) {
            List<BibliographicEntity> addedBibliographicEntityList = groupByBarcodeBibliographicEntityListMap.get(bibliographicEntity.getItemEntities().get(0).getBarcode());
            if (addedBibliographicEntityList != null) {
                List<BibliographicEntity> updatedBibliographicEntityList = new ArrayList<>(addedBibliographicEntityList);
                updatedBibliographicEntityList.add(bibliographicEntity);
                groupByBarcodeBibliographicEntityListMap.put(bibliographicEntity.getItemEntities().get(0).getBarcode(), updatedBibliographicEntityList);
            } else {
                groupByBarcodeBibliographicEntityListMap.put(bibliographicEntity.getItemEntities().get(0).getBarcode(), Collections.singletonList(bibliographicEntity));
            }
        }
        return groupByBarcodeBibliographicEntityListMap;
    }
}
