package org.recap.converter;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.marc4j.marc.Record;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.jaxb.Bib;
import org.recap.model.jpa.*;
import org.recap.model.marc.BibMarcRecord;
import org.recap.model.marc.HoldingsMarcRecord;
import org.recap.model.marc.ItemMarcRecord;
import org.recap.repository.jpa.ImsLocationDetailsRepository;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.jpa.ItemDetailsRepository;
import org.recap.repository.jpa.OwnerCodeDetailsRepository;
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
import java.util.Optional;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by premkb on 21/12/16.
 */
public class MarcToBibEntityConverterUT extends BaseTestCaseUT {

    @InjectMocks
    MarcToBibEntityConverter marcToBibEntityConverter;

    @Mock
    MarcUtil marcUtil;

    @Mock
    CommonUtil commonUtil;

    @Mock
    DBReportUtil dbReportUtil;

    @Mock
    BibMarcRecord bibMarcRecord;

    @Mock
    Record bibRecord;

    @Mock
    InstitutionDetailsRepository institutionDetailsRepository;

    @Mock
    HoldingsMarcRecord holdingsMarcRecord;

    @Mock
    ItemMarcRecord itemMarcRecord;

    @Mock
    Record itemRecord;

    @Mock
    OwnerCodeDetailsRepository ownerCodeDetailsRepository;

    @Mock
    ItemDetailsRepository itemDetailsRepository;

    @Mock
    ImsLocationDetailsRepository imsLocationDetailsRepository;

    @Mock
    ImsLocationEntity imsLocationEntity;

    @Mock
    InstitutionEntity institutionEntity;

    @Mock
    Bib bib;

    @Mock
    ReportEntity bibReportEntity;

    @Mock
    PropertyUtil propertyUtil;

    @Test
    public void convert() throws Exception {
        List<Record> records = getRecords();
        Map institutionEntityMap=new HashMap();
        institutionEntityMap.put("PUL",1);
        Mockito.when(commonUtil.getInstitutionEntityMap()).thenReturn(institutionEntityMap);
        Mockito.when(marcUtil.getControlFieldValue(Mockito.any(),Mockito.anyString())).thenReturn("1");
        Mockito.when(marcUtil.buildBibMarcRecord(Mockito.any(Record.class))).thenReturn(bibMarcRecord);
        Mockito.when(bibMarcRecord.getBibRecord()).thenReturn(bibRecord);
        List<HoldingsMarcRecord> holdingsMarcRecords=new ArrayList<>();
        holdingsMarcRecords.add(holdingsMarcRecord);
        Mockito.when(bibMarcRecord.getHoldingsMarcRecords()).thenReturn(holdingsMarcRecords);
        List<ItemMarcRecord> itemMarcRecordList=new ArrayList<>();
        itemMarcRecordList.add(itemMarcRecord);
        Mockito.when(ownerCodeDetailsRepository.findByOwnerCode(Mockito.anyString())).thenReturn(getOwnerCodeEntity());
        Mockito.when(holdingsMarcRecord.getItemMarcRecordList()).thenReturn(itemMarcRecordList);
        Mockito.when(itemMarcRecord.getItemRecord()).thenReturn(itemRecord);
        Mockito.when(marcUtil.getDataFieldValue(itemRecord, "876", 'z')).thenReturn("PA");
        Record record = (Record) records.get(0);
        Mockito.when(holdingsMarcRecord.getHoldingsRecord()).thenReturn(record);
        Mockito.when(commonUtil.buildHoldingsEntity(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.anyString(),Mockito.anyString())).thenReturn(saveBibSingleHoldingsSingleItem("32101095533293","PA","1","1").getHoldingsEntities().get(0));
        Mockito.when(marcUtil.getDataFieldValue(record, "852", '0')).thenReturn("5123222f-2333-413e-8c9c-cb8709f010c3");
        Map<String, Object> map1=new HashMap<>();
        map1.put("holdingsEntity",saveBibSingleHoldingsSingleItem("32101095533293","PA","1","1").getHoldingsEntities().get(0));
        Mockito.when(commonUtil.addHoldingsEntityToMap(Mockito.anyMap(),Mockito.any(),Mockito.anyString())).thenReturn(map1);
        Map<String, Object> map = new HashMap<>();
        BibliographicEntity bibliographicEntity1=saveBibSingleHoldingsSingleItem("32101095533293","PA","1","115115");
        map.put(ScsbConstants.BIBLIOGRAPHIC_ENTITY,bibliographicEntity1);
        Mockito.when(marcUtil.extractXmlAndSetEntityToMap(Mockito.any(),Mockito.any(),Mockito.anyMap(),Mockito.any())).thenReturn(map);
        Mockito.when(marcUtil.getDataFieldValue(itemRecord, "876", 'p')).thenReturn("32101095533293");
        Mockito.when(marcUtil.getDataFieldValue(itemRecord, "876", 't')).thenReturn("0");
        Mockito.when(marcUtil.getDataFieldValue(itemRecord, "876", 'k')).thenReturn("itemLibrary");
        Mockito.when(institutionDetailsRepository.findById(Mockito.anyInt())).thenReturn(Optional.of(getInstitutionEntity()));
        Mockito.when(marcUtil.getDataFieldValue(itemRecord, "876", 'x')).thenReturn(ScsbCommonConstants.SHARED_CGD);
        Map collectionGroupMap=new HashMap();
        collectionGroupMap.put("Shared",1);
        Mockito.when(imsLocationEntity.getId()).thenReturn(1);
        Mockito.when(imsLocationDetailsRepository.findByImsLocationCode("RECAP")).thenReturn(imsLocationEntity);
        Mockito.when(commonUtil.getCollectionGroupMap()).thenReturn(collectionGroupMap);
        Mockito.when(marcUtil.getDataFieldValue(itemRecord, "876", 'h')).thenReturn("Library");
        Mockito.when(marcUtil.getDataFieldValue(itemRecord, "876", 'a')).thenReturn("7453441");
        Mockito.when(marcUtil.getDataFieldValue(itemRecord, "876", 'l')).thenReturn("RECAP");
        Mockito.when(ownerCodeDetailsRepository.findByOwnerCodeAndImsLocationId(Mockito.anyString(),Mockito.anyInt())).thenReturn(getOwnerCodeEntity());
        Map convertedMap = marcToBibEntityConverter.convert(records.get(0),null);
        BibliographicEntity bibliographicEntity = (BibliographicEntity)convertedMap.get("bibliographicEntity");
        assertNotNull(bibliographicEntity);
        assertEquals("115115",bibliographicEntity.getOwningInstitutionBibId());
        assertEquals(Integer.valueOf(3),bibliographicEntity.getOwningInstitutionId());
        assertEquals("5123222f-2333-413e-8c9c-cb8709f010c3",bibliographicEntity.getHoldingsEntities().get(0).getOwningInstitutionHoldingsId());
    }

    @Test
    public void convertHoldingValidation() throws Exception {
        Mockito.when(bib.getOwningInstitutionBibId()).thenReturn("");
        Mockito.when(marcUtil.buildBibMarcRecord(Mockito.any(Record.class))).thenReturn(bibMarcRecord);
        Mockito.when(bibMarcRecord.getBibRecord()).thenReturn(bibRecord);
        Map<String, Object> map = new HashMap<>();
        map.put("bibReportEntity",bibReportEntity);
        Mockito.when(holdingsMarcRecord.getHoldingsRecord()).thenReturn(bibRecord);
        Mockito.when(commonUtil.addHoldingsEntityToMap(Mockito.anyMap(),Mockito.any(),Mockito.anyString())).thenReturn(map);
        Mockito.when(marcUtil.extractXmlAndSetEntityToMap(Mockito.any(),Mockito.any(),Mockito.anyMap(),Mockito.any())).thenReturn(map);
        Map result = marcToBibEntityConverter.convert(bibRecord,institutionEntity);
        assertNotNull(result);
    }

    @Test
    public void convertError() throws Exception {
        List<Record> records = getRecords();
        Map institutionEntityMap=new HashMap();
        institutionEntityMap.put("PUL",1);
        Mockito.when(commonUtil.getInstitutionEntityMap()).thenReturn(institutionEntityMap);
        Mockito.when(marcUtil.getControlFieldValue(Mockito.any(),Mockito.anyString())).thenReturn("");
        Mockito.when(marcUtil.buildBibMarcRecord(Mockito.any(Record.class))).thenReturn(bibMarcRecord);
        Mockito.when(bibMarcRecord.getBibRecord()).thenReturn(bibRecord);
        List<HoldingsMarcRecord> holdingsMarcRecords=new ArrayList<>();
        holdingsMarcRecords.add(holdingsMarcRecord);
        Mockito.when(bibMarcRecord.getHoldingsMarcRecords()).thenReturn(holdingsMarcRecords);
        List<ItemMarcRecord> itemMarcRecordList=new ArrayList<>();
        itemMarcRecordList.add(itemMarcRecord);
        Mockito.when(holdingsMarcRecord.getItemMarcRecordList()).thenReturn(itemMarcRecordList);
        Mockito.when(itemMarcRecord.getItemRecord()).thenReturn(itemRecord);
        Mockito.when(marcUtil.getDataFieldValue(itemRecord, "876", 'z')).thenReturn(null);
        Record record = (Record) records.get(0);
        Mockito.when(holdingsMarcRecord.getHoldingsRecord()).thenReturn(record);
        Mockito.when(commonUtil.buildHoldingsEntity(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.anyString(),Mockito.anyString())).thenReturn(saveBibSingleHoldingsSingleItem("32101095533293","PA","1","1").getHoldingsEntities().get(0));
        Mockito.when(marcUtil.getDataFieldValue(record, "852", '0')).thenReturn("5123222f-2333-413e-8c9c-cb8709f010c3");
        Map<String, Object> map1=new HashMap<>();
        map1.put("holdingsEntity",saveBibSingleHoldingsSingleItem("32101095533293","PA","1","1").getHoldingsEntities().get(0));
        Mockito.when(commonUtil.addHoldingsEntityToMap(Mockito.anyMap(),Mockito.any(),Mockito.anyString())).thenReturn(map1);
        Map<String, Object> map = new HashMap<>();
        BibliographicEntity bibliographicEntity1=saveBibSingleHoldingsSingleItem("32101095533293","PA","1","115115");
        map.put(ScsbConstants.BIBLIOGRAPHIC_ENTITY,bibliographicEntity1);
        Mockito.when(marcUtil.extractXmlAndSetEntityToMap(Mockito.any(),Mockito.any(),Mockito.anyMap(),Mockito.any())).thenReturn(map);
        Mockito.when(marcUtil.getDataFieldValue(itemRecord, "876", 'p')).thenReturn("32101095533293").thenReturn("");
        Mockito.when(marcUtil.getDataFieldValue(itemRecord, "876", 't')).thenReturn("0");
        Mockito.when(institutionDetailsRepository.findById(Mockito.anyInt())).thenReturn(Optional.of(getInstitutionEntity()));
        Mockito.when(marcUtil.getDataFieldValue(itemRecord, "876", 'x')).thenReturn(ScsbCommonConstants.SHARED_CGD);
        Map collectionGroupMap=new HashMap();
        collectionGroupMap.put("Shared",1);
        Mockito.when(imsLocationEntity.getId()).thenReturn(1);
        Mockito.when(imsLocationDetailsRepository.findByImsLocationCode(Mockito.anyString())).thenReturn(imsLocationEntity);
        Mockito.when(commonUtil.getCollectionGroupMap()).thenReturn(collectionGroupMap);
        Mockito.when(marcUtil.getDataFieldValue(itemRecord, "876", 'h')).thenReturn("Library");
        Mockito.when(marcUtil.getDataFieldValue(itemRecord, "876", 'a')).thenReturn("");
        Mockito.when(itemDetailsRepository.findByBarcode(Mockito.anyString())).thenReturn(saveBibSingleHoldingsSingleItem("32101095533293","PA","1","115115").getItemEntities());
        Map convertedMap = marcToBibEntityConverter.convert(records.get(0),null);
        BibliographicEntity bibliographicEntity = (BibliographicEntity)convertedMap.get("bibliographicEntity");
        assertNotNull(bibliographicEntity);
        assertEquals("115115",bibliographicEntity.getOwningInstitutionBibId());
        assertEquals(Integer.valueOf(3),bibliographicEntity.getOwningInstitutionId());
        assertEquals("5123222f-2333-413e-8c9c-cb8709f010c3",bibliographicEntity.getHoldingsEntities().get(0).getOwningInstitutionHoldingsId());
    }

    @Test
    public void convertException() throws Exception {
        List<Record> records = getRecords();
        Map institutionEntityMap=new HashMap();
        institutionEntityMap.put("PUL",1);
        Mockito.when(commonUtil.getInstitutionEntityMap()).thenReturn(institutionEntityMap);
        Mockito.when(marcUtil.getControlFieldValue(Mockito.any(),Mockito.anyString())).thenReturn("1");
        Mockito.when(marcUtil.buildBibMarcRecord(Mockito.any(Record.class))).thenThrow(NullPointerException.class);
        Map convertedMap = marcToBibEntityConverter.convert(records.get(0),null);
        BibliographicEntity bibliographicEntity = (BibliographicEntity)convertedMap.get("bibliographicEntity");
        assertNull(bibliographicEntity);
    }

    private List<Record> getRecords() throws Exception {
        URL resource = getClass().getResource("sampleRecord.xml");
        File file = new File(resource.toURI());
        String marcXmlString = FileUtils.readFileToString(file, "UTF-8");
        MarcUtil marcUtil = new MarcUtil();
        return marcUtil.readMarcXml(marcXmlString);
    }

    private OwnerCodeEntity getOwnerCodeEntity() {
        OwnerCodeEntity ownerCodeEntity=new OwnerCodeEntity();
        ownerCodeEntity.setId(1);
        ownerCodeEntity.setInstitutionId(1);
        ownerCodeEntity.setDescription(ScsbCommonConstants.PRINCETON);
        ownerCodeEntity.setOwnerCode("PUL");
        InstitutionEntity institutionEntity=new InstitutionEntity();
        institutionEntity.setId(1);
        institutionEntity.setInstitutionName(ScsbCommonConstants.PRINCETON);
        institutionEntity.setInstitutionCode("PUL");
        ownerCodeEntity.setInstitutionEntity(institutionEntity);
        return ownerCodeEntity;
    }
    private InstitutionEntity getInstitutionEntity() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setId(1);
        institutionEntity.setInstitutionName("PUL");
        institutionEntity.setInstitutionCode("PUL");
        return institutionEntity;
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