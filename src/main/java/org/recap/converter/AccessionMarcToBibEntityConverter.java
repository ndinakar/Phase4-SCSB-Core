package org.recap.converter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.marc4j.marc.Leader;
import org.marc4j.marc.Record;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.ImsLocationEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.jpa.ReportDataEntity;
import org.recap.model.marc.BibMarcRecord;
import org.recap.model.marc.HoldingsMarcRecord;
import org.recap.model.marc.ItemMarcRecord;
import org.recap.util.MarcUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccessionMarcToBibEntityConverter extends AccessionXmlConverterAbstract {

    /**
     * This method uses the marc record and builds the bibliographic entity. For exceptions, adds them to report entities. Also adds failed record counts.
     *
     * @param marcRecord
     * @param institutionName the institution name
     * @param accessionRequest    the customer code
     * @return
     */
    @Override
    public Map convert(Object marcRecord, String institutionName, AccessionRequest accessionRequest, ImsLocationEntity imsLocationEntity) {
        int failedItemCount = 0;
        int successItemCount = 0;
        String reasonForFailureItem = "";
        Map<String, Object> map = new HashMap<>();
        String incompleteResponse = "";

        Record record = (Record) marcRecord;
        List<HoldingsEntity> holdingsEntities = new ArrayList<>();
        List<ItemEntity> itemEntities = new ArrayList<>();
        StringBuilder errorMessage = new StringBuilder();

        dbReportUtil.setInstitutionEntitiesMap(commonUtil.getInstitutionEntityMap());
        dbReportUtil.setCollectionGroupMap(commonUtil.getCollectionGroupMap());

        BibMarcRecord bibMarcRecord = marcUtil.buildBibMarcRecord(record);
        Record bibRecord = bibMarcRecord.getBibRecord();
        Integer owningInstitutionId = commonUtil.getInstitutionEntityMap().get(institutionName);
        Date currentDate = new Date();
        Map<String, Object> bibMap = processAndValidateBibliographicEntity(bibRecord, owningInstitutionId, currentDate,errorMessage);
        BibliographicEntity bibliographicEntity = (BibliographicEntity) bibMap.get(ScsbCommonConstants.BIBLIOGRAPHICENTITY);

        map.put(ScsbCommonConstants.FAILED_BIB_COUNT, bibMap.get(ScsbCommonConstants.FAILED_BIB_COUNT));
        map.put(ScsbCommonConstants.SUCCESS_BIB_COUNT , bibMap.get(ScsbCommonConstants.SUCCESS_BIB_COUNT));
        map.put(ScsbCommonConstants.REASON_FOR_BIB_FAILURE , bibMap.get(ScsbCommonConstants.REASON_FOR_BIB_FAILURE));
        map.put(ScsbCommonConstants.EXIST_BIB_COUNT , bibMap.get(ScsbCommonConstants.EXIST_BIB_COUNT));

        if (errorMessage.length()==0) {
            List<HoldingsMarcRecord> holdingsMarcRecords = bibMarcRecord.getHoldingsMarcRecords();
            if (CollectionUtils.isNotEmpty(holdingsMarcRecords)) {
                for (HoldingsMarcRecord holdingsMarcRecord : holdingsMarcRecords) {
                    Record holdingsRecord = holdingsMarcRecord.getHoldingsRecord();
                    Map<String, Object> holdingsMap = processAndValidateHoldingsEntity(bibliographicEntity, holdingsRecord, currentDate,errorMessage);
                    HoldingsEntity holdingsEntity = (HoldingsEntity) holdingsMap.get("holdingsEntity");
                    if (errorMessage.length()==0) {
                        holdingsEntities.add(holdingsEntity);
                    }
                    String holdingsCallNumber = marcUtil.getDataFieldValue(holdingsRecord, "852", 'h');
                    if(holdingsCallNumber == null){
                        holdingsCallNumber = "";
                    }
                    Character holdingsCallNumberType = marcUtil.getInd1(holdingsRecord, "852", 'h');

                    List<ItemMarcRecord> itemMarcRecordList = holdingsMarcRecord.getItemMarcRecordList();
                    if (CollectionUtils.isNotEmpty(itemMarcRecordList)) {
                        for (ItemMarcRecord itemMarcRecord : itemMarcRecordList) {
                            Record itemRecord = itemMarcRecord.getItemRecord();

                          Map<String, Object> itemMap = processAndValidateItemEntity(bibliographicEntity, owningInstitutionId,accessionRequest.getCustomerCode(), holdingsCallNumber, holdingsCallNumberType, itemRecord, institutionName, bibRecord, currentDate,errorMessage,imsLocationEntity);

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
                                        reasonForFailureItem=stringBuilder.toString();
                                    }
                                }
                            }
                            if(itemMap.containsKey(ScsbCommonConstants.SUCCESS_ITEM_COUNT)){
                                successItemCount = successItemCount + (int) itemMap.get(ScsbCommonConstants.SUCCESS_ITEM_COUNT);
                            }
                            ItemEntity itemEntity = (ItemEntity) itemMap.get("itemEntity");
                            if (errorMessage.length() == 0) {
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
        if(errorMessage.length()>0) {//Added to remove "," from the error message
            errorMessage = new StringBuilder(errorMessage.substring(0,errorMessage.length()-1));
        }
        map.put("errorMessage",errorMessage);

        return map;
    }

    /**
     * This method is used to validate all necessary bibRecord fields
     * @param bibRecord
     * @param owningInstitutionId
     * @param currentDate
     * @param errorMessage
     * @return
     */
    private Map<String, Object> processAndValidateBibliographicEntity(Record bibRecord, Integer owningInstitutionId,Date currentDate,StringBuilder errorMessage) {
        int failedBibCount = 0;
        int successBibCount = 0;
        int exitsBibCount = 0;
        String reasonForFailureBib = "";
        Map<String, Object> map = new HashMap<>();

        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        String owningInstitutionBibId = marcUtil.getControlFieldValue(bibRecord, "001");
        if (StringUtils.isNotBlank(owningInstitutionBibId)) {
            bibliographicEntity.setOwningInstitutionBibId(owningInstitutionBibId);
        } else {
            errorMessage.append("Owning Institution Bib Id cannot be null").append(",");
        }
        if (owningInstitutionId != null) {
            bibliographicEntity.setOwningInstitutionId(owningInstitutionId);
        } else {
            errorMessage.append("Owning Institution Id cannot be null").append(",");
        }
        bibliographicEntity.setDeleted(false);
        bibliographicEntity.setCreatedDate(currentDate);
        bibliographicEntity.setCreatedBy(ScsbCommonConstants.ACCESSION);
        bibliographicEntity.setLastUpdatedDate(currentDate);
        bibliographicEntity.setLastUpdatedBy(ScsbCommonConstants.ACCESSION);

        String bibContent = marcUtil.writeMarcXml(bibRecord);
        if (StringUtils.isNotBlank(bibContent)) {
            bibliographicEntity.setContent(bibContent.getBytes());
        } else {
            errorMessage.append("Bib Content cannot be empty").append(",");
        }

        boolean subFieldExistsFor245 = marcUtil.isSubFieldExists(bibRecord, "245");
        if (!subFieldExistsFor245) {
            errorMessage.append("Atleast one subfield should be there for 245 tag").append(",");
        }
        Leader leader = bibRecord.getLeader();
        if(leader == null){
            errorMessage.append(" Leader field is missing").append(",");
        } else {
            String leaderValue = bibRecord.getLeader().toString();
            if (!(StringUtils.isNotBlank(leaderValue) && leaderValue.length() == 24)) {
                errorMessage.append("Leader Field value should be 24 characters").append(",");
            }
        }
        if(owningInstitutionId != null && StringUtils.isNotBlank(owningInstitutionBibId)){
            BibliographicEntity existBibliographicEntity = bibliographicDetailsRepository.findByOwningInstitutionIdAndOwningInstitutionBibIdAndIsDeletedFalse(owningInstitutionId,owningInstitutionBibId);
            if(null != existBibliographicEntity){
                exitsBibCount = 1;
            }
        }
        List<ReportDataEntity> reportDataEntities = null;

        if (errorMessage.toString().length() > 1) {
            if(exitsBibCount == 0){
                failedBibCount = failedBibCount+1;
            }
            reasonForFailureBib = errorMessage.toString();
            reportDataEntities = dbReportUtil.generateBibFailureReportEntity(bibliographicEntity, bibRecord);
            ReportDataEntity errorReportDataEntity = new ReportDataEntity();
            errorReportDataEntity.setHeaderName(ScsbCommonConstants.ERROR_DESCRIPTION);
            errorReportDataEntity.setHeaderValue(errorMessage.toString());
            reportDataEntities.add(errorReportDataEntity);
        }else if(exitsBibCount == 0){
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
     * This method is used to validate all the fields in holding content.
     * @param bibliographicEntity
     * @param holdingsRecord
     * @param currentDate
     * @param errorMessage
     * @return
     */
    private Map<String, Object> processAndValidateHoldingsEntity(BibliographicEntity bibliographicEntity,  Record holdingsRecord, Date currentDate,StringBuilder errorMessage) {
        Map<String, Object> map = new HashMap<>();
        String holdingsContent = new MarcUtil().writeMarcXml(holdingsRecord);
        HoldingsEntity holdingsEntity = commonUtil.buildHoldingsEntity(bibliographicEntity, currentDate, errorMessage, holdingsContent,ScsbCommonConstants.ACCESSION);
        String owningInstitutionHoldingsId = marcUtil.getDataFieldValue(holdingsRecord, "852", '0');
        return commonUtil.addHoldingsEntityToMap(map, holdingsEntity, owningInstitutionHoldingsId);
    }

    /**
     * This method is used to validate all the fields in item.
     * @param bibliographicEntity
     * @param owningInstitutionId
     * @param customerCode
     * @param holdingsCallNumber
     * @param holdingsCallNumberType
     * @param itemRecord
     * @param errorMessage
     * @return
     */

    private Map<String, Object> processAndValidateItemEntity(BibliographicEntity bibliographicEntity, Integer owningInstitutionId, String customerCode, String holdingsCallNumber, Character holdingsCallNumberType, Record itemRecord, String institutionName, Record bibRecord,
                                                             Date currentDate, StringBuilder errorMessage, ImsLocationEntity imsLocationEntity) {

        Map<String, Object> map = new HashMap<>();
        ItemEntity itemEntity = new ItemEntity();
        int failedItemCount = 0;
        int successItemCount = 0;
        boolean isComplete = true;
        String reasonForFailureItem = "";
        map.put(ScsbCommonConstants.FAILED_ITEM_COUNT,failedItemCount);
        map.put(ScsbCommonConstants.SUCCESS_ITEM_COUNT,successItemCount);
        map.put(ScsbCommonConstants.REASON_FOR_ITEM_FAILURE,reasonForFailureItem);
        String itemBarcode = marcUtil.getDataFieldValue(itemRecord, "876", 'p');
        if (StringUtils.isNotBlank(itemBarcode)) {
            itemEntity.setBarcode(itemBarcode);
            map.put("itemBarcode",itemBarcode);
        } else {
            errorMessage.append("Item Barcode cannot be null").append(",");
        }

        itemEntity.setCustomerCode(customerCode);
        itemEntity.setCallNumber(holdingsCallNumber);
        itemEntity.setCallNumberType(holdingsCallNumberType != null ? String.valueOf(holdingsCallNumberType) : "");
        String copyNumber = marcUtil.getDataFieldValue(itemRecord, "876", 't');
        if (StringUtils.isNotBlank(copyNumber) && NumberUtils.isCreatable(copyNumber)) {
            itemEntity.setCopyNumber(Integer.valueOf(copyNumber));
        }
        String itemLibrary = marcUtil.getDataFieldValue(itemRecord, "876", 'k');
        if (StringUtils.isNotBlank(itemLibrary)) {
            itemEntity.setItemLibrary(itemLibrary);
        }
        Map<String, String> itemLibraryPropertyMap = propertyUtil.getPropertyByKeyForAllInstitutions(PropertyKeyConstants.ILS.ILS_ITEM_LIBRARY_REQUIRED);
        Boolean isItemLibraryRequired = Boolean.parseBoolean(itemLibraryPropertyMap.get(institutionName));
        if(isItemLibraryRequired &&itemEntity.getItemLibrary() == null ) {
            isComplete = false;
        }
        if (owningInstitutionId != null) {
            itemEntity.setOwningInstitutionId(owningInstitutionId);
        } else {
            errorMessage.append("Owning Institution Id cannot be null").append(",");
        }
        String collectionGroupCode = marcUtil.getDataFieldValue(itemRecord, "876", 'x');
        if (StringUtils.isNotBlank(collectionGroupCode) && commonUtil.getCollectionGroupMap().containsKey(collectionGroupCode)) {
            itemEntity.setCollectionGroupId(commonUtil.getCollectionGroupMap().get(collectionGroupCode));
        } else {
            isComplete = false;
            itemEntity.setCollectionGroupId(commonUtil.getCollectionGroupMap().get(ScsbCommonConstants.NOT_AVAILABLE_CGD));
        }
        itemEntity.setDeleted(false);
        itemEntity.setCreatedDate(currentDate);
        itemEntity.setCreatedBy(ScsbCommonConstants.ACCESSION);
        itemEntity.setLastUpdatedDate(currentDate);
        itemEntity.setLastUpdatedBy(ScsbCommonConstants.ACCESSION);

        itemEntity.setImsLocationEntity(imsLocationEntity);
        itemEntity.setImsLocationId(imsLocationEntity.getId());

        String useRestrictions = marcUtil.getDataFieldValue(itemRecord, "876", 'h');
        if (StringUtils.isNotBlank(useRestrictions) && ("In Library Use".equalsIgnoreCase(useRestrictions) || "Supervised Use".equalsIgnoreCase(useRestrictions))) {
            itemEntity.setUseRestrictions(useRestrictions);
        } else if(null == useRestrictions){
            isComplete = false;
        }

        itemEntity.setVolumePartYear(marcUtil.getDataFieldValue(itemRecord, "876", '3'));
        String owningInstitutionItemId = marcUtil.getDataFieldValue(itemRecord, "876", 'a');
        if (StringUtils.isNotBlank(owningInstitutionItemId)) {
            itemEntity.setOwningInstitutionItemId(owningInstitutionItemId);
        } else {
            errorMessage.append("Item Owning Institution Id cannot be null").append(",");
        }

        if(isComplete){
            bibliographicEntity.setCatalogingStatus(ScsbCommonConstants.COMPLETE_STATUS);
            itemEntity.setItemAvailabilityStatusId(commonUtil.getItemStatusMap().get("Available"));
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

    @Override
    public boolean isFormat(String format) {
        return "MARC".equalsIgnoreCase(format);
    }
}
