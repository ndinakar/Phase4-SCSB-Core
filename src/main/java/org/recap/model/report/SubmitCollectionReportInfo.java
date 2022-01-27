package org.recap.model.report;

import lombok.Data;


/**
 * Created by premkb on 25/12/16.
 */
@Data
public class SubmitCollectionReportInfo {

    private String itemBarcode;

    private String customerCode;

    private String owningInstitution;

    private String message;

}
