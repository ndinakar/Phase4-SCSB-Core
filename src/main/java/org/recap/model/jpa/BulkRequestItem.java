package org.recap.model.jpa;

import lombok.Data;


/**
 * Created by rajeshbabuk on 10/10/17.
 */
@Data
public class BulkRequestItem {

    private String itemBarcode;
    private String customerCode;
    private String requestId;
    private String requestStatus;
    private String status;

}
