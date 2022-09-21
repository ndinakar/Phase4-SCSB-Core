package org.recap.service.accession;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.marc4j.marc.Record;
import org.recap.ScsbConstants;
import org.recap.model.ILSConfigProperties;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.accession.AccessionResponse;
import org.recap.model.jpa.ImsLocationEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.jpa.ReportDataEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.Map;
import java.util.Set;
@Slf4j
@Service
public class MarcFormatResolver extends AccessionResolverAbstract {


    @Override
    public boolean isFormat(String format) {
        return "MARC".equalsIgnoreCase(format);
    }

    @Override
    public String getBibData(String itemBarcode, String customerCode, String institution) {
        ILSConfigProperties ilsConfigProperties = propertyUtil.getILSConfigProperties(institution);
        BibDataForAccessionInterface bibDataForAccessionInterface = bibDataFactory.getAuth(ilsConfigProperties.getIlsBibdataApiAuth());
        return bibDataForAccessionInterface.getBibData(itemBarcode, customerCode, institution, ilsConfigProperties.getIlsBibdataApiEndpoint());
    }

    @Override
    public Object unmarshal(String unmarshal) {
        return commonUtil.marcRecordConvert(unmarshal);
    }

    @Override
    public String processXml(Set<AccessionResponse> accessionResponses, Object object, List<Map<String, String>> responseMapList, String owningInstitution, List<ReportDataEntity> reportDataEntityList, AccessionRequest accessionRequest, ImsLocationEntity imsLocationEntity) throws Exception {
        StopWatch stopWatch;
        String response = null;
        stopWatch = new StopWatch();
        stopWatch.start();
        List<Record> records = (List<Record>) object;
        boolean isBoundWithItem = isBoundWithItemForMarcRecord(records);
        boolean isValidBoundWithRecord = true;
        if (isBoundWithItem) {
            isValidBoundWithRecord = accessionValidationService.validateBoundWithMarcRecordFromIls(records, accessionRequest, imsLocationEntity);
        }
        if ((!isBoundWithItem) || (isBoundWithItem && isValidBoundWithRecord)) {
            if (CollectionUtils.isNotEmpty(records)) {
                int count = 1;
                for (Record record : records) {
                    response = commonUtil.getUpdatedDataResponse(accessionResponses, responseMapList, owningInstitution, reportDataEntityList, accessionRequest, isValidBoundWithRecord, count, record,imsLocationEntity);
                    count++;
                }
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
        return getItemEntityFormMarcRecord((List<Record>) object, owningInstitutionId);
    }

    private static boolean isBoundWithItemForMarcRecord(List<Record> recordList) {
        return recordList.size() > 1;
    }


    public ItemEntity getItemEntityFormMarcRecord(List<Record> object, Integer owningInstitutionId) {
        String owningInstitutionItemIdFromMarcRecord = getOwningInstitutionItemIdFromMarcRecord(object);
        if (StringUtils.isNotBlank(owningInstitutionItemIdFromMarcRecord)) {
            return itemDetailsRepository.findByOwningInstitutionItemIdAndOwningInstitutionId(owningInstitutionItemIdFromMarcRecord, owningInstitutionId);
        }

        return null;
    }

    private String getOwningInstitutionItemIdFromMarcRecord(List<Record> records) {
        if (CollectionUtils.isNotEmpty(records)) {
            return marcUtil.getDataFieldValue(records.get(0), "876", 'a');
        }
        return null;
    }

}
