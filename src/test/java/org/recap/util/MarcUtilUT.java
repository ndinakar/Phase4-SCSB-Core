package org.recap.util;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.marc4j.marc.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;
import org.recap.ScsbConstants;
import org.recap.model.jaxb.marc.BibRecords;
import org.recap.model.jaxb.marc.DataFieldType;
import org.recap.model.jaxb.marc.RecordType;
import org.recap.model.jaxb.marc.SubfieldatafieldType;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.marc.BibMarcRecord;
import org.recap.model.marc.HoldingsMarcRecord;
import org.recap.model.marc.ItemMarcRecord;
import org.springframework.test.util.ReflectionTestUtils;
import org.marc4j.marc.Record;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by pvsubrah on 6/15/16.
 */

public class MarcUtilUT extends BaseTestCaseUT {

    @InjectMocks
    MarcUtil marcUtil;

    @Mock
    Record record;

    @Mock
    VariableField variableField;

    @Mock
    DataField dataField;

    @Mock
    RecordType marcRecord;

    @Mock
    DataFieldType dataFieldType;

    @Mock
    Leader leader;

    @Mock
    SubfieldatafieldType subfieldatafieldType;

    @Mock
    Subfield subfield;

    private String marcXML = "<collection xmlns=\"http://www.loc.gov/MARC21/slim\">\n" +
            "          <record>\n" +
            "            <controlfield tag=\"001\">NYPG001000011-B</controlfield>\n" +
            "            <controlfield tag=\"005\">20001116192418.8</controlfield>\n" +
            "            <controlfield tag=\"008\">841106s1976    le       b    000 0 arax </controlfield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"010\">\n" +
            "              <subfield code=\"a\">79971032</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"035\">\n" +
            "              <subfield code=\"a\">NNSZ00100011</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"035\">\n" +
            "              <subfield code=\"a\">(WaOLN)nyp0200023</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"040\">\n" +
            "              <subfield code=\"c\">NN</subfield>\n" +
            "              <subfield code=\"d\">NN</subfield>\n" +
            "              <subfield code=\"d\">WaOLN</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"043\">\n" +
            "              <subfield code=\"a\">a-ba---</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\"0\" ind2=\"0\" tag=\"050\">\n" +
            "              <subfield code=\"a\">DS247.B28</subfield>\n" +
            "              <subfield code=\"b\">R85</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\"1\" ind2=\" \" tag=\"100\">\n" +
            "              <subfield code=\"a\">RumayhÌ£Ä«, MuhÌ£ammad GhÄ\u0081nim.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\"1\" ind2=\"3\" tag=\"245\">\n" +
            "              <subfield code=\"a\">al-BahÌ£rayn :</subfield>\n" +
            "              <subfield code=\"b\">mushkilÄ\u0081t al-taghyÄ«r al-siyÄ\u0081sÄ« wa-al-ijtimÄ\u0081Ê»Ä« /</subfield>\n" +
            "              <subfield code=\"c\">MuhÌ£ammad al-RumayhÌ£Ä«.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"250\">\n" +
            "              <subfield code=\"a\">al-TÌ£abÊ»ah 1.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"260\">\n" +
            "              <subfield code=\"a\">[BayrÅ«t] :</subfield>\n" +
            "              <subfield code=\"b\">DÄ\u0081r Ibn KhaldÅ«n,</subfield>\n" +
            "              <subfield code=\"c\">1976.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"300\">\n" +
            "              <subfield code=\"a\">264 p. ;</subfield>\n" +
            "              <subfield code=\"c\">24 cm.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"504\">\n" +
            "              <subfield code=\"a\">Includes bibliographies.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"546\">\n" +
            "              <subfield code=\"a\">In Arabic.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\"0\" tag=\"651\">\n" +
            "              <subfield code=\"a\">Bahrain</subfield>\n" +
            "              <subfield code=\"x\">History</subfield>\n" +
            "              <subfield code=\"y\">20th century.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\"0\" tag=\"651\">\n" +
            "              <subfield code=\"a\">Bahrain</subfield>\n" +
            "              <subfield code=\"x\">Economic conditions.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\"0\" tag=\"651\">\n" +
            "              <subfield code=\"a\">Bahrain</subfield>\n" +
            "              <subfield code=\"x\">Social conditions.</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"907\">\n" +
            "              <subfield code=\"a\">.b100000241</subfield>\n" +
            "              <subfield code=\"c\">m</subfield>\n" +
            "              <subfield code=\"d\">a</subfield>\n" +
            "              <subfield code=\"e\">-</subfield>\n" +
            "              <subfield code=\"f\">ara</subfield>\n" +
            "              <subfield code=\"g\">le </subfield>\n" +
            "              <subfield code=\"h\">3</subfield>\n" +
            "              <subfield code=\"i\">1</subfield>\n" +
            "            </datafield>\n" +
            "            <datafield ind1=\" \" ind2=\" \" tag=\"952\">\n" +
            "              <subfield code=\"h\">*OFK 84-1944</subfield>\n" +
            "            </datafield>\n" +
            "          </record>\n" +
            "        </collection>";

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
    public void getMultiFieldValues() throws Exception {

        List<Record> records = marcUtil.convertMarcXmlToRecord(marcXML);

        assertNotNull(records);
        assertTrue(records.size() == 1);


        List<String> oThirtyFives = marcUtil.getMultiDataFieldValues(records.get(0), "035", null, null, "a");
        assertEquals(2, oThirtyFives.size());


    }

    @Test
    public void getMultiFieldValues1() {
        List<VariableField> dataFields = new ArrayList<>();
        dataFields.add(dataField);
        Mockito.when(record.getVariableFields("035")).thenReturn(dataFields);
        List<String> oThirtyFives = marcUtil.getMultiDataFieldValues(record, "035", "p", "q", "a");
        assertNotNull(oThirtyFives);
    }

    @Test
    public void getDataFieldValueForRecordType() {
        List<DataFieldType> dataFields = new ArrayList<>();
        dataFields.add(dataFieldType);
        Mockito.when(dataFieldType.getTag()).thenReturn("876");
        Mockito.when(dataFieldType.getInd1()).thenReturn("p");
        Mockito.when(dataFieldType.getInd2()).thenReturn("q");
        Mockito.when(marcRecord.getDatafield()).thenReturn(dataFields);
        String fieldValue = marcUtil.getDataFieldValueForRecordType(marcRecord, "876", "p", "q", "877");
        assertNotNull(fieldValue);
    }

    @Test
    public void getDataFieldValuesNull() {
        ReflectionTestUtils.invokeMethod(marcUtil, "getDataFieldValue", dataField, 'a');
    }

    @Test
    public void getDataFieldValues() throws Exception {

        List<Record> records =
                marcUtil.convertMarcXmlToRecord(marcXML);

        assertNotNull(records);
        assertTrue(records.size() == 1);


        String title = marcUtil.getDataFieldValue(records.get(0), "245", null, null, "a");
        assertEquals("al-BahÌ£rayn :", title);

    }

    @Test
    public void getListOfDataFieldValuesStartsWith() {
        List<Character> subFieldTags = new ArrayList<>();
        subFieldTags.add('a');
        List<Record> records = marcUtil.convertMarcXmlToRecord(marcXML);
        records.get(0).getVariableFields().get(0).setId(1l);
        List<String> fieldValues = marcUtil.getListOfDataFieldValuesStartsWith(records.get(0), "9", subFieldTags);
        assertNotNull(fieldValues);
    }

    @Test
    public void extractXmlAndSetEntityToMap() {
        List<Record> records = marcUtil.convertMarcXmlToRecord(marcXML);
        StringBuilder errorMessage = new StringBuilder();
        Map<String, Object> map = new HashMap<>();
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        Map<String, Object> extractXmlAndSetEntityToMap = marcUtil.extractXmlAndSetEntityToMap(records.get(0), errorMessage, map, bibliographicEntity);
        assertNotNull(extractXmlAndSetEntityToMap);
    }

    @Test
    public void extractXmlAndSetEntityToMapError() {
        StringBuilder errorMessage = new StringBuilder();
        Map<String, Object> map = new HashMap<>();
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        Mockito.when(record.getLeader()).thenReturn(leader);
        Map<String, Object> extractXmlAndSetEntityToMap = marcUtil.extractXmlAndSetEntityToMap(record, errorMessage, map, bibliographicEntity);
        assertNotNull(extractXmlAndSetEntityToMap);
        assertEquals(" Atleast one subfield should be there for 245 tag Leader Field value should be 24 characters", errorMessage.toString());
    }

    @Test
    public void isSubFieldExistsForMarcDataFieldEmpty() {
        List<DataFieldType> dataFields = new ArrayList<>();
        dataFields.add(dataFieldType);
        Mockito.when(marcRecord.getDatafield()).thenReturn(dataFields);
        Mockito.when(dataFieldType.getTag()).thenReturn("853");
        boolean isSubFieldExists = marcUtil.isSubFieldExists(marcRecord, "852");
        assertFalse(isSubFieldExists);
    }

    @Test
    public void isSubFieldExistsRecord() {
        Mockito.when(record.getVariableField("")).thenReturn(dataField);
        List<Subfield> subfields = new ArrayList<>();
        subfields.add(subfield);
        Mockito.when(dataField.getSubfields()).thenReturn(subfields);
        boolean isSubFieldExist = marcUtil.isSubFieldExists(record, "");
        assertFalse(isSubFieldExist);
    }

    @Test
    public void getDataFieldValueNull() {
        String fieldValue = marcUtil.getDataFieldValue(record, "876", 'p');
        assertNull(fieldValue);
    }

    @Test
    public void getInd1ForRecordTypeDataFieldNull() {
        List<DataFieldType> dataFields = new ArrayList<>();
        dataFields.add(dataFieldType);
        Mockito.when(dataFieldType.getTag()).thenReturn("tag");
        Mockito.when(marcRecord.getDatafield()).thenReturn(dataFields);
        String isSubFieldExist = marcUtil.getInd1ForRecordType(marcRecord, "", "");
        assertNull(isSubFieldExist);
    }

    @Test
    public void getInd1ForRecordTypeSubDataFieldNull() {
        List<DataFieldType> dataFields = new ArrayList<>();
        dataFields.add(dataFieldType);
        Mockito.when(dataFieldType.getTag()).thenReturn("");
        List<SubfieldatafieldType> subFields = new ArrayList<>();
        subFields.add(subfieldatafieldType);
        Mockito.when(dataFieldType.getSubfield()).thenReturn(subFields);
        Mockito.when(subfieldatafieldType.getCode()).thenReturn("code");
        Mockito.when(dataFieldType.getInd1()).thenReturn("Ind1");
        Mockito.when(marcRecord.getDatafield()).thenReturn(dataFields);
        String isSubFieldExist = marcUtil.getInd1ForRecordType(marcRecord, "", "");
        assertNull(isSubFieldExist);
    }


    @Test
    public void isSubFieldExistsForMarcSubDataFieldEmpty() {
        List<DataFieldType> dataFields = new ArrayList<>();
        dataFields.add(dataFieldType);
        Mockito.when(marcRecord.getDatafield()).thenReturn(dataFields);
        Mockito.when(dataFieldType.getTag()).thenReturn("852");
        List<SubfieldatafieldType> subFields = new ArrayList<>();
        subFields.add(subfieldatafieldType);
        Mockito.when(subfieldatafieldType.getCode()).thenReturn("");
        Mockito.when(dataFieldType.getSubfield()).thenReturn(subFields);
        boolean isSubFieldExists = marcUtil.isSubFieldExists(marcRecord, "852");
        assertFalse(isSubFieldExists);
    }

    @Test
    public void convertAndValidateXml() {
        List<Record> records = marcUtil.convertMarcXmlToRecord(marcXML);
        ReflectionTestUtils.setField(marcUtil, "inputLimit", 0);
        String extractXmlAndSetEntityToMap = marcUtil.convertAndValidateXml(marcXML, true, records);
        assertTrue(extractXmlAndSetEntityToMap.contains(ScsbConstants.SUBMIT_COLLECTION_LIMIT_EXCEED_MESSAGE));
    }

    @Test
    public void convertAndValidateXmlInvalid() {
        List<Record> records = marcUtil.convertMarcXmlToRecord(marcXML);
        String extractXmlAndSetEntityToMap = marcUtil.convertAndValidateXml(marcXML, true, records);
        assertTrue(extractXmlAndSetEntityToMap.contains(ScsbConstants.INVALID_MARC_XML_FORMAT_MESSAGE));
    }


    @Test
    public void getDataFieldValueStartsWith() {

        List<Record> records =
                marcUtil.convertMarcXmlToRecord(marcXML);
        List<Character> subFields = new ArrayList<>();
        subFields.add('a');
        subFields.add('h');

        assertNotNull(records);
        assertTrue(records.size() == 1);
        String fieldVaule = marcUtil.getDataFieldValueStartsWith(records.get(0), "9");
        assertEquals(".b100000241 m a - ara le  3 1 *OFK 84-1944", fieldVaule);
        String subFieldValue = marcUtil.getDataFieldValueStartsWith(records.get(0), "9", subFields);
        assertEquals(".b100000241 3 *OFK 84-1944", subFieldValue);
    }

    @Test
    public void getControlFieldValues() {

        List<Record> records =
                marcUtil.convertMarcXmlToRecord(marcXML);

        assertNotNull(records);
        assertTrue(records.size() == 1);
        String controlField = marcUtil.getControlFieldValue(records.get(0), "001");
        assertEquals("NYPG001000011-B", controlField);


    }


    @Test
    public void math() throws Exception {
        int numThreads = 5;
        int docsPerThread = 1;
        int totalDocs = 15;

        assertNotNull(totalDocs % (docsPerThread * numThreads));
        assertNotNull(totalDocs / (docsPerThread * numThreads));

    }

    @Test
    public void getSecondIndicatorForDataField() throws Exception {
        List<Record> records = marcUtil.convertMarcXmlToRecord(marcXML);
        assertNotNull(records.get(0));
        Integer secondIndicatorForDataField = marcUtil.getSecondIndicatorForDataField(records.get(0), "245");
        assertEquals(secondIndicatorForDataField, Integer.valueOf(3));
    }

    @Test
    public void getSecondIndicatorForDataFieldReturnZero() {
        Integer secondIndicatorForDataField = marcUtil.getSecondIndicatorForDataField(record, "245");
        assertEquals(Integer.valueOf(0), secondIndicatorForDataField);
    }

    @Test
    public void getControlFieldValueNull() {
        String controlFieldValue = marcUtil.getControlFieldValue(record, "001");
        assertNull(controlFieldValue);
    }

    @Test
    public void getControlFieldValueVariableNull() {
        List<VariableField> variableFields = new ArrayList<>();
        variableFields.add(null);
        Mockito.when(record.getVariableFields("001")).thenReturn(variableFields);
        String controlFieldValue = marcUtil.getControlFieldValue(record, "001");
        assertNull(controlFieldValue);
    }

    private List<Record> getRecords() throws Exception {
        URL resource = getClass().getResource("singleRecord.xml");
        File file = new File(resource.toURI());
        String marcXmlString = FileUtils.readFileToString(file, "UTF-8");
        marcUtil = new MarcUtil();
        return marcUtil.readMarcXml(marcXmlString);
    }

    @Test
    public void readMarcXml() throws Exception {
        List<Record> records = getRecords();
        assertNotNull(records);
        assertEquals(1, records.size());
        Record record = records.get(0);
        assertNotNull(record);
    }

    @Test
    public void getDataFieldValue() throws Exception {
        List<Record> records = getRecords();
        assertNotNull(records);
        assertEquals(1, records.size());
        Record record = records.get(0);
        assertNotNull(record);
        String fieldValue = marcUtil.getDataFieldValue(record, "876", 'p');
        assertEquals("32101095533293", fieldValue);
    }

    @Test
    public void getInd1DataFieldNull() throws Exception {
        Character ind1 = marcUtil.getInd1(record, "876", 'h');
        assertNull(ind1);
    }

    @Test
    public void getControlFieldValue() throws Exception {
        List<Record> records = getRecords();
        assertNotNull(records);
        assertEquals(1, records.size());
        Record record = records.get(0);
        assertNotNull(record);
        String controlFieldValue = marcUtil.getControlFieldValue(record, "001");
        assertEquals("9919400", controlFieldValue);
    }

    @Test
    public void getInd1() throws Exception {
        List<Record> records = getRecords();
        assertNotNull(records);
        assertEquals(1, records.size());
        Record record = records.get(0);
        assertNotNull(record);
        Character ind1 = marcUtil.getInd1(record, "876", 'h');
        assertTrue(ind1 == '0');
    }

    @Test
    public void buildBibMarcRecord() throws Exception {
        List<Record> records = getRecords();
        assertNotNull(records);
        assertEquals(1, records.size());
        Record record = records.get(0);
        assertNotNull(record);

        BibMarcRecord bibMarcRecord = marcUtil.buildBibMarcRecord(record);
        assertNotNull(bibMarcRecord);
        Record bibRecord = bibMarcRecord.getBibRecord();
        assertNotNull(bibRecord);
        assertFalse(marcUtil.isSubFieldExists(record, "852"));
        assertFalse(marcUtil.isSubFieldExists(record, "866"));
        assertFalse(marcUtil.isSubFieldExists(record, "876"));

        List<HoldingsMarcRecord> holdingsMarcRecords = bibMarcRecord.getHoldingsMarcRecords();
        assertNotNull(holdingsMarcRecords);
        assertTrue(holdingsMarcRecords.size() == 1);
        HoldingsMarcRecord holdingsMarcRecord = holdingsMarcRecords.get(0);
        assertNotNull(holdingsMarcRecord);
        Record holdingsRecord = holdingsMarcRecord.getHoldingsRecord();
        assertNotNull(holdingsRecord);
        assertTrue(marcUtil.isSubFieldExists(holdingsRecord, "852"));
        assertTrue(marcUtil.isSubFieldExists(holdingsRecord, "866"));
        assertFalse(marcUtil.isSubFieldExists(holdingsRecord, "245"));
        assertFalse(marcUtil.isSubFieldExists(holdingsRecord, "876"));

        List<ItemMarcRecord> itemMarcRecordList = holdingsMarcRecord.getItemMarcRecordList();
        assertNotNull(itemMarcRecordList);
        assertTrue(itemMarcRecordList.size() == 1);
        ItemMarcRecord itemMarcRecord = itemMarcRecordList.get(0);
        assertNotNull(itemMarcRecord);
        Record itemRecord = itemMarcRecord.getItemRecord();
        assertNotNull(itemRecord);
        assertFalse(marcUtil.isSubFieldExists(itemRecord, "852"));
        assertFalse(marcUtil.isSubFieldExists(itemRecord, "245"));
        assertTrue(marcUtil.isSubFieldExists(itemRecord, "876"));
    }

    @Test
    public void buildBibRecord() throws Exception {
        BibRecords bibRecords = getBibRecords();
        BibMarcRecord bibMarcRecord = marcUtil.buildBibMarcRecord(bibRecords.getBibRecordList().get(0));
        assertNotNull(bibMarcRecord);
        RecordType marcRecord = getRecordType();
        String DataFieldValueForRecordType = marcUtil.getDataFieldValueForRecordType(marcRecord, "test", "", "", "code");
        assertNotNull(DataFieldValueForRecordType);
        Boolean isSubFieldExists = marcUtil.isSubFieldExists(marcRecord, "test");
        assertNotNull(isSubFieldExists);
        String getInd1ForRecordType = marcUtil.getInd1ForRecordType(marcRecord, "test", "code");
        assertNotNull(getInd1ForRecordType);
    }

    private BibRecords getBibRecords() throws JAXBException, XMLStreamException {
        JAXBContext context = JAXBContext.newInstance(BibRecords.class);
        XMLInputFactory xif = XMLInputFactory.newFactory();
        xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        InputStream stream = new ByteArrayInputStream(scsbXmlContent.getBytes(StandardCharsets.UTF_8));
        XMLStreamReader xsr = xif.createXMLStreamReader(stream);
        Unmarshaller um = context.createUnmarshaller();
        BibRecords bibRecords = (BibRecords) um.unmarshal(xsr);
        return bibRecords;
    }


    private RecordType getRecordType() {
        SubfieldatafieldType subfieldatafieldType = new SubfieldatafieldType();
        subfieldatafieldType.setCode("code");
        subfieldatafieldType.setValue("sucess");
        List<SubfieldatafieldType> subfieldatafieldTypeList = new ArrayList<>();
        subfieldatafieldTypeList.add(subfieldatafieldType);
        DataFieldType dataFieldType = new DataFieldType();
        dataFieldType.setTag("test");
        dataFieldType.setInd1("");
        dataFieldType.setInd2("");
        dataFieldType.setSubfield(subfieldatafieldTypeList);
        List<DataFieldType> dataFieldTypeList = new ArrayList<>();
        dataFieldTypeList.add(dataFieldType);
        RecordType marcRecord = new RecordType();
        marcRecord.setDatafield(dataFieldTypeList);
        return marcRecord;
    }


    @Test
    public void writeMarcXml() throws Exception {
        List<Record> records = getRecords();
        assertNotNull(records);
        assertEquals(1, records.size());
        Record record = records.get(0);
        assertNotNull(record);

        String content = marcUtil.writeMarcXml(record);
        assertNotNull(content);
        assertTrue(content.length() > 0);
    }
}