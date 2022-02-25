package org.recap.repository.jpa;

import org.recap.model.jpa.HoldingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


/**
 * Created by chenchulakshmig on 6/13/16.
 */
public interface HoldingsDetailsRepository extends BaseRepository<HoldingsEntity> {

    /**
     * Count by owning institution id long.
     *
     * @param owningInstitutionId the owning institution id
     * @return the long
     */
    Long countByOwningInstitutionId(Integer owningInstitutionId);

    /**
     * Gets non deleted items count.
     *
     * @param owningInstitutionId         the owning institution id
     * @param owningInstitutionHoldingsId the owning institution holdings id
     * @return the non deleted items count
     */
    @Query(value = "SELECT COUNT(*) FROM ITEM_T, ITEM_HOLDINGS_T, HOLDINGS_T WHERE " +
            "HOLDINGS_T.HOLDINGS_ID = ITEM_HOLDINGS_T.HOLDINGS_ID AND ITEM_T.ITEM_ID = ITEM_HOLDINGS_T.ITEM_ID " +
            "AND ITEM_T.IS_DELETED = 0 AND " +
            "HOLDINGS_T.OWNING_INST_HOLDINGS_ID = :owningInstitutionHoldingsId AND HOLDINGS_T.OWNING_INST_ID = :owningInstitutionId", nativeQuery = true)
    Long getNonDeletedItemsCount(@Param("owningInstitutionId") Integer owningInstitutionId, @Param("owningInstitutionHoldingsId") String owningInstitutionHoldingsId);

    /**
     * Mark holdings as deleted int.
     *
     * @param holdingIds      the holding ids
     * @param lastUpdatedBy   the last updated by
     * @param lastUpdatedDate the last updated date
     * @return the int
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE HoldingsEntity holdings SET holdings.isDeleted = true, holdings.lastUpdatedBy = :lastUpdatedBy, holdings.lastUpdatedDate = :lastUpdatedDate WHERE holdings.id IN :holdingIds")
    int markHoldingsAsDeleted(@Param("holdingIds") List<Integer> holdingIds, @Param("lastUpdatedBy") String lastUpdatedBy, @Param("lastUpdatedDate") Date lastUpdatedDate);

    /**
     * Mark holdings as not deleted int.
     *
     * @param holdingIds      the holding ids
     * @param lastUpdatedBy   the last updated by
     * @param lastUpdatedDate the last updated date
     * @return the int
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE HoldingsEntity holdings SET holdings.isDeleted = false, holdings.lastUpdatedBy = :lastUpdatedBy, holdings.lastUpdatedDate = :lastUpdatedDate WHERE holdings.id IN :holdingIds")
    int markHoldingsAsNotDeleted(@Param("holdingIds") List<Integer> holdingIds, @Param("lastUpdatedBy") String lastUpdatedBy, @Param("lastUpdatedDate") Date lastUpdatedDate);

    HoldingsEntity findByOwningInstitutionHoldingsIdAndOwningInstitutionId(String owningInstitutionHoldingsId, Integer owningInstitutionId);

    HoldingsEntity findByOwningInstitutionHoldingsId(String holdingId);
}
