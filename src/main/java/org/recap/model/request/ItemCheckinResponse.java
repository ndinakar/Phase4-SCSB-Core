package org.recap.model.request;

import lombok.Data;


import java.util.List;

/**
 * Created by sudhishk on 16/12/16.
 */
@Data
public class ItemCheckinResponse {
    private String itemBarcode;
    private String screenMessage;
    private boolean success;
    private String esipDataIn;
    private String esipDataOut;
    private List<String> itemBarcodes;
    private String itemOwningInstitution="";
    private boolean alert;
    private boolean magneticMedia;
    private boolean resensitize;
    private String transactionDate;
    private String institutionID;
    private String patronIdentifier;
    private String titleIdentifier;
    private String dueDate;
    private String feeType;
    private String securityInhibit;
    private String currencyType;
    private String feeAmount;
    private String mediaType;
    private String bibId;
    private String ISBN;
    private String LCCN;
    private String permanentLocation;
    private String sortBin;
    private String collectionCode;
    private String callNumber;
    private String destinationLocation;
    private String alertType;
    private String holdPatronId;
    private String holdPatronName;
    private String jobId;
    private boolean processed;
    private String updatedDate;
    private String createdDate;
}
