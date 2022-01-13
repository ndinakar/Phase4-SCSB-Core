package org.recap.model.jpa;

import lombok.Data;
import lombok.EqualsAndHashCode;


import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Created by pvsubrah on 6/11/16.
 */
@Data
@EqualsAndHashCode(callSuper=false)
@Entity
@Table(name = "request_item_t", catalog = "")
@AttributeOverride(name = "id", column = @Column(name = "REQUEST_ID"))
public class RequestItemEntity extends RequestItemAbstractEntity {

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "ITEM_ID", referencedColumnName = "ITEM_ID", insertable = false, updatable = false)
    private ItemEntity itemEntity;

    @Column(name = "EMAIL_ID")
    private String emailId;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinTable(name = "bulk_request_t",
            joinColumns = @JoinColumn(name = "REQUEST_ID"),
            inverseJoinColumns = @JoinColumn(name = "BULK_REQUEST_ID"))
    private BulkRequestItemEntity bulkRequestItemEntity;

}
