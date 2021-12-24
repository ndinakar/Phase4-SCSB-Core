package org.recap.converter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.marc4j.marc.Record;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbConstants;
import org.recap.ScsbCommonConstants;
import org.recap.model.jaxb.BibRecord;
import org.recap.model.jaxb.Holding;
import org.recap.model.jaxb.Holdings;
import org.recap.model.jaxb.Items;
import org.recap.model.jaxb.marc.CollectionType;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.jpa.ReportEntity;
import org.recap.model.marc.BibMarcRecord;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.util.CommonUtil;
import org.recap.util.DBReportUtil;
import org.recap.util.MarcUtil;
import org.recap.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by premkb on 15/12/16.
 */
@Service
public class SCSBToBibEntityConverter implements XmlToBibEntityConverterInterface {

    private static final Logger logger = LoggerFactory.getLogger(SCSBToBibEntityConverter.class);

    @Autowired
    private DBReportUtil dbReportUtil;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private MarcUtil marcUtil;

    @Autowired
    private PropertyUtil propertyUtil;

    /**
     * The Bibliographic details repository.
     */
    @Autowired
    BibliographicDetailsRepository bibliographicDetailsRepository;

    /**
     *
     * @param scsbRecord
     * @return
     */
    @Override
    public Map convert(Object scsbRecord, InstitutionEntity institutionEntity) {
        Map<String, Object> map = new HashMap<>();
        boolean processBib = false;

        List<HoldingsEntity> holdingsEntities = new ArrayList<>();
        List<ItemEntity> itemEntities = new ArrayList<>();
        List<ReportEntity> reportEntities = new ArrayList<>();

        getDbReportUtil().setInstitutionEntitiesMap(commonUtil.getInstitutionEntityMap());
        getDbReportUtil().setCollectionGroupMap(commonUtil.getCollectionGroupMap());

        BibRecord bibRecord = (BibRecord) scsbRecord;
        String owningInstitutionBibId = bibRecord.getBib().getOwningInstitutionBibId();
        StringBuilder errorMessage = new StringBuilder();
        try {
            BibMarcRecord bibMarcRecord = marcUtil.buildBibMarcRecord(bibRecord);
            Record bibRecordObject = bibMarcRecord.getBibRecord();
           
            Integer owningInstitutionId = institutionEntity.getId();
            String owningInstitutionCode = institutionEntity.getInstitutionCode();
            Map<String, Object> bibMap = processAndValidateBibliographicEntity(bibRecordObject, owningInstitutionId,  owningInstitutionBibId, errorMessage);
            BibliographicEntity bibliographicEntity = (BibliographicEntity) bibMap.get(ScsbConstants.BIBLIOGRAPHIC_ENTITY);
            ReportEntity bibReportEntity = (ReportEntity) bibMap.get("bibReportEntity");
            if (bibReportEntity != null) {
                reportEntities.add(bibReportEntity);
            } else {
                processBib = true;
            }

            List<Holdings> holdings = bibRecord.getHoldings();
            for(Holdings holdings1 : holdings){
                for(Holding holding:holdings1.getHolding()){
                    String owninigInstitutionHoldingId = holding.getOwningInstitutionHoldingsId();
                    CollectionType holdingContentCollection = holding.getContent().getCollection();
                    String holdingsContent = holdingContentCollection.serialize(holdingContentCollection);
                    List<Record> holdingsRecords = marcUtil.convertMarcXmlToRecord(holdingsContent);
                    Map<String, Object> holdingsMap = processAndValidateHoldingsEntity(bibliographicEntity, owninigInstitutionHoldingId, holdingsRecords.get(0), errorMessage);
                    HoldingsEntity holdingsEntity = (HoldingsEntity) holdingsMap.get("holdingsEntity");
                    ReportEntity holdingsReportEntity = (ReportEntity) holdingsMap.get("holdingsReportEntity");
                    boolean processHoldings = false;
                    if (holdingsReportEntity != null) {
                        reportEntities.add(holdingsReportEntity);
                    } else {
                        processHoldings = true;
                        holdingsEntities.add(holdingsEntity);
                    }
                    String holdingsCallNumber = marcUtil.getDataFieldValue(holdingsRecords.get(0), "852", 'h');
                    if(holdingsCallNumber == null){
                        holdingsCallNumber = "";
                    }
                    Character holdingsCallNumberType = marcUtil.getInd1(holdingsRecords.get(0), "852", 'h');
                    List<Items> itemEntityList = holding.getItems();
                    for(Items items:itemEntityList){
                        CollectionType itemContentCollection = items.getContent().getCollection();
                        String itemContent = itemContentCollection.serialize(itemContentCollection);
                        List<Record> itemRecordList = marcUtil.convertMarcXmlToRecord(itemContent);
                        for (Record itemRecord : itemRecordList) {
                            Map<String, Object> itemMap = processAndValidateItemEntity(owningInstitutionId, owningInstitutionCode, holdingsCallNumber, holdingsCallNumberType, itemRecord, errorMessage);
                            commonUtil.addItemAndReportEntities(itemEntities, reportEntities, processHoldings, holdingsEntity, itemMap);
                        }
                    }
                }
                bibliographicEntity.setHoldingsEntities(holdingsEntities);
                bibliographicEntity.setItemEntities(itemEntities);
            }
            if (processBib) {
                map.put(ScsbConstants.BIBLIOGRAPHIC_ENTITY, bibliographicEntity);
            }
        } catch (Exception e) {
            logger.error(ScsbCommonConstants.LOG_ERROR,e);
            errorMessage.append(e.getMessage());
        }
        map.put("errorMessage",errorMessage);
        return map;
    }

    /**
     *
     * @param bibRecord
     * @param owningInstitutionId
     * @param owningInstitutionBibId
     * @return
     */
    private Map<String, Object> processAndValidateBibliographicEntity(Record bibRecord, Integer owningInstitutionId, String owningInstitutionBibId,StringBuilder errorMessage) {
        Map<String, Object> map = new HashMap<>();
        Date currentDate = new Date();

        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        if(StringUtils.isEmpty(owningInstitutionBibId)){
            owningInstitutionBibId = marcUtil.getControlFieldValue(bibRecord, "001");
        }
        if (StringUtils.isNotBlank(owningInstitutionBibId)) {
            bibliographicEntity.setOwningInstitutionBibId(owningInstitutionBibId);
        } else {
            errorMessage.append(" Owning Institution Bib Id cannot be null");
        }
        if (owningInstitutionId != null) {
            bibliographicEntity.setOwningInstitutionId(owningInstitutionId);
        } else {
            errorMessage.append(" Owning Institution Id cannot be null");
        }
        bibliographicEntity.setCreatedDate(currentDate);
        bibliographicEntity.setCreatedBy(ScsbConstants.SUBMIT_COLLECTION);
        bibliographicEntity.setLastUpdatedDate(currentDate);
        bibliographicEntity.setLastUpdatedBy(ScsbConstants.SUBMIT_COLLECTION);
        return marcUtil.extractXmlAndSetEntityToMap(bibRecord, errorMessage, map, bibliographicEntity);
    }

    /**
     *
     * @param bibliographicEntity
     * @param holdingsRecord
     * @return
     */
    private Map<String, Object> processAndValidateHoldingsEntity(BibliographicEntity bibliographicEntity, String owningInstitutionHoldingsId,
                                                                 Record holdingsRecord, StringBuilder errorMessage) {
        Map<String, Object> map = new HashMap<>();
        Date currentDate = new Date();
        String holdingsContent = new MarcUtil().writeMarcXml(holdingsRecord);
        HoldingsEntity holdingsEntity = commonUtil.buildHoldingsEntity(bibliographicEntity, currentDate, errorMessage, holdingsContent, ScsbConstants.SUBMIT_COLLECTION);
        if (StringUtils.isBlank(owningInstitutionHoldingsId)) {
            owningInstitutionHoldingsId = UUID.randomUUID().toString();
        }
        holdingsEntity.setOwningInstitutionHoldingsId(owningInstitutionHoldingsId);
        map.put("holdingsEntity", holdingsEntity);
        return map;
    }

    /**
     *
     * @param owningInstitutionId
     * @param holdingsCallNumber
     * @param holdingsCallNumberType
     * @param itemRecord
     * @return
     */
    private Map<String, Object> processAndValidateItemEntity(Integer owningInstitutionId, String owningInstitutionCode, String holdingsCallNumber, Character holdingsCallNumberType, Record itemRecord,
                                                             StringBuilder errorMessage) {
        Map<String, Object> map = new HashMap<>();
        ItemEntity itemEntity = new ItemEntity();
        Date currentDate = new Date();
        String itemBarcode = marcUtil.getDataFieldValue(itemRecord, "876", 'p');
        if (StringUtils.isNotBlank(itemBarcode)) {
            itemEntity.setBarcode(itemBarcode);
            map.put("itemBarcode",itemBarcode);
        } else {
            errorMessage.append(" Item Barcode cannot be null");
        }
        String customerCode = marcUtil.getDataFieldValue(itemRecord, "900", 'b');
        if (StringUtils.isNotBlank(customerCode)) {
            itemEntity.setCustomerCode(customerCode);
        }
        String itemLibrary = marcUtil.getDataFieldValue(itemRecord, "876", 'k');
        Map<String, String> itemLibraryPropertyMap = propertyUtil.getPropertyByKeyForAllInstitutions(PropertyKeyConstants.ILS.ILS_ITEM_LIBRARY_REQUIRED);
        Boolean isItemLibraryRequired = Boolean.parseBoolean(itemLibraryPropertyMap.get(owningInstitutionCode));
        if (itemLibrary != null) {
            itemEntity.setItemLibrary(itemLibrary);
        }
        itemEntity.setCallNumber(holdingsCallNumber);
        itemEntity.setCallNumberType(holdingsCallNumberType != null ? String.valueOf(holdingsCallNumberType) : "");
        String copyNumber = marcUtil.getDataFieldValue(itemRecord, "876", 't');
        if (StringUtils.isNotBlank(copyNumber) && NumberUtils.isCreatable(copyNumber)) {
            itemEntity.setCopyNumber(Integer.valueOf(copyNumber));
        }
        if (owningInstitutionId != null) {
            itemEntity.setOwningInstitutionId(owningInstitutionId);
        } else {
            errorMessage.append(" Owning Institution Id cannot be null");
        }
        String collectionGroupCode = marcUtil.getDataFieldValue(itemRecord, "900", 'a');
        if (StringUtils.isNotBlank(collectionGroupCode) && commonUtil.getCollectionGroupMap().containsKey(collectionGroupCode)) {
            itemEntity.setCollectionGroupId(commonUtil.getCollectionGroupMap().get(collectionGroupCode));
        }

        String useRestrictions = marcUtil.getDataFieldValue(itemRecord, "876", 'h');
        if (useRestrictions != null) {
            itemEntity.setUseRestrictions(useRestrictions);
        }

        itemEntity.setVolumePartYear(marcUtil.getDataFieldValue(itemRecord, "876", '3'));
        String owningInstitutionItemId = marcUtil.getDataFieldValue(itemRecord, "876", 'a');
        if (StringUtils.isNotBlank(owningInstitutionItemId)) {
            itemEntity.setOwningInstitutionItemId(owningInstitutionItemId);
        } else {
            errorMessage.append(" Item Owning Institution Id cannot be null");
        }

        if(isItemLibraryRequired && itemEntity.getItemLibrary() == null) {
            itemEntity.setCatalogingStatus(ScsbCommonConstants.INCOMPLETE_STATUS);
        }
        itemEntity.setCreatedDate(currentDate);
        itemEntity.setCreatedBy(ScsbConstants.SUBMIT_COLLECTION);
        itemEntity.setLastUpdatedDate(currentDate);
        itemEntity.setLastUpdatedBy(ScsbConstants.SUBMIT_COLLECTION);
        map.put("itemEntity", itemEntity);
        return map;
    }

    /**
     * Gets db report util.
     *
     * @return the db report util
     */
    public DBReportUtil getDbReportUtil() {
        return dbReportUtil;
    }

    /**
     * Sets db report util.
     *
     * @param dbReportUtil the db report util
     */
    public void setDbReportUtil(DBReportUtil dbReportUtil) {
        this.dbReportUtil = dbReportUtil;
    }
}
