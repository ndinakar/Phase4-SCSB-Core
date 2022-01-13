package org.recap.model.jpa;

import lombok.Data;


import org.apache.commons.lang3.StringUtils;
import org.recap.ScsbCommonConstants;
import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.List;

/**
 * Created by pvsubrah on 6/11/16.
 */
@Data
@Entity
@Table(name = "item_t", catalog = "")
@AttributeOverride(name = "id", column = @Column(name = "ITEM_ID"))
public class ItemEntity extends ItemAbstractEntity {

    @Column(name = "IS_CGD_PROTECTION")
    private boolean isCgdProtection;

    @ManyToMany(mappedBy = "itemEntities",cascade = CascadeType.ALL)
    private List<HoldingsEntity> holdingsEntities;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "ITEM_AVAIL_STATUS_ID", insertable = false, updatable = false)
    private ItemStatusEntity itemStatusEntity;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "COLLECTION_GROUP_ID", insertable = false, updatable = false)
    private CollectionGroupEntity collectionGroupEntity;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "OWNING_INST_ID", insertable = false, updatable = false)
    private InstitutionEntity institutionEntity;

    @ManyToOne
    @JoinColumn(name = "IMS_LOCATION_ID", insertable = false, updatable = false)
    private ImsLocationEntity imsLocationEntity;

    @ManyToMany(mappedBy = "itemEntities",fetch = FetchType.EAGER,cascade = CascadeType.ALL)
    private List<BibliographicEntity> bibliographicEntities;

    /**
     * Instantiates a new Item entity.
     */
    public ItemEntity() {
        super();
    }

    /**
     * Is complete boolean.
     *
     * @return the boolean
     */
    public boolean isComplete() {
        return (StringUtils.isNotBlank(this.getCatalogingStatus()) && ScsbCommonConstants.COMPLETE_STATUS.equals(this.getCatalogingStatus()));
    }
}


