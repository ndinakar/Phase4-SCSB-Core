package org.recap.converter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.jaxb.BibRecord;
import org.recap.model.jaxb.Holding;
import org.recap.model.jaxb.Holdings;
import org.recap.model.jaxb.Items;
import org.recap.model.jaxb.marc.CollectionType;
import org.recap.model.jaxb.marc.ContentType;
import org.recap.model.jaxb.marc.LeaderFieldType;
import org.recap.model.jaxb.marc.RecordType;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.ImsLocationEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.util.DBReportUtil;
import org.recap.util.MarcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AccessionSCSBToBibEntityConverter extends AccessionXmlConverterAbstract {
    private static final Logger logger = LoggerFactory.getLogger(AccessionSCSBToBibEntityConverter.class);

    /**
     * This method is used to convert scsb record into bib entity
     * @param scsbRecord
     * @param institutionName the institution name
     * @param accessionRequest    the customer code
     * @return
     */
    @Override
    public Map convert(Object scsbRecord, String institutionName, AccessionRequest accessionRequest, ImsLocationEntity imsLocationEntity){
        int failedItemCount = 0;
        int successItemCount = 0;
        String reasonForFailureItem = "";
        Map<String, Object> map = new HashMap<>();
        String incompleteResponse = "";
        List<HoldingsEntity> holdingsEntities = new ArrayList<>();
        List<ItemEntity> itemEntities = new ArrayList<>();
        StringBuilder errorMessage = new StringBuilder();

        getDbReportUtil().setInstitutionEntitiesMap(commonUtil.getInstitutionEntityMap());
        getDbReportUtil().setCollectionGroupMap(commonUtil.getCollectionGroupMap());

        try {
            BibRecord bibRecord = (BibRecord) scsbRecord;
            Integer owningInstitutionId = commonUtil.getInstitutionEntityMap().get(institutionName);
            Date currentDate = new Date();

            Map<String, Object> bibMap = processAndValidateBibliographicEntity(bibRecord, owningInstitutionId, currentDate,errorMessage);
            BibliographicEntity bibliographicEntity = (BibliographicEntity) bibMap.get("bibliographicEntity");

            map.put(ScsbCommonConstants.FAILED_BIB_COUNT, bibMap.get(ScsbCommonConstants.FAILED_BIB_COUNT));
            map.put(ScsbCommonConstants.SUCCESS_BIB_COUNT , bibMap.get(ScsbCommonConstants.SUCCESS_BIB_COUNT));
            map.put(ScsbCommonConstants.REASON_FOR_BIB_FAILURE , bibMap.get(ScsbCommonConstants.REASON_FOR_BIB_FAILURE));
            map.put(ScsbCommonConstants.EXIST_BIB_COUNT , bibMap.get(ScsbCommonConstants.EXIST_BIB_COUNT));

            List<Holdings> holdingsList = bibRecord.getHoldings();
            if (errorMessage.length()==0) {
                for(Holdings holdings:holdingsList){
                    List<Holding> holdingList=null;
                    if (holdings.getHolding()!=null) {
                        holdingList = holdings.getHolding();
                    } else {
                        logger.error(String.format("holding is empty---%s",bibRecord.getBib().getOwningInstitutionBibId()));
                    }
                    for(Holding holding:holdingList){
                        if (holding.getContent() != null) {
                            CollectionType holdingContentCollection = holding.getContent().getCollection();
                            List<RecordType> holdingRecordTypes = holdingContentCollection.getRecord();
                            RecordType holdingsRecordType = holdingRecordTypes.get(0);

                            Map<String, Object> holdingsMap = processAndValidateHoldingsEntity(bibliographicEntity, holding, holdingContentCollection,currentDate,errorMessage);
                            HoldingsEntity holdingsEntity = (HoldingsEntity) holdingsMap.get("holdingsEntity");
                            if (errorMessage.length()==0) {
                                holdingsEntities.add(holdingsEntity);
                            }
                            String holdingsCallNumber = getMarcUtil().getDataFieldValueForRecordType(holdingsRecordType, "852", null, null, "h");
                            if(holdingsCallNumber == null){
                                holdingsCallNumber = "";
                            }
                            String holdingsCallNumberType = getMarcUtil().getInd1ForRecordType(holdingsRecordType, "852", "h");

                            List<Items> items = holding.getItems();
                            for (Items item : items) {
                                ContentType itemContent = item.getContent();
                                CollectionType itemContentCollection = itemContent.getCollection();

                                List<RecordType> itemRecordTypes = itemContentCollection.getRecord();
                                for (RecordType itemRecordType : itemRecordTypes) {

                                    Map<String, Object> itemMap = processAndValidateItemEntity(bibliographicEntity, owningInstitutionId,institutionName, holdingsCallNumber, holdingsCallNumberType, itemRecordType,accessionRequest,currentDate,errorMessage,imsLocationEntity);

                                    if (itemMap != null) {
                                        if(itemMap.containsKey(ScsbCommonConstants.FAILED_ITEM_COUNT)){
                                            failedItemCount = failedItemCount + (int) itemMap.get(ScsbCommonConstants.FAILED_ITEM_COUNT);
                                        }
                                        if(itemMap.containsKey(ScsbCommonConstants.ITEMBARCODE)){
                                            map.put(ScsbCommonConstants.ITEMBARCODE, itemMap.get(ScsbCommonConstants.ITEMBARCODE));
                                        }
                                        if(itemMap.containsKey(ScsbCommonConstants.REASON_FOR_ITEM_FAILURE)){
                                            String reason = (String)itemMap.get(ScsbCommonConstants.REASON_FOR_ITEM_FAILURE);
                                            if(!StringUtils.isEmpty(reason)){
                                                if(StringUtils.isEmpty(reasonForFailureItem)){
                                                    reasonForFailureItem = (String) itemMap.get(ScsbCommonConstants.REASON_FOR_ITEM_FAILURE);
                                                }else{
                                                    StringBuilder stringBuilder = new StringBuilder();
                                                    stringBuilder.append(itemMap.get(ScsbCommonConstants.REASON_FOR_ITEM_FAILURE));
                                                    stringBuilder.append(",");
                                                    stringBuilder.append(reasonForFailureItem);
                                                    reasonForFailureItem = stringBuilder.toString();
                                                }

                                            }
                                        }
                                        if(itemMap.containsKey(ScsbCommonConstants.SUCCESS_ITEM_COUNT)){
                                            successItemCount = successItemCount + (int) itemMap.get(ScsbCommonConstants.SUCCESS_ITEM_COUNT);
                                        }
                                        ItemEntity itemEntity = (ItemEntity) itemMap.get("itemEntity");
                                        if (errorMessage.length()==0) {
                                            if (holdingsEntity.getItemEntities() == null) {
                                                holdingsEntity.setItemEntities(new ArrayList<>());
                                            }
                                            holdingsEntity.getItemEntities().add(itemEntity);
                                            itemEntities.add(itemEntity);
                                        }
                                        if(ScsbCommonConstants.INCOMPLETE_STATUS.equalsIgnoreCase(itemEntity.getCatalogingStatus())){
                                            incompleteResponse = ScsbCommonConstants.INCOMPLETE_STATUS;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    bibliographicEntity.setHoldingsEntities(holdingsEntities);
                    bibliographicEntity.setItemEntities(itemEntities);
                }
            }
            if (errorMessage.length()==0) {
                map.put(ScsbCommonConstants.BIBLIOGRAPHICENTITY, bibliographicEntity);
            }
            map.put(ScsbCommonConstants.FAILED_ITEM_COUNT,failedItemCount);
            map.put(ScsbCommonConstants.SUCCESS_ITEM_COUNT,successItemCount);
            map.put(ScsbCommonConstants.REASON_FOR_ITEM_FAILURE,reasonForFailureItem);
            map.put(ScsbConstants.INCOMPLETE_RESPONSE,incompleteResponse);
        } catch (Exception e) {
            logger.error(ScsbCommonConstants.LOG_ERROR,e);
            errorMessage.append(e.getMessage());
        }

        if(errorMessage.length()>0) {//Added to remove "," from the error message
            errorMessage = new StringBuilder(errorMessage.substring(0,errorMessage.length()-1));
        }
        map.put("errorMessage",errorMessage);
        return map;
    }


    /**
     * This method is used to validate all necessary bib record fields
     * @param bibRecord
     * @param owningInstitutionId
     * @param currentDate
     * @return
     */
    private Map<String, Object> processAndValidateBibliographicEntity(BibRecord bibRecord,Integer owningInstitutionId,Date currentDate,StringBuilder errorMessage) {
        int failedBibCount = 0;
        int successBibCount = 0;
        int exitsBibCount = 0;
        String reasonForFailureBib = "";
        Map<String, Object> map = new HashMap<>();

        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        String owningInstitutionBibId = bibRecord.getBib().getOwningInstitutionBibId();

        if (StringUtils.isNotBlank(owningInstitutionBibId)) {
            bibliographicEntity.setOwningInstitutionBibId(owningInstitutionBibId);
        } else {
            errorMessage.append("Owning Institution Bib Id cannot be null").append(",");
        }
        if (owningInstitutionId != null) {
            bibliographicEntity.setOwningInstitutionId(owningInstitutionId);
        } else {
            errorMessage.append(" Owning Institution Id cannot be null ").append(",");
        }
        bibliographicEntity.setDeleted(false);
        bibliographicEntity.setCreatedDate(currentDate);
        bibliographicEntity.setCreatedBy(ScsbCommonConstants.ACCESSION);
        bibliographicEntity.setLastUpdatedDate(currentDate);
        bibliographicEntity.setLastUpdatedBy(ScsbCommonConstants.ACCESSION);

        ContentType bibContent = bibRecord.getBib().getContent();
        CollectionType bibContentCollection = bibContent.getCollection();
        String bibXmlContent = bibContentCollection.serialize(bibContentCollection);
        if (!StringUtils.isEmpty(bibXmlContent)) {
            bibliographicEntity.setContent(bibXmlContent.getBytes());
        } else {
            errorMessage.append(" Bib Content cannot be empty").append(",");
        }

        boolean subFieldExistsFor245 = getMarcUtil().isSubFieldExists(bibContentCollection.getRecord().get(0), "245");

        if (!subFieldExistsFor245) {
            errorMessage.append(" At least one subfield should be there for 245 tag").append(",");
        }

        LeaderFieldType leader = bibContentCollection.getRecord().get(0).getLeader();
        if(leader == null){
            errorMessage.append(" Leader field is missing").append(",");
        } else if (!(StringUtils.isNotBlank(leader.getValue()) && leader.getValue().length() == 24)) {
            errorMessage.append(" Leader field value should be 24 characters ").append(",");
        }

        if(owningInstitutionId != null && StringUtils.isNotBlank(owningInstitutionBibId)){
            BibliographicEntity existBibliographicEntity = bibliographicDetailsRepository.findByOwningInstitutionIdAndOwningInstitutionBibIdAndIsDeletedFalse(owningInstitutionId,owningInstitutionBibId);
            if(null != existBibliographicEntity){
                exitsBibCount = 1;
            }
        }
        if(errorMessage.toString().length()==0){
            successBibCount = successBibCount+1;
        }
        map.put(ScsbCommonConstants.FAILED_BIB_COUNT , failedBibCount);
        map.put(ScsbCommonConstants.REASON_FOR_BIB_FAILURE , reasonForFailureBib);
        map.put(ScsbCommonConstants.BIBLIOGRAPHICENTITY, bibliographicEntity);
        map.put(ScsbCommonConstants.SUCCESS_BIB_COUNT,successBibCount);
        map.put(ScsbCommonConstants.EXIST_BIB_COUNT,exitsBibCount);
        return map;
    }

    /**
     * This method is used to validate all necessary holdings fields required in the bib record.
     * @param bibliographicEntity
     * @param holding
     * @param holdingContentCollection
     * @param currentDate
     * @param errorMessage
     * @return
     */
    private Map<String, Object> processAndValidateHoldingsEntity(BibliographicEntity bibliographicEntity, Holding holding, CollectionType holdingContentCollection,Date currentDate,StringBuilder errorMessage) {
        Map<String, Object> map = new HashMap<>();
        String holdingsContent = holdingContentCollection.serialize(holdingContentCollection);
        HoldingsEntity holdingsEntity = commonUtil.buildHoldingsEntity(bibliographicEntity, currentDate, errorMessage, holdingsContent,ScsbCommonConstants.ACCESSION);
        String owningInstitutionHoldingsId = holding.getOwningInstitutionHoldingsId();
        return commonUtil.addHoldingsEntityToMap(map, holdingsEntity, owningInstitutionHoldingsId);
    }

    /**
     * This method is used to validate all necessary fields required for item.
     *
     * @param bibliographicEntity
     * @param owningInstitutionId
     * @param holdingsCallNumber
     * @param holdingsCallNumberType
     * @param itemRecordType
     * @param accessionRequest
     * @param currentDate
     * @param errorMessage
     * @return
     */
    private Map<String, Object> processAndValidateItemEntity(BibliographicEntity bibliographicEntity, Integer owningInstitutionId, String institutionName,
                                                             String holdingsCallNumber, String holdingsCallNumberType, RecordType itemRecordType,AccessionRequest accessionRequest,
                                                             Date currentDate,StringBuilder errorMessage,ImsLocationEntity imsLocationEntity) {

        Map<String, Object> map = new HashMap<>();
        ItemEntity itemEntity = new ItemEntity();
        int failedItemCount = 0;
        int successItemCount = 0;
        boolean isComplete = true;
        String reasonForFailureItem = "";
        map.put(ScsbCommonConstants.FAILED_ITEM_COUNT,failedItemCount);
        map.put(ScsbCommonConstants.SUCCESS_ITEM_COUNT,successItemCount);
        map.put(ScsbCommonConstants.REASON_FOR_ITEM_FAILURE,reasonForFailureItem);
        String itemBarcode = getMarcUtil().getDataFieldValueForRecordType(itemRecordType, "876", null, null, "p");
        if (accessionRequest.getItemBarcode().equals(itemBarcode)) {//This is to avoid creation of multiple items when response from partner service is having 1Bib 1Hold n items, accession should be done for one item which comes in the request and should not be done for other items which is linked with the same bib
            if (StringUtils.isNotBlank(itemBarcode)) {
                itemEntity.setBarcode(itemBarcode);
                map.put("itemBarcode",itemBarcode);
            } else {
                errorMessage.append("Item Barcode cannot be null").append(",");
            }
            itemEntity.setCustomerCode(accessionRequest.getCustomerCode());
            itemEntity.setCallNumber(holdingsCallNumber);
            itemEntity.setCallNumberType(holdingsCallNumberType != null ? holdingsCallNumberType : "");
            String copyNumber = getMarcUtil().getDataFieldValueForRecordType(itemRecordType, "876", null, null, "t");
            if (StringUtils.isNotBlank(copyNumber) && NumberUtils.isCreatable(copyNumber)) {
                itemEntity.setCopyNumber(Integer.valueOf(copyNumber));
            }
            if (owningInstitutionId != null) {
                itemEntity.setOwningInstitutionId(owningInstitutionId);
            } else {
                errorMessage.append("\n");
                errorMessage.append("Owning Institution Id cannot be null").append(",");
            }
            String collectionGroupCode = getMarcUtil().getDataFieldValueForRecordType(itemRecordType, "900", null, null, "a");
            if (StringUtils.isNotBlank(collectionGroupCode) && commonUtil.getCollectionGroupMap().containsKey(collectionGroupCode)) {
                itemEntity.setCollectionGroupId(commonUtil.getCollectionGroupMap().get(collectionGroupCode));
            } else {
                itemEntity.setCollectionGroupId(commonUtil.getCollectionGroupMap().get("Open"));
            }
            itemEntity.setDeleted(false);
            itemEntity.setCreatedDate(currentDate);
            itemEntity.setCreatedBy(ScsbCommonConstants.ACCESSION);
            itemEntity.setLastUpdatedDate(currentDate);
            itemEntity.setLastUpdatedBy(ScsbCommonConstants.ACCESSION);

            itemEntity.setImsLocationEntity(imsLocationEntity);
            itemEntity.setImsLocationId(imsLocationEntity.getId());

            String itemLibrary = getMarcUtil().getDataFieldValueForRecordType(itemRecordType, "876", null, null, "k");
            if (StringUtils.isNotBlank(itemLibrary) ) {
                itemEntity.setItemLibrary(itemLibrary);
            }
            Map<String, String> itemLibraryPropertyMap = propertyUtil.getPropertyByKeyForAllInstitutions(PropertyKeyConstants.ILS.ILS_ITEM_LIBRARY_REQUIRED);
            Boolean isItemLibraryRequired = Boolean.parseBoolean(itemLibraryPropertyMap.get(institutionName));
            if(isItemLibraryRequired &&itemEntity.getItemLibrary() == null ) {
                isComplete = false;
            }

            String useRestrictions = getMarcUtil().getDataFieldValueForRecordType(itemRecordType, "876", null, null, "h");
            if (StringUtils.isNotBlank(useRestrictions) && ("In Library Use".equalsIgnoreCase(useRestrictions) || "Supervised Use".equalsIgnoreCase(useRestrictions))) {
                itemEntity.setUseRestrictions(useRestrictions);
            } else if(null == useRestrictions){
                isComplete = false;
            }

            itemEntity.setVolumePartYear(getMarcUtil().getDataFieldValueForRecordType(itemRecordType, "876", null, null, "3"));
            String owningInstitutionItemId = getMarcUtil().getDataFieldValueForRecordType(itemRecordType, "876", null, null, "a");
            if (StringUtils.isNotBlank(owningInstitutionItemId)) {
                itemEntity.setOwningInstitutionItemId(owningInstitutionItemId);
            } else {
                errorMessage.append("Item Owning Institution Id cannot be null").append(",");
            }

            if(isComplete){
                itemEntity.setItemAvailabilityStatusId(commonUtil.getItemStatusMap().get("Available"));
                bibliographicEntity.setCatalogingStatus(ScsbCommonConstants.COMPLETE_STATUS);
                itemEntity.setCatalogingStatus(ScsbCommonConstants.COMPLETE_STATUS);
            } else {
                itemEntity.setItemAvailabilityStatusId(commonUtil.getItemStatusMap().get("Not Available"));
                bibliographicEntity.setCatalogingStatus(ScsbCommonConstants.INCOMPLETE_STATUS);
                itemEntity.setCatalogingStatus(ScsbCommonConstants.INCOMPLETE_STATUS);
            }
            if (errorMessage.toString().length() > 1) {
                if(map.containsKey(ScsbCommonConstants.FAILED_ITEM_COUNT)){
                    failedItemCount = ((int) map.get(ScsbCommonConstants.FAILED_ITEM_COUNT)) + 1;
                    map.put(ScsbCommonConstants.FAILED_ITEM_COUNT,failedItemCount);
                }
                if(map.containsKey(ScsbCommonConstants.REASON_FOR_ITEM_FAILURE)){
                    reasonForFailureItem = errorMessage.toString();
                    map.put(ScsbCommonConstants.REASON_FOR_ITEM_FAILURE,reasonForFailureItem);
                }
            }else{
                if(map.containsKey(ScsbCommonConstants.SUCCESS_ITEM_COUNT)){
                    successItemCount = (int) map.get(ScsbCommonConstants.SUCCESS_ITEM_COUNT) + 1;
                    map.put(ScsbCommonConstants.SUCCESS_ITEM_COUNT,successItemCount);
                }
            }
            map.put("itemEntity", itemEntity);
            return map;
        }
        return null;
    }

    /**
     * This method gets db report util.
     *
     * @return the db report util
     */
    public DBReportUtil getDbReportUtil() {
        return dbReportUtil;
    }

    /**
     * This method sets db report util.
     *
     * @param dbReportUtil the db report util
     */
    public void setDbReportUtil(DBReportUtil dbReportUtil) {
        this.dbReportUtil = dbReportUtil;
    }

    /**
     * Gets marc util.
     *
     * @return the marc util
     */
    public MarcUtil getMarcUtil() {
        if (null == marcUtil) {
            marcUtil = new MarcUtil();
        }
        return marcUtil;
    }

    @Override
    public boolean isFormat(String format) {
        return "SCSB".equalsIgnoreCase(format);
    }
}
