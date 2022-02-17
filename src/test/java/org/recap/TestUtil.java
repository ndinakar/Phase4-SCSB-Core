package org.recap;

import org.recap.model.jpa.CollectionGroupEntity;
import org.recap.model.jpa.ImsLocationEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.ItemStatusEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUtil {

    private TestUtil() {}


    public static ImsLocationEntity getImsLocationEntity (int id, String imsLocationCode, String imsLocationName) {
        ImsLocationEntity imsLocationEntity=new ImsLocationEntity();
        imsLocationEntity.setId(id);
        imsLocationEntity.setImsLocationCode(imsLocationCode);
        imsLocationEntity.setImsLocationName(imsLocationName);
        return imsLocationEntity;
    }

    public static InstitutionEntity getInstitutionEntity(int id,String institutionCode,String institutionName) {
        InstitutionEntity institutionEntity=new InstitutionEntity();
        institutionEntity.setId(id);
        institutionEntity.setInstitutionCode(String.valueOf(institutionCode));
        institutionEntity.setInstitutionName(institutionName);
        return institutionEntity;
    }

    public static CollectionGroupEntity getCollectionGroupEntities(int id,String collectionGroupCode,String collectionGroupDescription) {
        CollectionGroupEntity collectionGroupEntity = new CollectionGroupEntity();
        collectionGroupEntity.setId(id);
        collectionGroupEntity.setCollectionGroupCode(collectionGroupCode);
        collectionGroupEntity.setCollectionGroupDescription(collectionGroupDescription);
        return collectionGroupEntity;
    }

    public static ItemStatusEntity getItemStatusEntity(int id,String statusCode) {
        ItemStatusEntity itemStatusEntity = new ItemStatusEntity();
        itemStatusEntity.setId(id);
        itemStatusEntity.setStatusCode(statusCode);
        itemStatusEntity.setStatusDescription(statusCode);
        return itemStatusEntity;
    }
}
