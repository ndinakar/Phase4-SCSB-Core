package org.recap.util;

import com.csvreader.CsvWriter;
import com.poiji.bind.Poiji;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.marc4j.marc.Record;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.ILSConfigProperties;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.accession.AccessionResponse;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.ImsLocationEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.ItemBarcodeHistoryEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.jpa.ItemRefileResponse;
import org.recap.model.jpa.ReportDataEntity;
import org.recap.model.jpa.ReportEntity;
import org.recap.model.request.ItemCheckInRequest;
import org.recap.model.request.ItemCheckinResponse;
import org.recap.model.xl.ItemHoldingData;
import org.recap.model.xl.ItemReader;
import org.recap.repository.jpa.HoldingsDetailsRepository;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.jpa.ItemBarcodeHistoryDetailsRepository;
import org.recap.repository.jpa.ItemDetailsRepository;
import org.recap.repository.jpa.ReportDetailRepository;
import org.recap.service.accession.AccessionInterface;
import org.recap.service.accession.AccessionResolverFactory;
import org.recap.spring.SwaggerAPIProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sheiks on 26/05/17.
 */
@Service
@EnableAsync
public class AccessionProcessService {

    private static final Logger logger = LoggerFactory.getLogger(AccessionProcessService.class);

    @Autowired
    private ItemDetailsRepository itemDetailsRepository;

    @Autowired
    private ReportDetailRepository reportDetailRepository;

    @Autowired
    private ItemBarcodeHistoryDetailsRepository itemBarcodeHistoryDetailsRepository;

    @Autowired
    private InstitutionDetailsRepository institutionDetailsRepository;

    @Autowired
    CsvUtil csvUtil;

    @Autowired
    MarcUtil marcUtil;

    @Autowired
    HoldingsDetailsRepository holdingsDetailsRepository;

    @Autowired
    AccessionUtil accessionUtil;

    @Autowired
    PropertyUtil propertyUtil;

    @Autowired
    AccessionResolverFactory accessionResolverFactory;

    private Map<String, Integer> institutionEntityMap;

    @Value("${" + PropertyKeyConstants.SCSB_GATEWAY_URL + "}")
    private String scsbUrl;

    public Object processRecords(Set<AccessionResponse> accessionResponses, List<Map<String, String>> responseMaps,
                                 AccessionRequest accessionRequest, List<ReportDataEntity> reportDataEntitys,
                                 String owningInstitution, boolean writeToReport,ImsLocationEntity imsLocationEntity) {
        String customerCode = accessionRequest.getCustomerCode();
        String itemBarcode = accessionRequest.getItemBarcode();
        // Check item availability
        List<ItemEntity> itemEntityList = getItemEntityList(itemBarcode, customerCode);
        boolean itemExists = checkItemBarcodeAlreadyExist(itemEntityList);
        if (itemExists) { // If available check deaccessioned item or not

            boolean isDeaccessionedItem = isItemDeaccessioned(itemEntityList);
            if (isDeaccessionedItem) { // If deacccessioned item make it available
                String response = accessionUtil.reAccessionItem(itemEntityList);
                if (response.equals(ScsbCommonConstants.SUCCESS)) {
                    response = accessionUtil.indexReaccessionedItem(itemEntityList);
                    accessionUtil.saveItemChangeLogEntity(ScsbConstants.REACCESSION, ScsbConstants.ITEM_ISDELETED_TRUE_TO_FALSE, itemEntityList);
                    reAccessionedCheckin(itemEntityList);
                }
                accessionUtil.setAccessionResponse(accessionResponses, itemBarcode, response);
                reportDataEntitys.addAll(accessionUtil.createReportDataEntityList(accessionRequest, response));
            } else { // else, error response
                String itemAreadyAccessionedMessage;
                if (CollectionUtils.isNotEmpty(itemEntityList.get(0).getBibliographicEntities())) {
                    String itemAreadyAccessionedOwnInstBibId = itemEntityList.get(0).getBibliographicEntities() != null ? itemEntityList.get(0).getBibliographicEntities().get(0).getOwningInstitutionBibId() : " ";
                    String itemAreadyAccessionedOwnInstHoldingId = itemEntityList.get(0).getHoldingsEntities() != null ? itemEntityList.get(0).getHoldingsEntities().get(0).getOwningInstitutionHoldingsId() : " ";
                    itemAreadyAccessionedMessage = ScsbConstants.ITEM_ALREADY_ACCESSIONED + ScsbConstants.OWN_INST_BIB_ID + itemAreadyAccessionedOwnInstBibId + ScsbConstants.OWN_INST_HOLDING_ID + itemAreadyAccessionedOwnInstHoldingId + ScsbConstants.OWN_INST_ITEM_ID + itemEntityList.get(0).getOwningInstitutionItemId();
                } else {
                    itemAreadyAccessionedMessage = ScsbConstants.ITEM_ALREADY_ACCESSIONED;
                }
                accessionUtil.setAccessionResponse(accessionResponses, itemBarcode, itemAreadyAccessionedMessage);
                reportDataEntitys.addAll(accessionUtil.createReportDataEntityList(accessionRequest, itemAreadyAccessionedMessage));
            }

        } else { // If not available

            ILSConfigProperties ilsConfigProperties = propertyUtil.getILSConfigProperties(owningInstitution);
            AccessionInterface formatResolver = accessionResolverFactory.getFormatResolver(ilsConfigProperties.getBibDataFormat());

            // Call ILS - Bib Data API
            String bibData = getBibData(accessionResponses, accessionRequest, reportDataEntitys, owningInstitution, customerCode, itemBarcode, formatResolver,imsLocationEntity);
            if (bibData != null) {
                try { // Check whether owningInsitutionItemId attached with another barcode.
                    Object unmarshalObject = formatResolver.unmarshal(bibData);
                    Integer owningInstitutionId = getInstitutionIdCodeMap().get(owningInstitution);
                    ItemEntity itemEntity = formatResolver.getItemEntityFromRecord(unmarshalObject, owningInstitutionId);
                    boolean accessionProcess = formatResolver.isAccessionProcess(itemEntity, owningInstitution);
                    // Process XML Record
                    if (accessionProcess) { // Accession process
                        formatResolver.processXml(accessionResponses, unmarshalObject, responseMaps, owningInstitution, reportDataEntitys, accessionRequest,imsLocationEntity);
                        callCheckin(accessionRequest.getItemBarcode(), owningInstitution);
                    } else {  // If attached
                        String oldBarcode = itemEntity.getBarcode();
                        // update item record with new barcode. Accession Process
                        formatResolver.processXml(accessionResponses, unmarshalObject, responseMaps, owningInstitution, reportDataEntitys, accessionRequest,imsLocationEntity);
                        callCheckin(accessionRequest.getItemBarcode(), owningInstitution);
                        // Move item record information to history table
                        ItemBarcodeHistoryEntity itemBarcodeHistoryEntity = prepareBarcodeHistoryEntity(itemEntity, itemBarcode, oldBarcode);
                        itemBarcodeHistoryDetailsRepository.save(itemBarcodeHistoryEntity);
                    }
                } catch (Exception e) {
                    logger.info("Exception occured in accession process : {}",e.getMessage());
                    if (writeToReport) {
                        processException(accessionResponses, accessionRequest, reportDataEntitys, owningInstitution,imsLocationEntity ,e);
                    } else {
                        return accessionRequest;
                    }
                }
            }
            generateAccessionSummaryReport(responseMaps, owningInstitution);
        }

        // Save report
        accessionUtil.saveReportEntity(owningInstitution, reportDataEntitys);

        return accessionResponses;
    }

    public String getBibData(Set<AccessionResponse> accessionResponses, AccessionRequest accessionRequest, List<ReportDataEntity> reportDataEntitys, String owningInstitution, String customerCode, String itemBarcode, AccessionInterface formatResolver,ImsLocationEntity imsLocationEntity) {
        String bibData = null;
        StopWatch individualStopWatch = new StopWatch();
        individualStopWatch.start();
        try {
            // Calling ILS - Bib Data API
            bibData = formatResolver.getBibData(itemBarcode, customerCode, owningInstitution);
        } catch (Exception e) { // Process dummy record if record not found in ILS
            processException(accessionResponses, accessionRequest, reportDataEntitys, owningInstitution,imsLocationEntity, e);
        } finally {
            individualStopWatch.stop();
            logger.info("Time taken to get bib data from {} ILS : {}", owningInstitution, individualStopWatch.getTotalTimeSeconds());
        }
        return bibData;
    }


    /**
     * Get item entity list for the given item barcode and customer code.
     *
     * @param itemBarcode  the item barcode
     * @param customerCode the customer code
     * @return the list
     */
    public List<ItemEntity> getItemEntityList(String itemBarcode, String customerCode) {
        return itemDetailsRepository.findByBarcodeAndCustomerCode(itemBarcode, customerCode);
    }

    /**
     * This method checks item barcode already exist for the given item list.
     *
     * @param itemEntityList the item entity list
     * @return the boolean
     */
    public boolean checkItemBarcodeAlreadyExist(List<ItemEntity> itemEntityList) {
        boolean itemExists = false;
        if (itemEntityList != null && !itemEntityList.isEmpty()) {
            itemExists = true;
        }
        return itemExists;
    }

    /**
     * This method checks is item deaccessioned for the given item list.
     *
     * @param itemEntityList the item entity list
     * @return the boolean
     */
    public boolean isItemDeaccessioned(List<ItemEntity> itemEntityList) {
        boolean itemDeleted = false;
        if (itemEntityList != null && !itemEntityList.isEmpty()) {
            for (ItemEntity itemEntity : itemEntityList) {
                if(itemEntity.isDeleted()) {
                    return itemEntity.isDeleted();
                }
            }
        }
        return itemDeleted;
    }

    /**
     * This method is used to generate AccessionSummary Report
     * <p>
     * It saves the data in report_t and report_data_t
     *
     * @param responseMapList   the response map list
     * @param owningInstitution the owning institution
     */
    public void generateAccessionSummaryReport(List<Map<String, String>> responseMapList, String owningInstitution) {
        int successBibCount = 0;
        int successItemCount = 0;
        int failedBibCount = 0;
        int failedItemCount = 0;
        int exitsBibCount = 0;
        String reasonForFailureBib = "";
        String reasonForFailureItem = "";

        for (Map responseMap : responseMapList) {
            successBibCount = successBibCount + (responseMap.get(ScsbCommonConstants.SUCCESS_BIB_COUNT) != null ? (Integer) responseMap.get(ScsbCommonConstants.SUCCESS_BIB_COUNT) : 0);
            failedBibCount = failedBibCount + (responseMap.get(ScsbCommonConstants.FAILED_BIB_COUNT) != null ? (Integer) responseMap.get(ScsbCommonConstants.FAILED_BIB_COUNT) : 0);
            if (failedBibCount == 0) {
                if (StringUtils.isEmpty((String) responseMap.get(ScsbCommonConstants.REASON_FOR_ITEM_FAILURE))) {
                    successItemCount = 1;
                } else {
                    failedItemCount = 1;
                }
            }
            exitsBibCount = exitsBibCount + (responseMap.get(ScsbCommonConstants.EXIST_BIB_COUNT) != null ? (Integer) responseMap.get(ScsbCommonConstants.EXIST_BIB_COUNT) : 0);

            if (!StringUtils.isEmpty((String) responseMap.get(ScsbCommonConstants.REASON_FOR_BIB_FAILURE)) && !reasonForFailureBib.contains(responseMap.get(ScsbCommonConstants.REASON_FOR_BIB_FAILURE).toString())) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(responseMap.get(ScsbCommonConstants.REASON_FOR_BIB_FAILURE));
                stringBuilder.append(",");
                stringBuilder.append(reasonForFailureBib);
                reasonForFailureBib = stringBuilder.toString();
            }
            if ((!StringUtils.isEmpty((String) responseMap.get(ScsbCommonConstants.REASON_FOR_ITEM_FAILURE))) && StringUtils.isEmpty(reasonForFailureBib) &&
                    !reasonForFailureItem.contains((String) responseMap.get(ScsbCommonConstants.REASON_FOR_ITEM_FAILURE))) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(responseMap.get(ScsbCommonConstants.REASON_FOR_ITEM_FAILURE));
                stringBuilder.append(",");
                stringBuilder.append(reasonForFailureItem);
                reasonForFailureItem = stringBuilder.toString();
            }
        }

        List<ReportEntity> reportEntityList = new ArrayList<>();
        List<ReportDataEntity> reportDataEntities = new ArrayList<>();
        ReportEntity reportEntity = new ReportEntity();
        reportEntity.setFileName(ScsbCommonConstants.ACCESSION_REPORT);
        reportEntity.setType(ScsbCommonConstants.ACCESSION_SUMMARY_REPORT);
        reportEntity.setCreatedDate(new Date());
        reportEntity.setInstitutionName(owningInstitution);

        ReportDataEntity successBibCountReportDataEntity = new ReportDataEntity();
        successBibCountReportDataEntity.setHeaderName(ScsbCommonConstants.BIB_SUCCESS_COUNT);
        successBibCountReportDataEntity.setHeaderValue(String.valueOf(successBibCount));
        reportDataEntities.add(successBibCountReportDataEntity);

        ReportDataEntity successItemCountReportDataEntity = new ReportDataEntity();
        successItemCountReportDataEntity.setHeaderName(ScsbCommonConstants.ITEM_SUCCESS_COUNT);
        successItemCountReportDataEntity.setHeaderValue(String.valueOf(successItemCount));
        reportDataEntities.add(successItemCountReportDataEntity);

        ReportDataEntity existsBibCountReportDataEntity = new ReportDataEntity();
        existsBibCountReportDataEntity.setHeaderName(ScsbCommonConstants.NUMBER_OF_BIB_MATCHES);
        existsBibCountReportDataEntity.setHeaderValue(String.valueOf(exitsBibCount));
        reportDataEntities.add(existsBibCountReportDataEntity);

        ReportDataEntity failedBibCountReportDataEntity = new ReportDataEntity();
        failedBibCountReportDataEntity.setHeaderName(ScsbCommonConstants.BIB_FAILURE_COUNT);
        failedBibCountReportDataEntity.setHeaderValue(String.valueOf(failedBibCount));
        reportDataEntities.add(failedBibCountReportDataEntity);

        ReportDataEntity failedItemCountReportDataEntity = new ReportDataEntity();
        failedItemCountReportDataEntity.setHeaderName(ScsbCommonConstants.ITEM_FAILURE_COUNT);
        failedItemCountReportDataEntity.setHeaderValue(String.valueOf(failedItemCount));
        reportDataEntities.add(failedItemCountReportDataEntity);

        ReportDataEntity reasonForBibFailureReportDataEntity = new ReportDataEntity();
        reasonForBibFailureReportDataEntity.setHeaderName(ScsbConstants.FAILURE_BIB_REASON);
        if (reasonForFailureBib.startsWith("\n")) {
            reasonForFailureBib = reasonForFailureBib.substring(1, reasonForFailureBib.length() - 1);
        }
        reasonForFailureBib = reasonForFailureBib.replaceAll("\n", ",");
        reasonForFailureBib = reasonForFailureBib.replaceAll(",$", "");
        reasonForBibFailureReportDataEntity.setHeaderValue(reasonForFailureBib);
        reportDataEntities.add(reasonForBibFailureReportDataEntity);

        ReportDataEntity reasonForItemFailureReportDataEntity = new ReportDataEntity();
        reasonForItemFailureReportDataEntity.setHeaderName(ScsbConstants.FAILURE_ITEM_REASON);
        if (reasonForFailureItem.startsWith("\n")) {
            reasonForFailureItem = reasonForFailureItem.substring(1, reasonForFailureItem.length() - 1);
        }
        reasonForFailureItem = reasonForFailureItem.replaceAll("\n", ",");
        reasonForFailureItem = reasonForFailureItem.replaceAll(",$", "");
        reasonForItemFailureReportDataEntity.setHeaderValue(reasonForFailureItem);
        reportDataEntities.add(reasonForItemFailureReportDataEntity);

        reportEntity.setReportDataEntities(reportDataEntities);
        reportEntityList.add(reportEntity);
        reportDetailRepository.saveAll(reportEntityList);
    }

    public void processException(Set<AccessionResponse> accessionResponsesList, AccessionRequest accessionRequest,
                                 List<ReportDataEntity> reportDataEntityList, String owningInstitution,ImsLocationEntity imsLocationEntity ,Exception ex) {
        String response = ex.getMessage();
        if (StringUtils.contains(response, ScsbConstants.ITEM_BARCODE_NOT_FOUND)) {
            logger.error(ScsbCommonConstants.LOG_ERROR, response);
        } else if (StringUtils.contains(response, ScsbConstants.MARC_FORMAT_PARSER_ERROR)) {
            logger.error(ScsbCommonConstants.LOG_ERROR, response);
            response = ScsbConstants.INVALID_MARC_XML_ERROR_MSG;
            logger.error(ScsbConstants.EXCEPTION, ex);
        } else {
            response = ScsbConstants.EXCEPTION + response;
            logger.error(ScsbConstants.EXCEPTION, ex);
        }
        //Create dummy record
        response = accessionUtil.createDummyRecordIfAny(response, owningInstitution, reportDataEntityList, accessionRequest,imsLocationEntity);
        accessionUtil.setAccessionResponse(accessionResponsesList, accessionRequest.getItemBarcode(), response);
        reportDataEntityList.addAll(accessionUtil.createReportDataEntityList(accessionRequest, response));
    }


    public List<AccessionRequest> removeDuplicateRecord(List<AccessionRequest> trimmedAccessionRequests) {
        Set<AccessionRequest> accessionRequests = new HashSet<>(trimmedAccessionRequests);
        return new ArrayList<>(accessionRequests);
    }

    @Async
    public void callCheckin(String itemBarcode, String owningInstitutionId) {
        ItemCheckInRequest itemRequestInfo = new ItemCheckInRequest();
        RestTemplate restTemplate = new RestTemplate();
        try {
            itemRequestInfo.setItemBarcodes(Collections.singletonList(itemBarcode));
            itemRequestInfo.setItemOwningInstitution(owningInstitutionId);
            ILSConfigProperties ilsConfigProperties = propertyUtil.getILSConfigProperties(owningInstitutionId);
            if ("REST".equalsIgnoreCase(ilsConfigProperties.getIlsRefileEndpointProtocol())) {
                HttpEntity request = new HttpEntity<>(getHttpHeadersAuth());
                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(scsbUrl + ScsbConstants.SERVICEPATH.REFILE_ITEM_IN_ILS);
                builder.queryParam(ScsbCommonConstants.ITEMBARCODE, itemBarcode);
                builder.queryParam(ScsbConstants.OWNING_INST, owningInstitutionId);
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                ResponseEntity<ItemRefileResponse> responseEntity = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.POST, request, ItemRefileResponse.class);
                stopWatch.stop();
                logger.info("Time taken to refile item barcode {} is : {}", itemBarcode, stopWatch.getTotalTimeSeconds());
                logger.info("Refile response for item barcode {} : {}", itemBarcode, null != responseEntity.getBody() ? responseEntity.getBody().getScreenMessage() : null);
            } else {
                HttpEntity request = new HttpEntity<>(itemRequestInfo, getHttpHeadersAuth());
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                ResponseEntity<ItemCheckinResponse> responseEntity = restTemplate.exchange(scsbUrl + ScsbConstants.SERVICEPATH.CHECKIN_ITEM, HttpMethod.POST, request, ItemCheckinResponse.class);
                stopWatch.stop();
                logger.info("Time taken to checkin item barcode {} in {} : {}", itemBarcode, owningInstitutionId, stopWatch.getTotalTimeSeconds());
                logger.info("Checkin response for item barcode {} : {}", itemBarcode, null != responseEntity.getBody() ? responseEntity.getBody().getScreenMessage() : null);
            }
        } catch (Exception ex) {
            logger.error(ScsbConstants.EXCEPTION, ex);
        }
    }

    @Async
    void reAccessionedCheckin(List<ItemEntity> itemEntityList) {
        if (itemEntityList != null && !itemEntityList.isEmpty()) {
            for (ItemEntity itemEntity : itemEntityList) {
                callCheckin(itemEntity.getBarcode(), itemEntity.getInstitutionEntity().getInstitutionCode());
            }
        }
    }

    private HttpHeaders getHttpHeadersAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(ScsbCommonConstants.API_KEY, SwaggerAPIProvider.getInstance().getSwaggerApiKey());
        return headers;
    }

    public ItemBarcodeHistoryEntity prepareBarcodeHistoryEntity(ItemEntity itemEntity, String newBarcode, String oldBarcode) {
        ItemBarcodeHistoryEntity itemBarcodeHistoryEntity = new ItemBarcodeHistoryEntity();
        itemBarcodeHistoryEntity.setOwningingInstitution(itemEntity.getInstitutionEntity().getInstitutionCode());
        itemBarcodeHistoryEntity.setOwningingInstitutionItemId(itemEntity.getOwningInstitutionItemId());
        itemBarcodeHistoryEntity.setOldBarcode(oldBarcode);
        itemBarcodeHistoryEntity.setNewBarcode(newBarcode);
        itemBarcodeHistoryEntity.setCreatedDate(new Date());
        return itemBarcodeHistoryEntity;
    }

    /**
     * Gets institution id and institution code from db and puts it into a map where status id as key and status code as value.
     *
     * @return the institution entity map
     */
    public synchronized Map<String,Integer> getInstitutionIdCodeMap() {
        if (null == institutionEntityMap) {
            institutionEntityMap = new HashMap<>();
            try {
                Iterable<InstitutionEntity> institutionEntities = institutionDetailsRepository.findAll();
                for (InstitutionEntity institutionEntity : institutionEntities) {
                    institutionEntityMap.put(institutionEntity.getInstitutionCode(), institutionEntity.getId());
                }
            } catch (Exception e) {
                logger.error(ScsbConstants.EXCEPTION,e);
            }
        }
        return institutionEntityMap;
    }

    public String updateItemHoldings() {
        SimpleDateFormat sdf = new SimpleDateFormat(ScsbConstants.DATE_FORMAT_FOR_REPORTS);
        String formattedDate = sdf.format(new Date());
        String fileNameWithExtension = "/recap-vol/reports/item-holding/" + File.separator + "ITEM_HOLDINGS_DATA-" + formattedDate + ScsbConstants.CSV_EXTENSION;
        File file = new File(fileNameWithExtension);
        CsvWriter csvOutput = null;
        try{
            FileWriter fileWriter = new FileWriter(file, true);
            csvOutput = new CsvWriter(fileWriter, ',');
            csvUtil.writeHeaderRowForItemHoldingReport(csvOutput);
        } catch (Exception e) {
            logger.info("EXCEPTION OCCURED WHILE UPDATING ITEMHLODINGS DATA"+e.getMessage());
        }
        String customerCode = "";
        String bibData = null;
        List<ItemReader> itemReaderList = Poiji.fromExcel(new File("/recap-vol/reports/item-holding/INPUT_DATA.xlsx"), ItemReader.class);
        List<ItemHoldingData> itemHoldingDataList = new ArrayList<>();
        for (ItemReader itemReader : itemReaderList) {
            try {
                ILSConfigProperties ilsConfigProperties = propertyUtil.getILSConfigProperties(itemReader.getInstitution());
                AccessionInterface formatResolver = accessionResolverFactory.getFormatResolver(ilsConfigProperties.getBibDataFormat());
                bibData = formatResolver.getItemHoldingData(itemReader.getBarcode(), customerCode, itemReader.getInstitution());
                Object unmarshalObject = formatResolver.unmarshal(bibData);
                List<Record> records = (List<Record>) unmarshalObject;
                String holdingId = null;
                String barcode = null;
                for (Record record : records) {
                    holdingId = marcUtil.getDataFieldValue(record, "876", '0');
                    barcode = marcUtil.getDataFieldValue(record, "876", 'p');
                    if (holdingId != null) {
                        break;
                    }
                }
                if (holdingId != null) {
                    HoldingsEntity holdingsEntity = holdingsDetailsRepository.findByOwningInstitutionHoldingsId(holdingId);
                    List<ItemEntity> itemEntity = itemDetailsRepository.findByBarcode(itemReader.getBarcode());
                    ItemHoldingData itemHoldingData = new ItemHoldingData();
                    if (itemEntity.size() > 0) {
                        itemHoldingData.setItemId(String.valueOf(itemEntity.get(0).getId()));
                    }
                    itemHoldingData.setHoldingId(holdingId);
                    if (holdingsEntity != null) {
                        itemHoldingData.setScsbHoldingId(String.valueOf(holdingsEntity.getId()));
                    }
                    itemHoldingData.setBarcode(barcode);
                    itemHoldingDataList.add(itemHoldingData);
                }
            } catch (Exception e) {
                logger.info("EXCEPTION OCCURED WHILE UPDATING ITEMHLODINGS DATA" + e.getMessage());
            }
        }
        try {
            csvUtil.writeDataRowForItemHoldingReport(itemHoldingDataList, csvOutput);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (csvOutput != null) {
            csvOutput.flush();
            csvOutput.close();
        }
        return "PROCESS IS DONE";
    }

}
