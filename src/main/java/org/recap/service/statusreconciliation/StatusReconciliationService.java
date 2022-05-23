package org.recap.service.statusreconciliation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.ItemRefileRequest;
import org.recap.model.csv.StatusReconciliationCSVRecord;
import org.recap.model.csv.StatusReconciliationErrorCSVRecord;
import org.recap.model.gfa.ScsbLasItemStatusCheckModel;
import org.recap.model.jpa.*;
import org.recap.repository.jpa.*;
import org.recap.service.SolrDocIndexService;
import org.recap.spring.SwaggerAPIProvider;
import org.recap.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StatusReconciliationService {

    @Value("${" + PropertyKeyConstants.SCSB_GATEWAY_URL + "}")
    private String scsbUrl;

    @Value("${" + PropertyKeyConstants.SCSB_CIRC_URL + "}")
    private String scsbCircUrl;

    @Value("${" + PropertyKeyConstants.STATUS_RECONCILIATION_DAY_LIMIT + "}")
    private Integer statusReconciliationDayLimit;

    @Value("${" + PropertyKeyConstants.STATUS_RECONCILIATION_REFILE_MAX_CAP_LIMIT + "}")
    private int statusReconciliationRefileMaxCapLimit;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private ItemDetailsRepository itemDetailsRepository;

    @Autowired
    private ItemStatusDetailsRepository itemStatusDetailsRepository;

    @Autowired
    private RequestItemDetailsRepository requestItemDetailsRepository;

    @Autowired
    RequestItemStatusDetailsRepository requestItemStatusDetailsRepository;

    @Autowired
    private ItemChangeLogDetailsRepository itemChangeLogDetailsRepository;

    @Autowired
    private SolrDocIndexService solrDocIndexService;

    /**
     * To compare the item status for the status reconciliation process with LAS .
     *
     * @param itemEntityChunkList                    the item entity chunk list
     * @param statusReconciliationErrorCSVRecordList the status reconciliation error csv record list
     * @return the list
     */
    public List<StatusReconciliationCSVRecord> itemStatusComparison(List<List<ItemEntity>> itemEntityChunkList, List<StatusReconciliationErrorCSVRecord> statusReconciliationErrorCSVRecordList, int refileCount) {
        List<StatusReconciliationCSVRecord> statusReconciliationCSVRecordList = new ArrayList<>();
        List<ItemChangeLogEntity> itemChangeLogEntityList = new ArrayList<>();
        for (List<ItemEntity> itemEntities : itemEntityChunkList) {
            List<ScsbLasItemStatusCheckModel> gfaItemStatusCheckResponseItems = getGFAItemStatusCheckResponse(itemEntities);
            if (CollectionUtils.isNotEmpty(gfaItemStatusCheckResponseItems)) {
                String lasStatus = null;
                StatusReconciliationErrorCSVRecord statusReconciliationErrorCSVRecord = new StatusReconciliationErrorCSVRecord();
                for (ItemEntity itemEntity : itemEntities) {
                    boolean isBarcodeAvailableForErrorReport = false;
                    for (ScsbLasItemStatusCheckModel modelItem : gfaItemStatusCheckResponseItems) {
                        if (itemEntity.getBarcode().equalsIgnoreCase(modelItem.getItemBarcode())) {
                            isBarcodeAvailableForErrorReport = true;
                            lasStatus = StringUtils.isNotBlank(modelItem.getItemStatus()) ? modelItem.getItemStatus().toUpperCase() : modelItem.getItemStatus();
                            boolean isRequestableNotRetrievable = commonUtil.checkIfImsItemStatusIsRequestableNotRetrievable(itemEntity.getImsLocationEntity().getImsLocationCode(), lasStatus);
                            if (isRequestableNotRetrievable) {
                                refileCount = processMismatchStatus(statusReconciliationCSVRecordList, itemChangeLogEntityList, lasStatus, itemEntity, false, true, refileCount);
                            } else {
                                boolean isAvailable = commonUtil.checkIfImsItemStatusIsAvailableOrNotAvailable(itemEntity.getImsLocationEntity().getImsLocationCode(), lasStatus, true);
                                if (isAvailable) {
                                    refileCount = processMismatchStatus(statusReconciliationCSVRecordList, itemChangeLogEntityList, lasStatus, itemEntity, false, true, refileCount);
                                } else {
                                    boolean isNotAvailable = commonUtil.checkIfImsItemStatusIsAvailableOrNotAvailable(itemEntity.getImsLocationEntity().getImsLocationCode(), lasStatus, false);
                                    if (!isNotAvailable) {
                                        refileCount = processMismatchStatus(statusReconciliationCSVRecordList, itemChangeLogEntityList, lasStatus, itemEntity, true, false, refileCount);
                                    }
                                }
                            }
                        }
                    }
                    if (!isBarcodeAvailableForErrorReport) {
                        statusReconciliationErrorCSVRecord.setBarcode(itemEntity.getBarcode());
                        statusReconciliationErrorCSVRecord.setInstitution(itemEntity.getInstitutionEntity().getInstitutionCode());
                        statusReconciliationErrorCSVRecord.setImsLocation(itemEntity.getImsLocationEntity().getImsLocationCode());
                        statusReconciliationErrorCSVRecord.setReasonForFailure(ScsbConstants.BARCODE_NOT_FOUND_OR_COULD_NOT_FIND_LAS_ITEM_STATUS);
                        statusReconciliationErrorCSVRecordList.add(statusReconciliationErrorCSVRecord);
                    }
                }
            }
            itemChangeLogDetailsRepository.saveAll(itemChangeLogEntityList);
            itemChangeLogDetailsRepository.flush();
        }
        return statusReconciliationCSVRecordList;
    }

    private List<ScsbLasItemStatusCheckModel> getGFAItemStatusCheckResponse(List<ItemEntity> itemEntities) {
        ResponseEntity<List<ScsbLasItemStatusCheckModel>> responseEntity = null;
        try {
            HttpEntity httpEntity = new HttpEntity<>(commonUtil.getScsbItemStatusModelListByItemEntities(itemEntities));
            RestTemplate restTemplate = new RestTemplate();
            responseEntity = restTemplate.exchange(scsbCircUrl + ScsbConstants.GFA_MULTIPLE_ITEM_STATUS_URL, HttpMethod.POST, httpEntity, new ParameterizedTypeReference<List<ScsbLasItemStatusCheckModel>>() {});
        } catch (Exception e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e);
        }
        return responseEntity != null ? responseEntity.getBody() : null;
    }

    private int processMismatchStatus(List<StatusReconciliationCSVRecord> statusReconciliationCSVRecordList, List<ItemChangeLogEntity> itemChangeLogEntityList, String lasStatus, ItemEntity itemEntity, boolean isUnknownCode, boolean refileRequired, int refileCount) {
        StatusReconciliationCSVRecord statusReconciliationCSVRecord = new StatusReconciliationCSVRecord();
        List<String> requestStatusCodes = Arrays.asList(ScsbCommonConstants.REQUEST_STATUS_RETRIEVAL_ORDER_PLACED, ScsbCommonConstants.REQUEST_STATUS_EDD, ScsbCommonConstants.REQUEST_STATUS_CANCELED, ScsbCommonConstants.REQUEST_STATUS_INITIAL_LOAD);
        List<RequestStatusEntity> requestStatusEntityList = requestItemStatusDetailsRepository.findByRequestStatusCodeIn(requestStatusCodes);
        List<Integer> requestStatusIds = requestStatusEntityList.stream().map(RequestStatusEntity::getId).collect(Collectors.toList());
        List<Integer> requestid = requestItemDetailsRepository.getRequestItemEntitiesBasedOnDayLimit(itemEntity.getId(), requestStatusIds, statusReconciliationDayLimit);
        List<RequestItemEntity> requestItemEntityList = requestItemDetailsRepository.findByIdIn(requestid);
        List<String> barcodeList = new ArrayList<>();
        List<Integer> requestIdList = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ItemStatusEntity itemStatusEntity = itemStatusDetailsRepository.findById(itemEntity.getItemAvailabilityStatusId()).orElse(new ItemStatusEntity());
        boolean isRefileCapNotExceeded = refileCount < statusReconciliationRefileMaxCapLimit;
        if (!requestItemEntityList.isEmpty()) {
            for (RequestItemEntity requestItemEntity : requestItemEntityList) {
                if (!requestItemEntity.getRequestStatusEntity().getRequestStatusCode().equalsIgnoreCase(ScsbCommonConstants.REQUEST_STATUS_CANCELED)) {
                    statusReconciliationCSVRecord = getStatusReconciliationCSVRecord(lasStatus, itemEntity, barcodeList, requestIdList, simpleDateFormat, itemStatusEntity, requestItemEntity, isUnknownCode, refileRequired, isRefileCapNotExceeded);
                } else {
                    if (StringUtils.containsIgnoreCase(requestItemEntity.getNotes(), "Cancel requested")) {
                        statusReconciliationCSVRecord = getStatusReconciliationCSVRecord(lasStatus, itemEntity, barcodeList, requestIdList, simpleDateFormat, itemStatusEntity, requestItemEntity, isUnknownCode, refileRequired, isRefileCapNotExceeded);
                    } else if (!isUnknownCode && isRefileCapNotExceeded && refileRequired) {
                        RequestStatusEntity byRequestStatusCode = requestItemStatusDetailsRepository.findByRequestStatusCode(ScsbCommonConstants.REQUEST_STATUS_REFILED);
                        requestItemEntity.setRequestStatusId(byRequestStatusCode.getId());
                        requestItemEntity.setLastUpdatedDate(new Date());
                        requestItemDetailsRepository.save(requestItemEntity);
                        log.info("request status updated from cancel to refile for the request id : {}", requestItemEntity.getId());
                    }
                }
            }
        } else {
            statusReconciliationCSVRecord = getStatusReconciliationCSVRecord(itemEntity.getBarcode(), itemEntity.getInstitutionEntity().getInstitutionCode(), null, ScsbConstants.NO, null, lasStatus, simpleDateFormat.format(new Date()),  null, itemStatusEntity, itemEntity.getImsLocationEntity().getImsLocationCode(), isUnknownCode, refileRequired, isRefileCapNotExceeded);
            if (!isUnknownCode && isRefileCapNotExceeded && refileRequired) {
                refileCount = refileCount + 1;
                itemDetailsRepository.updateAvailabilityStatus(1, ScsbConstants.GUEST_USER, itemEntity.getBarcode());
                ItemChangeLogEntity itemChangeLogEntity = saveItemChangeLogEntity(itemEntity.getId(), itemEntity.getBarcode());
                itemChangeLogEntityList.add(itemChangeLogEntity);
                solrDocIndexService.updateSolrIndex(itemEntity);
                log.info("found mismatch in item status and updated availability status for the item barcode: {}", itemEntity.getBarcode());
            }
        }
        if (!barcodeList.isEmpty() && !requestIdList.isEmpty() && isRefileCapNotExceeded && refileRequired) {
            refileCount = refileCount + barcodeList.size();
        }
        statusReconciliationCSVRecordList.add(statusReconciliationCSVRecord);
        return refileCount;
    }

    private StatusReconciliationCSVRecord getStatusReconciliationCSVRecord(String lasStatus, ItemEntity itemEntity, List<String> barcodeList, List<Integer> requestIdList, SimpleDateFormat simpleDateFormat, ItemStatusEntity itemStatusEntity, RequestItemEntity requestItemEntity, boolean isUnknownCode, boolean refileRequired, boolean isRefileCapNotExceeded) {
        StatusReconciliationCSVRecord statusReconciliationCSVRecord = getStatusReconciliationCSVRecord(itemEntity.getBarcode(), itemEntity.getInstitutionEntity().getInstitutionCode(), requestItemEntity.getInstitutionEntity().getInstitutionCode(), ScsbConstants.YES, requestItemEntity.getId().toString(), lasStatus, simpleDateFormat.format(new Date()), simpleDateFormat.format(requestItemEntity.getLastUpdatedDate()), itemStatusEntity, itemEntity.getImsLocationEntity().getImsLocationCode(), isUnknownCode, refileRequired, isRefileCapNotExceeded);
        if (!isUnknownCode && isRefileCapNotExceeded && refileRequired) {
            barcodeList.add(itemEntity.getBarcode());
            requestIdList.add(requestItemEntity.getId());
            log.info("found mismatch in item status and refilled for the item id: {}", requestItemEntity.getItemId());
        }
        return statusReconciliationCSVRecord;
    }

    @Async
    public void reFileItems(List<String> itemBarcodes, List<Integer> requestIdList) {
        ItemRefileRequest itemRequestInfo = new ItemRefileRequest();
        RestTemplate restTemplate = new RestTemplate();
        try {
            itemRequestInfo.setItemBarcodes(itemBarcodes);
            itemRequestInfo.setRequestIds(requestIdList);
            HttpEntity request = new HttpEntity<>(itemRequestInfo, getHttpHeadersAuth());
            restTemplate.exchange(scsbUrl + ScsbConstants.SERVICEPATH.REFILE_ITEM, HttpMethod.POST, request, ItemRefileResponse.class);
        } catch (Exception ex) {
            log.error(ScsbConstants.EXCEPTION, ex);
        }
    }

    /**
     * For the given input this method prepares the status reconciliation csv record.
     *
     * @param barcode          the barcode
     * @param availability     the availability
     * @param requestId        the request id
     * @param statusInLas      the status in las
     * @param requestedDateTime the requested date time
     * @param updatedDateTime  the updated date time
     * @param itemStatusEntity the item status entity
     * @return the status reconciliation csv record
     */
    public StatusReconciliationCSVRecord getStatusReconciliationCSVRecord(String barcode, String owningInstitution, String requestingInstitution, String availability, String requestId, String statusInLas, String updatedDateTime, String requestedDateTime, ItemStatusEntity itemStatusEntity, String imsLocationCode, boolean isUnknownCode, boolean refileRequired, boolean isRefileCapNotExceeded) {
        StatusReconciliationCSVRecord statusReconciliationCSVRecord = new StatusReconciliationCSVRecord();
        statusReconciliationCSVRecord.setBarcode(barcode);
        statusReconciliationCSVRecord.setRequestAvailability(availability);
        statusReconciliationCSVRecord.setOwningInstitution(owningInstitution);
        statusReconciliationCSVRecord.setRequestingInstitution(requestingInstitution);
        statusReconciliationCSVRecord.setRequestId(requestId);
        statusReconciliationCSVRecord.setStatusInLas(statusInLas);
        if (itemStatusEntity != null) {
            statusReconciliationCSVRecord.setStatusInScsb(itemStatusEntity.getStatusDescription());
        }
        statusReconciliationCSVRecord.setImsLocation(imsLocationCode);
        statusReconciliationCSVRecord.setRequestedDateTime(requestedDateTime);
        statusReconciliationCSVRecord.setUpdatedDateTime(updatedDateTime);
        if (isUnknownCode) {
            statusReconciliationCSVRecord.setReconciliationStatus(ScsbConstants.UNKNOWN_CODE);
        } else if (isRefileCapNotExceeded && refileRequired) {
            statusReconciliationCSVRecord.setReconciliationStatus(ScsbConstants.CHANGED_TO_AVAILABLE);
        } else {
            statusReconciliationCSVRecord.setReconciliationStatus(ScsbConstants.UNCHANGED);
        }
        return statusReconciliationCSVRecord;
    }

    private ItemChangeLogEntity saveItemChangeLogEntity(Integer requestId, String barcode) {
        ItemChangeLogEntity itemChangeLogEntity = new ItemChangeLogEntity();
        String notes = "ItemBarcode:" + barcode + " , " + "ItemAvailabilityStatusChange" + ScsbConstants.REQUEST_ITEM_AVAILABILITY_STATUS_DATA_ROLLBACK;
        itemChangeLogEntity.setUpdatedBy(ScsbConstants.GUEST_USER);
        itemChangeLogEntity.setUpdatedDate(new Date());
        itemChangeLogEntity.setOperationType(ScsbConstants.STATUS_RECONCILIATION_CHANGE_LOG_OPERATION_TYPE);
        itemChangeLogEntity.setRecordId(requestId);
        itemChangeLogEntity.setNotes(notes);
        return itemChangeLogEntity;
    }

    private HttpHeaders getHttpHeadersAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(ScsbCommonConstants.API_KEY, SwaggerAPIProvider.getInstance().getSwaggerApiKey());
        return headers;
    }
}
