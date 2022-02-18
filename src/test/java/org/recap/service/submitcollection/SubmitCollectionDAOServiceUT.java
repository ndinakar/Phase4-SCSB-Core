package org.recap.service.submitcollection;


import org.junit.Before;
import org.junit.Test;
import org.marc4j.marc.Record;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.recap.BaseTestCaseUT;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.TestUtil;
import org.recap.model.jaxb.marc.ContentType;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.ItemChangeLogEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.report.SubmitCollectionReportInfo;
import org.recap.model.submitcollection.BoundWithBibliographicEntityObject;
import org.recap.model.submitcollection.NonBoundWithBibliographicEntityObject;
import org.recap.repository.jpa.*;
import org.recap.service.BibliographicRepositoryDAO;
import org.recap.service.common.RepositoryService;
import org.recap.service.common.SetupDataService;
import org.recap.service.submitcollection.callable.SubmitCollectionMatchPointsCheckCallable;
import org.recap.util.MarcUtil;
import org.recap.util.PropertyUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.EntityManager;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;


public class SubmitCollectionDAOServiceUT extends BaseTestCaseUT {

    @InjectMocks
    @Spy
    SubmitCollectionDAOService submitCollectionDAOService;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private PropertyUtil propertyUtil;

    @Mock
    ExecutorService executorService;

    @Mock
    InstitutionDetailsRepository institutionDetailsRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    ItemChangeLogDetailsRepository itemChangeLogDetailsRepository;

    @Mock
    BibliographicDetailsRepository  bibliographicDetailsRepository;

    @Mock
    private SetupDataService setupDataService;

    @Mock
    private SubmitCollectionReportHelperService submitCollectionReportHelperService;

    @Mock
    private SubmitCollectionValidationService submitCollectionValidationService;

    @Mock
    private SubmitCollectionHelperService submitCollectionHelperService;

    @Mock
    ItemDetailsRepository itemDetailsRepository;

    @Mock
    Map<Integer, String> itemStatusIdCodeMap;

    @Mock
    NonBoundWithBibliographicEntityObject nonBoundWithBibliographicEntityObject;

    @Mock
    ItemEntity incomingItemEntity;

    @Mock
    BibliographicEntity fetchedBibliographicEntity;

    @Mock
    BoundWithBibliographicEntityObject boundWithBibliographicEntityObject;

    @Mock
    SubmitCollectionReportInfo submitCollectionReportInfo;

    @Mock
    Set<Integer> processedBibIds;

    @Mock
    List<Map<String, String>> idMapToRemoveIndexList;

    @Mock
    List<Map<String, String>> bibIdMapToRemoveIndexList;

    @Mock
    Set<String> processedBarcodeSetForDummyRecords;

    @Mock
    ItemEntity existingItemEntity;

    @Mock
    BibliographicEntity existingBibliographicEntity;

    @Mock
    BibliographicEntity incomingBibliographicEntity;

    @Mock
    Map<String,BibliographicEntity> incomingOwnInstBibIdBibliographicEntityMap;

    @Mock
    HoldingsEntity holdingsEntity;

    @Mock
    Map<Integer, String> institutionEntityMap;

    @Mock
    ImsLocationDetailsRepository imsLocationDetailsRepository;

    @Mock
    BibliographicRepositoryDAO bibliographicRepositoryDAO;

    @Mock
    ItemEntity fetchedItemEntity;

    @Mock
    ApplicationContext applicationContext;

    @Mock
    BibliographicEntity savedBibliographicEntity;

    @Mock
    MarcUtil marcUtil;

    @Mock
    Record record;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(submitCollectionDAOService, "nonHoldingIdInstitution", "NYPL");
    }

    @Test
    public void updateExistingRecordException() throws Exception{
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<ItemEntity> itemEntity = getBibliographicEntityBoundwith("64343","435").getItemEntities();
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(itemEntity);
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getItemStatusIdCodeMapValue());
        Mockito.when(repositoryService.getBibliographicDetailsRepository()).thenThrow(NullPointerException.class);
        BibliographicEntity bibliographicEntity1=getBibliographicEntity("64343");
        bibliographicEntity1.getItemEntities().get(0).setDeleted(true);
        Mockito.when(imsLocationDetailsRepository.findByImsLocationCode(ScsbConstants.UNKNOWN_INSTITUTION)).thenReturn(TestUtil.getImsLocationEntity(1,"RECAP","RECAP"));
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        List<Future> futures = new ArrayList<>();
        BibliographicEntity bibliographicEntity=submitCollectionDAOService.updateBibliographicEntity(bibliographicEntity1,getSubmitCollectionReportInfoMap("1"),idMapToRemoveIndexList,processedBarcodeSetForDummyRecords, false, executorService, futures);
        assertNull(bibliographicEntity);
    }

    @Test
    public void updateExistingRecordExceptionForUnavailableBarcode() {
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap=new HashMap<>();
        List<ItemEntity> itemEntities=new ArrayList<>();
        itemEntities.add(incomingItemEntity);
        Mockito.when(incomingItemEntity.getBarcode()).thenReturn("123456");
        List<BibliographicEntity> bibliographicEntities=new ArrayList<>();
        bibliographicEntities.add(incomingBibliographicEntity);
        Mockito.when(incomingItemEntity.getBibliographicEntities()).thenReturn(bibliographicEntities);
        Mockito.when(bibliographicEntity.getItemEntities()).thenReturn(itemEntities);
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(itemEntities);
        Mockito.when(bibliographicEntity.getOwningInstitutionBibId()).thenReturn("1");
        Mockito.when(incomingBibliographicEntity.getOwningInstitutionBibId()).thenReturn("1");
        Mockito.when(bibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        Mockito.when(incomingBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());List<Record> fetchedRecords=new ArrayList<>();
        List<Record> fetchedRecords1=new ArrayList<>();
        fetchedRecords1.add(record);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        List<Future> futures = new ArrayList<>();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords1);
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        BibliographicEntity bibliographicEntity1=submitCollectionDAOService.updateBibliographicEntity(bibliographicEntity,submitCollectionReportInfoMap,idMapToRemoveIndexList,processedBarcodeSetForDummyRecords, false, executorService, futures);
        assertNull(bibliographicEntity1);
    }

    @Test
    public void updateExistingRecordExceptionForDeaccessionedItem() {
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap=new HashMap<>();
        List<ItemEntity> itemEntities=new ArrayList<>();
        itemEntities.add(fetchedItemEntity);
        Mockito.when(fetchedItemEntity.getBarcode()).thenReturn("123456");
        Mockito.when(fetchedItemEntity.isDeleted()).thenReturn(true);
        Mockito.when(fetchedItemEntity.getOwningInstitutionItemId()).thenReturn("2");
        List<BibliographicEntity> bibliographicEntities=new ArrayList<>();
        bibliographicEntities.add(fetchedBibliographicEntity);
        Mockito.when(fetchedItemEntity.getBibliographicEntities()).thenReturn(bibliographicEntities);
        Mockito.when(bibliographicEntity.getItemEntities()).thenReturn(itemEntities);
        Mockito.when(fetchedBibliographicEntity.getItemEntities()).thenReturn(itemEntities);
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(itemEntities);
        Mockito.when(bibliographicEntity.getOwningInstitutionBibId()).thenReturn("1");
        Mockito.when(fetchedBibliographicEntity.getOwningInstitutionBibId()).thenReturn("1");
        Mockito.when(fetchedBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        Mockito.when(bibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        List<Future> futures = new ArrayList<>();
        BibliographicEntity bibliographicEntity1=submitCollectionDAOService.updateBibliographicEntity(bibliographicEntity,submitCollectionReportInfoMap,idMapToRemoveIndexList,processedBarcodeSetForDummyRecords,false, executorService, futures);
        assertNull(bibliographicEntity1);
    }

    @Test
    public void updateExistingIncompleteRecordException() throws Exception{
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<ItemEntity> itemEntity = getBibliographicEntityBoundwith("64343","435").getItemEntities();
        itemEntity.get(0).setCollectionGroupEntity(TestUtil.getCollectionGroupEntities(2, "Available","Available"));
        itemEntity.get(0).setUseRestrictions("true");
        itemEntity.get(0).setCatalogingStatus(ScsbCommonConstants.INCOMPLETE_STATUS);
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(itemEntity);
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getItemStatusIdCodeMapValue());
        Mockito.when(repositoryService.getBibliographicDetailsRepository()).thenThrow(NullPointerException.class);
        BibliographicEntity bibliographicEntity1=getBibliographicEntity("64343");
        bibliographicEntity1.getItemEntities().get(0).setDeleted(true);
        bibliographicEntity1.getItemEntities().get(0).setUseRestrictions("true");
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(itemStatusIdCodeMap);
        Mockito.when(itemStatusIdCodeMap.get(null)).thenReturn(ScsbConstants.ITEM_STATUS_AVAILABLE);
        Mockito.when(imsLocationDetailsRepository.findByImsLocationCode(ScsbConstants.UNKNOWN_INSTITUTION)).thenReturn(TestUtil.getImsLocationEntity(1,"RECAP","RECAP"));
        Mockito.when(existingBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        Mockito.when(incomingBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        List<Future> futures = new ArrayList<>();
        BibliographicEntity bibliographicEntity=submitCollectionDAOService.updateBibliographicEntity(bibliographicEntity1,getSubmitCollectionReportInfoMap("1"),idMapToRemoveIndexList,processedBarcodeSetForDummyRecords,false, executorService, futures);
        assertNull(bibliographicEntity);
    }

    @Test
    public void updateExistingRecordnonHoldingIdInstitutionException() throws Exception{
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<ItemEntity> itemEntity = getBibliographicEntityBoundwith("64343","435").getItemEntities();
        itemEntity.get(0).setCollectionGroupEntity(TestUtil.getCollectionGroupEntities(2, ScsbCommonConstants.NOT_AVAILABLE_CGD,ScsbCommonConstants.NOT_AVAILABLE_CGD));
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(itemEntity);
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getItemStatusIdCodeMapValue());
        Mockito.when(repositoryService.getBibliographicDetailsRepository()).thenThrow(NullPointerException.class);
        BibliographicEntity bibliographicEntity1=getBibliographicEntity("64343");
        bibliographicEntity1.getItemEntities().get(0).setDeleted(true);
        bibliographicEntity1.getItemEntities().get(0).setUseRestrictions("true");
        bibliographicEntity1.getItemEntities().get(0).setCollectionGroupId(1);
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(institutionEntityMap);
        Mockito.when(institutionEntityMap.get(1)).thenReturn("NYPL");
        Mockito.when(imsLocationDetailsRepository.findByImsLocationCode(ScsbConstants.UNKNOWN_INSTITUTION)).thenReturn(TestUtil.getImsLocationEntity(1,"RECAP","RECAP"));
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        List<Future> futures = new ArrayList<>();
        BibliographicEntity bibliographicEntity=submitCollectionDAOService.updateBibliographicEntity(bibliographicEntity1,getSubmitCollectionReportInfoMap("1"),idMapToRemoveIndexList,processedBarcodeSetForDummyRecords,false, executorService, futures);
        assertNull(bibliographicEntity);
    }


    @Test
    public void updateBibliographicEntityInBatchForBoundWithEmptyHoldingupdateBibliographicEntityBoundwith() throws Exception{
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<ItemEntity> itemEntity = getBibliographicEntityBoundwith("64343","2435").getItemEntities();
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(itemEntity);
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getItemStatusIdCodeMapValue());
        Mockito.when(repositoryService.getBibliographicDetailsRepository()).thenReturn(bibliographicDetailsRepository);
        ReflectionTestUtils.setField(repositoryService,"itemChangeLogDetailsRepository",itemChangeLogDetailsRepository);
        Mockito.when(repositoryService.getItemChangeLogDetailsRepository()).thenCallRealMethod();
        Mockito.when(bibliographicDetailsRepository.saveAndFlush(any())).thenReturn(getBibliographicEntity("64343"));
        Mockito.when(imsLocationDetailsRepository.findByImsLocationCode(ScsbConstants.UNKNOWN_INSTITUTION)).thenReturn(TestUtil.getImsLocationEntity(1,"RECAP","RECAP"));
        Mockito.when(bibliographicRepositoryDAO.saveOrUpdate(any())).thenReturn(savedBibliographicEntity);
        Mockito.when(savedBibliographicEntity.getId()).thenReturn(1);
        Mockito.when(existingBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        Mockito.when(incomingBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        Mockito.doCallRealMethod().when(submitCollectionValidationService).verifyAndSetMisMatchBoundWithOwnInstBibIdIfAny(Mockito.anyList(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList());
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        List<Future> futures = new ArrayList<>();
        BibliographicEntity bibliographicEntity=submitCollectionDAOService.updateBibliographicEntity(getBibliographicEntity("64343"),getSubmitCollectionReportInfoMap("1"),idMapToRemoveIndexList,processedBarcodeSetForDummyRecords,false, executorService, futures);
        assertNotNull(bibliographicEntity);
    }

    @Test
    public void updateBibliographicEntity() throws Exception{
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<ItemEntity> itemEntity = getBibliographicEntity("1577261074").getItemEntities();
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(itemEntity);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        List<Future> futures = new ArrayList<>();
        BibliographicEntity bibliographicEntity=submitCollectionDAOService.updateBibliographicEntity(getBibliographicEntity("64343"),getSubmitCollectionReportInfoMap("1"),idMapToRemoveIndexList,processedBarcodeSetForDummyRecords,false, executorService, futures);
        assertNull(bibliographicEntity);
    }

    @Test
    public void updateBibliographicEntityException() throws Exception{
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(null);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        List<Future> futures = new ArrayList<>();
        BibliographicEntity bibliographicEntity=submitCollectionDAOService.updateBibliographicEntity(getBibliographicEntity("64343"),getSubmitCollectionReportInfoMap("1"),idMapToRemoveIndexList,processedBarcodeSetForDummyRecords, false, executorService, futures);
        assertNotNull(bibliographicEntity);
    }

    @Test
    public void updateBibliographicEntityDummy() throws Exception{
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<ItemEntity> itemEntity = getBibliographicEntity("d1").getItemEntities();
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        ReflectionTestUtils.setField(repositoryService,"bibliographicDetailsRepository",bibliographicDetailsRepository);
        ReflectionTestUtils.setField(repositoryService,"itemChangeLogDetailsRepository",itemChangeLogDetailsRepository);
        Mockito.when(repositoryService.getBibliographicDetailsRepository()).thenCallRealMethod();
        Mockito.when(repositoryService.getItemChangeLogDetailsRepository()).thenCallRealMethod();
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(itemEntity);
        Mockito.when(bibliographicDetailsRepository.saveAndFlush(any())).thenReturn(getBibliographicEntity("64343"));
        Mockito.when(bibliographicRepositoryDAO.saveOrUpdate(any())).thenReturn(savedBibliographicEntity);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        List<Future> futures = new ArrayList<>();
        BibliographicEntity bibliographicEntity=submitCollectionDAOService.updateBibliographicEntity(getBibliographicEntity("64343"),getSubmitCollectionReportInfoMap("1"),idMapToRemoveIndexList,processedBarcodeSetForDummyRecords, false, executorService, futures);
        assertNotNull(bibliographicEntity);
    }


    @Test
    public void updateBibliographicEntityInBatchForNonBoundWith() throws Exception{
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        List<NonBoundWithBibliographicEntityObject> nonBoundWithBibliographicEntityObjectList = new ArrayList<>();
        NonBoundWithBibliographicEntityObject nonBoundWithBibliographicEntityObject = getNonBoundWithBibliographicEntityObject("1577261074");
        nonBoundWithBibliographicEntityObjectList.add(nonBoundWithBibliographicEntityObject);
        Map<String, List< SubmitCollectionReportInfo >> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<ItemEntity> itemEntity = getBibliographicEntity("1577261074").getItemEntities();
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getItemStatusIdCodeMapValue());
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(getInstitutionIdCodeMapValue());
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(repositoryService.getItemDetailsRepository().findByBarcodeInAndOwningInstitutionId(Arrays.asList("123456"),1)).thenReturn(itemEntity);
        Mockito.when(submitCollectionValidationService.isExistingBoundWithItem(itemEntity.get(0))).thenReturn(false);
        Mockito.when(imsLocationDetailsRepository.findByImsLocationCode(ScsbConstants.UNKNOWN_INSTITUTION)).thenReturn(TestUtil.getImsLocationEntity(1,"RECAP","RECAP"));
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        List<Future> futures = new ArrayList<>();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        List<BibliographicEntity> bibliographicEntities =submitCollectionDAOService.updateBibliographicEntityInBatchForNonBoundWith(nonBoundWithBibliographicEntityObjectList,1,submitCollectionReportInfoMap,getIntegers(),idMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities);
    }

    @Test
    public void updateBibliographicEntityInBatchForNonBoundWithException() throws Exception{
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        List<NonBoundWithBibliographicEntityObject> nonBoundWithBibliographicEntityObjectList = new ArrayList<>();
        NonBoundWithBibliographicEntityObject nonBoundWithBibliographicEntityObject = getNonBoundWithBibliographicEntityObject("1577261074");
        nonBoundWithBibliographicEntityObjectList.add(nonBoundWithBibliographicEntityObject);
        Map<String, List< SubmitCollectionReportInfo >> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1577261074");
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<ItemEntity> itemEntity = getBibliographicEntity("1577261074").getItemEntities();
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getItemStatusIdCodeMapValue());
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(getInstitutionIdCodeMapValue());
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(repositoryService.getItemDetailsRepository().findByBarcodeInAndOwningInstitutionId(Arrays.asList("123456"),1)).thenReturn(itemEntity);
        Mockito.when(submitCollectionValidationService.isExistingBoundWithItem(itemEntity.get(0))).thenReturn(false);
        Mockito.when(submitCollectionReportHelperService.buildSubmitCollectionReportInfo(Mockito.anyMap(), any(), any())).thenThrow(NullPointerException.class);
        Mockito.when(imsLocationDetailsRepository.findByImsLocationCode(ScsbConstants.UNKNOWN_INSTITUTION)).thenReturn(TestUtil.getImsLocationEntity(1,"RECAP","RECAP"));
        Mockito.when(existingBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        List<Future> futures = new ArrayList<>();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        List<BibliographicEntity> bibliographicEntities =submitCollectionDAOService.updateBibliographicEntityInBatchForNonBoundWith(nonBoundWithBibliographicEntityObjectList,1,submitCollectionReportInfoMap,getIntegers(),idMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities);
    }

    @Test
    public void updateBibliographicEntityInBatchForNonBoundWithExceptionUnavailable() throws Exception{
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        List<NonBoundWithBibliographicEntityObject> nonBoundWithBibliographicEntityObjectList=new ArrayList<>();
        nonBoundWithBibliographicEntityObjectList.add(nonBoundWithBibliographicEntityObject);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap=new HashMap<>();
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        List<ItemEntity> fetchedItemEntityList=new ArrayList<>();
        fetchedItemEntityList.add(existingItemEntity);
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(fetchedItemEntityList);
        List<BibliographicEntity> incomingBibliographicEntityList=new ArrayList<>();
        incomingBibliographicEntityList.add(incomingBibliographicEntity);
        Mockito.when(nonBoundWithBibliographicEntityObject.getBibliographicEntityList()).thenReturn(incomingBibliographicEntityList);
        List<ItemEntity> incomingItemEntityList=new ArrayList<>();
        incomingItemEntityList.add(incomingItemEntity);
        List<BibliographicEntity> fetchedBibliographicEntityList=new ArrayList<>();
        fetchedBibliographicEntityList.add(fetchedBibliographicEntity);
        Mockito.when(fetchedBibliographicEntity.getOwningInstitutionBibId()).thenReturn("1");
        Mockito.when(incomingBibliographicEntity.getOwningInstitutionBibId()).thenReturn("1");
        Mockito.when(existingItemEntity.getBibliographicEntities()).thenReturn(fetchedBibliographicEntityList);
        Mockito.when(incomingBibliographicEntity.getItemEntities()).thenReturn(incomingItemEntityList);
        Mockito.when(fetchedBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        Mockito.when(incomingBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        List<Future> futures = new ArrayList<>();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        List<BibliographicEntity> bibliographicEntities =submitCollectionDAOService.updateBibliographicEntityInBatchForNonBoundWith(nonBoundWithBibliographicEntityObjectList,1,submitCollectionReportInfoMap,processedBibIds,idMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities);
    }

    private Map getInstitutionIdCodeMapValue() {
        Map institutionCodeIdMap=new HashMap();
        institutionCodeIdMap.put("NYPL",1);
        return institutionCodeIdMap;
    }

    private Map getItemStatusIdCodeMapValue() {
        Map itemStatusIdCodeMap = new HashMap();
        itemStatusIdCodeMap.put(1, ScsbConstants.ITEM_STATUS_AVAILABLE);
        return itemStatusIdCodeMap;
    }


    @Test
    public void updateBibliographicEntityInBatchForNonBoundWithDummy() throws Exception{
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        List<NonBoundWithBibliographicEntityObject> nonBoundWithBibliographicEntityObjectList = new ArrayList<>();
        NonBoundWithBibliographicEntityObject nonBoundWithBibliographicEntityObject = getNonBoundWithBibliographicEntityObject("8d");
        nonBoundWithBibliographicEntityObjectList.add(nonBoundWithBibliographicEntityObject);
        Map<String, List< SubmitCollectionReportInfo >> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1d");
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<ItemEntity> itemEntity = getBibliographicEntity("d1").getItemEntities();
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getItemStatusIdCodeMapValue());
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(getInstitutionIdCodeMapValue());
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(repositoryService.getItemDetailsRepository().findByBarcodeInAndOwningInstitutionId(Arrays.asList("123456"),1)).thenReturn(itemEntity);
        Mockito.when(repositoryService.getItemChangeLogDetailsRepository()).thenReturn(itemChangeLogDetailsRepository);
        Mockito.when(repositoryService.getBibliographicDetailsRepository()).thenReturn(bibliographicDetailsRepository);
        Mockito.when(submitCollectionValidationService.isExistingBoundWithItem(itemEntity.get(0))).thenReturn(false);
        Mockito.when(repositoryService.getBibliographicDetailsRepository()).thenReturn(bibliographicDetailsRepository);
        Mockito.doNothing().when(bibliographicDetailsRepository).delete(any());
        Mockito.doNothing().when(bibliographicDetailsRepository).flush();
        Mockito.when(bibliographicDetailsRepository.saveAndFlush(any())).thenReturn(incomingBibliographicEntity);
        Mockito.when(repositoryService.getBibliographicDetailsRepository().findByOwningInstitutionIdAndOwningInstitutionBibId(Mockito.anyInt(),Mockito.anyString())).thenReturn(getBibliographicEntity("1577261074"));
        Mockito.when(imsLocationDetailsRepository.findByImsLocationCode(ScsbConstants.UNKNOWN_INSTITUTION)).thenReturn(TestUtil.getImsLocationEntity(1,"RECAP","RECAP"));
        Mockito.when(bibliographicRepositoryDAO.saveOrUpdate(any())).thenReturn(savedBibliographicEntity);
        Mockito.when(savedBibliographicEntity.getId()).thenReturn(1);
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        List<Future> futures = new ArrayList<>();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        List<BibliographicEntity> bibliographicEntities =submitCollectionDAOService.updateBibliographicEntityInBatchForNonBoundWith(nonBoundWithBibliographicEntityObjectList,1,submitCollectionReportInfoMap,getIntegers(),idMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities);
    }

    @Test
    public void updateBibliographicEntityInBatchForNonBoundWithExistingBound() throws Exception{
        List<NonBoundWithBibliographicEntityObject> nonBoundWithBibliographicEntityObjectList = new ArrayList<>();
        NonBoundWithBibliographicEntityObject nonBoundWithBibliographicEntityObject = getNonBoundWithBibliographicEntityObject("1577261074");
        nonBoundWithBibliographicEntityObjectList.add(nonBoundWithBibliographicEntityObject);
        Map<String, List< SubmitCollectionReportInfo >> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap(ScsbConstants.SUBMIT_COLLECTION_FAILURE_LIST);
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<ItemEntity> itemEntity = getBibliographicEntity("1577261074").getItemEntities();
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(repositoryService.getItemDetailsRepository().findByBarcodeInAndOwningInstitutionId(Arrays.asList("123456"),1)).thenReturn(itemEntity);
        Mockito.when(submitCollectionValidationService.isExistingBoundWithItem(itemEntity.get(0))).thenReturn(true);
        List<Future> futures = new ArrayList<>();
        Mockito.doCallRealMethod().when(submitCollectionReportHelperService).setSubmitCollectionReportInfo(Mockito.anyList(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
        Mockito.when(submitCollectionHelperService.getBibliographicIdsInString(any())).thenCallRealMethod();
        List<BibliographicEntity> bibliographicEntities =submitCollectionDAOService.updateBibliographicEntityInBatchForNonBoundWith(nonBoundWithBibliographicEntityObjectList,1,submitCollectionReportInfoMap,getIntegers(),idMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities);
    }
    @Test
    public void updateBibliographicEntityInBatchForNonBoundWithDifferentOwingInsBibId() throws Exception{
        List<NonBoundWithBibliographicEntityObject> nonBoundWithBibliographicEntityObjectList = new ArrayList<>();
        NonBoundWithBibliographicEntityObject nonBoundWithBibliographicEntityObject = getNonBoundWithBibliographicEntityObject("1577261074");
        BibliographicEntity bibliographicEntity = getBibliographicEntity("1577261074");
        bibliographicEntity.setOwningInstitutionBibId("234566");
        nonBoundWithBibliographicEntityObject.setBibliographicEntityList(Arrays.asList(bibliographicEntity));
        nonBoundWithBibliographicEntityObjectList.add(nonBoundWithBibliographicEntityObject);
        Map<String, List< SubmitCollectionReportInfo >> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<Future> futures = new ArrayList<>();
        List<ItemEntity> itemEntity = getBibliographicEntity("1577261074").getItemEntities();
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(repositoryService.getItemDetailsRepository().findByBarcodeInAndOwningInstitutionId(Arrays.asList("123456"),1)).thenReturn(itemEntity);
        Mockito.when(submitCollectionValidationService.isExistingBoundWithItem(itemEntity.get(0))).thenReturn(false);
        List<BibliographicEntity> bibliographicEntities =submitCollectionDAOService.updateBibliographicEntityInBatchForNonBoundWith(nonBoundWithBibliographicEntityObjectList,1,submitCollectionReportInfoMap,getIntegers(),idMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities);
    }

    @Test
    public void updateBibliographicEntityInBatchForBoundWithMismatch(){
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        BibliographicEntity bibliographicEntity =new BibliographicEntity();
        bibliographicEntity.setMaQualifier(1);
        List<BoundWithBibliographicEntityObject> boundWithBibliographicEntityObjectList = new ArrayList<>();
        BoundWithBibliographicEntityObject boundWithBibliographicEntityObject = getBoundWithBibliographicEntityObject();
        boundWithBibliographicEntityObjectList.add(boundWithBibliographicEntityObject);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        List<Map<String, String>> bibIdMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<ItemEntity> itemEntity = getBibliographicEntity("1577261074").getItemEntities();
        Map<String,BibliographicEntity> bibliographicEntityMap = new HashMap<>();
        bibliographicEntityMap.put("1",getBibliographicEntity("1577261074"));
        Map<String, ItemEntity> fetchedBarcodeItemEntityMap = new HashMap<>();
        fetchedBarcodeItemEntityMap.put("123456",getBibliographicEntity("1577261074").getItemEntities().get(0));
        List<BibliographicEntity> fetchedBibliographicEntityList = new ArrayList<>();
        fetchedBibliographicEntityList.add(getBibliographicEntity("1577261074"));
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(repositoryService.getItemDetailsRepository().findByBarcodeInAndOwningInstitutionId(Arrays.asList("123456"),1)).thenReturn(itemEntity);
        Mockito.when(submitCollectionValidationService.validateIncomingItemHavingBibCountIsSameAsExistingItem(Mockito.anyMap(),Mockito.anyMap(),Mockito.anyList())).thenReturn(true);
        Map<String,BibliographicEntity> fetchedOwnInstBibIdBibliographicEntityMap=new HashMap<>();
        List<Future> futures = new ArrayList<>();
        fetchedOwnInstBibIdBibliographicEntityMap.put("34558",getBibliographicEntity("1577261074"));
        Mockito.when(submitCollectionValidationService.getOwnInstBibIdBibliographicEntityMap(Mockito.anyList())).thenReturn(fetchedOwnInstBibIdBibliographicEntityMap);
        Mockito.doCallRealMethod().when(submitCollectionValidationService).verifyAndSetMisMatchBoundWithOwnInstBibIdIfAny(Mockito.anyList(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList());
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        List<BibliographicEntity> bibliographicEntities = submitCollectionDAOService.updateBibliographicEntityInBatchForBoundWith(boundWithBibliographicEntityObjectList,1,submitCollectionReportInfoMap,getIntegers(),idMapToRemoveIndexList,bibIdMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities);

    }

    @Test
    public void updateBibliographicEntityInBatchForBoundWith(){
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        List<BoundWithBibliographicEntityObject> boundWithBibliographicEntityObjectList = new ArrayList<>();
        BoundWithBibliographicEntityObject boundWithBibliographicEntityObject = getBoundWithBibliographicEntityObject();
        boundWithBibliographicEntityObjectList.add(boundWithBibliographicEntityObject);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        List<Map<String, String>> bibIdMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<ItemEntity> itemEntity = getBibliographicEntity("1577261074").getItemEntities();
        Map<String,BibliographicEntity> bibliographicEntityMap = new HashMap<>();
        bibliographicEntityMap.put("1",getBibliographicEntity("1577261074"));
        Map<String, ItemEntity> fetchedBarcodeItemEntityMap = new HashMap<>();
        fetchedBarcodeItemEntityMap.put("123456",getBibliographicEntity("1577261074").getItemEntities().get(0));
        List<BibliographicEntity> fetchedBibliographicEntityList = new ArrayList<>();
        fetchedBibliographicEntityList.add(getBibliographicEntity("1577261074"));
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(repositoryService.getItemDetailsRepository().findByBarcodeInAndOwningInstitutionId(Arrays.asList("123456"),1)).thenReturn(itemEntity);
        Mockito.when(submitCollectionValidationService.validateIncomingItemHavingBibCountIsSameAsExistingItem(Mockito.anyMap(),Mockito.anyMap(),Mockito.anyList())).thenReturn(true);
        Map<String,BibliographicEntity> fetchedOwnInstBibIdBibliographicEntityMap=new HashMap<>();
        fetchedOwnInstBibIdBibliographicEntityMap.put("34558",getBibliographicEntity2("1577261074"));
        Mockito.when(submitCollectionValidationService.getOwnInstBibIdBibliographicEntityMap(Mockito.anyList())).thenReturn(fetchedOwnInstBibIdBibliographicEntityMap);
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getItemStatusIdCodeMapValue());
        Mockito.when(imsLocationDetailsRepository.findByImsLocationCode(ScsbConstants.UNKNOWN_INSTITUTION)).thenReturn(TestUtil.getImsLocationEntity(1,"RECAP","RECAP"));
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        List<Future> futures = new ArrayList<>();
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        List<BibliographicEntity> bibliographicEntities = submitCollectionDAOService.updateBibliographicEntityInBatchForBoundWith(boundWithBibliographicEntityObjectList,1,submitCollectionReportInfoMap,getIntegers(),idMapToRemoveIndexList,bibIdMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities);
    }

    @Test
    public void updateBibliographicEntityInBatchForBoundWithGreaterIncomingBibDummy(){
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        List<BoundWithBibliographicEntityObject> boundWithBibliographicEntityObjectList = new ArrayList<>();
        BoundWithBibliographicEntityObject boundWithBibliographicEntityObject = getBoundWithBibliographicEntityObject();
        BibliographicEntity bibliographicEntity = getBibliographicEntity("1577261074");
        bibliographicEntity.setOwningInstitutionBibId("245466");
        BibliographicEntity bibliographicEntity1 = getBibliographicEntity("1577261074");
        bibliographicEntity1.setOwningInstitutionBibId("657786");
        List<BibliographicEntity> bibliographicEntities = new ArrayList<>();
        bibliographicEntities.add(bibliographicEntity);
        bibliographicEntities.add(bibliographicEntity1);
        boundWithBibliographicEntityObject.setBibliographicEntityList(bibliographicEntities);
        boundWithBibliographicEntityObjectList.add(boundWithBibliographicEntityObject);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        List<Map<String, String>> bibIdMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<ItemEntity> itemEntity = getBibliographicEntity("d1").getItemEntities();
        Map<String,BibliographicEntity> bibliographicEntityMap = new HashMap<>();
        bibliographicEntityMap.put("1",getBibliographicEntity("1577261074"));
        Map<String, ItemEntity> fetchedBarcodeItemEntityMap = new HashMap<>();
        fetchedBarcodeItemEntityMap.put("123456",getBibliographicEntity("1577261074").getItemEntities().get(0));
        List<BibliographicEntity> fetchedBibliographicEntityList = new ArrayList<>();
        fetchedBibliographicEntityList.add(getBibliographicEntity("1577261074"));
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(repositoryService.getItemDetailsRepository().findByBarcodeInAndOwningInstitutionId(Arrays.asList("1234568"),1)).thenReturn(itemEntity);
        Mockito.when(submitCollectionValidationService.validateIncomingItemHavingBibCountGreaterThanExistingItem(Mockito.anyMap(),Mockito.anyList(),Mockito.anyList())).thenReturn(true);
        Mockito.when(submitCollectionHelperService.getBibliographicEntityIfExist(Mockito.anyString(),Mockito.anyInt())).thenReturn(getBibliographicEntity("1577261074"));
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getItemStatusIdCodeMapValue());
        Mockito.when(repositoryService.getBibliographicDetailsRepository()).thenReturn(bibliographicDetailsRepository);
        BibliographicEntity bibliographicEntity2=getBibliographicEntity("1577261074");
        bibliographicEntity2.getItemEntities().get(0).setBarcode("1234567");
        Mockito.when(bibliographicDetailsRepository.findByOwningInstitutionIdAndOwningInstitutionBibId(Mockito.anyInt(),Mockito.anyString())).thenReturn(bibliographicEntity2);
        Mockito.when(repositoryService.getItemChangeLogDetailsRepository()).thenReturn(itemChangeLogDetailsRepository);
        Mockito.when(bibliographicRepositoryDAO.saveOrUpdate(any())).thenReturn(savedBibliographicEntity);
        Mockito.when(savedBibliographicEntity.getId()).thenReturn(1);
        List<ItemEntity> incomingItemEntityList=new ArrayList<>();
        incomingItemEntityList.add(incomingItemEntity);
        Mockito.when(savedBibliographicEntity.getItemEntities()).thenReturn(incomingItemEntityList);
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        List<Future> futures = new ArrayList<>();
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        List<BibliographicEntity> bibliographicEntities1 = submitCollectionDAOService.updateBibliographicEntityInBatchForBoundWith(boundWithBibliographicEntityObjectList,1,submitCollectionReportInfoMap,getIntegers(),idMapToRemoveIndexList,bibIdMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities1);
    }

    @Test
    public void updateBibliographicEntityInBatchForBoundWithGreaterIncomingBibNoIMSLocation(){
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        List<BoundWithBibliographicEntityObject> boundWithBibliographicEntityObjectList = new ArrayList<>();
        BoundWithBibliographicEntityObject boundWithBibliographicEntityObject = getBoundWithBibliographicEntityObject();
        BibliographicEntity bibliographicEntity = getBibliographicEntity("1577261074");
        bibliographicEntity.setOwningInstitutionBibId("245466");
        BibliographicEntity bibliographicEntity1 = getBibliographicEntity("1577261074");
        bibliographicEntity1.setOwningInstitutionBibId("657786");
        List<BibliographicEntity> bibliographicEntities = new ArrayList<>();
        bibliographicEntities.add(bibliographicEntity);
        bibliographicEntities.add(bibliographicEntity1);
        boundWithBibliographicEntityObject.setBibliographicEntityList(bibliographicEntities);
        boundWithBibliographicEntityObjectList.add(boundWithBibliographicEntityObject);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        List<Map<String, String>> bibIdMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<ItemEntity> itemEntity = getBibliographicEntity("1577261074").getItemEntities();
        Map<String,BibliographicEntity> bibliographicEntityMap = new HashMap<>();
        bibliographicEntityMap.put("1",getBibliographicEntity("1577261074"));
        Map<String, ItemEntity> fetchedBarcodeItemEntityMap = new HashMap<>();
        fetchedBarcodeItemEntityMap.put("123456",getBibliographicEntity("1577261074").getItemEntities().get(0));
        List<BibliographicEntity> fetchedBibliographicEntityList = new ArrayList<>();
        fetchedBibliographicEntityList.add(getBibliographicEntity("1577261074"));
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(repositoryService.getItemDetailsRepository().findByBarcodeInAndOwningInstitutionId(Arrays.asList("123456"),1)).thenReturn(itemEntity);
        Mockito.when(submitCollectionValidationService.validateIncomingItemHavingBibCountGreaterThanExistingItem(Mockito.anyMap(),Mockito.anyList(),Mockito.anyList())).thenReturn(true);
        Mockito.when(submitCollectionHelperService.getBibliographicEntityIfExist(Mockito.anyString(),Mockito.anyInt())).thenReturn(getBibliographicEntity("1577261074"));
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getItemStatusIdCodeMapValue());
        Mockito.when(repositoryService.getBibliographicDetailsRepository()).thenReturn(bibliographicDetailsRepository);
        Mockito.when(bibliographicDetailsRepository.findByOwningInstitutionIdAndOwningInstitutionBibId(Mockito.anyInt(),Mockito.anyString())).thenReturn(getBibliographicEntity("1577261074"));
        Map<String,BibliographicEntity> fetchedOwnInstBibIdBibliographicEntityMap=new HashMap<>();
        fetchedOwnInstBibIdBibliographicEntityMap.put("245466",getBibliographicEntity("1577261074"));
        Mockito.when(submitCollectionValidationService.getOwnInstBibIdBibliographicEntityMap(Mockito.anyList())).thenReturn(fetchedOwnInstBibIdBibliographicEntityMap);
        Mockito.when(imsLocationDetailsRepository.findByImsLocationCode(ScsbConstants.UNKNOWN_INSTITUTION)).thenReturn(TestUtil.getImsLocationEntity(1,"RECAP","RECAP"));
        Mockito.when(existingBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        Mockito.when(incomingBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        List<Future> futures = new ArrayList<>();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        List<BibliographicEntity> bibliographicEntities1 = submitCollectionDAOService.updateBibliographicEntityInBatchForBoundWith(boundWithBibliographicEntityObjectList,1,submitCollectionReportInfoMap,getIntegers(),idMapToRemoveIndexList,bibIdMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities1);
    }

    @Mock
    BibliographicEntity bibliographicEntity;

    @Test
    public void updateBibliographicEntityInBatchForBoundWithGreaterIncomingBib(){
        List<BoundWithBibliographicEntityObject> boundWithBibliographicEntityObjectList=new ArrayList<>();
        boundWithBibliographicEntityObjectList.add(boundWithBibliographicEntityObject);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap=new HashMap<>();
        Mockito.when(repositoryService.getItemDetailsRepository()).thenCallRealMethod();
        ReflectionTestUtils.setField(repositoryService,"itemDetailsRepository",itemDetailsRepository);
        List<ItemEntity> itemEntities=new ArrayList<>();
        itemEntities.add(fetchedItemEntity);
        Mockito.when(boundWithBibliographicEntityObject.getBarcode()).thenReturn("123456");
        List<BibliographicEntity> bibliographicEntityList=new ArrayList<>();
        bibliographicEntityList.add(existingBibliographicEntity);
        bibliographicEntityList.add(incomingBibliographicEntity);
        Mockito.when(boundWithBibliographicEntityObject.getBibliographicEntityList()).thenReturn(bibliographicEntityList);
        Mockito.when(fetchedItemEntity.getBarcode()).thenReturn("123456");
        Mockito.when(fetchedItemEntity.getId()).thenReturn(1);
        List<BibliographicEntity> bibliographicEntities=new ArrayList<>();
        bibliographicEntities.add(incomingBibliographicEntity);
        Mockito.when(incomingBibliographicEntity.getOwningInstitutionBibId()).thenReturn("5");
        List<BibliographicEntity> bibliographicEntities2=new ArrayList<>();
        bibliographicEntities2.add(bibliographicEntity);
        bibliographicEntities2.add(bibliographicEntity);
        List<ItemEntity> itemEntitiesIncoming=new ArrayList<>();
        itemEntitiesIncoming.add(incomingItemEntity);
        Mockito.when(incomingItemEntity.getBarcode()).thenReturn("123456");
        Mockito.when(bibliographicEntity.getItemEntities()).thenReturn(itemEntitiesIncoming);
        List<HoldingsEntity> holdingsEntities=new ArrayList<>();
        holdingsEntities.add(holdingsEntity);
        Mockito.when(bibliographicEntity.getHoldingsEntities()).thenReturn(holdingsEntities);
        Mockito.when(fetchedItemEntity.getHoldingsEntities()).thenReturn(holdingsEntities);
        Mockito.when(boundWithBibliographicEntityObject.getBibliographicEntityList()).thenReturn(bibliographicEntities2);
        Mockito.when(fetchedItemEntity.getBibliographicEntities()).thenReturn(bibliographicEntities);
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(itemEntities);
        Mockito.when(submitCollectionValidationService.validateIncomingItemHavingBibCountGreaterThanExistingItem(Mockito.anyMap(),Mockito.anyList(),Mockito.anyList())).thenReturn(true);
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(itemStatusIdCodeMap);
        Mockito.when(itemStatusIdCodeMap.get(0)).thenReturn("Unavailable");
        Mockito.when(bibliographicRepositoryDAO.saveOrUpdate(any())).thenReturn(savedBibliographicEntity);
        Mockito.when(savedBibliographicEntity.getId()).thenReturn(1);
        List<Future> futures = new ArrayList<>();
        List<BibliographicEntity> bibliographicEntities1 = submitCollectionDAOService.updateBibliographicEntityInBatchForBoundWith(boundWithBibliographicEntityObjectList,1,submitCollectionReportInfoMap,getIntegers(),idMapToRemoveIndexList,bibIdMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities1);
    }

    @Test
    public void updateBibliographicEntityInBatchForBoundWithGreaterIncomingBibComplete(){
        List<BoundWithBibliographicEntityObject> boundWithBibliographicEntityObjectList = new ArrayList<>();
        BoundWithBibliographicEntityObject boundWithBibliographicEntityObject = getBoundWithBibliographicEntityObject();
        BibliographicEntity bibliographicEntity = getBibliographicEntity("1577261074");
        bibliographicEntity.setOwningInstitutionBibId("245466");
        BibliographicEntity bibliographicEntity1 = getBibliographicEntity("1577261074");
        bibliographicEntity1.setOwningInstitutionBibId("657786");
        List<BibliographicEntity> bibliographicEntities = new ArrayList<>();
        bibliographicEntities.add(bibliographicEntity);
        bibliographicEntities.add(bibliographicEntity1);
        boundWithBibliographicEntityObject.setBibliographicEntityList(bibliographicEntities);
        boundWithBibliographicEntityObjectList.add(boundWithBibliographicEntityObject);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        List<Map<String, String>> bibIdMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<ItemEntity> itemEntity = getBibliographicEntity("1577261074").getItemEntities();
        Map<String,BibliographicEntity> bibliographicEntityMap = new HashMap<>();
        bibliographicEntityMap.put("1",getBibliographicEntity("1577261074"));
        Map<String, ItemEntity> fetchedBarcodeItemEntityMap = new HashMap<>();
        fetchedBarcodeItemEntityMap.put("123456",getBibliographicEntity("1577261074").getItemEntities().get(0));
        List<BibliographicEntity> fetchedBibliographicEntityList = new ArrayList<>();
        fetchedBibliographicEntityList.add(getBibliographicEntity("1577261074"));
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(repositoryService.getItemDetailsRepository().findByBarcodeInAndOwningInstitutionId(Arrays.asList("123456"),1)).thenReturn(itemEntity);
        Mockito.when(submitCollectionValidationService.validateIncomingItemHavingBibCountGreaterThanExistingItem(Mockito.anyMap(),Mockito.anyList(),Mockito.anyList())).thenReturn(true);
        Mockito.when(submitCollectionHelperService.getBibliographicEntityIfExist(Mockito.anyString(),Mockito.anyInt())).thenReturn(null);
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getItemStatusIdCodeMapValue());
        Mockito.when(repositoryService.getBibliographicDetailsRepository()).thenReturn(bibliographicDetailsRepository);
        Mockito.when(bibliographicDetailsRepository.saveAndFlush(any())).thenReturn(getBibliographicEntity("1577261074"));
        Mockito.when(imsLocationDetailsRepository.findByImsLocationCode(ScsbConstants.UNKNOWN_INSTITUTION)).thenReturn(TestUtil.getImsLocationEntity(1,"RECAP","RECAP"));
        Mockito.when(bibliographicDetailsRepository.findByOwningInstitutionIdAndOwningInstitutionBibId(Mockito.anyInt(),Mockito.anyString())).thenReturn(getBibliographicEntity("1577261074"));
        Mockito.when(bibliographicRepositoryDAO.saveOrUpdate(any())).thenReturn(savedBibliographicEntity);
        Mockito.when(savedBibliographicEntity.getId()).thenReturn(1);
        List<Future> futures = new ArrayList<>();
        List<BibliographicEntity> bibliographicEntities1 = submitCollectionDAOService.updateBibliographicEntityInBatchForBoundWith(boundWithBibliographicEntityObjectList,1,submitCollectionReportInfoMap,getIntegers(),idMapToRemoveIndexList,bibIdMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities1);
    }

    @Test
    public void updateBibliographicEntityInBatchForBoundWithGreaterExistingBibliographicEntityelse(){
        List<BoundWithBibliographicEntityObject> boundWithBibliographicEntityObjectList = new ArrayList<>();
        BoundWithBibliographicEntityObject boundWithBibliographicEntityObject = getBoundWithBibliographicEntityObject();
        boundWithBibliographicEntityObjectList.add(boundWithBibliographicEntityObject);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        List<Map<String, String>> bibIdMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<String> itemBarcodeList = new ArrayList<>();
        itemBarcodeList.add("123456");
        List<ItemEntity> itemEntity = getBibliographicEntityBoundwith("1577261074","2435").getItemEntities();
        Map<String,BibliographicEntity> bibliographicEntityMap = new HashMap<>();
        bibliographicEntityMap.put("1",getBibliographicEntity("1577261074"));
        Map<String, ItemEntity> fetchedBarcodeItemEntityMap = new HashMap<>();
        fetchedBarcodeItemEntityMap.put("123456",getBibliographicEntity("1577261074").getItemEntities().get(0));
        List<BibliographicEntity> fetchedBibliographicEntityList = new ArrayList<>();
        fetchedBibliographicEntityList.add(getBibliographicEntity("1577261074"));
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(repositoryService.getItemDetailsRepository().findByBarcodeInAndOwningInstitutionId(itemBarcodeList,1)).thenReturn(itemEntity);
        List<Future> futures = new ArrayList<>();
        Mockito.when(submitCollectionValidationService.validateIncomingItemHavingBibCountLesserThanExistingItem(Mockito.anyMap(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList(), any())).thenReturn(true);
        List<BibliographicEntity> bibliographicEntities1 = submitCollectionDAOService.updateBibliographicEntityInBatchForBoundWith(boundWithBibliographicEntityObjectList,1,submitCollectionReportInfoMap,getIntegers(),idMapToRemoveIndexList,bibIdMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities1);
    }

    @Test
    public void updateBibliographicEntityInBatchForBoundWithGreaterExistingBibliographicEntityif(){
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        List<BoundWithBibliographicEntityObject> boundWithBibliographicEntityObjectList = new ArrayList<>();
        BoundWithBibliographicEntityObject boundWithBibliographicEntityObject = getBoundWithBibliographicEntityObject();
        boundWithBibliographicEntityObjectList.add(boundWithBibliographicEntityObject);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        List<Map<String, String>> bibIdMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        List<String> itemBarcodeList = new ArrayList<>();
        itemBarcodeList.add("123456");
        List<ItemEntity> itemEntity = getBibliographicEntityBoundwith("1577261074","2435").getItemEntities();
        Map<String,BibliographicEntity> bibliographicEntityMap = new HashMap<>();
        bibliographicEntityMap.put("1",getBibliographicEntity("1577261074"));
        Map<String, ItemEntity> fetchedBarcodeItemEntityMap = new HashMap<>();
        fetchedBarcodeItemEntityMap.put("123456",getBibliographicEntity("1577261074").getItemEntities().get(0));
        List<BibliographicEntity> fetchedBibliographicEntityList = new ArrayList<>();
        fetchedBibliographicEntityList.add(getBibliographicEntity("1577261074"));
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(repositoryService.getItemDetailsRepository().findByBarcodeInAndOwningInstitutionId(itemBarcodeList,1)).thenReturn(itemEntity);
        Mockito.when(submitCollectionValidationService.validateIncomingItemHavingBibCountLesserThanExistingItem(Mockito.anyMap(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList(), any())).thenReturn(true);
        Mockito.when(imsLocationDetailsRepository.findByImsLocationCode(ScsbConstants.UNKNOWN_INSTITUTION)).thenReturn(TestUtil.getImsLocationEntity(1,"RECAP","RECAP"));
        Map<String,BibliographicEntity> fetchedOwnInstBibIdBibliographicEntityMap=new HashMap<>();
        fetchedOwnInstBibIdBibliographicEntityMap.put("64343",getBibliographicEntity("1577261074"));
        Mockito.when(submitCollectionValidationService.getOwnInstBibIdBibliographicEntityMap(Mockito.anyList())).thenReturn(fetchedOwnInstBibIdBibliographicEntityMap);
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getItemStatusIdCodeMapValue());
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        List<Future> futures = new ArrayList<>();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        List<BibliographicEntity> bibliographicEntities1 = submitCollectionDAOService.updateBibliographicEntityInBatchForBoundWith(boundWithBibliographicEntityObjectList,1,submitCollectionReportInfoMap,getIntegers(),idMapToRemoveIndexList,bibIdMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities1);
    }

    @Test
    public void updateBibliographicEntityInBatchForBoundWithEmptyHolding(){
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        List<BoundWithBibliographicEntityObject> boundWithBibliographicEntityObjectList=new ArrayList<>();
        boundWithBibliographicEntityObjectList.add(boundWithBibliographicEntityObject);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap=new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfoList=new ArrayList<>();
        submitCollectionReportInfoList.add(submitCollectionReportInfo);
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        List<ItemEntity> fetchedItemEntityList=new ArrayList<>();
        fetchedItemEntityList.add(existingItemEntity);
        List<BibliographicEntity> fetchedBibEntityList=new ArrayList<>();
        fetchedBibEntityList.add(existingBibliographicEntity);
        Mockito.when(existingBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        Mockito.when(incomingBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        Mockito.when(existingItemEntity.getBibliographicEntities()).thenReturn(fetchedBibEntityList);
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(fetchedItemEntityList);
        Mockito.when(existingItemEntity.getId()).thenReturn(1);
        Mockito.when(submitCollectionValidationService.validateIncomingItemHavingBibCountLesserThanExistingItem(Mockito.anyMap(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList(), any())).thenReturn(true);
        Mockito.when(submitCollectionValidationService.getOwnInstBibIdBibliographicEntityMap(Mockito.anyList())).thenReturn(incomingOwnInstBibIdBibliographicEntityMap);
        Mockito.when(incomingOwnInstBibIdBibliographicEntityMap.get(null)).thenReturn(incomingBibliographicEntity);
        List<HoldingsEntity> holdingEntityList=new ArrayList<>();
        holdingEntityList.add(holdingsEntity);
        Mockito.when(holdingsEntity.getItemEntities()).thenReturn(fetchedItemEntityList);
        Mockito.when(incomingBibliographicEntity.getHoldingsEntities()).thenReturn(holdingEntityList);
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        Mockito.when(existingBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        Mockito.when(incomingBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        List<Record> fetchedRecords1=new ArrayList<>();
        fetchedRecords1.add(record);
        List<Future> futures = new ArrayList<>();
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords1);
        List<BibliographicEntity> bibliographicEntities1 = submitCollectionDAOService.updateBibliographicEntityInBatchForBoundWith(boundWithBibliographicEntityObjectList,1,submitCollectionReportInfoMap,processedBibIds,idMapToRemoveIndexList,bibIdMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities1);
    }



    @Test
    public void updateBibliographicEntityInBatchForBoundWithNonHoldingIdInstitution(){
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        List<BoundWithBibliographicEntityObject> boundWithBibliographicEntityObjectList=new ArrayList<>();
        boundWithBibliographicEntityObjectList.add(boundWithBibliographicEntityObject);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap=new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfoList=new ArrayList<>();
        submitCollectionReportInfoList.add(submitCollectionReportInfo);
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        List<ItemEntity> fetchedItemEntityList=new ArrayList<>();
        fetchedItemEntityList.add(existingItemEntity);
        List<BibliographicEntity> fetchedBibEntityList=new ArrayList<>();
        fetchedBibEntityList.add(existingBibliographicEntity);
        Mockito.when(existingItemEntity.getBibliographicEntities()).thenReturn(fetchedBibEntityList);
        Mockito.when(existingItemEntity.getOwningInstitutionItemId()).thenReturn("45454");
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(fetchedItemEntityList);
        Mockito.when(existingItemEntity.getId()).thenReturn(1);
        Mockito.when(submitCollectionValidationService.validateIncomingItemHavingBibCountLesserThanExistingItem(Mockito.anyMap(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList(), any())).thenReturn(true);
        Mockito.when(submitCollectionValidationService.getOwnInstBibIdBibliographicEntityMap(Mockito.anyList())).thenReturn(incomingOwnInstBibIdBibliographicEntityMap);
        Mockito.when(incomingOwnInstBibIdBibliographicEntityMap.get(null)).thenReturn(incomingBibliographicEntity);
        List<HoldingsEntity> holdingEntityList=new ArrayList<>();
        holdingEntityList.add(holdingsEntity);
        Mockito.when(holdingsEntity.getItemEntities()).thenReturn(fetchedItemEntityList);
        Mockito.when(incomingBibliographicEntity.getHoldingsEntities()).thenReturn(holdingEntityList);
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(institutionEntityMap);
        Mockito.when(institutionEntityMap.get(0)).thenReturn("NYPL");
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        Mockito.when(existingBibliographicEntity.getHoldingsEntities()).thenReturn(holdingEntityList);
        Mockito.when(existingBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        Mockito.when(incomingBibliographicEntity.getContent()).thenReturn("bibMarcContent".getBytes());
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        List<Future> futures = new ArrayList<>();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        List<BibliographicEntity> bibliographicEntities1 = submitCollectionDAOService.updateBibliographicEntityInBatchForBoundWith(boundWithBibliographicEntityObjectList,3,submitCollectionReportInfoMap,processedBibIds,idMapToRemoveIndexList,bibIdMapToRemoveIndexList,processedBarcodeSetForDummyRecords,executorService,futures);
        assertNotNull(bibliographicEntities1);
    }

    @Test
    public void prepareExceptionReport(){
        List<String> incomingItemBarcodeList = new ArrayList<>();
        incomingItemBarcodeList.add("123456");
        List<String> fetchedItemBarcodeList = new ArrayList<>();
        fetchedItemBarcodeList.add("67380");
        List<ItemEntity> itemEntityList = getBibliographicEntity("1577261074").getItemEntities();
        Map<String,ItemEntity> incomingBarcodeItemEntityMapFromBibliographicEntityList = new HashMap<>();
        incomingBarcodeItemEntityMapFromBibliographicEntityList.put("123456",getBibliographicEntity("1577261074").getItemEntities().get(0));
        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        Mockito.when(submitCollectionReportHelperService.isBarcodeAlreadyAdded("123456",submitCollectionReportInfoMap)).thenReturn(false);
        Mockito.when(submitCollectionReportHelperService.isBarcodeAlreadyAdded(itemEntityList.get(0).getBarcode(),submitCollectionReportInfoMap)).thenReturn(false);
        submitCollectionDAOService.prepareExceptionReport(Arrays.asList("123456"),fetchedItemBarcodeList,incomingBarcodeItemEntityMapFromBibliographicEntityList,submitCollectionReportInfoMap);
    }

    @Test
    public  void updateDummyRecordEmpty(){
        BibliographicEntity bibliographicEntity = getBibliographicEntity("1577261074");
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        List< ItemChangeLogEntity > itemChangeLogEntityList = new ArrayList<>();
        ItemChangeLogEntity itemChangeLogEntity = getItemChangeLogEntity();
        itemChangeLogEntityList.add(itemChangeLogEntity);
        Set<String> processedBarcodeSet = new HashSet<>();
        processedBarcodeSet.add("23");
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity("1577261074");
        BibliographicEntity fetchBibliographicEntity = getBibliographicEntity("1577261074");
        List<ItemEntity> itemEntities = new ArrayList<>();
        itemEntities.add(new ItemEntity());
        List<Future> futures = new ArrayList<>();
        Mockito.when(submitCollectionReportHelperService.getItemBasedOnOwningInstitutionItemIdAndOwningInstitutionId(bibliographicEntity.getItemEntities())).thenReturn(itemEntities);
        BibliographicEntity bibliographicEntity1 = submitCollectionDAOService.updateDummyRecord(bibliographicEntity,submitCollectionReportInfoMap,idMapToRemoveIndexList,processedBarcodeSet,savedBibliographicEntity,fetchBibliographicEntity,executorService,futures);
        assertNotNull(bibliographicEntity1);
    }

    @Test
    public  void updateDummyRecord(){
        BibliographicEntity bibliographicEntity = getBibliographicEntity("1577261074");
        bibliographicEntity.setId(1);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        List< ItemChangeLogEntity > itemChangeLogEntityList = new ArrayList<>();
        ItemChangeLogEntity itemChangeLogEntity = getItemChangeLogEntity();
        itemChangeLogEntityList.add(itemChangeLogEntity);
        Set<String> processedBarcodeSet = new HashSet<>();
        processedBarcodeSet.add("123456");
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity("1577261074");
        BibliographicEntity fetchBibliographicEntity = getBibliographicEntity("1577261074");
        List<ItemEntity> itemEntities = getBibliographicEntity("1577261074").getItemEntities();
        Mockito.when(submitCollectionReportHelperService.getItemBasedOnOwningInstitutionItemIdAndOwningInstitutionId(bibliographicEntity.getItemEntities())).thenReturn(itemEntities);
        Mockito.when(repositoryService.getBibliographicDetailsRepository()).thenReturn(bibliographicDetailsRepository);
        Mockito.when(repositoryService.getItemChangeLogDetailsRepository()).thenReturn(itemChangeLogDetailsRepository);
        Mockito.doNothing().when(bibliographicDetailsRepository).delete(fetchBibliographicEntity);
        Mockito.doNothing().when(bibliographicDetailsRepository).flush();
        Mockito.when(bibliographicDetailsRepository.saveAndFlush(bibliographicEntity)).thenReturn(bibliographicEntity);
        Mockito.when(repositoryService.getBibliographicDetailsRepository().findByOwningInstitutionIdAndOwningInstitutionBibId(bibliographicEntity.getOwningInstitutionId(), bibliographicEntity.getOwningInstitutionBibId())).thenReturn(bibliographicEntity);
        Mockito.doNothing().when(entityManager).refresh(bibliographicEntity);
        Mockito.when(bibliographicRepositoryDAO.saveOrUpdate(any())).thenReturn(savedBibliographicEntity);
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        List<Future> futures = new ArrayList<>();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        BibliographicEntity bibliographicEntity1 = submitCollectionDAOService.updateDummyRecord(bibliographicEntity,submitCollectionReportInfoMap,idMapToRemoveIndexList,processedBarcodeSet,savedBibliographicEntity,fetchBibliographicEntity,executorService,futures);
        assertNotNull(bibliographicEntity1);
    }

    @Test
    public  void updateDummyRecordWithoutCollectionGroupId(){
        BibliographicEntity bibliographicEntity = getBibliographicEntity2("1577261074");
        bibliographicEntity.getItemEntities().get(0).setCollectionGroupId(null);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSet = new HashSet<>();
        processedBarcodeSet.add("123456");
        List<Future> futures = new ArrayList<>();
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity("1577261074");
        BibliographicEntity fetchBibliographicEntity = getBibliographicEntity("1577261074");
        List<ItemEntity> itemEntities = getBibliographicEntity("1577261074").getItemEntities();
        Mockito.when(submitCollectionReportHelperService.getItemBasedOnOwningInstitutionItemIdAndOwningInstitutionId(bibliographicEntity.getItemEntities())).thenReturn(itemEntities);
        BibliographicEntity bibliographicEntity1 = submitCollectionDAOService.updateDummyRecord(bibliographicEntity,submitCollectionReportInfoMap,idMapToRemoveIndexList,processedBarcodeSet,savedBibliographicEntity,fetchBibliographicEntity,executorService,futures);
        assertNotNull(bibliographicEntity1);
    }
    @Test
    public void updateDummyRecordForNonBoundWith(){
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1577261074");
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSet = new HashSet<>();
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity("1577261074");
        BibliographicEntity fetchBibliographicEntity = getBibliographicEntity("1577261074");
        List<ItemChangeLogEntity> itemChangeLogEntityList = new ArrayList<>();
        Mockito.when(repositoryService.getBibliographicDetailsRepository()).thenReturn(bibliographicDetailsRepository);
        Mockito.doNothing().when(bibliographicDetailsRepository).delete(fetchBibliographicEntity);
        Mockito.doNothing().when(bibliographicDetailsRepository).flush();
        Mockito.when(bibliographicDetailsRepository.saveAndFlush(incomingBibliographicEntity)).thenReturn(incomingBibliographicEntity);
        Mockito.when(submitCollectionReportHelperService.getItemBasedOnOwningInstitutionItemIdAndOwningInstitutionId(incomingBibliographicEntity.getItemEntities())).thenReturn(Collections.EMPTY_LIST);
        Mockito.when(bibliographicRepositoryDAO.saveOrUpdate(any())).thenReturn(savedBibliographicEntity);
        List<Future> futures = new ArrayList<>();
        submitCollectionDAOService.updateDummyRecordForNonBoundWith(incomingBibliographicEntity,submitCollectionReportInfoMap,idMapToRemoveIndexList,processedBarcodeSet,savedBibliographicEntity,fetchBibliographicEntity,itemChangeLogEntityList,executorService,futures);
    }
    @Test
    public void updateDummyRecordForNonBoundWithoutCollectionGroupId(){
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1577261074");
        incomingBibliographicEntity.getItemEntities().get(0).setCollectionGroupId(null);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSet = new HashSet<>();
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity("1577261074");
        BibliographicEntity fetchBibliographicEntity = getBibliographicEntity("1577261074");
        List<ItemChangeLogEntity> itemChangeLogEntityList = new ArrayList<>();
        List<Future> futures = new ArrayList<>();
        Mockito.when(submitCollectionReportHelperService.getItemBasedOnOwningInstitutionItemIdAndOwningInstitutionId(incomingBibliographicEntity.getItemEntities())).thenReturn(Collections.EMPTY_LIST);
        submitCollectionDAOService.updateDummyRecordForNonBoundWith(incomingBibliographicEntity,submitCollectionReportInfoMap,idMapToRemoveIndexList,processedBarcodeSet,savedBibliographicEntity,fetchBibliographicEntity,itemChangeLogEntityList,executorService,futures);
    }
    @Test
    public void updateDummyRecordForNonBoundWithfetchedItemBasedOnOwningInstitutionItemId(){
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1577261074");
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("1");
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSet = new HashSet<>();
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity("1577261074");
        BibliographicEntity fetchBibliographicEntity = getBibliographicEntity("1577261074");
        List<ItemChangeLogEntity> itemChangeLogEntityList = new ArrayList<>();
        List<ItemEntity> fetchedItemBasedOnOwningInstitutionItemId = getBibliographicEntity("1577261074").getItemEntities();
        Mockito.when(submitCollectionReportHelperService.getItemBasedOnOwningInstitutionItemIdAndOwningInstitutionId(incomingBibliographicEntity.getItemEntities())).thenReturn(fetchedItemBasedOnOwningInstitutionItemId);
        List<Future> futures = new ArrayList<>();
        submitCollectionDAOService.updateDummyRecordForNonBoundWith(incomingBibliographicEntity,submitCollectionReportInfoMap,idMapToRemoveIndexList,processedBarcodeSet,savedBibliographicEntity,fetchBibliographicEntity,itemChangeLogEntityList,executorService,futures);
    }

    @Test
    public void updateDummyRecordForBoundWith(){
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity2("1577261074");
        incomingBibliographicEntity.getItemEntities().get(0).setItemAvailabilityStatusId(null);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSet = new HashSet<>();
        processedBarcodeSet.add("123456");
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity("1577261074");
        BibliographicEntity fetchBibliographicEntity = getBibliographicEntity("1577261074");
        List< ItemChangeLogEntity > itemChangeLogEntityList = new ArrayList<>();
        boolean deleteDummyRecord = true;
        Set<Integer> processedBibIds = new HashSet<>();
        processedBibIds.add(123456);
        Mockito.when(repositoryService.getBibliographicDetailsRepository()).thenReturn(bibliographicDetailsRepository);
        Mockito.doNothing().when(bibliographicDetailsRepository).delete(fetchBibliographicEntity);
        Mockito.doNothing().when(bibliographicDetailsRepository).flush();
        Mockito.when(repositoryService.getBibliographicDetailsRepository().findByOwningInstitutionIdAndOwningInstitutionBibId(incomingBibliographicEntity.getOwningInstitutionId(), incomingBibliographicEntity.getOwningInstitutionBibId())).thenReturn(getBibliographicEntity("1577261074"));
        Mockito.when(bibliographicRepositoryDAO.saveOrUpdate(any())).thenReturn(savedBibliographicEntity);
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        List<Record> fetchedRecords=new ArrayList<>();
        fetchedRecords.add(record);
        List<Future> futures = new ArrayList<>();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenReturn(fetchedRecords);
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        BibliographicEntity bibliographicEntity = submitCollectionDAOService.updateDummyRecordForBoundWith(incomingBibliographicEntity,submitCollectionReportInfoMap,idMapToRemoveIndexList,processedBarcodeSet,savedBibliographicEntity,fetchBibliographicEntity,itemChangeLogEntityList,deleteDummyRecord,processedBibIds,executorService,futures);
        assertNotNull(bibliographicEntity);
    }

    @Test
    public void updateDummyRecordForBoundWithSubmitCollectionReportInfoForInvalidDummyRecordBasedOnOwnInstItemId(){
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity2("1577261074");
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSet = new HashSet<>();
        processedBarcodeSet.add("123");
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity("1577261074");
        BibliographicEntity fetchBibliographicEntity = getBibliographicEntity("1577261074");
        List< ItemChangeLogEntity > itemChangeLogEntityList = new ArrayList<>();
        boolean deleteDummyRecord = true;
        Set<Integer> processedBibIds = new HashSet<>();
        processedBibIds.add(123456);
        Mockito.when(repositoryService.getBibliographicDetailsRepository()).thenReturn(bibliographicDetailsRepository);
        Mockito.doNothing().when(bibliographicDetailsRepository).delete(fetchBibliographicEntity);
        Mockito.doNothing().when(bibliographicDetailsRepository).flush();
        Mockito.when(repositoryService.getBibliographicDetailsRepository().findByOwningInstitutionIdAndOwningInstitutionBibId(incomingBibliographicEntity.getOwningInstitutionId(), incomingBibliographicEntity.getOwningInstitutionBibId())).thenReturn(getBibliographicEntity("1577261074"));
        List<ItemEntity> itemEntities=new ArrayList<>();
        itemEntities.add(incomingItemEntity);
        List<Future> futures = new ArrayList<>();

        Mockito.when(submitCollectionReportHelperService.getItemBasedOnOwningInstitutionItemIdAndOwningInstitutionId(Mockito.anyList())).thenReturn(itemEntities);
        BibliographicEntity bibliographicEntity = submitCollectionDAOService.updateDummyRecordForBoundWith(incomingBibliographicEntity,submitCollectionReportInfoMap,idMapToRemoveIndexList,processedBarcodeSet,savedBibliographicEntity,fetchBibliographicEntity,itemChangeLogEntityList,deleteDummyRecord,processedBibIds,executorService,futures);
        assertNotNull(bibliographicEntity);
    }
    @Test
    public void updateDummyRecordForBoundWithoutCollectionGroupId(){
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1577261074");
        incomingBibliographicEntity.getItemEntities().get(0).setCollectionGroupId(null);
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Set<String> processedBarcodeSet = new HashSet<>();
        processedBarcodeSet.add("123456");
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity("1577261074");
        BibliographicEntity fetchBibliographicEntity = getBibliographicEntity("1577261074");
        List< ItemChangeLogEntity > itemChangeLogEntityList = new ArrayList<>();
        boolean deleteDummyRecord = true;
        Set<Integer> processedBibIds = new HashSet<>();
        processedBibIds.add(123456);
        List<Future> futures = new ArrayList<>();
        BibliographicEntity bibliographicEntity = submitCollectionDAOService.updateDummyRecordForBoundWith(incomingBibliographicEntity,submitCollectionReportInfoMap,idMapToRemoveIndexList,processedBarcodeSet,savedBibliographicEntity,fetchBibliographicEntity,itemChangeLogEntityList,deleteDummyRecord,processedBibIds,executorService,futures);
        assertNotNull(bibliographicEntity);
    }

    @Test
    public void setMAQualifierToBibByCGD(){
        BibliographicEntity bibliographicEntity = getBibliographicEntity("1");
        String cgdCode =ScsbCommonConstants.SHARED_CGD;
        ReflectionTestUtils.invokeMethod(submitCollectionDAOService,"setMAQualifierToBibByCGD",bibliographicEntity,cgdCode);
    }

    @Test
    public void prepareItemChangeLogEntitiesForCgdUpdates(){
        List<ItemEntity> cgdUpdatedItemEntityList = new ArrayList<>();
        cgdUpdatedItemEntityList.add(getBibliographicEntity("1").getItemEntities().get(0));
        Map<String, String> cgdUpdatedItemMessageMap = new HashMap<>();
        ReflectionTestUtils.invokeMethod(submitCollectionDAOService,"prepareItemChangeLogEntitiesForCgdUpdates",cgdUpdatedItemEntityList,cgdUpdatedItemMessageMap);
    }

    @Test
    public void isNonCompleteBib(){
        BibliographicEntity bibliographicEntity = getBibliographicEntity("1");
        bibliographicEntity.setCatalogingStatus(ScsbCommonConstants.COMPLETE_STATUS);
        ReflectionTestUtils.invokeMethod(submitCollectionDAOService,"isNonCompleteBib",bibliographicEntity);
    }

    @Test
    public void isHasCgdShared(){
        BibliographicEntity bibliographicEntity = getBibliographicEntity("1");
        bibliographicEntity.getItemEntities().get(0).setCollectionGroupId(1);
        Map<Integer, String> collectionGroupIdCodeMap = new HashMap<>();
        collectionGroupIdCodeMap.put(1,ScsbCommonConstants.SHARED_CGD);
        Mockito.when(setupDataService.getCollectionGroupIdCodeMap()).thenReturn(collectionGroupIdCodeMap);
        ReflectionTestUtils.invokeMethod(submitCollectionDAOService,"isHasCgdShared",bibliographicEntity);
    }


    @Test
    public void updateCatalogingStatusForItem(){
        BibliographicEntity bibliographicEntity = getBibliographicEntity("1");
        bibliographicEntity.getItemEntities().get(0).setUseRestrictions("test");
        bibliographicEntity.getItemEntities().get(0).setCollectionGroupId(1);
        ReflectionTestUtils.invokeMethod(submitCollectionDAOService,"updateCatalogingStatusForItem",bibliographicEntity);
    }

    @Test
    public void updateCatalogingStatusForBib(){
        BibliographicEntity bibliographicEntity = getBibliographicEntity("1");
        bibliographicEntity.setCatalogingStatus(ScsbCommonConstants.INCOMPLETE_STATUS);
        bibliographicEntity.getItemEntities().get(0).setCollectionGroupId(1);
        Map<Integer, String> collectionGroupIdCodeMap = new HashMap<>();
        collectionGroupIdCodeMap.put(1,ScsbCommonConstants.SHARED_CGD);
        Mockito.when(setupDataService.getCollectionGroupIdCodeMap()).thenReturn(collectionGroupIdCodeMap);
        ReflectionTestUtils.invokeMethod(submitCollectionDAOService,"updateCatalogingStatusForBib",bibliographicEntity);
    }

    @Test
    public void saveItemChangeLogEntityList(){
        List<ItemChangeLogEntity> itemChangeLogEntityList = new ArrayList<>();
        itemChangeLogEntityList.add(getItemChangeLogEntity());
        Mockito.when(repositoryService.getItemChangeLogDetailsRepository()).thenReturn(itemChangeLogDetailsRepository);
        ReflectionTestUtils.invokeMethod(submitCollectionDAOService,"saveItemChangeLogEntityList",itemChangeLogEntityList);
    }

    @Test
    public void addNewBibToExistingItem(){
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = getSubmitCollectionReportInfoMap("test");
        Set<Integer> processedBibIds = new HashSet<>();
        processedBibIds.add(1);
        List<Map<String, String>> idMapToRemoveIndexList = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        idMapToRemoveIndexList.add(map);
        List<Map<String, String>> bibIdMapToRemoveIndexList = new ArrayList<>();
        bibIdMapToRemoveIndexList.add(map);
        Set<String> processedBarcodeSetForDummyRecords = new HashSet<>();
        processedBarcodeSetForDummyRecords.add("123456");
        Map<String, ItemEntity> fetchedBarcodeItemEntityMap = new HashMap<>();
        fetchedBarcodeItemEntityMap.put("123456",getBibliographicEntity("dd").getItemEntities().get(0));
        fetchedBarcodeItemEntityMap.put("34558",getBibliographicEntity("dd").getItemEntities().get(0));
        List<BibliographicEntity> updatedBibliographicEntityList = new ArrayList<>();
        updatedBibliographicEntityList.add(getBibliographicEntity("dd"));
        List<ItemChangeLogEntity> itemChangeLogEntityList = new ArrayList<>();
        itemChangeLogEntityList.add(getItemChangeLogEntity());
        BoundWithBibliographicEntityObject boundWithBibliographicEntityObject = getBoundWithBibliographicEntityObject();
        List<Future> futures = new ArrayList<>();
        Map<String,BibliographicEntity> fetchedOwnInstBibIdBibliographicEntityMap = new HashMap<>();
        fetchedOwnInstBibIdBibliographicEntityMap.put("34558",getBibliographicEntity("ddd"));
        Mockito.when(submitCollectionValidationService.validateIncomingItemHavingBibCountGreaterThanExistingItem(any(), anyList(),anyList())).thenReturn(Boolean.TRUE);
        Mockito.when(submitCollectionValidationService.getOwnInstBibIdBibliographicEntityMap(anyList())).thenReturn(fetchedOwnInstBibIdBibliographicEntityMap);
        try {
            ReflectionTestUtils.invokeMethod(submitCollectionDAOService,"addNewBibToExistingItem",submitCollectionReportInfoMap,processedBibIds,idMapToRemoveIndexList,bibIdMapToRemoveIndexList,processedBarcodeSetForDummyRecords,fetchedBarcodeItemEntityMap,updatedBibliographicEntityList,itemChangeLogEntityList,boundWithBibliographicEntityObject,executorService,futures);
        }catch (Exception e){}
    }

    @Test
    public void updateMatchingRecords(){
        BibliographicEntity fetchBibliographicEntity = getBibliographicEntity("1");
        fetchBibliographicEntity.setCatalogingStatus(ScsbCommonConstants.COMPLETE_STATUS);
        fetchBibliographicEntity.setId(1);
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1");
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        Map<String, ItemEntity> fetchedBarcodeItemEntityMap = new HashMap<>();
        Map<String, ItemEntity> incomingBarcodeItemEntityMap = new HashMap<>();
        SubmitCollectionMatchPointsCheckCallable submitCollectionMatchPointsCheckCallable = new SubmitCollectionMatchPointsCheckCallable();
        boolean isCGDProtected = false;
        List<Future> futures = new ArrayList<>();
        Mockito.when(applicationContext.getBean(SubmitCollectionMatchPointsCheckCallable.class)).thenReturn(submitCollectionMatchPointsCheckCallable);
        ReflectionTestUtils.invokeMethod(submitCollectionDAOService,"updateMatchingRecords",fetchBibliographicEntity,incomingBibliographicEntity,submitCollectionReportInfoMap,fetchedBarcodeItemEntityMap,incomingBarcodeItemEntityMap,isCGDProtected,executorService,futures);
    }

    private BoundWithBibliographicEntityObject getBoundWithBibliographicEntityObject(){
        BoundWithBibliographicEntityObject boundWithBibliographicEntityObject = new BoundWithBibliographicEntityObject();
        BibliographicEntity bibliographicEntity = getBibliographicEntity2("1577261074");
        boundWithBibliographicEntityObject.setBarcode("123456");
        boundWithBibliographicEntityObject.setBibliographicEntityList(Arrays.asList(bibliographicEntity));
        return boundWithBibliographicEntityObject;
    }
    private NonBoundWithBibliographicEntityObject getNonBoundWithBibliographicEntityObject(String OwningInstitutionBibId){
        NonBoundWithBibliographicEntityObject nonBoundWithBibliographicEntityObject = new NonBoundWithBibliographicEntityObject();
        BibliographicEntity bibliographicEntity = getBibliographicEntity(OwningInstitutionBibId);
        nonBoundWithBibliographicEntityObject.setBibliographicEntityList(Arrays.asList(bibliographicEntity));
        nonBoundWithBibliographicEntityObject.setOwningInstitutionBibId(OwningInstitutionBibId);
        return nonBoundWithBibliographicEntityObject;
    }

    private Map<String, List< SubmitCollectionReportInfo >> getSubmitCollectionReportInfoMap(String type){
        Map<String, List< SubmitCollectionReportInfo >> submitCollectionReportInfoMap = new HashMap<>();
        SubmitCollectionReportInfo submitCollectionReportInfo = new SubmitCollectionReportInfo();
        submitCollectionReportInfo.setMessage("SUCCESS");
        submitCollectionReportInfo.setCustomerCode("PA");
        submitCollectionReportInfo.setItemBarcode("123456");
        submitCollectionReportInfo.setOwningInstitution("PUL");
        List<SubmitCollectionReportInfo> submitCollectionReportInfoList=new ArrayList<>();
        submitCollectionReportInfoList.add(submitCollectionReportInfo);
        submitCollectionReportInfoMap.put(type,submitCollectionReportInfoList);
        return submitCollectionReportInfoMap;
    }
    private ItemChangeLogEntity getItemChangeLogEntity() {
        ItemChangeLogEntity itemChangeLogEntity = new ItemChangeLogEntity();
        itemChangeLogEntity.setNotes("test");
        itemChangeLogEntity.setOperationType("RECALL");
        itemChangeLogEntity.setRecordId(1);
        itemChangeLogEntity.setUpdatedBy("TEST");
        itemChangeLogEntity.setUpdatedDate(new Date());
        return itemChangeLogEntity;
    }

    private BibliographicEntity getBibliographicEntity(String OwningInstitutionBibId){

        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setId(4);
        bibliographicEntity.setContent("Test".getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionId(1);
        bibliographicEntity.setOwningInstitutionBibId(OwningInstitutionBibId);
        bibliographicEntity.setDeleted(false);
        bibliographicEntity.setCatalogingStatus("inComplete");

        List<ItemEntity> itemEntities = new ArrayList<>();
        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(1);
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setOwningInstitutionItemId("843617540");
        itemEntity.setOwningInstitutionId(1);
        itemEntity.setBarcode("123456");
        itemEntity.setCallNumber("x.12321");
        itemEntity.setCollectionGroupId(1);
        itemEntity.setCallNumberType("1");
        itemEntity.setCustomerCode("123");
        itemEntity.setCreatedDate(new Date());
        itemEntity.setCreatedBy("tst");
        itemEntity.setLastUpdatedBy("tst");
        itemEntity.setCatalogingStatus("Complete");
        itemEntity.setItemAvailabilityStatusId(1);
        itemEntity.setDeleted(false);
        itemEntities.add(itemEntity);

        List<HoldingsEntity> holdingsEntities = new ArrayList<>();
        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setCreatedBy("tst");
        holdingsEntity.setLastUpdatedBy("tst");
        holdingsEntity.setOwningInstitutionId(1);
        holdingsEntity.setOwningInstitutionHoldingsId("2435");
        holdingsEntity.setDeleted(false);
        holdingsEntities.add(holdingsEntity);


        itemEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        itemEntity.setBibliographicEntities(Arrays.asList(bibliographicEntity));
        itemEntity.setInstitutionEntity(getInstitutionEntity());
        holdingsEntity.setItemEntities(itemEntities);
        bibliographicEntity.setHoldingsEntities(holdingsEntities);
        bibliographicEntity.setItemEntities(itemEntities);

        return bibliographicEntity;
    }

    private BibliographicEntity getBibliographicEntityBoundwith(String OwningInstitutionBibId,String OwningInstitutionHoldingsId){

        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setId(4);
        bibliographicEntity.setContent("Test".getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionId(1);
        bibliographicEntity.setOwningInstitutionBibId(OwningInstitutionBibId);
        bibliographicEntity.setDeleted(false);
        bibliographicEntity.setCatalogingStatus("inComplete");

        BibliographicEntity bibliographicEntity1 =bibliographicEntity;
        bibliographicEntity.setOwningInstitutionBibId("64343");

        List<ItemEntity> itemEntities = new ArrayList<>();
        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(1);
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setOwningInstitutionItemId("843617540");
        itemEntity.setOwningInstitutionId(1);
        itemEntity.setBarcode("123456");
        itemEntity.setCallNumber("x.12321");
        itemEntity.setCollectionGroupId(1);
        itemEntity.setCallNumberType("1");
        itemEntity.setCustomerCode("123");
        itemEntity.setCreatedDate(new Date());
        itemEntity.setCreatedBy("tst");
        itemEntity.setLastUpdatedBy("tst");
        itemEntity.setCatalogingStatus("Complete");
        itemEntity.setItemAvailabilityStatusId(1);
        itemEntity.setDeleted(false);
        itemEntities.add(itemEntity);

        List<HoldingsEntity> holdingsEntities = new ArrayList<>();
        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setCreatedBy("tst");
        holdingsEntity.setLastUpdatedBy("tst");
        holdingsEntity.setOwningInstitutionId(1);
        holdingsEntity.setOwningInstitutionHoldingsId(OwningInstitutionHoldingsId);
        holdingsEntity.setDeleted(false);
        //holdingsEntity.setItemEntities(itemEntities);
        holdingsEntities.add(holdingsEntity);


        itemEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        itemEntity.setBibliographicEntities(Arrays.asList(bibliographicEntity,bibliographicEntity1));
        itemEntity.setInstitutionEntity(getInstitutionEntity());
        holdingsEntity.setItemEntities(itemEntities);
        bibliographicEntity.setHoldingsEntities(holdingsEntities);
        bibliographicEntity.setItemEntities(itemEntities);

        return bibliographicEntity;
    }

    private InstitutionEntity getInstitutionEntity() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setId(1);
        institutionEntity.setInstitutionName("PUL");
        institutionEntity.setInstitutionCode("PUL");
        return institutionEntity;
    }

    private BibliographicEntity getBibliographicEntity2(String OwningInstitutionBibId){

        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setId(123456);
        bibliographicEntity.setContent("Test".getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionId(1);
        bibliographicEntity.setOwningInstitutionBibId(OwningInstitutionBibId);
        bibliographicEntity.setDeleted(false);
        bibliographicEntity.setCatalogingStatus("inComplete");

        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setCreatedBy("tst");
        holdingsEntity.setLastUpdatedBy("tst");
        holdingsEntity.setOwningInstitutionId(1);
        holdingsEntity.setOwningInstitutionHoldingsId("34567");
        holdingsEntity.setDeleted(false);

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(1);
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setOwningInstitutionItemId("843617540");
        itemEntity.setOwningInstitutionId(1);
        itemEntity.setBarcode("123456");
        itemEntity.setCallNumber("x.12321");
        itemEntity.setCollectionGroupId(1);
        itemEntity.setCallNumberType("1");
        itemEntity.setCustomerCode("123");
        itemEntity.setCreatedDate(new Date());
        itemEntity.setCreatedBy("tst");
        itemEntity.setLastUpdatedBy("tst");
        itemEntity.setCatalogingStatus("Complete");
        itemEntity.setItemAvailabilityStatusId(1);
        itemEntity.setDeleted(false);
        itemEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        itemEntity.setBibliographicEntities(Arrays.asList(bibliographicEntity));
        itemEntity.setInstitutionEntity(getInstitutionEntity());
        List<BibliographicEntity> bibliographicEntities = new ArrayList<>();
        BibliographicEntity bibliographicEntity1 = getBibliographicEntity("1577261074");
        bibliographicEntity.setOwningInstitutionBibId("34558");
        BibliographicEntity bibliographicEntity2= getBibliographicEntity("1577261074");
        bibliographicEntity.setOwningInstitutionBibId("34558");
        bibliographicEntity1.setOwningInstitutionBibId("45568");
        bibliographicEntities.add(bibliographicEntity1);
        bibliographicEntities.add(bibliographicEntity2);
        itemEntity.setBibliographicEntities(bibliographicEntities);
        holdingsEntity.setItemEntities(Arrays.asList(itemEntity));
        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        bibliographicEntity.setItemEntities(Arrays.asList(itemEntity));

        return bibliographicEntity;
    }

    private Set<Integer> getIntegers() {
        Set<Integer> processedBibIds = new HashSet<>();
        processedBibIds.add(1);
        return processedBibIds;
    }

}
