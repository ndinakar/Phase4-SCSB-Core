package org.recap.converter;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.marc4j.marc.Leader;
import org.marc4j.marc.Record;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;
import org.recap.ScsbCommonConstants;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.ImsLocationEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.marc.BibMarcRecord;
import org.recap.model.marc.HoldingsMarcRecord;
import org.recap.model.marc.ItemMarcRecord;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.util.CommonUtil;
import org.recap.util.DBReportUtil;
import org.recap.util.MarcUtil;
import org.recap.util.PropertyUtil;

import java.io.File;
import java.net.URL;
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
 * Created by chenchulakshmig on 20/10/16.
 */


public class AccessionMarcToBibEntityConverterUT extends BaseTestCaseUT {

    @InjectMocks
    AccessionMarcToBibEntityConverter marcToBibEntityConverter;

    @Mock
    DBReportUtil dbReportUtil;

    @Mock
    CommonUtil commonUtil;

    @Mock
    MarcUtil marcUtil;

    @Mock
    BibMarcRecord bibMarcRecord;

    @Mock
    Record bibRecord;

    @Mock
    Leader leader;

    @Mock
    BibliographicDetailsRepository bibliographicDetailsRepository;

    @Mock
    HoldingsMarcRecord holdingsMarcRecord;

    @Mock
    ItemMarcRecord itemMarcRecord;

    @Mock
    PropertyUtil propertyUtil;

    @Test
    public void convert() throws Exception {
        List<Record> records = getRecords();
        AccessionRequest accessionRequest = new AccessionRequest();
        accessionRequest.setCustomerCode("PA");
        accessionRequest.setItemBarcode("32101095533293");
        Record record = (Record) records.get(0);
        MarcUtil marcUtil1=new MarcUtil();
        Mockito.when(marcUtil.buildBibMarcRecord(Mockito.any(Record.class))).thenReturn(bibMarcRecord);
        Mockito.when(bibMarcRecord.getBibRecord()).thenReturn(bibRecord);
        Map institutionEntityMap=new HashMap();
        institutionEntityMap.put("PUL",1);
        Mockito.when(commonUtil.getInstitutionEntityMap()).thenReturn(institutionEntityMap);
        Mockito.when(marcUtil.getControlFieldValue(Mockito.any(),Mockito.anyString())).thenReturn("1");
        String bibContent = marcUtil1.writeMarcXml(record);
        Mockito.when(marcUtil.writeMarcXml(Mockito.any())).thenReturn(bibContent);
        Mockito.when(marcUtil.isSubFieldExists(bibRecord, "245")).thenReturn(true);
        Mockito.when(bibRecord.getLeader()).thenReturn(leader);
        Mockito.when(leader.toString()).thenReturn("01750cam a2200493 i 4500");
        Mockito.when(bibliographicDetailsRepository.findByOwningInstitutionIdAndOwningInstitutionBibIdAndIsDeletedFalse(Mockito.anyInt(),Mockito.anyString())).thenReturn(saveBibSingleHoldingsSingleItem("32101095533293","PA","1","1"));
        List<HoldingsMarcRecord> holdingsMarcRecords=new ArrayList<>();
        holdingsMarcRecords.add(holdingsMarcRecord);
        Mockito.when(bibMarcRecord.getHoldingsMarcRecords()).thenReturn(holdingsMarcRecords);
        Mockito.when(holdingsMarcRecord.getHoldingsRecord()).thenReturn(record);
        Mockito.when(commonUtil.buildHoldingsEntity(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.anyString(),Mockito.anyString())).thenReturn(saveBibSingleHoldingsSingleItem("32101095533293","PA","1","1").getHoldingsEntities().get(0));
        Mockito.when(marcUtil.getDataFieldValue(Mockito.any(Record.class),Mockito.anyString(),Mockito.anyChar())).thenReturn("1");
        Map<String, Object> map1=new HashMap<>();
        map1.put("holdingsEntity",saveBibSingleHoldingsSingleItem("32101095533293","PA","1","1").getHoldingsEntities().get(0));
        Mockito.when(commonUtil.addHoldingsEntityToMap(Mockito.anyMap(),Mockito.any(),Mockito.anyString())).thenReturn(map1);
        List<ItemMarcRecord> itemMarcRecordList=new ArrayList<>();
        itemMarcRecordList.add(itemMarcRecord);
        Mockito.when(holdingsMarcRecord.getItemMarcRecordList()).thenReturn(itemMarcRecordList);
        Mockito.when( marcUtil.getDataFieldValue(null,"876", 'p')).thenReturn("32101095533293");
        Mockito.when( marcUtil.getDataFieldValue(null,"876", 't')).thenReturn("0");
        Mockito.when( marcUtil.getDataFieldValue(null,"876", 'x')).thenReturn("Shared");
        Mockito.when( marcUtil.getDataFieldValue(null,"876", 'h')).thenReturn("In Library Use");
        Mockito.when( marcUtil.getDataFieldValue(null,"876", 'a')).thenReturn("7453441");
        Mockito.when( marcUtil.getDataFieldValue(record, "852", 'h')).thenReturn(null);
        Map collectionGroupMap=new HashMap();
        collectionGroupMap.put("Shared",1);
        Mockito.when(commonUtil.getCollectionGroupMap()).thenReturn(collectionGroupMap);
        ImsLocationEntity imsLocationEntity=new ImsLocationEntity();
        Map map = marcToBibEntityConverter.convert(records.get(0), "PUL",accessionRequest,imsLocationEntity);
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
    public void isFormat() throws Exception {
        boolean isFormat= marcToBibEntityConverter.isFormat("MARC");
        assertTrue(isFormat);
    }

    @Test
    public void processAndValidateItemEntity() throws Exception {
        List<Record> records = getRecords();
        AccessionRequest accessionRequest = new AccessionRequest();
        accessionRequest.setCustomerCode("PA");
        accessionRequest.setItemBarcode("32101095533293");
        Record record = (Record) records.get(0);
        MarcUtil marcUtil1=new MarcUtil();
        Mockito.when(marcUtil.buildBibMarcRecord(Mockito.any(Record.class))).thenReturn(bibMarcRecord);
        Mockito.when(bibMarcRecord.getBibRecord()).thenReturn(bibRecord);
        Map institutionEntityMap=new HashMap();
        institutionEntityMap.put("PUL",1);
        Mockito.when(commonUtil.getInstitutionEntityMap()).thenReturn(institutionEntityMap);
        Mockito.when(marcUtil.getControlFieldValue(Mockito.any(),Mockito.anyString())).thenReturn("1");
        String bibContent = marcUtil1.writeMarcXml(record);
        Mockito.when(marcUtil.writeMarcXml(Mockito.any())).thenReturn(bibContent);
        Mockito.when(marcUtil.isSubFieldExists(bibRecord, "245")).thenReturn(true);
        Mockito.when(bibRecord.getLeader()).thenReturn(leader);
        Mockito.when(leader.toString()).thenReturn("01750cam a2200493 i 4500");
        Mockito.when(bibliographicDetailsRepository.findByOwningInstitutionIdAndOwningInstitutionBibIdAndIsDeletedFalse(Mockito.anyInt(),Mockito.anyString())).thenReturn(saveBibSingleHoldingsSingleItem("32101095533293","PA","1","1"));
        List<HoldingsMarcRecord> holdingsMarcRecords=new ArrayList<>();
        holdingsMarcRecords.add(holdingsMarcRecord);
        Mockito.when(bibMarcRecord.getHoldingsMarcRecords()).thenReturn(holdingsMarcRecords);
        Mockito.when(holdingsMarcRecord.getHoldingsRecord()).thenReturn(record);
        Mockito.when(commonUtil.buildHoldingsEntity(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.anyString(),Mockito.anyString())).thenReturn(saveBibSingleHoldingsSingleItem("32101095533293","PA","1","1").getHoldingsEntities().get(0));
        Mockito.when(marcUtil.getDataFieldValue(Mockito.any(Record.class),Mockito.anyString(),Mockito.anyChar())).thenReturn("1");
        Map<String, Object> map1=new HashMap<>();
        map1.put("holdingsEntity",saveBibSingleHoldingsSingleItem("32101095533293","PA","1","1").getHoldingsEntities().get(0));
        Mockito.when(commonUtil.addHoldingsEntityToMap(Mockito.anyMap(),Mockito.any(),Mockito.anyString())).thenReturn(map1);
        List<ItemMarcRecord> itemMarcRecordList=new ArrayList<>();
        itemMarcRecordList.add(itemMarcRecord);
        Mockito.when(holdingsMarcRecord.getItemMarcRecordList()).thenReturn(itemMarcRecordList);
        Mockito.when( marcUtil.getDataFieldValue(null,"876", 'p')).thenReturn("");
        Mockito.when( marcUtil.getDataFieldValue(null,"876", 't')).thenReturn("");
        Mockito.when( marcUtil.getDataFieldValue(null,"876", 'k')).thenReturn("itemLibrary");
        Mockito.when( marcUtil.getDataFieldValue(null,"876", 'x')).thenReturn("");
        Mockito.when( marcUtil.getDataFieldValue(null,"876", 'h')).thenReturn(null);
        Mockito.when( marcUtil.getDataFieldValue(null,"876", 'a')).thenReturn("");
        Map collectionGroupMap=new HashMap();
        collectionGroupMap.put(ScsbCommonConstants.SHARED_CGD,1);
        collectionGroupMap.put(ScsbCommonConstants.NOT_AVAILABLE_CGD,4);
        Mockito.when(commonUtil.getCollectionGroupMap()).thenReturn(collectionGroupMap);
        ImsLocationEntity imsLocationEntity=new ImsLocationEntity();
        Map map = marcToBibEntityConverter.convert(records.get(0), "PUL",accessionRequest,imsLocationEntity);
        assertNotNull(map);
    }

    @Test
    public void processAndValidateBibliographicEntity() throws Exception {
        List<Record> records = getRecords();
        AccessionRequest accessionRequest = new AccessionRequest();
        accessionRequest.setCustomerCode("PA");
        accessionRequest.setItemBarcode("32101095533293");
        Record record = (Record) records.get(0);
        MarcUtil marcUtil1=new MarcUtil();
        Mockito.when(marcUtil.buildBibMarcRecord(Mockito.any(Record.class))).thenReturn(bibMarcRecord);
        Mockito.when(bibMarcRecord.getBibRecord()).thenReturn(bibRecord);
        Map institutionEntityMap=new HashMap();
        Mockito.when(commonUtil.getInstitutionEntityMap()).thenReturn(institutionEntityMap);
        String bibContent = marcUtil1.writeMarcXml(record);
        Mockito.when(marcUtil.writeMarcXml(Mockito.any())).thenReturn(null);
        Mockito.when(marcUtil.isSubFieldExists(bibRecord, "245")).thenReturn(false);
        Mockito.when(bibRecord.getLeader()).thenReturn(null);
        List<HoldingsMarcRecord> holdingsMarcRecords=new ArrayList<>();
        holdingsMarcRecords.add(holdingsMarcRecord);
        Mockito.when(bibMarcRecord.getHoldingsMarcRecords()).thenReturn(holdingsMarcRecords);
        Mockito.when(holdingsMarcRecord.getHoldingsRecord()).thenReturn(record);
        Mockito.when(commonUtil.buildHoldingsEntity(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.anyString(),Mockito.anyString())).thenReturn(saveBibSingleHoldingsSingleItem("32101095533293","PA","1","1").getHoldingsEntities().get(0));
        Mockito.when(marcUtil.getDataFieldValue(Mockito.any(Record.class),Mockito.anyString(),Mockito.anyChar())).thenReturn("1");
        Map<String, Object> map1=new HashMap<>();
        map1.put("holdingsEntity",saveBibSingleHoldingsSingleItem("32101095533293","PA","1","1").getHoldingsEntities().get(0));
        Mockito.when(commonUtil.addHoldingsEntityToMap(Mockito.anyMap(),Mockito.any(),Mockito.anyString())).thenReturn(map1);
        List<ItemMarcRecord> itemMarcRecordList=new ArrayList<>();
        itemMarcRecordList.add(itemMarcRecord);
        Map collectionGroupMap=new HashMap();
        collectionGroupMap.put(ScsbCommonConstants.SHARED_CGD,1);
        collectionGroupMap.put(ScsbCommonConstants.NOT_AVAILABLE_CGD,4);
        Mockito.when(commonUtil.getCollectionGroupMap()).thenReturn(collectionGroupMap);
        ImsLocationEntity imsLocationEntity=new ImsLocationEntity();
        Map map = marcToBibEntityConverter.convert(records.get(0), "PUL",accessionRequest,imsLocationEntity);
        assertNotNull(map);
    }

    private List<Record> getRecords() throws Exception {
        URL resource = getClass().getResource("sampleRecord.xml");
        File file = new File(resource.toURI());
        String marcXmlString = FileUtils.readFileToString(file, "UTF-8");
        MarcUtil marcUtil = new MarcUtil();
        return marcUtil.readMarcXml(marcXmlString);
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