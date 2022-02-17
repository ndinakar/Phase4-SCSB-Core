package org.recap.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.marc4j.marc.Record;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.submitcollection.BibMatchPointInfo;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.accession.AccessionResponse;
import org.recap.model.gfa.ScsbLasItemStatusCheckModel;
import org.recap.model.jaxb.marc.BibRecords;
import org.recap.model.jpa.*;
import org.recap.model.report.SubmitCollectionReportInfo;
import org.recap.repository.jpa.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
@Slf4j
@Service
public class CommonUtil {



    private Map<String, Integer> itemStatusMap;
    private Map<String, Integer> collectionGroupMap;
    private Map<String, Integer> institutionEntityMap;

    @Autowired
    private ItemStatusDetailsRepository itemStatusDetailsRepository;

    @Autowired
    private CollectionGroupDetailsRepository collectionGroupDetailsRepository;

    @Autowired
    private InstitutionDetailsRepository institutionDetailsRepository;

    @Autowired
    private ItemDetailsRepository itemDetailsRepository;

    @Autowired
    ItemChangeLogDetailsRepository itemChangeLogDetailsRepository;

    @Autowired
    BibliographicDetailsRepository bibliographicDetailsRepository;

    @Autowired
    MarcUtil marcUtil;

    @Autowired
    AccessionUtil accessionUtil;

    @Autowired
    private PropertyUtil propertyUtil;

    @Autowired
    private BibJSONUtil bibJSONUtil;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${" + PropertyKeyConstants.SCSB_SUPPORT_INSTITUTION + "}")
    private String supportInstitution;

    @Value("${" + PropertyKeyConstants.NONHOLDINGID_INSTITUTION + "}")
    private String nonHoldingIdInstitution;

    @Value("${" + PropertyKeyConstants.SCSB_SOLR_DOC_URL + "}")
    private String scsbSolrClientUrl;

    /**
     * This method builds Holdings Entity from holdings content
     * @param bibliographicEntity
     * @param currentDate
     * @param errorMessage
     * @param holdingsContent
     * @return
     */
    public HoldingsEntity buildHoldingsEntity(BibliographicEntity bibliographicEntity, Date currentDate, StringBuilder errorMessage, String holdingsContent,String processName) {
        HoldingsEntity holdingsEntity = new HoldingsEntity();
        if (StringUtils.isNotBlank(holdingsContent)) {
            holdingsEntity.setContent(holdingsContent.getBytes());
        } else {
            errorMessage.append(" Holdings Content cannot be empty");
        }
        holdingsEntity.setCreatedDate(currentDate);
        holdingsEntity.setCreatedBy(processName);
        holdingsEntity.setLastUpdatedDate(currentDate);
        holdingsEntity.setLastUpdatedBy(processName);
        Integer owningInstitutionId = bibliographicEntity.getOwningInstitutionId();
        holdingsEntity.setOwningInstitutionId(owningInstitutionId);
        return holdingsEntity;
    }

    public void buildSubmitCollectionReportInfoAndAddFailures(BibliographicEntity fetchedBibliographicEntity, List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList, String owningInstitution, Map.Entry<String, Map<String, ItemEntity>> incomingHoldingItemMapEntry, ItemEntity incomingItemEntity) {
        SubmitCollectionReportInfo submitCollectionReportInfo = new SubmitCollectionReportInfo();
        submitCollectionReportInfo.setItemBarcode(incomingItemEntity.getBarcode());
        submitCollectionReportInfo.setCustomerCode(incomingItemEntity.getCustomerCode());
        submitCollectionReportInfo.setOwningInstitution(owningInstitution);
        String existingOwningInstitutionHoldingsId = getExistingItemEntityOwningInstItemId(fetchedBibliographicEntity,incomingItemEntity);
        submitCollectionReportInfo.setMessage(ScsbConstants.SUBMIT_COLLECTION_FAILED_RECORD+" - Owning institution holdings id mismatch - incoming owning institution holdings id " +incomingHoldingItemMapEntry.getKey()+ ", existing owning institution item id "+incomingItemEntity.getOwningInstitutionItemId()
                +", existing owning institution holdings id "+existingOwningInstitutionHoldingsId+", existing owning institution bib id "+fetchedBibliographicEntity.getOwningInstitutionBibId());
        failureSubmitCollectionReportInfoList.add(submitCollectionReportInfo);
    }

    public void buildSubmitCollectionReportInfoWhenNoGroupIdAndAddFailures(BibliographicEntity incomingBibliographicEntity, List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList, String owningInstitution, ItemEntity incomingItemEntity) {
        SubmitCollectionReportInfo submitCollectionReportInfo = new SubmitCollectionReportInfo();
        submitCollectionReportInfo.setItemBarcode(incomingItemEntity.getBarcode());
        submitCollectionReportInfo.setCustomerCode(incomingItemEntity.getCustomerCode());
        submitCollectionReportInfo.setOwningInstitution(owningInstitution);
        submitCollectionReportInfo.setMessage(ScsbConstants.SUBMIT_COLLECTION_FAILED_RECORD+" - "+"Unable to update dummy record, CGD is unavailable in the incoming xml record - incoming owning institution bib id - "+incomingBibliographicEntity.getOwningInstitutionBibId()
                +", incoming owning institution item id - "+incomingItemEntity.getOwningInstitutionItemId());
        failureSubmitCollectionReportInfoList.add(submitCollectionReportInfo);
    }

    private String getExistingItemEntityOwningInstItemId(BibliographicEntity fetchedBibliographicEntity,ItemEntity incomingItemEntity){
        for(ItemEntity fetchedItemEntity:fetchedBibliographicEntity.getItemEntities()){
            if(fetchedItemEntity.getOwningInstitutionItemId().equals(incomingItemEntity.getOwningInstitutionItemId())){
                return fetchedItemEntity.getHoldingsEntities().get(0).getOwningInstitutionHoldingsId();
            }
        }
        return "";
    }

    public void addItemAndReportEntities(List<ItemEntity> itemEntities, List<ReportEntity> reportEntities, boolean processHoldings, HoldingsEntity holdingsEntity, Map<String, Object> itemMap) {
        ItemEntity itemEntity = (ItemEntity) itemMap.get("itemEntity");
        ReportEntity itemReportEntity = (ReportEntity) itemMap.get("itemReportEntity");
        if (itemReportEntity != null) {
            reportEntities.add(itemReportEntity);
        } else if (processHoldings) {
            if (holdingsEntity.getItemEntities() == null) {
                holdingsEntity.setItemEntities(new ArrayList<>());
            }
            holdingsEntity.getItemEntities().add(itemEntity);
            itemEntities.add(itemEntity);
        }
    }

    /**
     * Add Holdings Entity to Map
     * @param map
     * @param holdingsEntity
     * @param owningInstitutionHoldingsId
     * @return
     */
    public Map<String, Object> addHoldingsEntityToMap(Map<String, Object> map, HoldingsEntity holdingsEntity, String owningInstitutionHoldingsId) {
        if (StringUtils.isBlank(owningInstitutionHoldingsId) || owningInstitutionHoldingsId.length() > 100) {
            owningInstitutionHoldingsId = UUID.randomUUID().toString();
        }
        holdingsEntity.setOwningInstitutionHoldingsId(owningInstitutionHoldingsId);
        map.put("holdingsEntity", holdingsEntity);
        return map;
    }

    /**
     * Gets item status map.
     *
     * @return the item status map
     */
    public Map<String, Integer> getItemStatusMap() {
        if (null == itemStatusMap) {
            itemStatusMap = new HashMap<>();
            try {
                Iterable<ItemStatusEntity> itemStatusEntities = itemStatusDetailsRepository.findAll();
                for (Iterator<ItemStatusEntity> iterator = itemStatusEntities.iterator(); iterator.hasNext(); ) {
                    ItemStatusEntity itemStatusEntity = iterator.next();
                    itemStatusMap.put(itemStatusEntity.getStatusCode(), itemStatusEntity.getId());
                }
            } catch (Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR,e);
            }
        }
        return itemStatusMap;
    }

    /**
     * Gets collection group map.
     *
     * @return the collection group map
     */
    public Map<String, Integer> getCollectionGroupMap() {
        if (null == collectionGroupMap) {
            collectionGroupMap = new HashMap<>();
            try {
                Iterable<CollectionGroupEntity> collectionGroupEntities = collectionGroupDetailsRepository.findAll();
                for (Iterator<CollectionGroupEntity> iterator = collectionGroupEntities.iterator(); iterator.hasNext(); ) {
                    CollectionGroupEntity collectionGroupEntity = iterator.next();
                    collectionGroupMap.put(collectionGroupEntity.getCollectionGroupCode(), collectionGroupEntity.getId());
                }
            } catch (Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR,e);
            }
        }
        return collectionGroupMap;
    }

    /**
     * Gets institution entity map.
     *
     * @return the institution entity map
     */
    public Map<String, Integer> getInstitutionEntityMap() {
        if (null == institutionEntityMap) {
            institutionEntityMap = new HashMap<>();
            try {
                Iterable<InstitutionEntity> institutionEntities = institutionDetailsRepository.findAll();
                for (Iterator<InstitutionEntity> iterator = institutionEntities.iterator(); iterator.hasNext(); ) {
                    InstitutionEntity institutionEntity = iterator.next();
                    institutionEntityMap.put(institutionEntity.getInstitutionCode(), institutionEntity.getId());
                }
            } catch (Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR,e);
            }
        }
        return institutionEntityMap;
    }

    /**
     * Rollback update item availabiluty status.
     *
     * @param itemEntity the item entity
     * @param userName   the user name
     */
    public void rollbackUpdateItemAvailabilityStatus(ItemEntity itemEntity, String userName) {
        ItemStatusEntity itemStatusEntity = itemStatusDetailsRepository.findByStatusCode(ScsbCommonConstants.AVAILABLE);
        itemEntity.setItemAvailabilityStatusId(itemStatusEntity.getId()); // Available
        itemEntity.setLastUpdatedBy(getUser(userName));
        itemDetailsRepository.save(itemEntity);
        saveItemChangeLogEntity(itemEntity.getId(), getUser(userName), ScsbConstants.REQUEST_ITEM_AVAILABILITY_STATUS_UPDATE, ScsbConstants.REQUEST_ITEM_AVAILABILITY_STATUS_DATA_ROLLBACK);
    }

    /**
     * Save item change log entity.
     *
     * @param recordId      the record id
     * @param userName      the user name
     * @param operationType the operation type
     * @param notes         the notes
     */
    public void saveItemChangeLogEntity(Integer recordId, String userName, String operationType, String notes) {
        ItemChangeLogEntity itemChangeLogEntity = new ItemChangeLogEntity();
        itemChangeLogEntity.setUpdatedBy(userName);
        itemChangeLogEntity.setUpdatedDate(new Date());
        itemChangeLogEntity.setOperationType(operationType);
        itemChangeLogEntity.setRecordId(recordId);
        itemChangeLogEntity.setNotes(notes);
        itemChangeLogDetailsRepository.save(itemChangeLogEntity);
    }

    /**
     * Gets user.
     *
     * @param userId the user id
     * @return the user
     */
    public String getUser(String userId) {
        if (StringUtils.isBlank(userId)) {
            return "Discovery";
        } else {
            return userId;
        }
    }

    /**
     * This method gets input string where the input will be in  SCSB xml format, unmarshals and maps to BibRecords and then returns BibRecords
     * @param unmarshal - this the input xml which will be in SCSB xml format
     * @return BibRecords
     */
    public BibRecords getBibRecordsForSCSBFormat(String unmarshal) {
        BibRecords bibRecords = null;
        try {
             bibRecords = extractBibRecords(unmarshal);
        } catch (JAXBException e) {
            log.error(ScsbCommonConstants.LOG_ERROR,e);
        }
        return bibRecords;
    }

    public Object marcRecordConvert(String bibDataResponse) {
        List<Record> records = new ArrayList<>();
        if (StringUtils.isNotBlank(bibDataResponse)) {
            records = marcUtil.readMarcXml(bibDataResponse);
        }
        return records;
    }

    public BibRecords extractBibRecords(String inputRecords) throws JAXBException {
        BibRecords bibRecords;
        JAXBContext context = JAXBContext.newInstance(BibRecords.class);
        XMLInputFactory xif = XMLInputFactory.newFactory();
        xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        xif.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        xif.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        InputStream stream = new ByteArrayInputStream(inputRecords.getBytes(StandardCharsets.UTF_8));
        XMLStreamReader xsr = null;
        try {
            xsr = xif.createXMLStreamReader(stream);
        } catch (XMLStreamException e) {
            log.error(e.getMessage());
        }
        Unmarshaller um = context.createUnmarshaller();
        bibRecords = (BibRecords) um.unmarshal(xsr);
        log.info("bibrecord size {}", bibRecords.getBibRecordList().size());
        return bibRecords;
    }

    public String getUpdatedDataResponse(Set<AccessionResponse> accessionResponsesList, List<Map<String, String>> responseMapList, String owningInstitution, List<ReportDataEntity> reportDataEntityList, AccessionRequest accessionRequest, boolean isValidBoundWithRecord, int count, Object record, ImsLocationEntity imsLocationEntity) {
        String response;
        boolean isFirstRecord = false;
        if (count == 1) {
            isFirstRecord = true;
        }
        response = accessionUtil.updateData(record, owningInstitution, responseMapList, accessionRequest, isValidBoundWithRecord, isFirstRecord,imsLocationEntity);
        accessionUtil.setAccessionResponse(accessionResponsesList, accessionRequest.getItemBarcode(), response);
        reportDataEntityList.addAll(accessionUtil.createReportDataEntityList(accessionRequest, response));
        return response;
    }

    public StringBuilder getContentByFileName(String vmFileName) {
        InputStream inputStream = getClass().getResourceAsStream(vmFileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder out = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    out.append(line);
                }
                out.append("\n");
            }
        } catch (IOException e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e);
        }
        return out;
    }

    /**
     * Get list of barcodes from item entities
     * @param itemEntities
     * @return
     */
    public List<String> getBarcodesList(List<ItemEntity> itemEntities) {
        List<String> itemBarcodes = new ArrayList<>();
        if (!itemEntities.isEmpty()) {
            for (ItemEntity itemEntity : itemEntities) {
                itemBarcodes.add(itemEntity.getBarcode());
            }
        }
        return itemBarcodes;
    }

    /**
     * Get list of SCSB Item Check Models from item entities
     * @param itemEntities ItemEntities
     * @return List of ScsbLasItemStatusCheckModel
     */
    public List<ScsbLasItemStatusCheckModel> getScsbItemStatusModelListByItemEntities(List<ItemEntity> itemEntities) {
        List<ScsbLasItemStatusCheckModel> itemStatusCheckModelList = new ArrayList<>();
        if (!itemEntities.isEmpty()) {
            for (ItemEntity itemEntity : itemEntities) {
                ScsbLasItemStatusCheckModel itemStatusCheckModel = new ScsbLasItemStatusCheckModel();
                itemStatusCheckModel.setItemBarcode(itemEntity.getBarcode());
                itemStatusCheckModel.setImsLocation(itemEntity.getImsLocationEntity().getImsLocationCode());
                itemStatusCheckModelList.add(itemStatusCheckModel);
            }
        }
        return itemStatusCheckModelList;
    }

    /**
     * Get All Institution Codes Except Support Institution
     * @return institutionCodes
     */
    public List<String> findAllInstitutionCodesExceptSupportInstitution() {
        return institutionDetailsRepository.findAllInstitutionCodesExceptSupportInstitution(supportInstitution);
    }

    /**
     * Get All Institution Codes Except Support Institution
     * @return institutionCodes
     */
    public List<InstitutionEntity> findAllInstitutionsExceptSupportInstitution() {
        return institutionDetailsRepository.findAllInstitutionsExceptSupportInstitution(supportInstitution);
    }

    /**
     * Checks if the IMS item status is available or not available
     * @param imsLocationCode IMS Location Code
     * @param imsItemStatus IMS Item Status
     * @param checkAvailable Check Available
     * @return boolean
     */
    public boolean checkIfImsItemStatusIsAvailableOrNotAvailable(String imsLocationCode, String imsItemStatus, boolean checkAvailable) {
        String propertyKey = checkAvailable ? PropertyKeyConstants.IMS.IMS_AVAILABLE_ITEM_STATUS_CODES : PropertyKeyConstants.IMS.IMS_NOT_AVAILABLE_ITEM_STATUS_CODES;
        String imsItemStatusCodes = propertyUtil.getPropertyByImsLocationAndKey(imsLocationCode, propertyKey);
        return StringUtils.startsWithAny(imsItemStatus, imsItemStatusCodes.split(","));
    }

    /**
     * Checks if the IMS item status is requestable but not retrievable (In first scan)
     * @param imsLocationCode IMS Location Code
     * @param imsItemStatus IMS Item Status
     * @return boolean
     */
    public boolean checkIfImsItemStatusIsRequestableNotRetrievable(String imsLocationCode, String imsItemStatus) {
        String imsItemStatusCodes = propertyUtil.getPropertyByImsLocationAndKey(imsLocationCode, PropertyKeyConstants.IMS.IMS_REQUESTABLE_NOT_RETRIEVABLE_ITEM_STATUS_CODES);
        return StringUtils.isNotBlank(imsItemStatusCodes) && StringUtils.startsWithAny(imsItemStatus, imsItemStatusCodes.split(","));
    }

    public boolean checkIfMatchPointsChanged(String incomingMarcXml, String existingMarcXml, String institutionCode) {
        return !compareMatchPointsByMarcXml(incomingMarcXml, existingMarcXml, institutionCode);
    }

    public boolean compareMatchPointsByMarcXml(String incomingMarcXml, String existingMarcXml, String institutionCode) {
        List<Record> incomingMarcRecords = marcUtil.convertMarcXmlToRecord(incomingMarcXml);
        List<Record> existingMarcRecords = marcUtil.convertMarcXmlToRecord(existingMarcXml);
       return compareMatchPoints(incomingMarcRecords.get(0), existingMarcRecords.get(0), institutionCode);
    }

    public boolean compareMatchPoints(Record incomingMarcRecord, Record existingMarcRecord, String institutionCode) {
        BibMatchPointInfo incomingBibMatchPointInfo = getBibMatchPointInfoForMarcRecord(incomingMarcRecord, institutionCode);
        BibMatchPointInfo existingBibMatchPointInfo = getBibMatchPointInfoForMarcRecord(existingMarcRecord, institutionCode);
        return incomingBibMatchPointInfo.equals(existingBibMatchPointInfo);
    }

    public BibMatchPointInfo getBibMatchPointInfoForMarcRecord(Record marcRecord, String institutionCode) {
        BibMatchPointInfo bibMatchPointInfo = new BibMatchPointInfo();
        bibJSONUtil.setNonHoldingInstitutions(Arrays.asList(nonHoldingIdInstitution.split(",")));
        bibMatchPointInfo.setTitle(bibJSONUtil.getTitle(marcRecord));
        bibMatchPointInfo.setLccn(bibJSONUtil.getLCCNValue(marcRecord));
        bibMatchPointInfo.setIsbn(bibJSONUtil.getISBNNumber(marcRecord));
        bibMatchPointInfo.setIssn(bibJSONUtil.getISSNNumber(marcRecord));
        bibMatchPointInfo.setOclc(bibJSONUtil.getOCLCNumbers(marcRecord, institutionCode));
        return bibMatchPointInfo;
    }

    public boolean isCgdChangedToShared(Map<String, ItemEntity> fetchedBarcodeItemEntityMap, Map<String, ItemEntity> incomingBarcodeItemEntityMap, Map<Integer, String> collectionGroupIdCodeMap, Map<Integer, String> itemStatusIdCodeMap, boolean checkExisting) {
        boolean isCgdChangedToShared = false;
        for (Map.Entry<String, ItemEntity> incomingBarcodeItemEntityMapEntry : incomingBarcodeItemEntityMap.entrySet()) {
            ItemEntity incomingItemEntity = incomingBarcodeItemEntityMapEntry.getValue();
            ItemEntity fetchedItemEntity = fetchedBarcodeItemEntityMap.get(incomingBarcodeItemEntityMapEntry.getKey());
            if (fetchedItemEntity != null && fetchedItemEntity.getOwningInstitutionItemId().equalsIgnoreCase(incomingItemEntity.getOwningInstitutionItemId()) && fetchedItemEntity.getBarcode().equals(incomingItemEntity.getBarcode()) && !fetchedItemEntity.isDeleted()) {
                Integer fetchedCgdId = null != fetchedItemEntity.getCollectionGroupEntity() ? fetchedItemEntity.getCollectionGroupEntity().getId() : fetchedItemEntity.getCollectionGroupId();
                Integer incomingCgdId = null != incomingItemEntity.getCollectionGroupEntity() ? incomingItemEntity.getCollectionGroupEntity().getId() : incomingItemEntity.getCollectionGroupId();
                String itemStatusCode = itemStatusIdCodeMap.get(fetchedItemEntity.getItemAvailabilityStatusId());
                boolean isItemAvailable = ScsbConstants.ITEM_STATUS_AVAILABLE.equalsIgnoreCase(itemStatusCode);
                if (fetchedCgdId != null && incomingCgdId != null) {
                    String fetchedCgdCode = collectionGroupIdCodeMap.get(fetchedItemEntity.getCollectionGroupId());
                    String incomingCgdCode = collectionGroupIdCodeMap.get(incomingItemEntity.getCollectionGroupId());
                    if ((checkExisting && ScsbCommonConstants.SHARED_CGD.equalsIgnoreCase(fetchedCgdCode))
                            || (!checkExisting && isItemAvailable && fetchedCgdId.intValue() != incomingCgdId.intValue() && ScsbCommonConstants.SHARED_CGD.equalsIgnoreCase(incomingCgdCode))) {
                        isCgdChangedToShared = true;
                        break;
                    }
                }
            }
        }
        return isCgdChangedToShared;
    }

    public boolean isCgdAlreadyShared(Map<String, ItemEntity> fetchedBarcodeItemEntityMap, Map<String, ItemEntity> incomingBarcodeItemEntityMap, Map<Integer, String> collectionGroupIdCodeMap, Map<Integer, String> itemStatusIdCodeMap) {
       return isCgdChangedToShared(fetchedBarcodeItemEntityMap, incomingBarcodeItemEntityMap, collectionGroupIdCodeMap, itemStatusIdCodeMap, true);
    }

    public void collectSharedAndNonSharedBibIdsForMatchingId(Set<Integer> sharedBibIds, Set<Integer> nonSharedBibIds, String matchingIdentifier, Map<Integer, String> collectionGroupIdCodeMap) {
        List<Object[]> bibIdAndCgdIdByMatchingIdentityObjectList = bibliographicDetailsRepository.findBibIdAndCgdIdByMatchingIdentity(matchingIdentifier);
        if (!bibIdAndCgdIdByMatchingIdentityObjectList.isEmpty()) {
            Map<Integer, Set<String>> mapWithBibIdAndCgdCodes = getMapWithBibIdAndCgdCodes(bibIdAndCgdIdByMatchingIdentityObjectList, collectionGroupIdCodeMap);
            for (Map.Entry<Integer, Set<String>> bibIdAndCgdCodes : mapWithBibIdAndCgdCodes.entrySet()) {
                Integer bibId = bibIdAndCgdCodes.getKey();
                Set<String> cgdCodes = bibIdAndCgdCodes.getValue();
                if (cgdCodes.contains(ScsbCommonConstants.SHARED_CGD)) {
                    sharedBibIds.add(bibId);
                } else {
                    nonSharedBibIds.add(bibId);
                }
            }
        }
    }

    private Map<Integer, Set<String>> getMapWithBibIdAndCgdCodes(List<Object[]> bibIdAndCgdIdByMatchingIdentityObjectList, Map<Integer, String> collectionGroupIdCodeMap) {
        Map<Integer, Set<String>> bibIdAndCgdCodesMap = new HashMap<>();
        for (Object[] bibIdAndCgdIdObj : bibIdAndCgdIdByMatchingIdentityObjectList) {
            Integer bibId = Integer.parseInt(bibIdAndCgdIdObj[0].toString());
            String collectionGroupCode = null;
            if (bibIdAndCgdIdObj.length > 1) {
                collectionGroupCode = collectionGroupIdCodeMap.get(Integer.parseInt(bibIdAndCgdIdObj[1].toString()));
            }
            if (bibIdAndCgdCodesMap.containsKey(bibId)) {
                bibIdAndCgdCodesMap.get(bibId).add(collectionGroupCode);
            } else {
                Set<String> cgdCodes = new HashSet<>();
                cgdCodes.add(collectionGroupCode);
                bibIdAndCgdCodesMap.put(bibId, cgdCodes);
            }
        }
        return bibIdAndCgdCodesMap;
    }

    public Set<Integer> collectFuturesAndUpdateMAQualifier(List<Future> futures) {
        Set<Integer> bibIds = new HashSet<>();
        Set<Integer> allBibIdsToResetAndSetQualifierTo1 = new HashSet<>();
        Set<Integer> allBibIdsToSetQualifierTo2 = new HashSet<>();
        Set<Integer> allBibIdsToResetAndSetQualifierTo3 = new HashSet<>();
        for (Future future : futures) {
            try {
                Map<Integer, Set<Integer>> responseMap = (Map<Integer, Set<Integer>>) future.get();
                if (!responseMap.isEmpty()) {
                    Set<Integer> bibIdsToResetAndSetQualifierTo1 = responseMap.get(ScsbCommonConstants.MA_QUALIFIER_1);
                    Set<Integer> bibIdsToSetQualifierTo2 = responseMap.get(ScsbCommonConstants.MA_QUALIFIER_2);
                    Set<Integer> bibIdsToResetAndSetQualifierTo3 = responseMap.get(ScsbCommonConstants.MA_QUALIFIER_3);
                    if (bibIdsToResetAndSetQualifierTo1 != null) {
                        allBibIdsToResetAndSetQualifierTo1.addAll(bibIdsToResetAndSetQualifierTo1);
                        bibIds.addAll(bibIdsToResetAndSetQualifierTo1);
                    }
                    if (bibIdsToSetQualifierTo2 != null) {
                        allBibIdsToSetQualifierTo2.addAll(bibIdsToSetQualifierTo2);
                        bibIds.addAll(bibIdsToSetQualifierTo2);
                    }
                    if (bibIdsToResetAndSetQualifierTo3 != null) {
                        allBibIdsToResetAndSetQualifierTo3.addAll(bibIdsToResetAndSetQualifierTo3);
                        bibIds.addAll(bibIdsToResetAndSetQualifierTo3);
                    }
                }
            } catch (Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR, e);
            }
        }
        log.info("Total Number of Bib Ids Collected for MA Qualifier Update: {}", bibIds.size());
        updateMAQualifierByBibIdSets(allBibIdsToResetAndSetQualifierTo1, allBibIdsToSetQualifierTo2, allBibIdsToResetAndSetQualifierTo3);
        return bibIds;
    }

    private void updateMAQualifierByBibIdSets(Set<Integer> allBibIdsToResetAndSetQualifierTo1, Set<Integer> allBibIdsToSetQualifierTo2, Set<Integer> allBibIdsToResetAndSetQualifierTo3) {
        Set<Integer> duplicateBibIds = allBibIdsToResetAndSetQualifierTo1.stream().filter(allBibIdsToSetQualifierTo2::contains).collect(Collectors.toSet());
        if (!duplicateBibIds.isEmpty()) {
            log.info("{} Duplicate Bib Ids between MA Qualifier 1 and 2 moved to 3: {}", duplicateBibIds.size(), duplicateBibIds);
            allBibIdsToResetAndSetQualifierTo1.removeAll(duplicateBibIds);
            allBibIdsToSetQualifierTo2.removeAll(duplicateBibIds);
            allBibIdsToResetAndSetQualifierTo3.addAll(duplicateBibIds);
        }

        duplicateBibIds = allBibIdsToSetQualifierTo2.stream().filter(allBibIdsToResetAndSetQualifierTo3::contains).collect(Collectors.toSet());
        if (!duplicateBibIds.isEmpty()) {
            log.info("{} Duplicate Bib Ids between MA Qualifier 2 and 3 removed from 2: {}", duplicateBibIds.size(), duplicateBibIds);
            allBibIdsToSetQualifierTo2.removeAll(duplicateBibIds);
        }

        duplicateBibIds = allBibIdsToResetAndSetQualifierTo1.stream().filter(allBibIdsToResetAndSetQualifierTo3::contains).collect(Collectors.toSet());
        if (!duplicateBibIds.isEmpty()) {
            log.info("{} Duplicate Bib Ids between MA Qualifier 1 and 3 removed from 1: {}", duplicateBibIds.size(), duplicateBibIds);
            allBibIdsToResetAndSetQualifierTo1.removeAll(duplicateBibIds);
        }

        if (!allBibIdsToResetAndSetQualifierTo1.isEmpty()) {
            int countOfUpdatedTo1 = bibliographicDetailsRepository.resetMatchingColumnsAndUpdateMaQualifier(allBibIdsToResetAndSetQualifierTo1, ScsbCommonConstants.MA_QUALIFIER_1);
            log.info(ScsbConstants.LOG_MA_QUALIFIER_UPDATE, ScsbCommonConstants.MA_QUALIFIER_1, countOfUpdatedTo1);
        }
        if (!allBibIdsToSetQualifierTo2.isEmpty()) {
            int countOfUpdatedTo2 = bibliographicDetailsRepository.updateMaQualifier(allBibIdsToSetQualifierTo2, ScsbCommonConstants.MA_QUALIFIER_2);
            log.info(ScsbConstants.LOG_MA_QUALIFIER_UPDATE, ScsbCommonConstants.MA_QUALIFIER_2, countOfUpdatedTo2);
        }
        if (!allBibIdsToResetAndSetQualifierTo3.isEmpty()) {
            int countOfUpdatedTo3 = bibliographicDetailsRepository.resetMatchingColumnsAndUpdateMaQualifier(allBibIdsToResetAndSetQualifierTo3, ScsbCommonConstants.MA_QUALIFIER_3);
            log.info(ScsbConstants.LOG_MA_QUALIFIER_UPDATE, ScsbCommonConstants.MA_QUALIFIER_3, countOfUpdatedTo3);
        }
    }

}
