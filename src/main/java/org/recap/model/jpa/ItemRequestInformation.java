package org.recap.model.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;


import java.util.List;

/**
 * Created by hemalathas on 1/11/16.
 */
@Data
public class ItemRequestInformation {

    private List<String> itemBarcodes;
    private String titleIdentifier;
    private String itemOwningInstitution = "";
    private String patronBarcode = "";
    private String emailAddress = "";
    private String requestingInstitution = "";
    private String requestType = "";
    private String deliveryLocation = "";
    private String customerCode = "";
    private String requestNotes = "";
    private String trackingId;
    private String author;
    private String callNumber;

    /**
     * EDD Request
     */
    private String startPage;
    private String endPage;
    private String chapterTitle = "";
    private String expirationDate;
    private String bibId = "";
    private String username;
    private String issue;
    private String volume;
    private String itemAuthor;
    private String itemVolume;
    private Integer requestId;
    private String pickupLocation;
    private String eddNotes;


    /**
     * Is owning institution item boolean.
     *
     * @return the boolean
     */
    @JsonIgnore
    public boolean isOwningInstitutionItem() {
        boolean bSuccess;
        bSuccess = itemOwningInstitution.equalsIgnoreCase(requestingInstitution);
        return bSuccess;
    }


}