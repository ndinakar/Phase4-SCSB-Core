package org.recap.service.accession;

import org.recap.model.accession.AccessionRequest;
import org.recap.model.accession.AccessionResponse;
import org.recap.model.jpa.ImsLocationEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.jpa.ReportDataEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AccessionInterface {

     String getBibData(String itemBarcode, String customerCode,String institution);
     Object unmarshal(String unmarshal);
     String processXml(Set<AccessionResponse> accessionResponses, Object object,
                       List<Map<String, String>> responseMapList, String owningInstitution,
                       List<ReportDataEntity> reportDataEntityList, AccessionRequest accessionRequest, ImsLocationEntity imsLocationEntity) throws Exception ;
     ItemEntity getItemEntityFromRecord(Object object, Integer owningInstitutionId);

     boolean isAccessionProcess(ItemEntity itemEntity, String owningInstitution);

     String getItemHoldingData(String itemBarcode, String customerCode,String institution);
}
