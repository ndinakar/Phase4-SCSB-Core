package org.recap.service.accession;

import org.apache.camel.Exchange;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.accession.AccessionModelRequest;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.accession.AccessionResponse;
import org.recap.model.accession.AccessionSummary;
import org.recap.model.jpa.ReportDataEntity;
import org.recap.util.AccessionProcessService;
import org.recap.util.AccessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;


/**
 * Created by chenchulakshmig on 20/10/16.
 */
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AccessionService {

    @Autowired
    protected AccessionProcessService accessionProcessService;

    @Autowired
    AccessionUtil accessionUtil;

    @Autowired
    private AccessionValidationService accessionValidationService;


    public List<AccessionResponse> doAccession(AccessionModelRequest accessionModelRequest, AccessionSummary accessionSummary, Exchange exchange) {

        // Trim accession request
        List<AccessionRequest> trimmedAccessionRequests = getTrimmedAccessionRequests(accessionModelRequest.getAccessionRequests());

        // Remove duplicate barcodes
        trimmedAccessionRequests = accessionProcessService.removeDuplicateRecord(trimmedAccessionRequests);
        int requestedCount = accessionModelRequest.getAccessionRequests().size();
        int duplicateCount = requestedCount - trimmedAccessionRequests.size();

        accessionSummary.setRequestedRecords(requestedCount);
        accessionSummary.setDuplicateRecords(duplicateCount);

        Set<AccessionResponse> accessionResponses = new HashSet<>();
        List<Map<String, String>> responseMaps = new ArrayList<>();
        List<ReportDataEntity> reportDataEntitys = new ArrayList<>();
        //validate ims_location
        String imsLocationCode=accessionModelRequest.getImsLocationCode();// validateCode(code,codeType)
        AccessionValidationService.AccessionValidationResponse imsValidation = accessionValidationService.validateImsLocationCode(imsLocationCode);

        if(imsValidation.isValid()) {
            // iterate Request
            for (AccessionRequest accessionRequest : trimmedAccessionRequests) {

                // validate empty barcode ,customer code and owning institution
                String itemBarcode = accessionRequest.getItemBarcode();
                String customerCode = accessionRequest.getCustomerCode();
                AccessionValidationService.AccessionValidationResponse accessionValidationResponse = accessionValidationService.validateBarcodeOrCustomerCode(itemBarcode, customerCode, imsLocationCode);

                String owningInstitution = accessionValidationResponse.getOwningInstitution();

                if (!accessionValidationResponse.isValid()) {
                    String message = accessionValidationResponse.getMessage();
                    accessionUtil.setAccessionResponse(accessionResponses, itemBarcode, message);
                    reportDataEntitys.addAll(accessionUtil.createReportDataEntityList(accessionRequest, message));
                    continue;
                }
                accessionProcessService.processRecords(accessionResponses, responseMaps, accessionRequest,reportDataEntitys, owningInstitution, true,imsValidation.getImsLocationEntity());

            }
        }
        else{
            String message = imsValidation.getMessage();
            trimmedAccessionRequests.forEach(accessionRequest -> {
                accessionUtil.setAccessionResponse(accessionResponses,accessionRequest.getItemBarcode(),message);
                reportDataEntitys.addAll(accessionUtil.createReportDataEntityList(accessionRequest, message));
            });
        }

        prepareSummary(accessionSummary, accessionResponses);

        return new ArrayList<>(accessionResponses);
    }


    public List<AccessionRequest> getTrimmedAccessionRequests(List<AccessionRequest> accessionRequestList) {
        List<AccessionRequest> trimmedAccessionRequests = new ArrayList<>();
        for (AccessionRequest accessionRequest : accessionRequestList) {
            AccessionRequest request = new AccessionRequest();
            request.setItemBarcode(accessionRequest.getItemBarcode().trim());
            request.setCustomerCode(accessionRequest.getCustomerCode().trim().toUpperCase());
            trimmedAccessionRequests.add(request);
        }
        return trimmedAccessionRequests;
    }

    public void prepareSummary(AccessionSummary accessionSummary, Object object) {
        Set<AccessionResponse> accessionResponses = (Set<AccessionResponse>) object;
        if(CollectionUtils.isNotEmpty(accessionResponses)) {
            for (AccessionResponse accessionResponse : accessionResponses) {
                String message = accessionResponse.getMessage();
                addCountToSummary(accessionSummary, message);
            }
        }
    }

    protected void addCountToSummary(AccessionSummary accessionSummary, String message) {
        if(message.contains(ScsbCommonConstants.SUCCESS)) {
            accessionSummary.addSuccessRecord(1);
        }else if(message.contains(ScsbConstants.ITEM_ALREADY_ACCESSIONED)) {
            accessionSummary.addAlreadyAccessioned(1);
        } else if(message.contains(ScsbConstants.ACCESSION_DUMMY_RECORD)) {
            accessionSummary.addDummyRecords(1);
        } else if(message.contains(ScsbConstants.EXCEPTION)) {
            accessionSummary.addException(1);
        } else if(StringUtils.equalsIgnoreCase(ScsbConstants.INVALID_BARCODE_LENGTH, message)) {
            accessionSummary.addInvalidLenghBarcode(1);
        } else if(StringUtils.equalsIgnoreCase(ScsbConstants.OWNING_INST_EMPTY, message)) {
            accessionSummary.addEmptyOwningInst(1);
        } else if(StringUtils.equalsIgnoreCase(ScsbConstants.ITEM_BARCODE_EMPTY, message)) {
            accessionSummary.addEmptyBarcodes(1);
        } else if(StringUtils.equalsIgnoreCase(ScsbConstants.CUSTOMER_CODE_EMPTY, message)) {
            accessionSummary.addEmptyCustomerCode(1);
        }  else if(StringUtils.equalsIgnoreCase(ScsbCommonConstants.CUSTOMER_CODE_DOESNOT_EXIST, message)) {
            accessionSummary.addCustomerCodeDoesNotExist(1);
        }
        else if(StringUtils.equalsIgnoreCase(ScsbConstants.INVALID_IMS_LOCACTION_CODE, message)){
            accessionSummary.addInvalidImsLocation(1);
        }
        else if(StringUtils.equalsIgnoreCase(ScsbConstants.IMS_LOCACTION_CODE_IS_BLANK, message)){
            accessionSummary.addEmptyImsLocation(1);
        }
        else {
            accessionSummary.addFailure(1);
        }
    }

    public void createSummaryReport(String summary, String type) {
        List<ReportDataEntity> reportDataEntityList = new ArrayList<>();
        ReportDataEntity reportDataEntityMessage = new ReportDataEntity();
        reportDataEntityMessage.setHeaderName(type);
        reportDataEntityMessage.setHeaderValue(summary);
        reportDataEntityList.add(reportDataEntityMessage);
        accessionUtil.saveReportEntity(null, reportDataEntityList);
    }

    public String updateItemHoldings() {
        return accessionProcessService.updateItemHoldings();
    }
}
