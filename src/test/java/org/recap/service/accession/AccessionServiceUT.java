package org.recap.service.accession;

import org.apache.camel.Exchange;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.accession.AccessionModelRequest;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.accession.AccessionResponse;
import org.recap.model.accession.AccessionSummary;
import org.recap.util.AccessionProcessService;
import org.recap.util.AccessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by chenchulakshmig on 20/10/16.
 */


public class AccessionServiceUT extends BaseTestCaseUT {


    @InjectMocks
    AccessionService mockAccessionService;

    @Mock
    Exchange exchange;

    @Mock
    AccessionProcessService accessionProcessService;

    @Mock
    AccessionValidationService accessionValidationService;

    @Mock
    AccessionValidationService.AccessionValidationResponse accessionValidationResponse;

    @Mock
    AccessionUtil accessionUtil;

    @Test
    public void doAccession() {
        List<AccessionRequest> accessionRequestList=getAccessionRequests();
        AccessionSummary accessionSummary=new AccessionSummary("test");
        Mockito.when(accessionProcessService.removeDuplicateRecord(Mockito.anyList())).thenReturn(removeDuplicateRecord(accessionRequestList));
        Mockito.when(accessionValidationService.validateBarcodeOrCustomerCode(Mockito.anyString(),Mockito.anyString(), Mockito.anyString())).thenReturn(accessionValidationResponse);
        Mockito.when(accessionValidationResponse.getOwningInstitution()).thenReturn("PUL");
        Mockito.when(accessionValidationResponse.isValid()).thenReturn(true);
        AccessionModelRequest accessionModelRequest=new AccessionModelRequest();
        accessionModelRequest.setAccessionRequests(accessionRequestList);
        accessionModelRequest.setImsLocationCode("test");
        Mockito.when(accessionValidationService.validateImsLocationCode(Mockito.anyString())).thenReturn(accessionValidationResponse);
        List<AccessionResponse> response=mockAccessionService.doAccession(accessionModelRequest,accessionSummary,exchange);
        assertNotNull(response);
    }

    @Test
    public void doAccessionInvalidBarcode() {
        List<AccessionRequest> accessionRequestList=getAccessionRequests();
        AccessionSummary accessionSummary=new AccessionSummary("test");
        Mockito.when(accessionProcessService.removeDuplicateRecord(Mockito.anyList())).thenReturn(removeDuplicateRecord(accessionRequestList));
        Mockito.when(accessionValidationService.validateBarcodeOrCustomerCode(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(accessionValidationResponse);
        Mockito.when(accessionValidationResponse.getOwningInstitution()).thenReturn("PUL");
        Mockito.when(accessionValidationResponse.isValid()).thenReturn(true).thenReturn(false);
        AccessionModelRequest accessionModelRequest=new AccessionModelRequest();
        accessionModelRequest.setAccessionRequests(accessionRequestList);
        accessionModelRequest.setImsLocationCode("test");
        Mockito.when(accessionValidationService.validateImsLocationCode(Mockito.anyString())).thenReturn(accessionValidationResponse);
        List<AccessionResponse> response=mockAccessionService.doAccession(accessionModelRequest,accessionSummary,exchange);
        assertNotNull(response);
    }

    @Test
    public void doAccessionInvalid() {
        List<AccessionRequest> accessionRequestList=getAccessionRequests();
        AccessionSummary accessionSummary=new AccessionSummary("test");
        Mockito.when(accessionProcessService.removeDuplicateRecord(Mockito.anyList())).thenReturn(removeDuplicateRecord(accessionRequestList));
        Mockito.when(accessionValidationService.validateBarcodeOrCustomerCode(Mockito.anyString(),Mockito.anyString(), Mockito.anyString())).thenReturn(accessionValidationResponse);
        AccessionModelRequest accessionModelRequest=new AccessionModelRequest();
        accessionModelRequest.setAccessionRequests(accessionRequestList);
        accessionModelRequest.setImsLocationCode("test");
        Mockito.when(accessionValidationService.validateImsLocationCode(Mockito.anyString())).thenReturn(accessionValidationResponse);
        List<AccessionResponse> response=mockAccessionService.doAccession(accessionModelRequest,accessionSummary,exchange);
        assertNotNull(response);
    }

    @Test
    public void prepareSummary() {
        String[] messages={ScsbCommonConstants.SUCCESS, ScsbConstants.ITEM_ALREADY_ACCESSIONED, ScsbConstants.ACCESSION_DUMMY_RECORD, ScsbConstants.EXCEPTION, ScsbConstants.INVALID_BARCODE_LENGTH, ScsbConstants.OWNING_INST_EMPTY, ScsbConstants.ITEM_BARCODE_EMPTY, ScsbConstants.CUSTOMER_CODE_EMPTY,ScsbCommonConstants.CUSTOMER_CODE_DOESNOT_EXIST,"test", ScsbConstants.INVALID_IMS_LOCACTION_CODE, ScsbConstants.IMS_LOCACTION_CODE_IS_BLANK};
        for (String message:messages) {
            AccessionSummary accessionSummary=new AccessionSummary(message);
        AccessionResponse accessionResponse=new AccessionResponse();
        accessionResponse.setMessage(message);
        Set<AccessionResponse> accessionResponses=new HashSet<>();
        accessionResponses.add(accessionResponse);
        mockAccessionService.prepareSummary(accessionSummary,accessionResponses);
        mockAccessionService.createSummaryReport(message,message);
        assertNotNull(message);
        }
    }

    public List<AccessionRequest> removeDuplicateRecord(List<AccessionRequest> trimmedAccessionRequests) {
        Set<AccessionRequest> accessionRequests = new HashSet<>(trimmedAccessionRequests);
        return new ArrayList<>(accessionRequests);
    }

    private List<AccessionRequest> getAccessionRequests() {
        List<AccessionRequest> accessionRequestList = new ArrayList<>();
        AccessionRequest accessionRequest = new AccessionRequest();
        accessionRequest.setCustomerCode("PA");
        accessionRequest.setItemBarcode("32101095533293");
        accessionRequestList.add(accessionRequest);
        return accessionRequestList;
    }

}
