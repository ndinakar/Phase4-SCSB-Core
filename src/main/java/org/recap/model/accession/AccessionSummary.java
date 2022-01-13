package org.recap.model.accession;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * Created by sheiks on 15/06/17.
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class AccessionSummary {
    @NonNull
    private String type;
    private int requestedRecords;
    private int successRecords;
    private int dummyRecords;
    private int duplicateRecords;
    private int emptyBarcodes;
    private int emptyCustomerCode;
    private int customerCodeDoesNotExist;
    private int emptyOwningInst;
    private int alreadyAccessioned;
    private int exception;
    private int failure;
    private int invalidLenghBarcode;
    private int invalidImsLocation;
    private int emptyImsLocation;
    private String timeElapsed;
    private String imsLocation;


    public void addDummyRecords(int dummyRecords) {
        this.dummyRecords += dummyRecords;
    }

    public void addEmptyBarcodes(int emptyBarcodes) {
        this.emptyBarcodes += emptyBarcodes;
    }

    public void addEmptyOwningInst(int emptyOwningInst) {
        this.emptyOwningInst += emptyOwningInst;
    }

    public void addAlreadyAccessioned(int alreadyAccessioned) {
        this.alreadyAccessioned += alreadyAccessioned;
    }

    public void addException(int exception) {
        this.exception += exception;
    }

    public void addInvalidLenghBarcode(int invalidLenghBarcode) {
        this.invalidLenghBarcode += invalidLenghBarcode;
    }

    public void addSuccessRecord(int successRecords) {
        this.successRecords += successRecords;
    }

    public void addFailure(int failure) {
        this.failure += failure;
    }

    public void addEmptyCustomerCode(int emptyCustomerCode) {
        this.emptyCustomerCode += emptyCustomerCode;
    }

    public void addCustomerCodeDoesNotExist(int customerCodeDoesNotExist) { this.customerCodeDoesNotExist += customerCodeDoesNotExist; }

    public void addInvalidImsLocation(int invalidImsLocation) {
        this.invalidImsLocation += invalidImsLocation;
    }

    public void addEmptyImsLocation(int emptyImsLocation) {
        this.emptyImsLocation += emptyImsLocation;
    }

    public void addRequestedRecords(int requestedRecords) {
        this.requestedRecords += requestedRecords;
    }

    public void addDuplicateRecords(int duplicateRecords) {
        this.duplicateRecords += duplicateRecords;
    }

}
