package org.recap.model.jpa;

import lombok.Data;

import org.recap.model.AbstractResponseItem;

/**
 * Created by sudhishk on 15/12/16.
 */
@Data
public class ItemRefileResponse extends AbstractResponseItem {

    private Integer requestId;
    private String jobId;

}
