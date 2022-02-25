package org.recap.service.accession.callable;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.recap.BaseTestCaseUT;
import org.recap.TestUtil;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.accession.AccessionResponse;
import org.recap.model.jpa.ImsLocationEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.jpa.ReportDataEntity;
import org.recap.service.accession.AccessionResolverAbstract;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccessionResolverAbstractUT extends BaseTestCaseUT {

    @Mock
    ItemEntity itemEntity;

    @Test
    public void isAccessionProcess() throws Exception {
        Mockito.when(itemEntity.getInstitutionEntity()).thenReturn(TestUtil.getInstitutionEntity(1,"PUL","PUL"));
        AccessionResolverAbstract accessionResolverAbstract= new AccessionResolverAbstract() {
            @Override
            public boolean isFormat(String format) {
                return false;
            }

            @Override
            public String getBibData(String itemBarcode, String customerCode, String institution) {
                return null;
            }

            @Override
            public String getItemHoldingData(String itemBarcode, String customerCode, String institution) {
                return null;
            }

            @Override
            public Object unmarshal(String unmarshal) {
                return null;
            }

            @Override
            public String processXml(Set<AccessionResponse> accessionResponses, Object object, List<Map<String, String>> responseMapList, String owningInstitution, List<ReportDataEntity> reportDataEntityList, AccessionRequest accessionRequest, ImsLocationEntity imsLocationEntity) throws Exception {
                return null;
            }

            @Override
            public ItemEntity getItemEntityFromRecord(Object object, Integer owningInstitutionId) {
                return null;
            }
        };
        boolean isAccessionProcess=accessionResolverAbstract.isAccessionProcess(itemEntity,"PUL");
        assertFalse(isAccessionProcess);
    }

    @Test
    public void isAccessionProcessMismatchOwningInst() throws Exception {
        Mockito.when(itemEntity.getInstitutionEntity()).thenReturn(TestUtil.getInstitutionEntity(1,"PUL","PUL"));
        AccessionResolverAbstract accessionResolverAbstract= new AccessionResolverAbstract() {
            @Override
            public boolean isFormat(String format) {
                return false;
            }

            @Override
            public String getBibData(String itemBarcode, String customerCode, String institution) {
                return null;
            }

            @Override
            public Object unmarshal(String unmarshal) {
                return null;
            }

            @Override
            public String getItemHoldingData(String itemBarcode, String customerCode, String institution) {
                return null;
            }

            @Override
            public String processXml(Set<AccessionResponse> accessionResponses, Object object, List<Map<String, String>> responseMapList, String owningInstitution, List<ReportDataEntity> reportDataEntityList, AccessionRequest accessionRequest, ImsLocationEntity imsLocationEntity) throws Exception {
                return null;
            }

            @Override
            public ItemEntity getItemEntityFromRecord(Object object, Integer owningInstitutionId) {
                return null;
            }
        };
        boolean isAccessionProcess=accessionResolverAbstract.isAccessionProcess(itemEntity,"CUL");
        assertTrue(isAccessionProcess);
    }


    @Test
    public void isAccessionProcessNullItemEntity() throws Exception {
        AccessionResolverAbstract accessionResolverAbstract= new AccessionResolverAbstract() {
            @Override
            public boolean isFormat(String format) {
                return false;
            }

            @Override
            public String getBibData(String itemBarcode, String customerCode, String institution) {
                return null;
            }

            @Override
            public Object unmarshal(String unmarshal) {
                return null;
            }

            @Override
            public String processXml(Set<AccessionResponse> accessionResponses, Object object, List<Map<String, String>> responseMapList, String owningInstitution, List<ReportDataEntity> reportDataEntityList, AccessionRequest accessionRequest, ImsLocationEntity imsLocationEntity) throws Exception {
                return null;
            }
            @Override
            public String getItemHoldingData(String itemBarcode, String customerCode, String institution) {
                return null;
            }

            @Override
            public ItemEntity getItemEntityFromRecord(Object object, Integer owningInstitutionId) {
                return null;
            }
        };
        boolean isAccessionProcess=accessionResolverAbstract.isAccessionProcess(null,"PUL");
        assertTrue(isAccessionProcess);
    }


}
