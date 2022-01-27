package org.recap.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.marc4j.marc.Leader;
import org.marc4j.marc.Record;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.converter.AccessionXmlToBibEntityConverterInterface;
import org.recap.converter.XmlToBibEntityConverterFactory;
import org.recap.model.ILSConfigProperties;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.accession.AccessionResponse;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.ImsLocationEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.ItemChangeLogEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.jpa.OwnerCodeEntity;
import org.recap.model.jpa.ReportDataEntity;
import org.recap.model.jpa.ReportEntity;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.repository.jpa.ImsLocationDetailsRepository;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.jpa.ItemChangeLogDetailsRepository;
import org.recap.repository.jpa.ItemDetailsRepository;
import org.recap.repository.jpa.OwnerCodeDetailsRepository;
import org.recap.service.BibliographicRepositoryDAO;
import org.recap.service.accession.AccessionValidationService;
import org.recap.service.accession.DummyDataService;
import org.recap.service.common.SetupDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
@Slf4j
@Service
public class AccessionUtil {



    @Autowired
    private PropertyUtil propertyUtil;

    @Autowired
    DummyDataService dummyDataService;

    @Autowired
    MarcUtil marcUtil;

    @Autowired
    DBReportUtil dbReportUtil;

    @Autowired
    BibliographicDetailsRepository bibliographicDetailsRepository;

    @Autowired
    OwnerCodeDetailsRepository ownerCodeDetailsRepository;

    @Autowired
    ItemDetailsRepository itemDetailsRepository;

    @Autowired
    InstitutionDetailsRepository institutionDetailsRepository;

    @Autowired
    XmlToBibEntityConverterFactory xmlToBibEntityConverterFactory;

    @Autowired
    AccessionValidationService accessionValidationService;

    @Autowired
    BibliographicRepositoryDAO bibliographicRepositoryDAO;

    @Autowired
    ItemChangeLogDetailsRepository itemChangeLogDetailsRepository;

    @Autowired
    ProducerTemplate producerTemplate;

    @Autowired
    ImsLocationDetailsRepository imsLocationDetailsRepository;

    @Value("${" + PropertyKeyConstants.SCSB_SOLR_DOC_URL + "}")
    private String scsbSolrClientUrl;

    private RestTemplate restTemplate;

    @Autowired
    private SetupDataService setupDataService;


    /**
     * Gets rest template.
     *
     * @return the rest template
     */
    public RestTemplate getRestTemplate() {
        if(restTemplate == null){
            restTemplate = new RestTemplate();
        }
        return restTemplate;
    }


    private Map<String,Integer> institutionEntityMap;


    public Map<String, Object> processAndValidateBibliographicEntity(Record bibRecord, Integer owningInstitutionId, Date currentDate, StringBuilder errorMessage) {
        int failedBibCount = 0;
        int successBibCount = 0;
        int exitsBibCount = 0;
        String reasonForFailureBib = "";
        Map<String, Object> map = new HashMap<>();

        BibliographicEntity bibliographicEntity = new BibliographicEntity();

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


    // FORMAT SPECIFIC

        String owningInstitutionBibId = marcUtil.getControlFieldValue(bibRecord, "001");
        if (StringUtils.isNotBlank(owningInstitutionBibId)) {
            bibliographicEntity.setOwningInstitutionBibId(owningInstitutionBibId);
        } else {
            errorMessage.append("Owning Institution Bib Id cannot be null").append(",");
        }
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
     * Create report data entity list for accessioned item.
     *
     * @param accessionRequest the accession request
     * @param response         the response
     * @return the list
     */
    public List<ReportDataEntity> createReportDataEntityList(AccessionRequest accessionRequest, String response) {
        List<ReportDataEntity> reportDataEntityList = new ArrayList<>();
        if (StringUtils.isNotBlank(accessionRequest.getCustomerCode())) {
            ReportDataEntity reportDataEntityOwnerCode = new ReportDataEntity();
            reportDataEntityOwnerCode.setHeaderName(ScsbCommonConstants.CUSTOMER_CODE);
            reportDataEntityOwnerCode.setHeaderValue(accessionRequest.getCustomerCode());
            reportDataEntityList.add(reportDataEntityOwnerCode);
        }
        if (StringUtils.isNotBlank(accessionRequest.getItemBarcode())) {
            ReportDataEntity reportDataEntityItemBarcode = new ReportDataEntity();
            reportDataEntityItemBarcode.setHeaderName(ScsbCommonConstants.ITEM_BARCODE);
            reportDataEntityItemBarcode.setHeaderValue(accessionRequest.getItemBarcode());
            reportDataEntityList.add(reportDataEntityItemBarcode);
        }
        ReportDataEntity reportDataEntityMessage = new ReportDataEntity();
        reportDataEntityMessage.setHeaderName(ScsbCommonConstants.MESSAGE);
        reportDataEntityMessage.setHeaderValue(response);
        reportDataEntityList.add(reportDataEntityMessage);
        return reportDataEntityList;
    }


    /**
     * Sets accession response.
     *
     * @param accessionResponseList the accession response list
     * @param itemBarcode           the item barcode
     * @param message               the message
     */
    public void setAccessionResponse(Set<AccessionResponse> accessionResponseList, String itemBarcode, String message) {
        AccessionResponse accessionResponse = new AccessionResponse();
        accessionResponse.setItemBarcode(itemBarcode);
        accessionResponse.setMessage(message);
        accessionResponseList.add(accessionResponse);
    }


    /**
     * This method is used to find the owning institution code based on the customer code parameter value.
     *
     * @param customerCode
     * @return
     */
    public String getOwningInstitution(String customerCode, String imsLocationCode) {
        String owningInstitution = null;
        try {
            ImsLocationEntity imsLocationEntity = imsLocationDetailsRepository.findByImsLocationCode(imsLocationCode);
            OwnerCodeEntity ownerCodeEntity = ownerCodeDetailsRepository.findByOwnerCodeAndImsLocationId(customerCode, imsLocationEntity.getId());

            if (null != ownerCodeEntity) {
                owningInstitution = ownerCodeEntity.getInstitutionEntity().getInstitutionCode();
            }
        } catch (Exception e) {
            log.error(ScsbConstants.EXCEPTION,e);
        }
        return owningInstitution;
    }


    /**
     * This method is used to create dummy record if the item barcode is not found.
     * @param response
     * @param owningInstitution
     * @param reportDataEntityList
     * @param accessionRequest
     */
    public String createDummyRecordIfAny(String response, String owningInstitution, List<ReportDataEntity> reportDataEntityList, AccessionRequest accessionRequest, ImsLocationEntity imsLocationEntity) {
        String message = response;
        if (response != null && (response.contains(ScsbConstants.ITEM_BARCODE_NOT_FOUND) ||
                response.contains(ScsbConstants.INVALID_MARC_XML_ERROR_MSG)) ) {
            BibliographicEntity fetchBibliographicEntity = getBibEntityUsingBarcodeForIncompleteRecord(accessionRequest.getItemBarcode());
            if (fetchBibliographicEntity == null) {
                String dummyRecordResponse = createDummyRecord(accessionRequest, owningInstitution,imsLocationEntity);
                message = response+", "+dummyRecordResponse;
            } else {
                message = ScsbConstants.ITEM_BARCODE_ALREADY_ACCESSIONED_MSG;
            }
        }
        return message;
    }

    /**
     * This method is used to get the BibliographicEntity from ItemEntity list using item barcode.
     * @param itemBarcode
     * @return
     */
    private BibliographicEntity getBibEntityUsingBarcodeForIncompleteRecord(String itemBarcode){
        List<String> itemBarcodeList = new ArrayList<>();
        itemBarcodeList.add(itemBarcode);
        List<ItemEntity> itemEntityList = itemDetailsRepository.findByBarcodeIn(itemBarcodeList);
        BibliographicEntity fetchedBibliographicEntity = null;
        if(itemEntityList != null && !itemEntityList.isEmpty() && itemEntityList.get(0).getBibliographicEntities() != null){
            fetchedBibliographicEntity = itemEntityList.get(0).getBibliographicEntities().get(0);
        }
        return fetchedBibliographicEntity;
    }

    /**
     * This method is used to create dummy record for bib in the database and index them in Solr.
     * @param accessionRequest
     * @param owningInstitution
     * @return
     */
    public String createDummyRecord(AccessionRequest accessionRequest, String owningInstitution,ImsLocationEntity imsLocationEntity) {
        String response;
        Integer owningInstitutionId = (Integer) getInstitutionEntityMap().get(owningInstitution);
        BibliographicEntity dummyBibliographicEntity = dummyDataService.createDummyDataAsIncomplete(owningInstitutionId,accessionRequest.getItemBarcode(),accessionRequest.getCustomerCode(),imsLocationEntity);
        indexData(Set.of(dummyBibliographicEntity.getId()));
        response = ScsbConstants.ACCESSION_DUMMY_RECORD;
        return response;
    }

    /**
     * Index data string.
     *
     * @param bibliographicIdList the bibliographic id list
     * @return the string
     */
    public String indexData(Set<Integer> bibliographicIdList){
        return getRestTemplate().postForObject(scsbSolrClientUrl + "solrIndexer/indexByBibliographicId", bibliographicIdList, String.class);
    }

    private synchronized Map<String, Integer> getInstitutionEntityMap() {
        if (null == institutionEntityMap) {
            institutionEntityMap = new HashMap<>();
            try {
                Iterable<InstitutionEntity> institutionEntities = institutionDetailsRepository.findAll();
                for (InstitutionEntity institutionEntity : institutionEntities) {
                    institutionEntityMap.put(institutionEntity.getInstitutionCode(), institutionEntity.getId());
                }
            } catch (Exception e) {
                log.error(ScsbConstants.EXCEPTION,e);
            }
        }
        return institutionEntityMap;
    }

    /**
     * This method is used to update the incoming data to the existing bib or create a new bib and save them in database,
     * Once saved in database they are indexed in Solr.
     * @param record
     * @param owningInstitution
     * @param responseMapList
     * @param accessionRequest
     * @return
     */
    public synchronized String updateData(Object record, String owningInstitution, List<Map<String, String>> responseMapList, AccessionRequest accessionRequest, boolean isValidBoundWithRecord, boolean isFirstRecord,ImsLocationEntity imsLocationEntity){
        String response = null;
        ILSConfigProperties ilsConfigProperties = propertyUtil.getILSConfigProperties(owningInstitution);
        AccessionXmlToBibEntityConverterInterface converter = xmlToBibEntityConverterFactory.getConverter(ilsConfigProperties.getBibDataFormat());
        if (null != converter) {
            Map responseMap = converter.convert(record, owningInstitution, accessionRequest,imsLocationEntity);
            responseMapList.add(responseMap);
            StringBuilder errorMessage = (StringBuilder)responseMap.get("errorMessage");
            BibliographicEntity bibliographicEntity = (BibliographicEntity) responseMap.get(ScsbCommonConstants.BIBLIOGRAPHICENTITY);
            String incompleteResponse = (String) responseMap.get(ScsbConstants.INCOMPLETE_RESPONSE);
            if (errorMessage != null && errorMessage.length()==0) {//Valid bibliographic entity is returned for further processing
                if (bibliographicEntity != null) {
                    boolean isValidItemAndHolding = accessionValidationService.validateItemAndHolding(bibliographicEntity,isValidBoundWithRecord,isFirstRecord,errorMessage);
                    if (isValidItemAndHolding) {
                        BibliographicEntity savedBibliographicEntity = updateBibliographicEntity(bibliographicEntity);
                        if (null != savedBibliographicEntity) {
                            response = indexBibliographicRecord(savedBibliographicEntity.getId());
                        }
                    } else {
                        response = errorMessage.toString();
                    }
                }
                if (StringUtils.isNotEmpty(response) && StringUtils.isNotEmpty(incompleteResponse) && ScsbCommonConstants.SUCCESS.equalsIgnoreCase(response)){
                    return ScsbConstants.SUCCESS_INCOMPLETE_RECORD;
                }
            } else{
                if(errorMessage != null) {
                    return ScsbConstants.FAILED + ScsbCommonConstants.HYPHEN + errorMessage.toString();
                }
                else {
                    return ScsbConstants.FAILED;
                }
            }

        }
        return response;
    }

    /**
     * This method is used to index Bibliographic Record in solr and return a response.
     * @param bibliographicId
     * @return
     */
    private String indexBibliographicRecord(Integer bibliographicId) {
        String response;
        indexData(Set.of(bibliographicId));
        response = ScsbCommonConstants.SUCCESS;
        return response;
    }

    /**
     *This method is used to update bibs if exists or create and save the bibs.
     * @param bibliographicEntity
     * @return
     */
    public BibliographicEntity updateBibliographicEntity(BibliographicEntity bibliographicEntity) {
        BibliographicEntity savedBibliographicEntity=null;
        BibliographicEntity fetchBibliographicEntity = bibliographicDetailsRepository.findByOwningInstitutionIdAndOwningInstitutionBibId(bibliographicEntity.getOwningInstitutionId(),bibliographicEntity.getOwningInstitutionBibId());
        if(fetchBibliographicEntity ==null) { // New Bib Record
            checkAndSetMAQualifier(bibliographicEntity, null);
            savedBibliographicEntity = bibliographicRepositoryDAO.saveOrUpdate(bibliographicEntity);
        }else{ // Existing bib Record
            // Bib
            checkAndSetMAQualifier(bibliographicEntity, fetchBibliographicEntity);
            fetchBibliographicEntity.setContent(bibliographicEntity.getContent());
            fetchBibliographicEntity.setLastUpdatedBy(bibliographicEntity.getLastUpdatedBy());
            fetchBibliographicEntity.setLastUpdatedDate(bibliographicEntity.getLastUpdatedDate());
            fetchBibliographicEntity.setDeleted(bibliographicEntity.isDeleted());
            if (fetchBibliographicEntity.getCatalogingStatus().equals(ScsbCommonConstants.INCOMPLETE_STATUS)) {
                fetchBibliographicEntity.setCatalogingStatus(bibliographicEntity.getCatalogingStatus());
            }

            // Holding
            List<HoldingsEntity> fetchHoldingsEntities =fetchBibliographicEntity.getHoldingsEntities();
            List<HoldingsEntity> holdingsEntities = bibliographicEntity.getHoldingsEntities();

            log.info("Owning Inst Bib Id :  = {}",bibliographicEntity.getOwningInstitutionBibId());
            log.info("Fetched Item Entities = {}",fetchHoldingsEntities.size());
            log.info("Incoming Item Entities = {}",holdingsEntities.size());

            for (Iterator<HoldingsEntity> iholdings = holdingsEntities.iterator(); iholdings.hasNext();) {
                HoldingsEntity holdingsEntity = iholdings.next();
                for (HoldingsEntity fetchHolding : fetchHoldingsEntities) {
                    if (fetchHolding.getOwningInstitutionHoldingsId().equalsIgnoreCase(holdingsEntity.getOwningInstitutionHoldingsId()) && fetchHolding.getOwningInstitutionId().intValue() == holdingsEntity.getOwningInstitutionId().intValue()) {
                        copyHoldingsEntity(fetchHolding, holdingsEntity);
                        iholdings.remove();
                    }else{
                        // Added for Boundwith scenarios
                        List<ItemEntity> fetchedItemEntityList = fetchHolding.getItemEntities();
                        List<ItemEntity> itemEntityList = holdingsEntity.getItemEntities();
                        if(CollectionUtils.isNotEmpty(itemEntityList)) {
                            List<ItemEntity> itemsToProcess = new ArrayList<>(itemEntityList);
                            for(ItemEntity fetchedItemEntity : fetchedItemEntityList){
                                for(ItemEntity itemEntity : itemsToProcess){
                                    if(fetchedItemEntity.getOwningInstitutionItemId().equals(itemEntity.getOwningInstitutionItemId())){
                                        copyHoldingsEntity(fetchHolding,holdingsEntity);
                                        iholdings.remove();
                                    }
                                }
                            }
                        }

                    }
                }
            }
            fetchHoldingsEntities.addAll(holdingsEntities);
            log.info("Holding Final Count = {}",fetchHoldingsEntities.size());


            // Item
            List<ItemEntity> fetchItemsEntities =fetchBibliographicEntity.getItemEntities();
            List<ItemEntity> incomingItemsEntities = bibliographicEntity.getItemEntities();

            log.info("Fetched Item Entities = {}",CollectionUtils.isNotEmpty(fetchItemsEntities) ? fetchItemsEntities.size() : 0);
            log.info("Incoming Item Entities = {}",CollectionUtils.isNotEmpty(incomingItemsEntities) ? incomingItemsEntities.size() : 0);

            List<ItemEntity> finalItemEntities = new ArrayList<>();

            for (HoldingsEntity holdingsEntity : fetchHoldingsEntities) {
                finalItemEntities.addAll(holdingsEntity.getItemEntities());
            }

            log.info("Item Final Count = {}",finalItemEntities.size());

            fetchBibliographicEntity.setHoldingsEntities(fetchHoldingsEntities);
            fetchBibliographicEntity.setItemEntities(finalItemEntities);

            savedBibliographicEntity = saveBibRecord(fetchBibliographicEntity);
        }
        return savedBibliographicEntity;
    }

    private void checkAndSetMAQualifier(BibliographicEntity bibliographicEntity, BibliographicEntity fetchBibliographicEntity) {
        if (bibliographicEntity.getItemEntities() != null) {
            ItemEntity incomingItemEntity = bibliographicEntity.getItemEntities().get(0);
            String incomingItemCgd = setupDataService.getCollectionGroupIdCodeMap().get(incomingItemEntity.getCollectionGroupId());
            if (ScsbCommonConstants.SHARED_CGD.equalsIgnoreCase(incomingItemCgd)) {
                bibliographicEntity.setMaQualifier(ScsbCommonConstants.MA_QUALIFIER_3);
            } else {
                if (null != fetchBibliographicEntity && ScsbCommonConstants.MA_QUALIFIER_3.intValue() != fetchBibliographicEntity.getMaQualifier()) {
                    fetchBibliographicEntity.setMaQualifier(ScsbCommonConstants.MA_QUALIFIER_1);
                } else {
                    bibliographicEntity.setMaQualifier(ScsbCommonConstants.MA_QUALIFIER_1);
                }
            }
        }
    }

    public BibliographicEntity saveBibRecord(BibliographicEntity fetchBibliographicEntity) {
        try {
            return bibliographicRepositoryDAO.saveOrUpdate(fetchBibliographicEntity);
        } catch (Exception e) {
            log.info(ScsbConstants.EXCEPTION,e);
        }
        return null;
    }

    private HoldingsEntity copyHoldingsEntity(HoldingsEntity fetchHoldingsEntity, HoldingsEntity holdingsEntity){
        fetchHoldingsEntity.setContent(holdingsEntity.getContent());
        fetchHoldingsEntity.setLastUpdatedBy(holdingsEntity.getLastUpdatedBy());
        fetchHoldingsEntity.setLastUpdatedDate(holdingsEntity.getLastUpdatedDate());
        fetchHoldingsEntity.setDeleted(holdingsEntity.isDeleted());
        List<ItemEntity> fetchedItemEntities = fetchHoldingsEntity.getItemEntities();

        processItems(fetchedItemEntities, holdingsEntity.getItemEntities());
        fetchHoldingsEntity.setItemEntities(fetchedItemEntities);

        return fetchHoldingsEntity;
    }

    private void processItems(List<ItemEntity> fetchItemsEntities, List<ItemEntity> itemsEntities) {
        for (Iterator<ItemEntity> iItems = itemsEntities.iterator(); iItems.hasNext();) {
            ItemEntity itemEntity = iItems.next();
            for (ItemEntity fetchItem : fetchItemsEntities) {
                if (fetchItem.getOwningInstitutionItemId().equalsIgnoreCase(itemEntity.getOwningInstitutionItemId()) && fetchItem.getOwningInstitutionId().intValue() == itemEntity.getOwningInstitutionId().intValue()) {
                    copyItemEntity(fetchItem, itemEntity);
                    iItems.remove();
                }
            }
        }
        fetchItemsEntities.addAll(itemsEntities);
    }

    private ItemEntity copyItemEntity(ItemEntity fetchItemEntity, ItemEntity itemEntity){
        fetchItemEntity.setBarcode(itemEntity.getBarcode());
        fetchItemEntity.setLastUpdatedBy(itemEntity.getLastUpdatedBy());
        fetchItemEntity.setLastUpdatedDate(itemEntity.getLastUpdatedDate());
        fetchItemEntity.setCallNumber(itemEntity.getCallNumber());
        fetchItemEntity.setCustomerCode(itemEntity.getCustomerCode());
        fetchItemEntity.setCallNumberType(itemEntity.getCallNumberType());
        fetchItemEntity.setItemAvailabilityStatusId(itemEntity.getItemAvailabilityStatusId());
        fetchItemEntity.setCopyNumber(itemEntity.getCopyNumber());
        fetchItemEntity.setCollectionGroupId(itemEntity.getCollectionGroupId());
        fetchItemEntity.setUseRestrictions(itemEntity.getUseRestrictions());
        fetchItemEntity.setVolumePartYear(itemEntity.getVolumePartYear());
        fetchItemEntity.setDeleted(itemEntity.isDeleted());
        fetchItemEntity.setCatalogingStatus(itemEntity.getCatalogingStatus());
        return fetchItemEntity;
    }

    /**
     * This method is used to re-accession the item for the item which is de-accessioned.
     * @param itemEntityList
     * @return
     */
    public String reAccessionItem(List<ItemEntity> itemEntityList){
        try {
            for(ItemEntity itemEntity:itemEntityList){
                itemEntity.setDeleted(false);
                Date currentDateTime = new Date();
                itemEntity.setLastUpdatedDate(currentDateTime);
                itemEntity.setLastUpdatedBy(ScsbConstants.REACCESSION);

                for (HoldingsEntity holdingsEntity:itemEntity.getHoldingsEntities()) {
                    holdingsEntity.setDeleted(false);
                    holdingsEntity.setLastUpdatedDate(currentDateTime);
                    holdingsEntity.setLastUpdatedBy(ScsbConstants.REACCESSION);
                }
                for(BibliographicEntity bibliographicEntity:itemEntity.getBibliographicEntities()) {
                    String incomingItemCgd = setupDataService.getCollectionGroupIdCodeMap().get(itemEntity.getCollectionGroupId());
                    if (ScsbCommonConstants.SHARED_CGD.equalsIgnoreCase(incomingItemCgd)) {
                        bibliographicEntity.setMaQualifier(ScsbCommonConstants.MA_QUALIFIER_3);
                    } else {
                        if (!bibliographicEntity.isDeleted() && ScsbCommonConstants.MA_QUALIFIER_3.intValue() != bibliographicEntity.getMaQualifier()) {
                            bibliographicEntity.setMaQualifier(ScsbCommonConstants.MA_QUALIFIER_1);
                        } else {
                            bibliographicEntity.setMaQualifier(ScsbCommonConstants.MA_QUALIFIER_1);
                        }
                    }
                    bibliographicEntity.setDeleted(false);
                    bibliographicEntity.setLastUpdatedDate(currentDateTime);
                    bibliographicEntity.setLastUpdatedBy(ScsbConstants.REACCESSION);
                }
            }
            itemDetailsRepository.saveAll(itemEntityList);
            itemDetailsRepository.flush();
        } catch (Exception e) {
            log.error(ScsbConstants.EXCEPTION,e);
            return ScsbCommonConstants.FAILURE;
        }
        return ScsbCommonConstants.SUCCESS;
    }

    /**
     * This method is used to index the re-accessioned item in solr.
     * @param itemEntityList
     * @return
     */
    public String indexReaccessionedItem(List<ItemEntity> itemEntityList){
        try {
            for(ItemEntity itemEntity:itemEntityList){
                List<BibliographicEntity>  bibliographicEntities = itemEntity.getBibliographicEntities();
                for (BibliographicEntity bibliographicEntity:bibliographicEntities) {
                    indexBibliographicRecord(bibliographicEntity.getId());
                }
            }
        } catch (Exception e) {
            log.error(ScsbConstants.EXCEPTION,e);
            return ScsbCommonConstants.FAILURE;
        }
        return ScsbCommonConstants.SUCCESS;
    }

    public void saveItemChangeLogEntity(String operationType, String message, List<ItemEntity> itemEntityList) {
        List<ItemChangeLogEntity> itemChangeLogEntityList = new ArrayList<>();
        for (ItemEntity itemEntity:itemEntityList) {
            ItemChangeLogEntity itemChangeLogEntity = new ItemChangeLogEntity();
            itemChangeLogEntity.setOperationType(ScsbCommonConstants.ACCESSION);
            itemChangeLogEntity.setUpdatedBy(operationType);
            itemChangeLogEntity.setUpdatedDate(new Date());
            itemChangeLogEntity.setRecordId(itemEntity.getId());
            itemChangeLogEntity.setNotes(message);
            itemChangeLogEntityList.add(itemChangeLogEntity);
        }
        itemChangeLogDetailsRepository.saveAll(itemChangeLogEntityList);
    }

    /**
     * This method is used to save the ReportEntity in the database.
     * @param owningInstitution
     * @param reportDataEntityList
     */
    @Transactional
    public void saveReportEntity(String owningInstitution, List<ReportDataEntity> reportDataEntityList) {
        ReportEntity reportEntity;
        reportEntity = getReportEntity(owningInstitution!=null ? owningInstitution : ScsbConstants.UNKNOWN_INSTITUTION);
        reportEntity.setReportDataEntities(reportDataEntityList);
        producerTemplate.sendBody(ScsbConstants.ACCESSION_REPORT_Q, reportEntity);
    }

    private ReportEntity getReportEntity(String owningInstitution){
        ReportEntity reportEntity = new ReportEntity();
        reportEntity.setFileName(ScsbCommonConstants.ACCESSION_REPORT);
        reportEntity.setType(ScsbConstants.ONGOING_ACCESSION_REPORT);
        reportEntity.setInstitutionName(owningInstitution);
        reportEntity.setCreatedDate(new Date());
        return reportEntity;
    }
}
