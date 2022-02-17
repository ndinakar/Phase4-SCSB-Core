package org.recap.model.jpa;

import lombok.Data;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQuery;
import javax.persistence.Table;
import java.util.List;

/**
 * Created by pvsubrah on 6/10/16.
 */
@Data
@Entity
@Table(name = "bibliographic_t", catalog = "")
@AttributeOverride(name = "id", column = @Column(name = "BIBLIOGRAPHIC_ID"))
@NamedNativeQuery(
        name = "BibliographicEntity.getNonDeletedHoldingsEntities",
        query = "SELECT HOLDINGS_T.* FROM HOLDINGS_T, BIBLIOGRAPHIC_HOLDINGS_T, BIBLIOGRAPHIC_T WHERE " +
                "BIBLIOGRAPHIC_T.BIBLIOGRAPHIC_ID = BIBLIOGRAPHIC_HOLDINGS_T.BIBLIOGRAPHIC_ID AND HOLDINGS_T.HOLDINGS_ID = BIBLIOGRAPHIC_HOLDINGS_T.HOLDINGS_ID " +
                "AND HOLDINGS_T.IS_DELETED = 0 AND " +
                "BIBLIOGRAPHIC_T.OWNING_INST_BIB_ID = :owningInstitutionBibId AND BIBLIOGRAPHIC_T.OWNING_INST_ID = :owningInstitutionId",
        resultClass = HoldingsEntity.class)
@NamedNativeQuery(
        name = "BibliographicEntity.getNonDeletedItemEntities",
        query = "SELECT ITEM_T.* FROM ITEM_T, BIBLIOGRAPHIC_ITEM_T, BIBLIOGRAPHIC_T WHERE " +
                "BIBLIOGRAPHIC_T.BIBLIOGRAPHIC_ID = BIBLIOGRAPHIC_ITEM_T.BIBLIOGRAPHIC_ID AND ITEM_T.ITEM_ID = BIBLIOGRAPHIC_ITEM_T.ITEM_ID " +
                "AND ITEM_T.IS_DELETED = 0 " +
                "AND BIBLIOGRAPHIC_T.OWNING_INST_BIB_ID = :owningInstitutionBibId AND BIBLIOGRAPHIC_T.OWNING_INST_ID = :owningInstitutionId",
        resultClass = ItemEntity.class)

public class BibliographicEntity extends  BibliographicAbstractEntity {

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "OWNING_INST_ID", insertable = false, updatable = false)
    private InstitutionEntity institutionEntity;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "bibliographic_holdings_t", joinColumns = {
            @JoinColumn(name = "BIBLIOGRAPHIC_ID", referencedColumnName = "BIBLIOGRAPHIC_ID")},
            inverseJoinColumns = {
                    @JoinColumn(name = "HOLDINGS_ID", referencedColumnName = "HOLDINGS_ID")})
    private List<HoldingsEntity> holdingsEntities;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "bibliographic_item_t", joinColumns = {
            @JoinColumn(name="BIBLIOGRAPHIC_ID", referencedColumnName = "BIBLIOGRAPHIC_ID")},
            inverseJoinColumns = {
                    @JoinColumn(name="ITEM_ID", referencedColumnName = "ITEM_ID")})
    private List<ItemEntity> itemEntities;

    /**
     * Instantiates a new Bibliographic entity.
     */
    public BibliographicEntity() {
        super();
    }
}

