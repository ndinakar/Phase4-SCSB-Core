package org.recap.service.common;

import lombok.extern.slf4j.Slf4j;
import org.recap.ScsbCommonConstants;
import org.recap.model.jpa.CollectionGroupEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.ItemStatusEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by premkb on 11/6/17.
 */
@Slf4j
@Service
public class SetupDataService {


    @Autowired
    private RepositoryService repositoryService;

    private Map<Integer,String> itemStatusIdCodeMap;

    private Map<String,Integer> itemStatusCodeIdMap;

    private Map<Integer,String> institutionEntityMap;

    private Map<String,Integer> institutionCodeIdMap;

    private Map<String,Integer> collectionGroupMap;

    private Map<Integer,String> collectionGroupIdCodeMap;

    /**
     * Gets item status id and item status code from db and puts it into a map where status id as key and status code as value.
     *
     * @return the item status id code map
     */
    public Map<Integer, String> getItemStatusIdCodeMap() {
        if (null == itemStatusIdCodeMap) {
            itemStatusIdCodeMap = new HashMap<>();
            try {
                Iterable<ItemStatusEntity> itemStatusEntities = repositoryService.getItemStatusDetailsRepository().findAll();
                for (ItemStatusEntity itemStatusEntity : itemStatusEntities) {
                    itemStatusIdCodeMap.put(itemStatusEntity.getId(), itemStatusEntity.getStatusCode());
                }
            } catch (Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR,e);
            }
        }
        return itemStatusIdCodeMap;
    }

    /**
     * Gets item status code and item status id from db and puts it into a map where status code as key and status id as value.
     *
     * @return the item status code id map
     */
    public Map<String, Integer> getItemStatusCodeIdMap() {
        if (null == itemStatusCodeIdMap) {
            itemStatusCodeIdMap = new HashMap<>();
            try {
                Iterable<ItemStatusEntity> itemStatusEntities = repositoryService.getItemStatusDetailsRepository().findAll();
                for (ItemStatusEntity itemStatusEntity : itemStatusEntities) {
                    itemStatusCodeIdMap.put(itemStatusEntity.getStatusCode(), itemStatusEntity.getId());
                }
            } catch (Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR,e);
            }
        }
        return itemStatusCodeIdMap;
    }

    /**
     * Gets institution id and institution code from db and puts it into a map where status id as key and status code as value.
     *
     * @return the institution entity map
     */
    public Map<Integer, String> getInstitutionIdCodeMap() {
        if (null == institutionEntityMap) {
            institutionEntityMap = new HashMap<>();
            try {
                Iterable<InstitutionEntity> institutionEntities = repositoryService.getInstitutionDetailsRepository().findAll();
                for (InstitutionEntity institutionEntity : institutionEntities) {
                    institutionEntityMap.put(institutionEntity.getId(), institutionEntity.getInstitutionCode());
                }
            } catch (Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR,e);
            }
        }
        return institutionEntityMap;
    }

    public Map<String, Integer> getInstitutionCodeIdMap() {
        if (null == institutionCodeIdMap) {
            institutionCodeIdMap = new HashMap<>();
            try {
                Iterable<InstitutionEntity> institutionEntities = repositoryService.getInstitutionDetailsRepository().findAll();
                for (InstitutionEntity institutionEntity : institutionEntities) {
                    institutionCodeIdMap.put(institutionEntity.getInstitutionCode(), institutionEntity.getId());
                }
            } catch (Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR,e);
            }
        }
        return institutionCodeIdMap;
    }

    public Map<String, Integer> getCollectionGroupMap() {
        if (null == collectionGroupMap) {
            collectionGroupMap = new HashMap<>();
            try {
                Iterable<CollectionGroupEntity> collectionGroupEntities = repositoryService.getCollectionGroupDetailsRepository().findAll();
                for (CollectionGroupEntity collectionGroupEntity : collectionGroupEntities) {
                    collectionGroupMap.put(collectionGroupEntity.getCollectionGroupCode(), collectionGroupEntity.getId());
                }
            } catch (Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR,e);
            }
        }
        return collectionGroupMap;
    }

    public Map<Integer, String> getCollectionGroupIdCodeMap() {
        if (null == collectionGroupIdCodeMap) {
            collectionGroupIdCodeMap = new HashMap<>();
            try {
                Iterable<CollectionGroupEntity> collectionGroupEntities = repositoryService.getCollectionGroupDetailsRepository().findAll();
                for (CollectionGroupEntity collectionGroupEntity : collectionGroupEntities) {
                    collectionGroupIdCodeMap.put(collectionGroupEntity.getId(), collectionGroupEntity.getCollectionGroupCode());
                }
            } catch (Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR,e);
            }
        }
        return collectionGroupIdCodeMap;
    }
}
