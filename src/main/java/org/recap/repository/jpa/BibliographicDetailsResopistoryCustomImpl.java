package org.recap.repository.jpa;

import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.ItemEntity;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Optional;

@Transactional
public class BibliographicDetailsResopistoryCustomImpl implements BibliographicDetailsResopistoryCustom {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public <S extends BibliographicEntity> S updateBibForSubmitCollection(S entity, ItemEntity fetchedItemEntity) {
        attachExistingHoldingAndItemId(entity, fetchedItemEntity);
        return entityManager.merge(entity);
    }

    private <S extends BibliographicEntity> void attachExistingHoldingAndItemId(S entity, ItemEntity fetchedItemEntity) {
        attachExistingHoldingId(entity, fetchedItemEntity);
        attachExistingItemId(entity, fetchedItemEntity);
    }

    private <S extends BibliographicEntity> void attachExistingItemId(S entity, ItemEntity fetchedItemEntity) {
        Optional<ItemEntity> itemEntityToAttach = entity.getItemEntities()
                .stream()
                .filter(itemEntity -> itemEntity.getId() == null)
                .findFirst();
        if(itemEntityToAttach.isPresent()){
            itemEntityToAttach.get().setId(fetchedItemEntity.getId());
        }
    }

    private <S extends BibliographicEntity> void attachExistingHoldingId(S entity, ItemEntity fetchedItemEntity) {
        Optional<HoldingsEntity> holdingsEntityToAttach = entity.getHoldingsEntities()
                .stream()
                .filter(holdingsEntity -> holdingsEntity.getId() == null)
                .findFirst();
        if(holdingsEntityToAttach.isPresent()){
            holdingsEntityToAttach.get().setId(fetchedItemEntity.getHoldingsEntities().get(0).getId());
        }
    }
}
