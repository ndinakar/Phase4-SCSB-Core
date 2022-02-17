package org.recap.model.jpa;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by sheiks on 07/07/17.
 */
@Data
@Entity
@Table(name = "item_barcode_history_t", catalog = "")
@AttributeOverride(name = "id", column = @Column(name = "HISTORY_ID"))
public class ItemBarcodeHistoryEntity extends AbstractEntity<Integer> {
    @Column(name = "OWNING_INST")
    private String owningingInstitution;

    @Column(name = "OWNING_INST_ITEM_ID")
    private String owningingInstitutionItemId;

    @Column(name = "OLD_BARCODE")
    private String oldBarcode;

    @Column(name = "NEW_BARCODE")
    private String newBarcode;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_DATE")
    private Date createdDate;
}
