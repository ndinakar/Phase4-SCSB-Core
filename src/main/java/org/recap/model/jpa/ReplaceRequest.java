package org.recap.model.jpa;

import lombok.Data;


/**
 * Created by rajeshbabuk on 15/11/17.
 */
@Data
public class ReplaceRequest {

    private String replaceRequestByType;
    private String requestStatus;
    private String requestIds;
    private String startRequestId;
    private String endRequestId;
    private String fromDate;
    private String toDate;

}
