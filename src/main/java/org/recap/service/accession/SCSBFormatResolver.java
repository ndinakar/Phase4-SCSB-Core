package org.recap.service.accession;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.recap.ScsbConstants;
import org.recap.model.ILSConfigProperties;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.accession.AccessionResponse;
import org.recap.model.jaxb.BibRecord;
import org.recap.model.jaxb.Holding;
import org.recap.model.jaxb.Holdings;
import org.recap.model.jaxb.Items;
import org.recap.model.jaxb.marc.BibRecords;
import org.recap.model.jaxb.marc.CollectionType;
import org.recap.model.jaxb.marc.ContentType;
import org.recap.model.jaxb.marc.RecordType;
import org.recap.model.jpa.ImsLocationEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.jpa.ReportDataEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
@Slf4j
@Service
public class SCSBFormatResolver extends AccessionResolverAbstract {



    @Override
    public boolean isFormat(String format) {
        return "SCSB".equalsIgnoreCase(format);
    }

    @Override
    public String getBibData(String itemBarcode, String customerCode, String institution) {
        ILSConfigProperties ilsConfigProperties = propertyUtil.getILSConfigProperties(institution);
        BibDataForAccessionInterface bibDataForAccessionInterface = bibDataFactory.getAuth(ilsConfigProperties.getIlsBibdataApiAuth());
        return bibDataForAccessionInterface.getBibData(itemBarcode, customerCode, institution, ilsConfigProperties.getIlsBibdataApiEndpoint() + ilsConfigProperties.getIlsBibdataApiParameter());

    }

    @Override
    public Object unmarshal(String unmarshal) {
        return commonUtil.getBibRecordsForSCSBFormat(unmarshal);
    }

    @Override
    public String processXml(Set<AccessionResponse> accessionResponses, Object object, List<Map<String, String>> responseMapList, String owningInstitution, List<ReportDataEntity> reportDataEntityList, AccessionRequest accessionRequest, ImsLocationEntity imsLocationEntity) throws Exception {
        String response = null;
        BibRecords bibRecords = (BibRecords) object;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        boolean isBoundWithItem = isBoundWithItemForScsbRecord(bibRecords.getBibRecordList());
        boolean isValidBoundWithRecord = true;
        if (isBoundWithItem) {
            isValidBoundWithRecord = accessionValidationService.validateBoundWithScsbRecordFromIls(bibRecords.getBibRecordList());
        }
        if ((!isBoundWithItem) || (isBoundWithItem && isValidBoundWithRecord)) {
            int count = 1;
            for (BibRecord bibRecord : bibRecords.getBibRecordList()) {
                response = commonUtil.getUpdatedDataResponse(accessionResponses, responseMapList, owningInstitution, reportDataEntityList, accessionRequest, isValidBoundWithRecord, count, bibRecord,imsLocationEntity);
            }
        } else {
            response = ScsbConstants.INVALID_BOUNDWITH_RECORD;
            accessionUtil.setAccessionResponse(accessionResponses, accessionRequest.getItemBarcode(), response);
            reportDataEntityList.addAll(accessionUtil.createReportDataEntityList(accessionRequest, response));
        }
        stopWatch.stop();
        log.info("Total time taken to save records for accession : {}", stopWatch.getTotalTimeSeconds());
        return response;
    }

    @Override
    public ItemEntity getItemEntityFromRecord(Object object, Integer owningInstitutionId) {
        BibRecords bibRecords = (BibRecords) object;
        List<BibRecord> bibRecordList = bibRecords.getBibRecordList();
        if (CollectionUtils.isNotEmpty(bibRecordList)) {
            String owningInstitutionItemId = getOwningInstitutionItemIdFromBibRecord(bibRecordList.get(0));
            if (StringUtils.isNotBlank(owningInstitutionItemId)) {
                return itemDetailsRepository.findByOwningInstitutionItemIdAndOwningInstitutionId(owningInstitutionItemId, owningInstitutionId);
            }
        }
        return null;
    }

    private String getOwningInstitutionItemIdFromBibRecord(BibRecord bibRecord) {
        List<Holdings> holdings = bibRecord.getHoldings();
        if (CollectionUtils.isNotEmpty(holdings)) {
            for (Iterator<Holdings> iterator = holdings.iterator(); iterator.hasNext(); ) {
                Holdings holdingsRecord = iterator.next();
                List<Holding> holdingList = holdingsRecord.getHolding();
                if (CollectionUtils.isNotEmpty(holdingList)) {
                    for (Iterator<Holding> holdingIterator = holdingList.iterator(); holdingIterator.hasNext(); ) {
                        Holding holding = holdingIterator.next();
                        List<Items> items = holding.getItems();
                        if (CollectionUtils.isNotEmpty(items)) {
                            for (Iterator<Items> itemsIterator = items.iterator(); itemsIterator.hasNext(); ) {
                                Items item = itemsIterator.next();

                                ContentType itemContent = item.getContent();
                                CollectionType itemContentCollection = itemContent.getCollection();

                                List<RecordType> itemRecordTypes = itemContentCollection.getRecord();

                                if (CollectionUtils.isNotEmpty(itemRecordTypes)) {
                                    for (Iterator<RecordType> recordTypeIterator = itemRecordTypes.iterator(); recordTypeIterator.hasNext(); ) {
                                        RecordType recordType = recordTypeIterator.next();
                                        return marcUtil.getDataFieldValueForRecordType(recordType,
                                                "876", null, null, "a");

                                    }
                                }

                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isBoundWithItemForScsbRecord(List<BibRecord> bibRecordList) {
        return bibRecordList.size() > 1;
    }
}
