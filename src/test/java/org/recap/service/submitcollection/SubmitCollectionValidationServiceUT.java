package org.recap.service.submitcollection;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;
import org.recap.ScsbConstants;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.report.SubmitCollectionReportInfo;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.service.common.SetupDataService;
import org.recap.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class SubmitCollectionValidationServiceUT extends BaseTestCaseUT {

    @InjectMocks
    SubmitCollectionValidationService submitCollectionValidationService;
    @Mock
    InstitutionDetailsRepository institutionDetailsRepository;
    @Mock
    SubmitCollectionHelperService submitCollectionHelperService;
    @Mock
    SetupDataService setupDataService;
    @Mock
    CommonUtil commonUtil;

    @Mock
    SubmitCollectionReportHelperService submitCollectionReportHelperService;

    @Mock
    List<BibliographicEntity> bibliographicEntities;

    @Mock
    BibliographicEntity fetchedBibliographicEntity;

    @Mock
    BibliographicEntity incomingBibliographicEntity;

    @Mock
    ItemEntity incomingItemEntity;

    @Mock
    BibliographicEntity existingBibliographicEntity;
    @Mock
    BibliographicEntity incomingBibliographicEntity1;


    @Mock
    ItemEntity existingItemEntity;

    @Mock
    InstitutionEntity institutionEntity;

    @Mock
    HoldingsEntity incomingHoldingsEntity;

    @Mock
    HoldingsEntity incomingHoldingsEntity1;

    @Mock
    Map<Integer,String> itemStatusIdCodeMap;

    @Mock
    List<String> incomingBibsNotInExistingBibs;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(submitCollectionValidationService, "nonHoldingIdInstitution", "NYPL");
    }
    @Test
    public void validateInstitution(){
        String institutionCode = "PUL";
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setId(1);
        institutionEntity.setInstitutionCode("PUL");
        institutionEntity.setInstitutionName("PUL");
        Mockito.when(institutionDetailsRepository.findByInstitutionCode(institutionCode)).thenReturn(institutionEntity);
        boolean result = submitCollectionValidationService.validateInstitution(institutionCode);
        assertEquals(true,result);
    }
    //@Test
    public void validateIncomingEntities(){
        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos = new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo());
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionFailureList",Arrays.asList(getSubmitCollectionReportInfo()));
        BibliographicEntity fetchedBibliographicEntity = getBibliographicEntity();
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity();
        Map<String,Map<String,ItemEntity>> holdingsItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        itemEntityMap.put("1",getItemEntity());
        holdingsItemMap.put("1",itemEntityMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(fetchedBibliographicEntity)).thenReturn(holdingsItemMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(incomingBibliographicEntity)).thenReturn(holdingsItemMap);
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn( getInstitutionEntityMap("PUL",5,1));
        boolean result = submitCollectionValidationService.validateIncomingEntities(submitCollectionReportInfoMap,fetchedBibliographicEntity,incomingBibliographicEntity);
        assertTrue(result);
    }

    @Test
    public void validateIncomingEntitiesIdMismatch(){
        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(itemStatusIdCodeMap);
        Mockito.when(itemStatusIdCodeMap.get(1)).thenReturn("NYPL");
        Mockito.when(incomingBibliographicEntity.getOwningInstitutionId()).thenReturn(1);
        List<ItemEntity> existingItemEntityList=new ArrayList<>();
        existingItemEntityList.add(existingItemEntity);
        Mockito.when(existingItemEntity.getOwningInstitutionItemId()).thenReturn("7");
        Mockito.when(incomingBibliographicEntity.getItemEntities()).thenReturn(existingItemEntityList);
        Map<String,Map<String,ItemEntity>> incomingHoldingItemMap=new HashMap<>();
        Map<String,ItemEntity> incomingItemEntityList=new HashMap<>();
        incomingItemEntityList.put("1",incomingItemEntity);
        incomingHoldingItemMap.put("1",incomingItemEntityList);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(incomingBibliographicEntity)).thenReturn(incomingHoldingItemMap);
        boolean result = submitCollectionValidationService.validateIncomingEntities(submitCollectionReportInfoMap,fetchedBibliographicEntity,incomingBibliographicEntity);
        assertFalse(result);
        Mockito.when(bibliographicEntities.size()).thenReturn(2);
        Mockito.when(incomingItemEntity.getBibliographicEntities()).thenReturn(bibliographicEntities);
        boolean isExistingBoundWithItem=submitCollectionValidationService.isExistingBoundWithItem(incomingItemEntity);
        assertTrue(isExistingBoundWithItem);
    }
    //@Test
    public void validateIncomingEntitiesForNYPL(){
        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos = new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo());
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionFailureList",Arrays.asList(getSubmitCollectionReportInfo()));
        BibliographicEntity fetchedBibliographicEntity = getBibliographicEntity();
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity();
        Map<String,Map<String,ItemEntity>> holdingsItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        itemEntityMap.put("1",getItemEntity());
        holdingsItemMap.put("1",itemEntityMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(fetchedBibliographicEntity)).thenReturn(holdingsItemMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(incomingBibliographicEntity)).thenReturn(holdingsItemMap);
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(getInstitutionEntityMap("NYPL",5,1));
        boolean result = submitCollectionValidationService.validateIncomingEntities(submitCollectionReportInfoMap,fetchedBibliographicEntity,incomingBibliographicEntity);
        assertTrue(result);
    }

    @Test
    public void validateIncomingEntitiesWithoutFetchedHoldingsItemId(){
        Map<String,Map<String,ItemEntity>> incomingHoldingItemMap=new HashMap<>();
        Map<String,ItemEntity> itemEntityMap=new HashMap<>();
        itemEntityMap.put("1",incomingItemEntity);
        incomingHoldingItemMap.put("1",itemEntityMap);
        Mockito.when(incomingItemEntity.getCollectionGroupId()).thenReturn(null);
        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap=new HashMap<>();
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(incomingBibliographicEntity)).thenReturn(incomingHoldingItemMap);
        boolean result = submitCollectionValidationService.validateIncomingEntities(submitCollectionReportInfoMap,fetchedBibliographicEntity,incomingBibliographicEntity);
        assertFalse(result);
    }
    @Test
    public void validateIncomingEntitiesWithoutFetchedHoldingsItemIdAndCollectionGroupId(){
        Map<String,Map<String,ItemEntity>> incomingHoldingItemMap=new HashMap<>();
        Map<String,ItemEntity> itemEntityMap=new HashMap<>();
        itemEntityMap.put("1",incomingItemEntity);
        incomingHoldingItemMap.put("1",itemEntityMap);
        Mockito.when(incomingItemEntity.getCollectionGroupId()).thenReturn(1);
        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap=new HashMap<>();
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(incomingBibliographicEntity)).thenReturn(incomingHoldingItemMap);
        boolean result = submitCollectionValidationService.validateIncomingEntities(submitCollectionReportInfoMap,fetchedBibliographicEntity,incomingBibliographicEntity);
        assertFalse(result);
    }

    private Map getInstitutionEntityMap(String institution,int status,int instId) {
        Map institutionEntityMap = new HashMap();
        institutionEntityMap.put(status,"Available");
        institutionEntityMap.put(instId,institution);
        return institutionEntityMap;
    }

    @Test
    public void getOwningBibIdOwnInstHoldingsIdIfAnyHoldingMismatch(){
        List<BibliographicEntity> bibliographicEntityList = new ArrayList<>();
        bibliographicEntityList.add(getBibliographicEntity());
        List<String> holdingsIdUniqueList = new ArrayList<>();
        holdingsIdUniqueList.add("12345");
        Map<String,String> stringMap = submitCollectionValidationService.getOwningBibIdOwnInstHoldingsIdIfAnyHoldingMismatch(bibliographicEntityList,holdingsIdUniqueList);
        assertNotNull(stringMap);
    }
    @Test
    public  void validateIncomingItemHavingBibCountIsSameAsExistingItem(){
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos = new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo());
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionFailureList",Arrays.asList(getSubmitCollectionReportInfo()));
        List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList = new ArrayList<>();
        failureSubmitCollectionReportInfoList.add(getSubmitCollectionReportInfo());
        Map<String,Map<String,ItemEntity>> holdingsItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        itemEntityMap.put("1",getItemEntity());
        holdingsItemMap.put("1",itemEntityMap);
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(getInstitutionEntityMap("NYPL",5,1));
        Map<String, ItemEntity> fetchedBarcodeItemEntityMap = new HashMap<>();
        fetchedBarcodeItemEntityMap.put("123456",getItemEntity());
        List<BibliographicEntity> incomingBibliographicEntityList = new ArrayList<>();
        incomingBibliographicEntityList.add(getBibliographicEntity());
        boolean result = submitCollectionValidationService.validateIncomingItemHavingBibCountIsSameAsExistingItem(submitCollectionReportInfoMap,fetchedBarcodeItemEntityMap,incomingBibliographicEntityList);
        assertTrue(result);
    }
    @Test
    public  void validateIncomingItemHavingBibCountIsSameAsExistingItemDummy(){
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos = new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo());
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionFailureList",Arrays.asList(getSubmitCollectionReportInfo()));
        List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList = new ArrayList<>();
        failureSubmitCollectionReportInfoList.add(getSubmitCollectionReportInfo());
        Map institutionEntityMap = new HashMap();
        institutionEntityMap.put(5,"Available");
        institutionEntityMap.put(1,"NYPL");
        Map<String,Map<String,ItemEntity>> holdingsItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        itemEntityMap.put("1",getItemEntity());
        holdingsItemMap.put("1",itemEntityMap);
        Map<String, ItemEntity> fetchedBarcodeItemEntityMap = new HashMap<>();
        ItemEntity fetchItemEntity = getItemEntity();
        BibliographicEntity bibliographicEntity1 = new BibliographicEntity();
        bibliographicEntity1.setOwningInstitutionBibId("d");
        fetchItemEntity.setBibliographicEntities(Arrays.asList(bibliographicEntity1));
        fetchedBarcodeItemEntityMap.put("123456",fetchItemEntity);
        List<BibliographicEntity> incomingBibliographicEntityList = new ArrayList<>();
        BibliographicEntity bibliographicEntity = getBibliographicEntity();
        bibliographicEntity.setOwningInstitutionBibId("897546");
        incomingBibliographicEntityList.add(bibliographicEntity);
        ItemEntity incomingItemEntity = getItemEntity();
        Mockito.when(submitCollectionReportHelperService.isBarcodeAlreadyAdded(incomingItemEntity.getBarcode(),submitCollectionReportInfoMap)).thenReturn(false);
        Mockito.when(submitCollectionReportHelperService.isBarcodeAlreadyAdded(incomingItemEntity.getBarcode(),submitCollectionReportInfoMap)).thenReturn(false);
        boolean result = submitCollectionValidationService.validateIncomingItemHavingBibCountIsSameAsExistingItem(submitCollectionReportInfoMap,fetchedBarcodeItemEntityMap,incomingBibliographicEntityList);
        assertTrue(result);
    }

    @Test
    public  void validateIncomingItemHavingBibCountIsSameAsExistingItemWithMatching(){
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos = new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo());
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionFailureList",Arrays.asList(getSubmitCollectionReportInfo()));
        List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList = new ArrayList<>();
        failureSubmitCollectionReportInfoList.add(getSubmitCollectionReportInfo());
        Map institutionEntityMap = new HashMap();
        institutionEntityMap.put(5,"Available");
        institutionEntityMap.put(1,"NYPL");
        Map<String,Map<String,ItemEntity>> holdingsItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        itemEntityMap.put("1",getItemEntity());
        holdingsItemMap.put("1",itemEntityMap);
        Map<String, ItemEntity> fetchedBarcodeItemEntityMap = new HashMap<>();
        fetchedBarcodeItemEntityMap.put("123456",getItemEntity());
        List<BibliographicEntity> incomingBibliographicEntityList = new ArrayList<>();
        BibliographicEntity bibliographicEntity = getBibliographicEntity();
        bibliographicEntity.setOwningInstitutionBibId("897546");
        incomingBibliographicEntityList.add(bibliographicEntity);
        ItemEntity incomingItemEntity = getItemEntity();
        Mockito.when(submitCollectionReportHelperService.isBarcodeAlreadyAdded(incomingItemEntity.getBarcode(),submitCollectionReportInfoMap)).thenReturn(false);
        Mockito.when(submitCollectionReportHelperService.isBarcodeAlreadyAdded(incomingItemEntity.getBarcode(),submitCollectionReportInfoMap)).thenReturn(false);
        boolean result = submitCollectionValidationService.validateIncomingItemHavingBibCountIsSameAsExistingItem(submitCollectionReportInfoMap,fetchedBarcodeItemEntityMap,incomingBibliographicEntityList);
        assertFalse(result);
    }
    @Test
    public void validateIncomingItemHavingBibCountGreaterThanExistingItem(){
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos = new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo());
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionFailureList",Arrays.asList(getSubmitCollectionReportInfo()));
        List<BibliographicEntity> incomingBibliographicEntityList = new ArrayList<>();
        incomingBibliographicEntityList.add(getBibliographicEntity());
        List<BibliographicEntity> existingBibliographicEntityList = new ArrayList<>();
        existingBibliographicEntityList.add(getBibliographicEntity());
        boolean result = submitCollectionValidationService.validateIncomingItemHavingBibCountGreaterThanExistingItem(submitCollectionReportInfoMap,incomingBibliographicEntityList,existingBibliographicEntityList);
        assertTrue(result);
    }

    @Test
    public void validateIncomingItemHavingBibCountGreaterThanExistingItem1(){
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap=new HashMap<>();
        List<BibliographicEntity> incomingBibliographicEntityList=new ArrayList<>();
        incomingBibliographicEntityList.add(incomingBibliographicEntity);
        incomingBibliographicEntityList.add(incomingBibliographicEntity1);
        List<BibliographicEntity> existingBibliographicEntityList=new ArrayList<>();
        existingBibliographicEntityList.add(existingBibliographicEntity);
        List<ItemEntity> existingItemEntityList=new ArrayList<>();
        existingItemEntityList.add(existingItemEntity);
        Mockito.when(existingItemEntity.getInstitutionEntity()).thenReturn(institutionEntity);
        Mockito.when(institutionEntity.getInstitutionCode()).thenReturn("PUL");
        Mockito.when(existingBibliographicEntity.getItemEntities()).thenReturn(existingItemEntityList);
        Mockito.when(existingBibliographicEntity.getOwningInstitutionBibId()).thenReturn("1");
        Mockito.when(incomingBibliographicEntity.getOwningInstitutionBibId()).thenReturn("1");
        Mockito.when(incomingBibliographicEntity1.getOwningInstitutionBibId()).thenReturn("2");
        Mockito.when(incomingBibliographicEntity.getItemEntities()).thenReturn(existingItemEntityList);
        Mockito.when(incomingBibliographicEntity1.getItemEntities()).thenReturn(existingItemEntityList);
        List<HoldingsEntity> incomingHoldingsEntityList=new ArrayList<>();
        incomingHoldingsEntityList.add(incomingHoldingsEntity);
        List<HoldingsEntity> incomingHoldingsEntityList1=new ArrayList<>();
        incomingHoldingsEntityList1.add(incomingHoldingsEntity1);
        Mockito.when(incomingHoldingsEntity.getOwningInstitutionHoldingsId()).thenReturn("5");
        Mockito.when(incomingHoldingsEntity1.getOwningInstitutionHoldingsId()).thenReturn("6");
        Mockito.when(incomingBibliographicEntity.getHoldingsEntities()).thenReturn(incomingHoldingsEntityList);
        Mockito.when(incomingBibliographicEntity1.getHoldingsEntities()).thenReturn(incomingHoldingsEntityList1);
        Mockito.when(incomingHoldingsEntity.getOwningInstitutionHoldingsId()).thenReturn("2");
        boolean result = submitCollectionValidationService.validateIncomingItemHavingBibCountGreaterThanExistingItem(submitCollectionReportInfoMap,incomingBibliographicEntityList,existingBibliographicEntityList);
        assertFalse(result);
    }

    @Test
    public void validateIncomingItemHavingBibCountGreaterThanExistingItem2(){
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos = new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo());
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionFailureList",Arrays.asList(getSubmitCollectionReportInfo()));
        List<BibliographicEntity> incomingBibliographicEntityList = new ArrayList<>();
        incomingBibliographicEntityList.add(getBibliographicEntity());
        List<BibliographicEntity> existingBibliographicEntityList = new ArrayList<>();
        BibliographicEntity bibliographicEntity = getBibliographicEntity();
        bibliographicEntity.setOwningInstitutionBibId("67890");
        existingBibliographicEntityList.add(bibliographicEntity);
        boolean result = submitCollectionValidationService.validateIncomingItemHavingBibCountGreaterThanExistingItem(submitCollectionReportInfoMap,incomingBibliographicEntityList,existingBibliographicEntityList);
        assertFalse(result);
    }

    @Test
    public void validateIncomingItemHavingBibCountGreaterThanExistingItemDummy(){
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos = new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo());
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",Arrays.asList(getSubmitCollectionReportInfo()));
        submitCollectionReportInfoMap.put("submitCollectionFailureList",Arrays.asList(getSubmitCollectionReportInfo()));
        List<BibliographicEntity> incomingBibliographicEntityList = new ArrayList<>();
        BibliographicEntity bibliographicEntity=getBibliographicEntity();
        bibliographicEntity.setOwningInstitutionBibId("9");
        incomingBibliographicEntityList.add(bibliographicEntity);
        List<BibliographicEntity> existingBibliographicEntityList = new ArrayList<>();
        BibliographicEntity bibliographicEntity1=getBibliographicEntity();
        bibliographicEntity1.setOwningInstitutionBibId("d");
        existingBibliographicEntityList.add(bibliographicEntity1);
        boolean result = submitCollectionValidationService.validateIncomingItemHavingBibCountGreaterThanExistingItem(submitCollectionReportInfoMap,incomingBibliographicEntityList,existingBibliographicEntityList);
        assertTrue(result);
    }

    @Test
    public void validateIncomingItemHavingBibCountLesserThanExistingItem(){
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos = new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo());
        List<BibliographicEntity> incomingBibliographicEntityList = new ArrayList<>();
        incomingBibliographicEntityList.add(getBibliographicEntity());
        List<BibliographicEntity> existingBibliographicEntityList = new ArrayList<>();
        existingBibliographicEntityList.add(getBibliographicEntity());
        List<String> incomingBibsNotInExistingBibs = new ArrayList<>();
        incomingBibsNotInExistingBibs.add("23567");
        ItemEntity existingItemEntity = getItemEntity();
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getInstitutionEntityMap("PUL",1,2));
        boolean result = submitCollectionValidationService.validateIncomingItemHavingBibCountLesserThanExistingItem(submitCollectionReportInfoMap,incomingBibliographicEntityList,existingBibliographicEntityList,incomingBibsNotInExistingBibs,existingItemEntity);
        assertFalse(result);
    }

    @Test
    public void validateIncomingItemHavingBibCountLesserThanExistingItemForHoldings(){
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<BibliographicEntity> incomingBibliographicEntityList = new ArrayList<>();
        incomingBibliographicEntityList.add(incomingBibliographicEntity);
        incomingBibliographicEntityList.add(incomingBibliographicEntity1);
        Mockito.when(incomingBibliographicEntity1.getOwningInstitutionBibId()).thenReturn("2");
        List<BibliographicEntity> existingBibliographicEntityList = new ArrayList<>();
        existingBibliographicEntityList.add(existingBibliographicEntity);
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(itemStatusIdCodeMap);
        Mockito.when(itemStatusIdCodeMap.get(1)).thenReturn(ScsbConstants.ITEM_STATUS_AVAILABLE);
        Mockito.when(existingItemEntity.getItemAvailabilityStatusId()).thenReturn(1);
        Mockito.when(incomingItemEntity.getItemAvailabilityStatusId()).thenReturn(1);
        List<ItemEntity> existingItemEntityList=new ArrayList<>();
        existingItemEntityList.add(existingItemEntity);
        existingItemEntityList.add(incomingItemEntity);
        Mockito.when(existingItemEntity.getInstitutionEntity()).thenReturn(institutionEntity);
        Mockito.when(incomingItemEntity.getInstitutionEntity()).thenReturn(institutionEntity);
        Mockito.when(existingItemEntity.getOwningInstitutionItemId()).thenReturn("7");
        Mockito.when(incomingItemEntity.getOwningInstitutionItemId()).thenReturn("8");
        List<HoldingsEntity> incomingHoldingsEntityList=new ArrayList<>();
        incomingHoldingsEntityList.add(incomingHoldingsEntity);
        Mockito.when(incomingHoldingsEntity.getOwningInstitutionHoldingsId()).thenReturn("2");
        Mockito.when(incomingBibliographicEntity.getHoldingsEntities()).thenReturn(incomingHoldingsEntityList);
        Mockito.when(incomingBibliographicEntity.getOwningInstitutionBibId()).thenReturn("1");
        Mockito.when(institutionEntity.getInstitutionCode()).thenReturn("PUL");
        Mockito.when(incomingBibsNotInExistingBibs.isEmpty()).thenReturn(true);
        Mockito.when(incomingBibliographicEntity1.getItemEntities()).thenReturn(existingItemEntityList);
        List<HoldingsEntity> incomingHoldingsEntityList1=new ArrayList<>();
        incomingHoldingsEntityList1.add(incomingHoldingsEntity1);
        Mockito.when(existingBibliographicEntity.getItemEntities()).thenReturn(existingItemEntityList);
        Mockito.when(incomingBibliographicEntity.getItemEntities()).thenReturn(existingItemEntityList);
        Mockito.when(incomingBibliographicEntity1.getHoldingsEntities()).thenReturn(incomingHoldingsEntityList1);
        boolean result = submitCollectionValidationService.validateIncomingItemHavingBibCountLesserThanExistingItem(submitCollectionReportInfoMap,incomingBibliographicEntityList,existingBibliographicEntityList,incomingBibsNotInExistingBibs,existingItemEntity);
        assertFalse(result);
    }

    @Test
    public void validateIncomingItemHavingBibCountLesserThanExistingItemAvailable(){
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos = new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo());
        List<BibliographicEntity> incomingBibliographicEntityList = new ArrayList<>();
        incomingBibliographicEntityList.add(getBibliographicEntity());
        List<BibliographicEntity> existingBibliographicEntityList = new ArrayList<>();
        existingBibliographicEntityList.add(getBibliographicEntity());
        List<String> incomingBibsNotInExistingBibs = new ArrayList<>();
        incomingBibsNotInExistingBibs.add("23567");
        ItemEntity existingItemEntity = getItemEntity();
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getInstitutionEntityMap("PUL",2,1));
        boolean result = submitCollectionValidationService.validateIncomingItemHavingBibCountLesserThanExistingItem(submitCollectionReportInfoMap,incomingBibliographicEntityList,existingBibliographicEntityList,incomingBibsNotInExistingBibs,existingItemEntity);
        assertFalse(result);
    }
    @Test
    public void validateIncomingItemHavingBibCountLesserThanExistingItemWithoutIncomingBibsNotInExistingBibs(){
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos = new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo());
        List<BibliographicEntity> incomingBibliographicEntityList = new ArrayList<>();
        incomingBibliographicEntityList.add(getBibliographicEntity());
        List<BibliographicEntity> existingBibliographicEntityList = new ArrayList<>();
        existingBibliographicEntityList.add(getBibliographicEntity());
        List<String> incomingBibsNotInExistingBibs = new ArrayList<>();
        ItemEntity existingItemEntity = getItemEntity();
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getInstitutionEntityMap("PUL",1,2));
        boolean result = submitCollectionValidationService.validateIncomingItemHavingBibCountLesserThanExistingItem(submitCollectionReportInfoMap,incomingBibliographicEntityList,existingBibliographicEntityList,incomingBibsNotInExistingBibs,existingItemEntity);
        assertTrue(result);
    }
    private BibliographicEntity getBibliographicEntity(){
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setId(123456);
        bibliographicEntity.setContent("Test".getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionId(1);
        bibliographicEntity.setOwningInstitutionBibId("1577261074");
        bibliographicEntity.setDeleted(false);

        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setId(1);
        institutionEntity.setInstitutionName("PUL");
        institutionEntity.setInstitutionCode("PUL");

        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setCreatedBy("tst");
        holdingsEntity.setLastUpdatedBy("tst");
        holdingsEntity.setOwningInstitutionId(1);
        holdingsEntity.setOwningInstitutionHoldingsId("34567");
        holdingsEntity.setDeleted(false);

        ItemEntity itemEntity = new ItemEntity();
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
        itemEntity.setInstitutionEntity(institutionEntity);
        itemEntity.setBibliographicEntities(Arrays.asList(bibliographicEntity));
        itemEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));

        holdingsEntity.setItemEntities(Arrays.asList(itemEntity));
        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        bibliographicEntity.setItemEntities(Arrays.asList(itemEntity));

        return bibliographicEntity;
    }
    private SubmitCollectionReportInfo getSubmitCollectionReportInfo(){
        SubmitCollectionReportInfo submitCollectionReportInfo = new SubmitCollectionReportInfo();
        submitCollectionReportInfo.setOwningInstitution("PUL");
        submitCollectionReportInfo.setItemBarcode("123456");
        submitCollectionReportInfo.setCustomerCode("PA");
        submitCollectionReportInfo.setMessage("SUCCESS");
        return submitCollectionReportInfo;
    }

    private ItemEntity getItemEntity(){
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setId(1);
        institutionEntity.setInstitutionName("PUL");
        institutionEntity.setInstitutionCode("PUL");
        ItemEntity itemEntity = new ItemEntity();
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
        itemEntity.setCatalogingStatus("Incomplete");
        itemEntity.setItemAvailabilityStatusId(1);
        itemEntity.setUseRestrictions("restrictions");
        itemEntity.setDeleted(false);
        itemEntity.setInstitutionEntity(institutionEntity);
        itemEntity.setBibliographicEntities(Arrays.asList(getBibliographicEntity()));
        return itemEntity;
    }
}
