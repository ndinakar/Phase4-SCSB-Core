package org.recap.converter;


import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;
import org.recap.ScsbCommonConstants;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.jaxb.Bib;
import org.recap.model.jaxb.BibRecord;
import org.recap.model.jaxb.Holdings;
import org.recap.model.jaxb.marc.BibRecords;
import org.recap.model.jaxb.marc.CollectionType;
import org.recap.model.jaxb.marc.ContentType;
import org.recap.model.jaxb.marc.LeaderFieldType;
import org.recap.model.jaxb.marc.RecordType;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.ImsLocationEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.util.CommonUtil;
import org.recap.util.DBReportUtil;
import org.recap.util.MarcUtil;
import org.recap.util.PropertyUtil;
import org.springframework.test.util.ReflectionTestUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by premkb on 15/12/16.
 */

public class AccessionSCSBToBibEntityConverterUT extends BaseTestCaseUT {

    @InjectMocks
    AccessionSCSBToBibEntityConverter scsbToBibEntityConverter;

    @Mock
    CommonUtil commonUtil;

    @Mock
    DBReportUtil dbReportUtil;

    @Mock
    MarcUtil marcUtil;

    @Mock
    BibliographicDetailsRepository bibliographicDetailsRepository;

    @Mock
    BibRecord bibRecord;

    @Mock
    CollectionType collectionType;

    @Mock
    Holdings holdings;

    @Mock
    Bib bib;

    @Mock
    ContentType bibContent;

    @Mock
    CollectionType bibContentCollection;

    @Mock
    RecordType recordType;

    @Mock
    LeaderFieldType leader;

    @Mock
    PropertyUtil propertyUtil;

    private String scsbXmlContent = "<bibRecords>\n" +
            "    <bibRecord>\n" +
            "        <bib>\n" +
            "            <owningInstitutionId>NYPL</owningInstitutionId>\n" +
            "            <owningInstitutionBibId>.b100000186</owningInstitutionBibId>\n" +
            "            <content>\n" +
            "                <collection xmlns=\"http://www.loc.gov/MARC21/slim\">\n" +
            "                <record>\n" +
            "                    <controlfield tag=\"001\">NYPG001000008-B</controlfield>\n" +
            "                    <controlfield tag=\"005\">20001116192418.8</controlfield>\n" +
            "                    <controlfield tag=\"008\">841106s1975 le b 000 0 arax cam i</controlfield>\n" +
            "                    <datafield ind1=\"1\" ind2=\" \" tag=\"100\">\n" +
            "                        <subfield code=\"a\">Bashsh.</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\"8\" ind2=\" \" tag=\"952\">\n" +
            "                        <subfield code=\"h\">*OFX 84-1995</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\" \" ind2=\"0\" tag=\"650\">\n" +
            "                        <subfield code=\"a\">Women</subfield>\n" +
            "                        <subfield code=\"z\">Lebanon.</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\" \" ind2=\" \" tag=\"250\">\n" +
            "                        <subfield code=\"a\">al-Tah 1.</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\" \" ind2=\" \" tag=\"010\">\n" +
            "                        <subfield code=\"a\">78970449</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\" \" ind2=\" \" tag=\"504\">\n" +
            "                        <subfield code=\"a\">Includes bibliographical references.</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\" \" ind2=\" \" tag=\"546\">\n" +
            "                        <subfield code=\"a\">In Arabic.</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\" \" ind2=\" \" tag=\"260\">\n" +
            "                        <subfield code=\"a\">Bayr:</subfield>\n" +
            "                        <subfield code=\"b\">Dr al-Tah,</subfield>\n" +
            "                        <subfield code=\"c\">1975.</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\"8\" ind2=\" \" tag=\"952\">\n" +
            "                        <subfield code=\"h\">*OFX 84-1995</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\" \" ind2=\" \" tag=\"300\">\n" +
            "                        <subfield code=\"a\">68 p. ;</subfield>\n" +
            "                        <subfield code=\"c\">20 cm.</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\"1\" ind2=\"3\" tag=\"245\">\n" +
            "                        <subfield code=\"a\">al-Marah al-Lubnyah :</subfield>\n" +
            "                        <subfield code=\"b\">wwa-qad/</subfield>\n" +
            "                        <subfield code=\"c\">NajlBashsh</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\" \" ind2=\" \" tag=\"959\">\n" +
            "                        <subfield code=\"a\">.b10000197</subfield>\n" +
            "                        <subfield code=\"b\">07-18-08</subfield>\n" +
            "                        <subfield code=\"c\">07-29-91</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\" \" ind2=\" \" tag=\"040\">\n" +
            "                        <subfield code=\"c\">NN</subfield>\n" +
            "                        <subfield code=\"d\">NN</subfield>\n" +
            "                        <subfield code=\"d\">WaOLN</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\" \" ind2=\" \" tag=\"043\">\n" +
            "                        <subfield code=\"a\">a-le---</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\"0\" ind2=\"0\" tag=\"050\">\n" +
            "                        <subfield code=\"a\">HQ1728</subfield>\n" +
            "                        <subfield code=\"b\">.B37</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\"0\" ind2=\"0\" tag=\"908\">\n" +
            "                        <subfield code=\"a\">HQ1728</subfield>\n" +
            "                        <subfield code=\"b\">.B37</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\" \" ind2=\" \" tag=\"997\">\n" +
            "                        <subfield code=\"a\">ho</subfield>\n" +
            "                        <subfield code=\"b\">12-15-00</subfield>\n" +
            "                        <subfield code=\"c\">m</subfield>\n" +
            "                        <subfield code=\"d\">a</subfield>\n" +
            "                        <subfield code=\"e\">-</subfield>\n" +
            "                        <subfield code=\"f\">ara</subfield>\n" +
            "                        <subfield code=\"g\">le</subfield>\n" +
            "                        <subfield code=\"h\">3</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\"0\" ind2=\"0\" tag=\"907\">\n" +
            "                        <subfield code=\"a\">.b100000186</subfield>\n" +
            "                    </datafield>\n" +
            "                    <leader>00777cam a2200229 i 4500</leader>\n" +
            "                </record>\n" +
            "            </collection>\n" +
            "        </content>\n" +
            "    </bib>\n" +
            "    <holdings>\n" +
            "        <holding>\n" +
            "            <owningInstitutionHoldingsId/>\n" +
            "            <content>\n" +
            "                <collection xmlns=\"http://www.loc.gov/MARC21/slim\">\n" +
            "                <record>\n" +
            "                    <datafield ind1=\"8\" ind2=\" \" tag=\"852\">\n" +
            "                        <subfield code=\"b\">rcma2</subfield>\n" +
            "                        <subfield code=\"h\">*OFX 84-1995</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\" \" ind2=\" \" tag=\"866\">\n" +
            "                        <subfield code=\"a\"/>\n" +
            "                    </datafield>\n" +
            "                </record>\n" +
            "            </collection>\n" +
            "        </content>\n" +
            "        <items>\n" +
            "            <content>\n" +
            "                <collection xmlns=\"http://www.loc.gov/MARC21/slim\">\n" +
            "                <record>\n" +
            "                    <datafield ind1=\" \" ind2=\" \" tag=\"876\">\n" +
            "                        <subfield code=\"p\">33433002031718</subfield>\n" +
            "                        <subfield code=\"h\">In Library Use</subfield>\n" +
            "                        <subfield code=\"a\">.i100000046</subfield>\n" +
            "                        <subfield code=\"j\">Available</subfield>\n" +
            "                        <subfield code=\"t\">1</subfield>\n" +
            "                    </datafield>\n" +
            "                    <datafield ind1=\" \" ind2=\" \" tag=\"900\">\n" +
            "                        <subfield code=\"a\">Shared</subfield>\n" +
            "                        <subfield code=\"b\">NA</subfield>\n" +
            "                    </datafield>\n" +
            "                </record>\n" +
            "            </collection>\n" +
            "        </content>\n" +
            "    </items>\n" +
            "</holding>\n" +
            "</holdings>\n" +
            "</bibRecord>\n" +
            "</bibRecords>\n";
    @Test
    public void convert() throws Exception {
        BibRecords bibRecords = getBibRecords();
        AccessionRequest accessionRequest = new AccessionRequest();
        accessionRequest.setCustomerCode("NA");
        accessionRequest.setItemBarcode("33433002031718");
        Map collectionGroupMap=new HashMap();
        collectionGroupMap.put("Shared",1);
        Map<String, Object> holdingsMap=new HashMap<>();
        holdingsMap.put("holdingsEntity", saveBibSingleHoldingsSingleItem("33433002031718","NA","NYPL",".b100000186").getHoldingsEntities().get(0));
        Mockito.when(commonUtil.getInstitutionEntityMap()).thenReturn(getMap());
        Mockito.when(marcUtil.isSubFieldExists(bibRecords.getBibRecordList().get(0).getBib().getContent().getCollection().getRecord().get(0), "245")).thenReturn(true);
        Mockito.when(bibliographicDetailsRepository.findByOwningInstitutionIdAndOwningInstitutionBibIdAndIsDeletedFalse(3,".b100000186")).thenReturn((saveBibSingleHoldingsSingleItem("33433002031718","NA","NYPL",".b100000186")));
        Mockito.when(marcUtil.getInd1ForRecordType(bibRecords.getBibRecordList().get(0).getBib().getContent().getCollection().getRecord().get(0),"852","h")).thenReturn("In Library Use");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "p")).thenReturn("33433002031718");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "a")).thenReturn(".i100000046");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "900", null, null, "a")).thenReturn("Shared");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "t")).thenReturn("1");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "h")).thenReturn("In Library Use");
        Mockito.when(commonUtil.getCollectionGroupMap()).thenReturn(collectionGroupMap);
        Mockito.when(commonUtil.buildHoldingsEntity(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.anyString(),Mockito.anyString())).thenReturn((saveBibSingleHoldingsSingleItem("33433002031718","NA","NYPL",".b100000186").getHoldingsEntities().get(0)));
        Mockito.when(commonUtil.addHoldingsEntityToMap(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(holdingsMap);
        ImsLocationEntity imsLocationEntity=new ImsLocationEntity();
        Map map = scsbToBibEntityConverter.convert(bibRecords.getBibRecordList().get(0), "NYPL",accessionRequest,imsLocationEntity);
        assertNotNull(map);
        BibliographicEntity bibliographicEntity = (BibliographicEntity) map.get("bibliographicEntity");
        assertNotNull(bibliographicEntity);
        List<HoldingsEntity> holdingsEntities = bibliographicEntity.getHoldingsEntities();
        assertNotNull(holdingsEntities);
        assertTrue(holdingsEntities.size() == 1);
        List<ItemEntity> itemEntities = bibliographicEntity.getItemEntities();
        assertNotNull(itemEntities);
        assertTrue(itemEntities.size() == 1);
    }

    @Test
    public void processAndValidateItemEntity() throws Exception {
        BibRecords bibRecords = getBibRecords();
        AccessionRequest accessionRequest = new AccessionRequest();
        accessionRequest.setCustomerCode("NA");
        accessionRequest.setItemBarcode("33433002031718");
        Map collectionGroupMap=new HashMap();
        collectionGroupMap.put("Shared",1);
        Map<String, Object> holdingsMap=new HashMap<>();
        holdingsMap.put("holdingsEntity", saveBibSingleHoldingsSingleItem("33433002031718","NA","NYPL",".b100000186").getHoldingsEntities().get(0));
        Mockito.when(commonUtil.getInstitutionEntityMap()).thenReturn(getMap());
        Mockito.when(marcUtil.isSubFieldExists(bibRecords.getBibRecordList().get(0).getBib().getContent().getCollection().getRecord().get(0), "245")).thenReturn(true);
        Mockito.when(bibliographicDetailsRepository.findByOwningInstitutionIdAndOwningInstitutionBibIdAndIsDeletedFalse(3,".b100000186")).thenReturn((saveBibSingleHoldingsSingleItem("33433002031718","NA","NYPL",".b100000186")));
        Mockito.when(marcUtil.getInd1ForRecordType(bibRecords.getBibRecordList().get(0).getBib().getContent().getCollection().getRecord().get(0),"852","h")).thenReturn("In Library Use");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "p")).thenReturn("33433002031718");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "a")).thenReturn("");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "900", null, null, "a")).thenReturn("");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "t")).thenReturn("1");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "h")).thenReturn(null);
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "k")).thenReturn("itemLibrary");
        Mockito.when(commonUtil.getCollectionGroupMap()).thenReturn(collectionGroupMap);
        Mockito.when(commonUtil.buildHoldingsEntity(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.anyString(),Mockito.anyString())).thenReturn((saveBibSingleHoldingsSingleItem("33433002031718","NA","NYPL",".b100000186").getHoldingsEntities().get(0)));
        Mockito.when(commonUtil.addHoldingsEntityToMap(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(holdingsMap);
        ImsLocationEntity imsLocationEntity=new ImsLocationEntity();

        Map map = scsbToBibEntityConverter.convert(bibRecords.getBibRecordList().get(0), "NYPL",accessionRequest,imsLocationEntity);
        assertNotNull(map);
    }

    @Test
    public void processAndValidateItemEntityNullLeaderFieldType() throws Exception {
        AccessionRequest accessionRequest = new AccessionRequest();
        accessionRequest.setCustomerCode("NA");
        accessionRequest.setItemBarcode("33433002031718");
        Map collectionGroupMap=new HashMap();
        collectionGroupMap.put("Shared",1);
        Map<String, Object> holdingsMap=new HashMap<>();
        holdingsMap.put("holdingsEntity", saveBibSingleHoldingsSingleItem("33433002031718","NA","NYPL",".b100000186").getHoldingsEntities().get(0));
        Mockito.when(commonUtil.getInstitutionEntityMap()).thenReturn(getMap());
        List<Holdings> holdingsList=new ArrayList<>();
        holdingsList.add(holdings);
        Mockito.when(bibRecord.getHoldings()).thenReturn(holdingsList);
        Mockito.when(bib.getOwningInstitutionBibId()).thenReturn(".b100000186");
        Mockito.when(bib.getContent()).thenReturn(bibContent);
        Mockito.when(bibContent.getCollection()).thenReturn(bibContentCollection);
        Mockito.when(bibRecord.getBib()).thenReturn(bib);
        List<RecordType> record=new ArrayList<>();
        record.add(recordType);
        Mockito.when(bibContentCollection.getRecord()).thenReturn(record);
        Mockito.when(bibContentCollection.serialize(Mockito.any())).thenReturn("bibXmlContent");
        Mockito.when(marcUtil.isSubFieldExists(recordType, "245")).thenReturn(true);
        ImsLocationEntity imsLocationEntity=new ImsLocationEntity();
        Map map = scsbToBibEntityConverter.convert(bibRecord, "NYPL",accessionRequest,imsLocationEntity);
        assertNotNull(map);
    }

    @Test
    public void processAndValidateItemEntityException() throws Exception {
        AccessionRequest accessionRequest = new AccessionRequest();
        accessionRequest.setCustomerCode("NA");
        accessionRequest.setItemBarcode("33433002031718");
        Map collectionGroupMap=new HashMap();
        collectionGroupMap.put("Shared",1);
        Map<String, Object> holdingsMap=new HashMap<>();
        holdingsMap.put("holdingsEntity", saveBibSingleHoldingsSingleItem("33433002031718","NA","NYPL",".b100000186").getHoldingsEntities().get(0));
        Mockito.when(commonUtil.getInstitutionEntityMap()).thenReturn(getMap());
        List<Holdings> holdingsList=new ArrayList<>();
        holdingsList.add(holdings);
        Mockito.when(holdings.getHolding()).thenReturn(null);
        Mockito.when(bibRecord.getHoldings()).thenReturn(holdingsList);
        Mockito.when(bib.getOwningInstitutionBibId()).thenReturn(".b100000186");
        Mockito.when(bib.getContent()).thenReturn(bibContent);
        Mockito.when(bibContent.getCollection()).thenReturn(bibContentCollection);
        Mockito.when(bibRecord.getBib()).thenReturn(bib);
        List<RecordType> record=new ArrayList<>();
        record.add(recordType);
        Mockito.when(bibContentCollection.getRecord()).thenReturn(record);
        Mockito.when(bibContentCollection.serialize(Mockito.any())).thenReturn("bibXmlContent");
        Mockito.when(marcUtil.isSubFieldExists(recordType, "245")).thenReturn(true);
        Mockito.when(recordType.getLeader()).thenReturn(leader);
        Mockito.when(leader.getValue()).thenReturn("00777cam a2200229 i 4500");
        ImsLocationEntity imsLocationEntity=new ImsLocationEntity();
        Map map = scsbToBibEntityConverter.convert(bibRecord, "NYPL",accessionRequest,imsLocationEntity);
        assertNotNull(map);
    }

    @Test
    public void processAndValidateItemEntityEmptyBarcode() throws Exception {
        BibRecords bibRecords = getBibRecords();
        AccessionRequest accessionRequest = new AccessionRequest();
        accessionRequest.setCustomerCode("NA");
        accessionRequest.setItemBarcode("");
        Map collectionGroupMap=new HashMap();
        collectionGroupMap.put("Shared",1);
        Map<String, Object> holdingsMap=new HashMap<>();
        holdingsMap.put("holdingsEntity", saveBibSingleHoldingsSingleItem("33433002031718","NA","NYPL",".b100000186").getHoldingsEntities().get(0));
        Mockito.when(commonUtil.getInstitutionEntityMap()).thenReturn(getMap());
        Mockito.when(marcUtil.isSubFieldExists(bibRecords.getBibRecordList().get(0).getBib().getContent().getCollection().getRecord().get(0), "245")).thenReturn(true);
        Mockito.when(bibliographicDetailsRepository.findByOwningInstitutionIdAndOwningInstitutionBibIdAndIsDeletedFalse(3,".b100000186")).thenReturn((saveBibSingleHoldingsSingleItem("33433002031718","NA","NYPL",".b100000186")));
        Mockito.when(marcUtil.getInd1ForRecordType(bibRecords.getBibRecordList().get(0).getBib().getContent().getCollection().getRecord().get(0),"852","h")).thenReturn("In Library Use");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "p")).thenReturn("");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "a")).thenReturn("");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "900", null, null, "a")).thenReturn("");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "t")).thenReturn("1");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "h")).thenReturn(null);
        Mockito.when(commonUtil.getCollectionGroupMap()).thenReturn(collectionGroupMap);
        Mockito.when(commonUtil.buildHoldingsEntity(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.anyString(),Mockito.anyString())).thenReturn((saveBibSingleHoldingsSingleItem("33433002031718","NA","NYPL",".b100000186").getHoldingsEntities().get(0)));
        Mockito.when(commonUtil.addHoldingsEntityToMap(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(holdingsMap);
        ImsLocationEntity imsLocationEntity=new ImsLocationEntity();
        Map map = scsbToBibEntityConverter.convert(bibRecords.getBibRecordList().get(0), "NYPL",accessionRequest,imsLocationEntity);
        assertNotNull(map);
    }

    @Test
    public void processAndValidateItemEntityMismatchBarcode() throws Exception {
        BibRecords bibRecords = getBibRecords();
        AccessionRequest accessionRequest = new AccessionRequest();
        accessionRequest.setCustomerCode("NA");
        accessionRequest.setItemBarcode("33433002031718");
        Map collectionGroupMap=new HashMap();
        collectionGroupMap.put("Shared",1);
        Map<String, Object> holdingsMap=new HashMap<>();
        holdingsMap.put("holdingsEntity", saveBibSingleHoldingsSingleItem("33433002031718","NA","NYPL",".b100000186").getHoldingsEntities().get(0));
        Mockito.when(commonUtil.getInstitutionEntityMap()).thenReturn(getMap());
        Mockito.when(marcUtil.isSubFieldExists(bibRecords.getBibRecordList().get(0).getBib().getContent().getCollection().getRecord().get(0), "245")).thenReturn(true);
        Mockito.when(bibliographicDetailsRepository.findByOwningInstitutionIdAndOwningInstitutionBibIdAndIsDeletedFalse(3,".b100000186")).thenReturn((saveBibSingleHoldingsSingleItem("33433002031718","NA","NYPL",".b100000186")));
        Mockito.when(marcUtil.getInd1ForRecordType(bibRecords.getBibRecordList().get(0).getBib().getContent().getCollection().getRecord().get(0),"852","h")).thenReturn("In Library Use");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "p")).thenReturn("");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "a")).thenReturn("");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "900", null, null, "a")).thenReturn("");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "t")).thenReturn("1");
        Mockito.when(marcUtil.getDataFieldValueForRecordType(bibRecords.getBibRecordList().get(0).getHoldings().get(0).getHolding().get(0).getItems().get(0).getContent().getCollection().getRecord().get(0), "876", null, null, "h")).thenReturn(null);
        Mockito.when(commonUtil.getCollectionGroupMap()).thenReturn(collectionGroupMap);
        Mockito.when(commonUtil.buildHoldingsEntity(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.anyString(),Mockito.anyString())).thenReturn((saveBibSingleHoldingsSingleItem("33433002031718","NA","NYPL",".b100000186").getHoldingsEntities().get(0)));
        Mockito.when(commonUtil.addHoldingsEntityToMap(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(holdingsMap);
        ImsLocationEntity imsLocationEntity=new ImsLocationEntity();
        Map map = scsbToBibEntityConverter.convert(bibRecords.getBibRecordList().get(0), "NYPL",accessionRequest,imsLocationEntity);
        assertNotNull(map);
    }

    private Map getMap() {
        Map institutionEntityMap  = new HashMap();
        institutionEntityMap.put("NYPL",3);
        return institutionEntityMap;
    }

    @Test
    public void getMarcUtil() throws Exception {
        ReflectionTestUtils.setField(scsbToBibEntityConverter,"marcUtil",null);
        MarcUtil marcUtil=scsbToBibEntityConverter.getMarcUtil();
        assertNotNull(marcUtil);
    }

    @Test
    public void isFormat() throws Exception {
        boolean format=scsbToBibEntityConverter.isFormat("SCSB");
        assertTrue(format);
    }

    @Test
    public void processAndValidateBibliographicEntity() throws Exception {
        AccessionRequest accessionRequest = new AccessionRequest();
        accessionRequest.setCustomerCode("NA");
        accessionRequest.setItemBarcode("33433002031718");
        Bib bib=new Bib();
        bib.setOwningInstitutionBibId("");
        ContentType contentType=new ContentType();
        contentType.setCollection(collectionType);
        Mockito.when(collectionType.serialize(Mockito.any())).thenReturn("");
        bib.setContent(contentType);
        Mockito.when(bibRecord.getBib()).thenReturn(bib);
        RecordType recordType=new RecordType();
        LeaderFieldType leaderFieldType=new LeaderFieldType();
        leaderFieldType.setValue("00777cam a2200229 i 45001");
        recordType.setLeader(leaderFieldType);
        List<RecordType> recordTypes=new ArrayList<>();
        recordTypes.add(recordType);
        Mockito.when(collectionType.getRecord()).thenReturn(recordTypes);
        Mockito.when(marcUtil.isSubFieldExists(Mockito.any(RecordType.class),Mockito.anyString())).thenReturn(false);
        ImsLocationEntity imsLocationEntity=new ImsLocationEntity();
        Map map = scsbToBibEntityConverter.convert(bibRecord, "NYPL",accessionRequest,imsLocationEntity);
        assertNotNull(map);
    }

    private BibRecords getBibRecords() throws JAXBException, XMLStreamException {
        JAXBContext context = JAXBContext.newInstance(BibRecords.class);
        XMLInputFactory xif = XMLInputFactory.newFactory();
        xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        InputStream stream = new ByteArrayInputStream(scsbXmlContent.getBytes(StandardCharsets.UTF_8));
        XMLStreamReader xsr = xif.createXMLStreamReader(stream);
        Unmarshaller um = context.createUnmarshaller();
        BibRecords   bibRecords = (BibRecords) JAXBIntrospector.getValue(um.unmarshal(xsr));
        return bibRecords;
    }

    public BibliographicEntity saveBibSingleHoldingsSingleItem(String itemBarcode, String customerCode, String institution,String owningInstBibId) throws Exception {

        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent("sourceBibContent".getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionId(3);
        bibliographicEntity.setOwningInstitutionBibId(owningInstBibId);

        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setDeleted(false);
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setCreatedBy(ScsbCommonConstants.ACCESSION);
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setLastUpdatedBy(ScsbCommonConstants.ACCESSION);
        holdingsEntity.setOwningInstitutionId(3);
        holdingsEntity.setOwningInstitutionHoldingsId("5123222f-2333-413e-8c9c-cb8709f010c3");

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setOwningInstitutionItemId(".i100000046");
        itemEntity.setOwningInstitutionId(1);
        itemEntity.setBarcode(itemBarcode);
        itemEntity.setCallNumber("1");
        itemEntity.setCollectionGroupId(1);
        itemEntity.setCallNumberType("1");
        itemEntity.setCustomerCode(customerCode);
        itemEntity.setCreatedDate(new Date());
        itemEntity.setCreatedBy("tst");
        itemEntity.setLastUpdatedBy("tst");
        itemEntity.setItemAvailabilityStatusId(1);itemEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        itemEntity.setBibliographicEntities(Arrays.asList(bibliographicEntity));
        List<ItemEntity> itemEntitylist = new LinkedList(Arrays.asList(itemEntity));
        holdingsEntity.setItemEntities(itemEntitylist);
        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        bibliographicEntity.setItemEntities(Arrays.asList(itemEntity));
        return bibliographicEntity;

    }

}
