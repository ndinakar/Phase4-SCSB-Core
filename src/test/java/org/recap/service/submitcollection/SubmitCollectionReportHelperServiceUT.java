package org.recap.service.submitcollection;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
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
import org.recap.repository.jpa.ItemDetailsRepository;
import org.recap.service.common.RepositoryService;
import org.recap.service.common.SetupDataService;
import org.recap.util.CommonUtil;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import static org.junit.Assert.assertNotNull;


/**
 * Created by premkb on 18/6/17.
 */

public class SubmitCollectionReportHelperServiceUT extends BaseTestCaseUT {

    @InjectMocks
    private SubmitCollectionReportHelperService submitCollectionReportHelperService;

    @Mock
    private ItemDetailsRepository itemDetailsRepository;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private SetupDataService setupDataService;

    @Mock
    private SubmitCollectionHelperService submitCollectionHelperService;

    @Mock
    private CommonUtil commonUtil;

    @Mock
    BibliographicEntity fetchedBibliographicEntity;

    @Mock
    BibliographicEntity incomingBibliographicEntity;

    @Mock
    Map<String,Map<String,ItemEntity>> fetchedHoldingItemMap;

    @Mock
    ItemEntity fetchedItemEntity;

    @Mock
    ItemEntity incomingItemEntity;

    @Mock
    HoldingsEntity fetchedHoldingsEntity;

    @Mock
    SubmitCollectionReportInfo submitCollectionReportInfo;


    @Before
    public void setUp() {
        ReflectionTestUtils.setField(submitCollectionReportHelperService, "nonHoldingIdInstitution", "NYPL");
    }

    @Test
    public  void setSubmitCollectionExceptionReportInfo(){
        List<ItemEntity> itemEntityList = new ArrayList<>();
        itemEntityList.add(getItemEntity("123456"));
        List<SubmitCollectionReportInfo> submitCollectionExceptionInfos = new ArrayList<>();
        submitCollectionExceptionInfos.add(getSubmitCollectionReportInfo("123456"));
        String message = "SUBMIT COLLECTION";
        submitCollectionReportHelperService.setSubmitCollectionExceptionReportInfo(itemEntityList,submitCollectionExceptionInfos,message);
    }

    @Test
    public void setSubmitCollectionReportInfoForOwningInstitutionBibIdMismatch(){
        BibliographicEntity fetchedBibliographicEntity = getBibliographicEntity("1577261074");
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1577261074");
        incomingBibliographicEntity.setOwningInstitutionBibId("2345");
        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionExceptionInfos = new ArrayList<>();
        submitCollectionExceptionInfos.add(getSubmitCollectionReportInfo("23"));
        submitCollectionReportInfoMap.put("submitCollectionFailureList",submitCollectionExceptionInfos);
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(getInstitutionEntityMap("PUL"));
        submitCollectionReportHelperService.setSubmitCollectionReportInfoForOwningInstitutionBibIdMismatch(fetchedBibliographicEntity,incomingBibliographicEntity,submitCollectionReportInfoMap);
        assertNotNull(fetchedBibliographicEntity);
    }

    @Test
    public  void setSubmitCollectionReportInfoForOwningInstitutionBibIdMismatchForBoundWith(){
        BibliographicEntity fetchedBibliographicEntity = getBibliographicEntity("1577261074");
        List<String> notMatchedIncomingOwnInstBibId = new ArrayList<>();
        notMatchedIncomingOwnInstBibId.add("345677");
        List<String> notMatchedFetchedOwnInstBibId = new ArrayList<>();
        notMatchedFetchedOwnInstBibId.add("123456");
        ItemEntity incomingItemEntity = getItemEntity("123456");
        ItemEntity fetchedItemEntity = getItemEntity("123456");
        fetchedItemEntity.setHoldingsEntities(Arrays.asList(getHoldingsEntity()));
        List<SubmitCollectionReportInfo> submitCollectionExceptionInfos = new ArrayList<>();
        submitCollectionExceptionInfos.add(getSubmitCollectionReportInfo("123456"));
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(getInstitutionEntityMap("PUL"));
        submitCollectionReportHelperService.setSubmitCollectionReportInfoForOwningInstitutionBibIdMismatchForBoundWith(notMatchedIncomingOwnInstBibId,notMatchedFetchedOwnInstBibId,incomingItemEntity,fetchedItemEntity,submitCollectionExceptionInfos);
    }

    @Test
    public void setSubmitCollectionReportInfoForInvalidDummyRecordBasedOnOwnInstItemId(){
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1577261074");
        List<SubmitCollectionReportInfo> submitCollectionReportInfoList = new ArrayList<>();
        submitCollectionReportInfoList.add(getSubmitCollectionReportInfo("123456"));
        List<ItemEntity> fetchedCompleteItem = new ArrayList<>();
        ItemEntity itemEntity =  getItemEntity("123456");
        itemEntity.setHoldingsEntities(Arrays.asList(getHoldingsEntity()));
        fetchedCompleteItem.add(itemEntity);
        submitCollectionReportHelperService.setSubmitCollectionReportInfoForInvalidDummyRecordBasedOnOwnInstItemId(incomingBibliographicEntity,submitCollectionReportInfoList,fetchedCompleteItem);
    }

    @Test
    public void setSubmitCollectionReportInfoForInvalidXml(){
        String institutionCode = "PUL";
        List<SubmitCollectionReportInfo> submitCollectionExceptionInfos = new ArrayList<>();
        submitCollectionExceptionInfos.add(getSubmitCollectionReportInfo("123456"));
        String message = "SUBMIT COLLECTION";
        submitCollectionReportHelperService.setSubmitCollectionReportInfoForInvalidXml(institutionCode,submitCollectionExceptionInfos,message);
    }

    @Test
    public void setFailureSubmitCollectionReportInfoList(){
        List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList = new ArrayList<>();
        failureSubmitCollectionReportInfoList.add(getSubmitCollectionReportInfo("123456"));
        Map<String, ItemEntity> fetchedOwningItemIdEntityMap=new HashMap<>();
        ItemEntity itemEntity=getItemEntity("123456");
        fetchedOwningItemIdEntityMap.put("123456",itemEntity);
        ItemEntity incomingItemEntity =  getItemEntity("123456");
        ItemEntity fetchedItemEntity =  getItemEntity("123456");
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"existingBibid",", existing owning institution bib id ");
        submitCollectionReportHelperService.setFailureSubmitCollectionReportInfoList(failureSubmitCollectionReportInfoList,"PUL",fetchedOwningItemIdEntityMap,"10",incomingItemEntity,fetchedItemEntity);
        assertNotNull(failureSubmitCollectionReportInfoList);
    }

    @Ignore
    public void buildSubmitCollectionReportInfo(){
        Map institutionEntityMap = new HashMap();
        institutionEntityMap.put(5,"Available");
        institutionEntityMap.put(1,"PUL");
        institutionEntityMap.put(2,"CUL");
        institutionEntityMap.put(3,"NYPL");
        institutionEntityMap.put(4,"NYPL");
        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",Arrays.asList(getSubmitCollectionReportInfo("123456")));
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",Arrays.asList(getSubmitCollectionReportInfo("123456")));
        submitCollectionReportInfoMap.put("submitCollectionFailureList",Arrays.asList(getSubmitCollectionReportInfo("123456")));
        BibliographicEntity fetchedBibliographicEntity = getBibliographicEntity("1577261074");
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1577261074");
        ItemEntity itemEntity = getItemEntity("1");
        itemEntity.setCatalogingStatus("Complete");
        Map<String,Map<String,ItemEntity>> holdingsItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        itemEntityMap.put("1",itemEntity);
        holdingsItemMap.put("1",itemEntityMap);
        ItemEntity incomingItemEntity = getItemEntity("2");
        incomingItemEntity.setCatalogingStatus("Complete");
        Map<String,Map<String,ItemEntity>> holdingsItemMap1 = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap1 = new HashMap<>();
        itemEntityMap1.put("1",incomingItemEntity);
        holdingsItemMap1.put("1",itemEntityMap1);
        List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList = new ArrayList<>();
        failureSubmitCollectionReportInfoList.add(getSubmitCollectionReportInfo("123456"));
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(fetchedBibliographicEntity)).thenReturn(holdingsItemMap1);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(incomingBibliographicEntity)).thenReturn(holdingsItemMap);
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(institutionEntityMap);
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getItemStatusIdCodeMap());
        Map<String,List<SubmitCollectionReportInfo>> listMap = submitCollectionReportHelperService.buildSubmitCollectionReportInfo(submitCollectionReportInfoMap,fetchedBibliographicEntity,incomingBibliographicEntity);
        assertNotNull(listMap);
    }

    //@Test
    public void buildSubmitCollectionReportInfoForIncompleteItemEntity(){
        Map institutionEntityMap = new HashMap();
        institutionEntityMap.put(5,"Available");
        institutionEntityMap.put(1,"PUL");
        institutionEntityMap.put(2,"CUL");
        institutionEntityMap.put(3,"NYPL");
        institutionEntityMap.put(4,"NYPL");
        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",Arrays.asList(getSubmitCollectionReportInfo("123456")));
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",Arrays.asList(getSubmitCollectionReportInfo("123456")));
        submitCollectionReportInfoMap.put("submitCollectionFailureList",Arrays.asList(getSubmitCollectionReportInfo("123456")));
        BibliographicEntity fetchedBibliographicEntity = getBibliographicEntity("1577261074");
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1577261074");
        ItemEntity itemEntity = getItemEntity("123456");
        Map<String,Map<String,ItemEntity>> holdingsItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        itemEntityMap.put("1",itemEntity);
        holdingsItemMap.put("1",itemEntityMap);
        List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList = new ArrayList<>();
        failureSubmitCollectionReportInfoList.add(getSubmitCollectionReportInfo("123456"));
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(fetchedBibliographicEntity)).thenReturn(holdingsItemMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(incomingBibliographicEntity)).thenReturn(holdingsItemMap);
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(institutionEntityMap);
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(institutionEntityMap);
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(institutionEntityMap);
        Map<String,List<SubmitCollectionReportInfo>> listMap = submitCollectionReportHelperService.buildSubmitCollectionReportInfo(submitCollectionReportInfoMap,fetchedBibliographicEntity,incomingBibliographicEntity);
        assertNotNull(listMap);
    }

    @Test
    public void setFailureSubmitCollectionReportInfoListFailure(){
        Map institutionEntityMap = new HashMap();
        institutionEntityMap.put(5,"Available");
        institutionEntityMap.put(1,"PUL");
        institutionEntityMap.put(2,"CUL");
        institutionEntityMap.put(3,"NYPL");
        institutionEntityMap.put(4,"NYPL");
        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos=new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo("123456547457"));
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",Arrays.asList(getSubmitCollectionReportInfo("123456")));
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",Arrays.asList(getSubmitCollectionReportInfo("123456")));
        submitCollectionReportInfoMap.put("submitCollectionFailureList",submitCollectionReportInfos);
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1577261074");
        ItemEntity itemEntity = getItemEntity("123456");
        Map<String,Map<String,ItemEntity>> holdingsItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        itemEntityMap.put("1",itemEntity);
        holdingsItemMap.put("1",itemEntityMap);
        List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList = new ArrayList<>();
        failureSubmitCollectionReportInfoList.add(getSubmitCollectionReportInfo("123456"));
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(fetchedBibliographicEntity)).thenReturn(fetchedHoldingItemMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(incomingBibliographicEntity)).thenReturn(holdingsItemMap);
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(institutionEntityMap);
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(institutionEntityMap);
        Map<String, ItemEntity> fetchedOwningItemIdEntityMap=new HashMap<>();
        ItemEntity itemEntity1=getItemEntity("1234561");
        fetchedOwningItemIdEntityMap.put("123456",itemEntity1);
        Mockito.when(fetchedHoldingItemMap.get(Mockito.anyString())).thenReturn(fetchedOwningItemIdEntityMap);
        Mockito.when(itemDetailsRepository.findByBarcode(Mockito.anyString())).thenReturn(Arrays.asList(itemEntity));
        Map<String,List<SubmitCollectionReportInfo>> listMap = submitCollectionReportHelperService.buildSubmitCollectionReportInfo(submitCollectionReportInfoMap,fetchedBibliographicEntity,incomingBibliographicEntity);
        assertNotNull(listMap);
    }

    //@Test
    public void buildSubmitCollectionReportInfoForNYPL(){

        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",Arrays.asList(getSubmitCollectionReportInfo("123456")));
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",Arrays.asList(getSubmitCollectionReportInfo("123456")));
        submitCollectionReportInfoMap.put("submitCollectionFailureList",Arrays.asList(getSubmitCollectionReportInfo("123456")));
        BibliographicEntity fetchedBibliographicEntity = getBibliographicEntity("1577261074");
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1577261074");
        ItemEntity itemEntity = getItemEntity("123456");
        Map<String,Map<String,ItemEntity>> holdingsItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        itemEntityMap.put("1",itemEntity);
        holdingsItemMap.put("1",itemEntityMap);
        List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList = new ArrayList<>();
        failureSubmitCollectionReportInfoList.add(getSubmitCollectionReportInfo("123456"));
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(fetchedBibliographicEntity)).thenReturn(holdingsItemMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(incomingBibliographicEntity)).thenReturn(holdingsItemMap);
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(getInstitutionEntityMap("NYPL"));
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(getInstitutionEntityMap("NYPL"));
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getInstitutionEntityMap("NYPL"));
        Map<String,List<SubmitCollectionReportInfo>> listMap = submitCollectionReportHelperService.buildSubmitCollectionReportInfo(submitCollectionReportInfoMap,fetchedBibliographicEntity,incomingBibliographicEntity);
        assertNotNull(listMap);
    }

    //@Test
    public void setReportInfoForMatchedRecord(){
        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos=new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo("123456547457"));
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",Arrays.asList(getSubmitCollectionReportInfo("123456")));
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",submitCollectionReportInfos);
        submitCollectionReportInfoMap.put("submitCollectionFailureList",Arrays.asList(getSubmitCollectionReportInfo("123456")));
        BibliographicEntity fetchedBibliographicEntity = getBibliographicEntity("1577261074");
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1577261074");
        ItemEntity itemEntity = getItemEntity("123456");
        Map<String,Map<String,ItemEntity>> holdingsItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        itemEntityMap.put("1",itemEntity);
        holdingsItemMap.put("1",itemEntityMap);
        ItemEntity incomingItemEntity = itemEntity;
        List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList = new ArrayList<>();
        failureSubmitCollectionReportInfoList.add(getSubmitCollectionReportInfo("123456"));
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(fetchedBibliographicEntity)).thenReturn(holdingsItemMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(incomingBibliographicEntity)).thenReturn(holdingsItemMap);
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(getInstitutionEntityMap("NYPL"));
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(getInstitutionEntityMap("NYPL"));
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getInstitutionEntityMap("NYPL"));
        Map<String,List<SubmitCollectionReportInfo>> listMap = submitCollectionReportHelperService.buildSubmitCollectionReportInfo(submitCollectionReportInfoMap,fetchedBibliographicEntity,incomingBibliographicEntity);
        assertNotNull(listMap);
    }

    private Map getInstitutionEntityMap(String institution) {
        Map institutionEntityMap = new HashMap();
        institutionEntityMap.put(5,"Available");
        institutionEntityMap.put(1,institution);
        return institutionEntityMap;
    }

    private Map getItemStatusIdCodeMap() {
        Map<Integer,String> itemStatusIdCodeMap = new HashMap();
        itemStatusIdCodeMap.put(1, ScsbConstants.ITEM_STATUS_AVAILABLE);
        return itemStatusIdCodeMap;
    }

    @Test
    public void buildSubmitCollectionReportInfoWhenNoGroupIdAndAddFailures(){
        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos=new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo("123456"));
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",submitCollectionReportInfos);
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",submitCollectionReportInfos);
        submitCollectionReportInfoMap.put("submitCollectionFailureList",submitCollectionReportInfos);
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1577261074");
        ItemEntity itemEntity = getItemEntity("123456");
        itemEntity.setCollectionGroupId(null);
        Map<String,Map<String,ItemEntity>> holdingsItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        itemEntityMap.put("1",itemEntity);
        holdingsItemMap.put("1",itemEntityMap);
        List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList = new ArrayList<>();
        failureSubmitCollectionReportInfoList.add(getSubmitCollectionReportInfo("123456"));
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(fetchedBibliographicEntity)).thenReturn(fetchedHoldingItemMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(incomingBibliographicEntity)).thenReturn(holdingsItemMap);
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(getInstitutionEntityMap("PUL"));
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getItemStatusIdCodeMap());
        Mockito.when(fetchedBibliographicEntity.getOwningInstitutionId()).thenReturn(1);
        Mockito.doCallRealMethod().when(commonUtil).buildSubmitCollectionReportInfoWhenNoGroupIdAndAddFailures(Mockito.any(),Mockito.anyList(),Mockito.anyString(),Mockito.any());
        Map<String,List<SubmitCollectionReportInfo>> listMap = submitCollectionReportHelperService.buildSubmitCollectionReportInfo(submitCollectionReportInfoMap,fetchedBibliographicEntity,incomingBibliographicEntity);
        assertNotNull(listMap);
    }

    @Test
    public void buildSubmitCollectionReportInfoAndAddFailures(){
        Map<String,List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos=new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo("123456"));
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",submitCollectionReportInfos);
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",submitCollectionReportInfos);
        submitCollectionReportInfoMap.put("submitCollectionFailureList",submitCollectionReportInfos);
        BibliographicEntity incomingBibliographicEntity = getBibliographicEntity("1577261074");
        ItemEntity itemEntity = getItemEntity("123456");
        Map<String,Map<String,ItemEntity>> holdingsItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        itemEntityMap.put("1",itemEntity);
        holdingsItemMap.put("1",itemEntityMap);
        List<SubmitCollectionReportInfo> failureSubmitCollectionReportInfoList = new ArrayList<>();
        failureSubmitCollectionReportInfoList.add(getSubmitCollectionReportInfo("123456"));
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(fetchedBibliographicEntity)).thenReturn(fetchedHoldingItemMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(incomingBibliographicEntity)).thenReturn(holdingsItemMap);
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(getInstitutionEntityMap("PUL"));
        Mockito.when(setupDataService.getItemStatusIdCodeMap()).thenReturn(getItemStatusIdCodeMap());
        Mockito.when(fetchedBibliographicEntity.getOwningInstitutionId()).thenReturn(1);
        List<ItemEntity> fetchedItemEntityList=new ArrayList<>();
        fetchedItemEntityList.add(fetchedItemEntity);
        Mockito.when(fetchedBibliographicEntity.getItemEntities()).thenReturn(fetchedItemEntityList);
        Mockito.when(fetchedItemEntity.getOwningInstitutionItemId()).thenReturn("843617540");
        List<HoldingsEntity> fetchedHoldingsEntityList=new ArrayList<>();
        fetchedHoldingsEntityList.add(fetchedHoldingsEntity);
        Mockito.when(fetchedItemEntity.getHoldingsEntities()).thenReturn(fetchedHoldingsEntityList);
        Mockito.when(fetchedHoldingsEntity.getOwningInstitutionHoldingsId()).thenReturn("34567");
        Mockito.doCallRealMethod().when(commonUtil).buildSubmitCollectionReportInfoAndAddFailures(Mockito.any(),Mockito.anyList(),Mockito.anyString(),Mockito.any(),Mockito.any());
        Map<String,List<SubmitCollectionReportInfo>> listMap = submitCollectionReportHelperService.buildSubmitCollectionReportInfo(submitCollectionReportInfoMap,fetchedBibliographicEntity,incomingBibliographicEntity);
        assertNotNull(listMap);
    }

    @Test
    public void getIncomingItemIsComplete(){
        List<ItemEntity> itemEntityList = new ArrayList<>();
        itemEntityList.add(getItemEntity("123456"));
        List<String> barcodes = new ArrayList<>();
        barcodes.add("123456");
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(repositoryService.getItemDetailsRepository().findByBarcodeInAndComplete(barcodes)).thenReturn(itemEntityList);
        List<ItemEntity> itemEntities = submitCollectionReportHelperService.getIncomingItemIsComplete(itemEntityList);
        assertNotNull(itemEntities);
    }
    @Test
    public void getItemBasedOnOwningInstitutionItemIdAndOwningInstitutionId(){
        List<ItemEntity> itemEntityList = new ArrayList<>();
        itemEntityList.add(getItemEntity("123456"));
        List<String> owningInstitutionItemIdList = new ArrayList<>();
        owningInstitutionItemIdList.add("843617540");
        Mockito.when(repositoryService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(repositoryService.getItemDetailsRepository().findByOwningInstitutionItemIdInAndOwningInstitutionId(owningInstitutionItemIdList,itemEntityList.get(0).getOwningInstitutionId())).thenReturn(itemEntityList);
        List<ItemEntity> itemEntities = submitCollectionReportHelperService.getItemBasedOnOwningInstitutionItemIdAndOwningInstitutionId(itemEntityList);
        assertNotNull(itemEntities);
    }
    @Test
    public void setSubmitCollectionFailureReportForUnexpectedException(){
        BibliographicEntity bibliographicEntity = getBibliographicEntity("1577261074");
        List<SubmitCollectionReportInfo> submitCollectionReportInfoList = new ArrayList<>();
        submitCollectionReportInfoList.add(getSubmitCollectionReportInfo("123456"));
        String message = "Message";
        InstitutionEntity institutionEntity = getBibliographicEntity("1577261074").getInstitutionEntity();
        submitCollectionReportHelperService.setSubmitCollectionFailureReportForUnexpectedException(bibliographicEntity,submitCollectionReportInfoList,message,institutionEntity);
    }
    @Test
    public void setSubmitCollectionFailureReportForUnexpectedExceptionWithoutBibliographicEntity(){
        BibliographicEntity bibliographicEntity = null;
        List<SubmitCollectionReportInfo> submitCollectionReportInfoList = new ArrayList<>();
        submitCollectionReportInfoList.add(getSubmitCollectionReportInfo("123456"));
        String message = "Message";
        InstitutionEntity institutionEntity = getBibliographicEntity("1577261074").getInstitutionEntity();
        submitCollectionReportHelperService.setSubmitCollectionFailureReportForUnexpectedException(bibliographicEntity,submitCollectionReportInfoList,message,institutionEntity);
    }

    @Test
    public void updateSuccessMessageForAdditionalBibsAdded(){
        List<BibliographicEntity> incomingBibliographicEntityList = new ArrayList<>();
        incomingBibliographicEntityList.add(getBibliographicEntity("1577261074"));
        List<BibliographicEntity> existingBibliographicEntityList = new ArrayList<>();
        existingBibliographicEntityList.add(getBibliographicEntity("1577261074"));
        ItemEntity existingItemEntity = getItemEntity("123456");
        String barcode = "123456";
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos = new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo("123456"));
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",submitCollectionReportInfos);
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",submitCollectionReportInfos);
        boolean isItemAvailable = true;
        String result = submitCollectionReportHelperService.updateSuccessMessageForAdditionalBibsAdded(incomingBibliographicEntityList,existingBibliographicEntityList,existingItemEntity,barcode,submitCollectionReportInfoMap,isItemAvailable);
        assertNotNull(result);
    }
    @Test
    public void updateSuccessMessageForAdditionalBibsAddedWithoutSuccessAndRejectMessage(){
        List<BibliographicEntity> incomingBibliographicEntityList = new ArrayList<>();
        incomingBibliographicEntityList.add(getBibliographicEntity("1577261074"));
        List<BibliographicEntity> existingBibliographicEntityList = new ArrayList<>();
        existingBibliographicEntityList.add(getBibliographicEntity("1577261074"));
        ItemEntity existingItemEntity = getItemEntity("123456");
        String barcode = "67890";
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos = new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo("123456"));
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",submitCollectionReportInfos);
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",submitCollectionReportInfos);
        boolean isItemAvailable = true;
        String result = submitCollectionReportHelperService.updateSuccessMessageForAdditionalBibsAdded(incomingBibliographicEntityList,existingBibliographicEntityList,existingItemEntity,barcode,submitCollectionReportInfoMap,isItemAvailable);
        assertNotNull(result);
    }

    @Test
    public void updateSuccessMessageForAdditionalBibsAddedWithoutSuccessAndRejectMessageUnavailable(){
        List<BibliographicEntity> incomingBibliographicEntityList = new ArrayList<>();
        incomingBibliographicEntityList.add(getBibliographicEntity("1577261074"));
        List<BibliographicEntity> existingBibliographicEntityList = new ArrayList<>();
        existingBibliographicEntityList.add(getBibliographicEntity("1577261074"));
        ItemEntity existingItemEntity = getItemEntity("123456");
        String barcode = "67890";
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos = new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo("123456"));
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",submitCollectionReportInfos);
        submitCollectionReportInfoMap.put("submitCollectionRejectionList",submitCollectionReportInfos);
        boolean isItemAvailable = false;
        String result = submitCollectionReportHelperService.updateSuccessMessageForAdditionalBibsAdded(incomingBibliographicEntityList,existingBibliographicEntityList,existingItemEntity,barcode,submitCollectionReportInfoMap,isItemAvailable);
        assertNotNull(result);
    }

    @Test
    public void updateSuccessMessageForRemovedBibs(){
        List<BibliographicEntity> incomingBibliographicEntityList = new ArrayList<>();
        incomingBibliographicEntityList.add(getBibliographicEntity("1577261074"));
        List<BibliographicEntity> existingBibliographicEntityList = new ArrayList<>();
        existingBibliographicEntityList.add(getBibliographicEntity("1577261074"));
        ItemEntity existingItemEntity = getItemEntity("123456");
        String barcode = "34568";
        Map<String, List<SubmitCollectionReportInfo>> submitCollectionReportInfoMap = new HashMap<>();
        List<SubmitCollectionReportInfo> submitCollectionReportInfos = new ArrayList<>();
        submitCollectionReportInfos.add(getSubmitCollectionReportInfo("123456"));
        submitCollectionReportInfoMap.put("submitCollectionSuccessList",submitCollectionReportInfos);
        String result = submitCollectionReportHelperService.updateSuccessMessageForRemovedBibs(incomingBibliographicEntityList,existingBibliographicEntityList,existingItemEntity,barcode,submitCollectionReportInfoMap);
        assertNotNull(result);
    }
    private HoldingsEntity getHoldingsEntity() {
        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setCreatedBy("tst");
        holdingsEntity.setLastUpdatedBy("tst");
        holdingsEntity.setOwningInstitutionId(1);
        holdingsEntity.setOwningInstitutionHoldingsId("12345");
        holdingsEntity.setDeleted(false);
        return holdingsEntity;
    }
    private BibliographicEntity getBibliographicEntity(String OwningInstitutionBibId){

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
        itemEntity.setBibliographicEntities(Arrays.asList(bibliographicEntity));
        itemEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));

        holdingsEntity.setItemEntities(Arrays.asList(itemEntity));
        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        bibliographicEntity.setItemEntities(Arrays.asList(itemEntity));

        return bibliographicEntity;
    }
    private SubmitCollectionReportInfo getSubmitCollectionReportInfo(String itemBarcode){
        SubmitCollectionReportInfo submitCollectionReportInfo = new SubmitCollectionReportInfo();
        submitCollectionReportInfo.setOwningInstitution("PUL");
        submitCollectionReportInfo.setItemBarcode(itemBarcode);
        submitCollectionReportInfo.setCustomerCode("PA");
        submitCollectionReportInfo.setMessage("SUCCESS");
        return submitCollectionReportInfo;
    }

    private ItemEntity getItemEntity(String barcode){
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setId(1);
        institutionEntity.setInstitutionName("PUL");
        institutionEntity.setInstitutionCode("PUL");
        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setOwningInstitutionItemId("843617540");
        itemEntity.setOwningInstitutionId(1);
        itemEntity.setBarcode(barcode);
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
        itemEntity.setBibliographicEntities(Arrays.asList(getBibliographicEntity("1577261074")));
        itemEntity.setHoldingsEntities(Arrays.asList(getHoldingsEntity()));
        return itemEntity;
    }


    public BibliographicEntity saveBibSingleHoldingsSingleItem(Integer itemCount, String itemBarcode, String customerCode, String callnumber, String institution, String owningInstBibId, String owningInstHoldingId, String owningInstItemId, boolean availableItem) throws Exception {
        File bibContentFile = getBibContentFile(institution);
        File holdingsContentFile = getHoldingsContentFile(institution);
        String sourceBibContent = FileUtils.readFileToString(bibContentFile, "UTF-8");
        String sourceHoldingsContent = FileUtils.readFileToString(holdingsContentFile, "UTF-8");

        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent(sourceBibContent.getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionId(1);
        bibliographicEntity.setOwningInstitutionBibId(owningInstBibId);
        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setContent(sourceHoldingsContent.getBytes());
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setCreatedBy("tst");
        holdingsEntity.setLastUpdatedBy("tst");
        holdingsEntity.setOwningInstitutionId(1);
        holdingsEntity.setOwningInstitutionHoldingsId(String.valueOf(owningInstHoldingId));

        List<ItemEntity> itemEntityList = getItemEntityList(itemCount, itemBarcode, customerCode, callnumber, owningInstItemId, availableItem);
        for (ItemEntity itemEntity1 : itemEntityList) {
            itemEntity1.setHoldingsEntities(Arrays.asList(holdingsEntity));
            itemEntity1.setBibliographicEntities(Arrays.asList(bibliographicEntity));
        }
        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        bibliographicEntity.setItemEntities(itemEntityList);
        holdingsEntity.setItemEntities(itemEntityList);
        return bibliographicEntity;

    }

    public List<ItemEntity> getItemEntityList(Integer itemCount, String itemBarcode, String customerCode, String callnumber, String owningInstItemId, boolean availableItem) {
        List<ItemEntity> itemEntityList = new ArrayList<>();
        for (int count = 0; count < itemCount; count++) {
            ItemEntity itemEntity = new ItemEntity();
            itemEntity.setLastUpdatedDate(new Date());
            if (count == 0) {
                itemEntity.setOwningInstitutionItemId(owningInstItemId);
                itemEntity.setBarcode(itemBarcode);
            } else {
                itemEntity.setOwningInstitutionItemId(owningInstItemId + count);
                itemEntity.setBarcode(itemBarcode + count);
            }
            itemEntity.setOwningInstitutionId(1);
            itemEntity.setCallNumber(callnumber);
            itemEntity.setCollectionGroupId(1);
            itemEntity.setCallNumberType("1");
            itemEntity.setCustomerCode(customerCode);
            itemEntity.setCatalogingStatus("Complete");
            if (availableItem) {
                itemEntity.setItemAvailabilityStatusId(1);
            } else {
                itemEntity.setItemAvailabilityStatusId(2);
            }
            itemEntity.setCreatedDate(new Date());
            itemEntity.setCreatedBy("tst");
            itemEntity.setLastUpdatedBy("tst");
            itemEntityList.add(itemEntity);
        }
        return itemEntityList;
    }

    private File getBibContentFile(String institution) throws URISyntaxException {
        URL resource = null;
        resource = getClass().getResource("PUL-BibContent.xml");
        return new File(resource.toURI());
    }

    private File getHoldingsContentFile(String institution) throws URISyntaxException {
        URL resource = null;
        resource = getClass().getResource("PUL-HoldingsContent.xml");
        return new File(resource.toURI());
    }
}
