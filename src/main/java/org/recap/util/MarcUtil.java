package org.recap.util;

import info.freelibrary.marc4j.impl.ControlFieldImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.marc4j.MarcReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlReader;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.*;
import org.marc4j.marc.Record;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.jaxb.BibRecord;
import org.recap.model.jaxb.Holding;
import org.recap.model.jaxb.Items;
import org.recap.model.jaxb.marc.*;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.marc.BibMarcRecord;
import org.recap.model.marc.HoldingsMarcRecord;
import org.recap.model.marc.ItemMarcRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by pvsubrah on 6/15/16.
 */
@Slf4j
@Service
public class MarcUtil {



    @Value("${" + PropertyKeyConstants.SUBMIT_COLLECTION_INPUT_LIMIT + "}")
    private Integer inputLimit;

    /**
     * Convert marc xml to record list.
     *
     * @param marcXml the marc xml
     * @return the list
     */
    public List<Record> convertMarcXmlToRecord(String marcXml) {
        List<Record> records = new ArrayList<>();
        MarcReader reader = new MarcXmlReader(IOUtils.toInputStream(marcXml, StandardCharsets.UTF_8));
        while (reader.hasNext()) {
            Record record = reader.next();
            records.add(record);
        }
        return records;
    }

    /**
     * Gets data field value starts with.
     *
     * @param record            the record
     * @param dataFieldStartTag the data field start tag
     * @return the data field value starts with
     */
    public String getDataFieldValueStartsWith(Record record, String dataFieldStartTag) {
        StringBuilder fieldValue = new StringBuilder();
        if (record != null) {
            List<VariableField> variableFields = record.getVariableFields();
            if (!CollectionUtils.isEmpty(variableFields)) {
                for (VariableField variableField : variableFields) {
                    if (variableField != null && StringUtils.isNotBlank(variableField.getTag()) && variableField.getTag().startsWith(dataFieldStartTag)) {
                        DataField dataField = (DataField) variableField;
                        List<Subfield> subfields = dataField.getSubfields();
                        for (Subfield subfield : subfields) {
                            if (subfield != null && StringUtils.isNotBlank(subfield.getData())) {
                                fieldValue.append(subfield.getData());
                                fieldValue.append(" ");
                            }
                        }
                    }
                }
            }
        }
        return fieldValue.toString().trim();
    }

    /**
     * Gets data field value starts with.
     *
     * @param record            the record
     * @param dataFieldStartTag the data field start tag
     * @param subFieldTags      the sub field tags
     * @return the data field value starts with
     */
    public String getDataFieldValueStartsWith(Record record, String dataFieldStartTag, List<Character> subFieldTags) {
        StringBuilder fieldValue = new StringBuilder();
        if (record != null) {
            List<VariableField> variableFields = record.getVariableFields();
            if (!CollectionUtils.isEmpty(variableFields)) {
                Subfield subfield;
                for (VariableField variableField : variableFields) {
                    if (variableField != null && StringUtils.isNotBlank(variableField.getTag()) && variableField.getTag().startsWith(dataFieldStartTag)) {
                        DataField dataField = (DataField) variableField;
                        for (Character subFieldTag : subFieldTags){
                            subfield = dataField.getSubfield(subFieldTag);
                            if (subfield != null) {
                                fieldValue.append(subfield.getData());
                                fieldValue.append(" ");
                            }
                        }
                    }
                }
            }
        }
        return fieldValue.toString().trim();
    }

    /**
     * Gets list of data field values starts with.
     *
     * @param record            the record
     * @param dataFieldStartTag the data field start tag
     * @param subFieldTags      the sub field tags
     * @return the list of data field values starts with
     */
    public List<String> getListOfDataFieldValuesStartsWith(Record record, String dataFieldStartTag, List<Character> subFieldTags) {
        List<String> fieldValues = new ArrayList<>();
        if (record != null) {
            List<VariableField> variableFields = record.getVariableFields();
            if (!CollectionUtils.isEmpty(variableFields)) {
                Subfield subfield;
                for (VariableField variableField : variableFields) {
                    if (variableField != null && StringUtils.isNotBlank(variableField.getTag()) && variableField.getTag().startsWith(dataFieldStartTag)) {
                        DataField dataField = (DataField) variableField;
                        for (Character subFieldTag : subFieldTags){
                            subfield = dataField.getSubfield(subFieldTag);
                            if (subfield != null) {
                                String data = subfield.getData();
                                if (StringUtils.isNotBlank(data)){
                                    fieldValues.add(data);
                                }
                            }
                        }
                    }
                }
            }
        }
        return fieldValues;
    }

    /**
     * Gets data field value.
     *
     * @param marcRecord the marc record
     * @param field      the field
     * @param ind1       the ind 1
     * @param ind2       the ind 2
     * @param subField   the sub field
     * @return the data field value
     */
    public String getDataFieldValue(Record marcRecord, String field, String ind1, String ind2, String subField) {
        List<String> strings = resolveValue(marcRecord, field, ind1, ind2, subField);
        return CollectionUtils.isEmpty(strings)? "" : strings.get(0);
    }

    /**
     * Gets multi data field values.
     *
     * @param marcRecord the marc record
     * @param field      the field
     * @param ind1       the ind 1
     * @param ind2       the ind 2
     * @param subField   the sub field
     * @return the multi data field values
     */
    public List<String> getMultiDataFieldValues(Record marcRecord, String field, String ind1, String ind2, String subField) {
        return resolveValue(marcRecord, field, ind1, ind2, subField);
    }

    private static List<String> resolveValue(Record marcRecord, String field, String ind1, String ind2, String subField) {
        List<String> values = new ArrayList<>();
        String indicator1 = StringUtils.isNotBlank(ind1) ? String.valueOf(ind1.charAt(0)) : " ";
        String indicator2 = StringUtils.isNotBlank(ind2) ? String.valueOf(ind2.charAt(0)) : " ";
        List<VariableField> dataFields = marcRecord.getVariableFields(field);

        for (VariableField variableField : dataFields) {
            DataField dataField = (DataField) variableField;
            if (dataField != null && doIndicatorsMatch(indicator1, indicator2, dataField)) {
                List<Subfield> subFields = dataField.getSubfields(subField);
                for (Subfield subfield : subFields) {
                    if (subField != null) {
                        String data = subfield.getData();
                        if (StringUtils.isNotBlank(data)) {
                            values.add(data);
                        }
                    }
                }
            }
        }
        return values;
    }

    private static boolean doIndicatorsMatch(String indicator1, String indicator2, DataField dataField) {
        boolean result = true;
        if (StringUtils.isNotBlank(indicator1)) {
            result = dataField.getIndicator1() == indicator1.charAt(0);
        }
        if (StringUtils.isNotBlank(indicator2)) {
            result &= dataField.getIndicator2() == indicator2.charAt(0);
        }
        return result;
    }

    /**
     * Gets control field value.
     *
     * @param marcRecord the marc record
     * @param field      the field
     * @return the control field value
     */
    public String getControlFieldValue(Record marcRecord, String field) {
        List<VariableField> variableFields = marcRecord.getVariableFields(field);
        for (VariableField variableField : variableFields) {
            ControlFieldImpl controlField = (ControlFieldImpl) variableField;
            if (controlField != null) {
                return controlField.getData();
            }
        }
        return null;
    }

    /**
     * Gets second indicator for data field.
     *
     * @param marcRecord the marc record
     * @param field      the field
     * @return the second indicator for data field
     */
    public Integer getSecondIndicatorForDataField(Record marcRecord, String field) {
        List<VariableField> dataFields = marcRecord.getVariableFields(field);
        if (!CollectionUtils.isEmpty(dataFields)) {
            DataField dataField = (DataField) dataFields.get(0);
            char dataFieldIndicator2 = dataField.getIndicator2();
            if (Character.isDigit(dataFieldIndicator2)) {
                return Character.getNumericValue(dataFieldIndicator2);
            }
        }
        return 0;
    }

    /**
     * Read marc xml list.
     *
     * @param marcXmlString the marc xml string
     * @return the list
     */
    public List<Record> readMarcXml(String marcXmlString) {
        List<Record> recordList = new ArrayList<>();
        InputStream in = new ByteArrayInputStream(marcXmlString.getBytes());
        MarcReader reader = new MarcXmlReader(in);
        while (reader.hasNext()) {
            Record record = reader.next();
            recordList.add(record);
        }
        return recordList;
    }

    /**
     * Gets data field value.
     *
     * @param record   the record
     * @param field    the field
     * @param subField the sub field
     * @return the data field value
     */
    public String getDataFieldValue(Record record, String field, char subField) {
        DataField dataField = getDataField(record, field);
        if (dataField != null) {
            Subfield subfield = dataField.getSubfield(subField);
            if (subfield != null) {
                return subfield.getData();
            }
        }
        return null;
    }

    private static String getDataFieldValue(DataField dataField, char subField) {
        Subfield subfield = dataField.getSubfield(subField);
        if (subfield != null) {
            return subfield.getData();
        }
        return null;
    }

    /**
     * Is sub field exists boolean.
     *
     * @param record the record
     * @param field  the field
     * @return the boolean
     */
    public boolean isSubFieldExists(Record record, String field) {
        DataField dataField = getDataField(record, field);
        if (dataField != null) {
            List<Subfield> subfields = dataField.getSubfields();
            if (org.apache.commons.collections.CollectionUtils.isNotEmpty(subfields)) {
                for (Subfield subfield : subfields) {
                    String data = subfield.getData();
                    if (StringUtils.isNotBlank(data)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isSubFieldExists(RecordType marcRecord, String field) {
        List<DataFieldType> dataFields = marcRecord.getDatafield();
        for (Iterator<DataFieldType> dataFieldIterator = dataFields.iterator(); dataFieldIterator.hasNext(); ) {
            DataFieldType dataField = dataFieldIterator.next();
            if (dataField != null && dataField.getTag().equals(field)) {
                List<SubfieldatafieldType> subFields = dataField.getSubfield();
                for (Iterator<SubfieldatafieldType> subfieldIterator = subFields.iterator(); subfieldIterator.hasNext(); ) {
                    SubfieldatafieldType subfieldatafieldType = subfieldIterator.next();
                    String data = subfieldatafieldType.getCode();
                    if (StringUtils.isNotBlank(data)) {
                        String value = subfieldatafieldType.getValue();
                        if (StringUtils.isNotBlank(value)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }
        return false;
    }

    /**
     * Gets data field.
     *
     * @param record the record
     * @param field  the field
     * @return the data field
     */
    public DataField getDataField(Record record, String field) {
        VariableField variableField = record.getVariableField(field);
        if (variableField != null) {
            return (DataField) variableField;
        }
        return null;
    }

    /**
     * Gets ind 1.
     *
     * @param record   the record
     * @param field    the field
     * @param subField the sub field
     * @return the ind 1
     */
    public Character getInd1(Record record, String field, char subField) {
        DataField dataField = getDataField(record, field);
        if (dataField != null) {
            Subfield subfield = dataField.getSubfield(subField);
            if (subfield != null) {
                return dataField.getIndicator1();
            }
        }
        return null;
    }

    /**
     * Build bib marc record bib marc record.
     *
     * @param record the record
     * @return the bib marc record
     */
    public BibMarcRecord buildBibMarcRecord(Record record) {
        Record bibRecord = record;
        String[] holdingsTags = {"852"};
        String[] holdings866Tags = {"866"};
        String[] itemTags = {"876"};
        List<VariableField> holdingsVariableFields = new ArrayList<>(bibRecord.getVariableFields(holdingsTags));
        List<VariableField> holdings866VariableFields = new ArrayList<>(bibRecord.getVariableFields(holdings866Tags));
        List<VariableField> itemVariableFields = new ArrayList<>(bibRecord.getVariableFields(itemTags));

        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(holdingsVariableFields)) {
            for (VariableField holdingsVariableField : holdingsVariableFields) {
                bibRecord.removeVariableField(holdingsVariableField);
            }
        }
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(holdings866VariableFields)) {
            for (VariableField holdings866VariableField : holdings866VariableFields) {
                bibRecord.removeVariableField(holdings866VariableField);
            }
        }
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(itemVariableFields)) {
            for (VariableField itemVariableField : itemVariableFields) {
                bibRecord.removeVariableField(itemVariableField);
            }
        }
        BibMarcRecord bibMarcRecord = new BibMarcRecord();
        bibMarcRecord.setBibRecord(bibRecord);

        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(holdingsVariableFields)) {
            MarcFactory marcFactory = MarcFactory.newInstance();
            List<HoldingsMarcRecord> holdingsMarcRecordList = new ArrayList<>();
            for (VariableField holdingsVariableField : holdingsVariableFields) {
                List<ItemMarcRecord> itemMarcRecords = new ArrayList<>();
                DataField holdingsDataField = (DataField) holdingsVariableField;
                String holdingsData = getDataFieldValue(holdingsDataField, '0');
                if (StringUtils.isNotBlank(holdingsData)) {
                    HoldingsMarcRecord holdingsMarcRecord = new HoldingsMarcRecord();
                    Record holdingsRecord = marcFactory.newRecord();
                    holdingsRecord.getDataFields().add(holdingsDataField);
                    if (org.apache.commons.collections.CollectionUtils.isNotEmpty(holdings866VariableFields)) {
                        for (VariableField variableField866 : holdings866VariableFields) {
                            DataField holdingsDataField866 = (DataField) variableField866;
                            String holdingsData866 = getDataFieldValue(holdingsDataField866, '0');
                            if (StringUtils.isNotBlank(holdingsData866) && holdingsData866.equalsIgnoreCase(holdingsData)) {
                                holdingsRecord.getDataFields().add(holdingsDataField866);
                            }
                        }
                    }
                    holdingsMarcRecord.setHoldingsRecord(holdingsRecord);
                    if (org.apache.commons.collections.CollectionUtils.isNotEmpty(itemVariableFields)) {
                        for (VariableField itemVariableField : itemVariableFields) {
                            DataField itemDataField = (DataField) itemVariableField;
                            String itemData = getDataFieldValue(itemDataField, '0');
                            if (StringUtils.isNotBlank(itemData) && itemData.equalsIgnoreCase(holdingsData)) {
                                ItemMarcRecord itemMarcRecord = new ItemMarcRecord();
                                Record itemRecord = marcFactory.newRecord();
                                itemRecord.getDataFields().add(itemDataField);
                                itemMarcRecord.setItemRecord(itemRecord);
                                itemMarcRecords.add(itemMarcRecord);
                            }
                        }
                    }
                    holdingsMarcRecord.setItemMarcRecordList(itemMarcRecords);
                    holdingsMarcRecordList.add(holdingsMarcRecord);
                }
            }
            bibMarcRecord.setHoldingsMarcRecords(holdingsMarcRecordList);
        }
        return bibMarcRecord;
    }

    /**
     * Build bib marc record bib marc record.
     *
     * @param bibRecord the bib record
     * @return the bib marc record
     */
    public BibMarcRecord buildBibMarcRecord(BibRecord bibRecord) {
        BibMarcRecord bibMarcRecord = new BibMarcRecord();
        List<HoldingsMarcRecord> holdingsMarcRecordList = new ArrayList<>();
        ContentType bibContent = bibRecord.getBib().getContent();
        CollectionType bibContentCollection = bibContent.getCollection();
        String bibXmlContent = bibContentCollection.serialize(bibContentCollection);
        Record bibContentRecord = getRecordFromContent(bibXmlContent.getBytes());
        bibMarcRecord.setBibRecord(bibContentRecord);
        for(Holding holding : bibRecord.getHoldings().get(0).getHolding()){
            HoldingsMarcRecord holdingsMarcRecord = new HoldingsMarcRecord();
            List<ItemMarcRecord> itemMarcRecords = new ArrayList<>();
            ContentType holdingContent = holding.getContent();
            CollectionType holdingContentCollection = holdingContent.getCollection();
            String holdingXmlContent = holdingContentCollection.serialize(holdingContentCollection);
            Record holdingContentRecord = getRecordFromContent(holdingXmlContent.getBytes());
            holdingsMarcRecord.setHoldingsRecord(holdingContentRecord);
            for(Items item : holding.getItems()){
                ItemMarcRecord itemMarcRecord = new ItemMarcRecord();
                ContentType itemContent = item.getContent();
                CollectionType itemContentCollection = itemContent.getCollection();
                String itemXmlContent = itemContentCollection.serialize(itemContentCollection);
                Record itemContentRecord = getRecordFromContent(itemXmlContent.getBytes());
                itemMarcRecord.setItemRecord(itemContentRecord);
                itemMarcRecords.add(itemMarcRecord);
            }
            holdingsMarcRecord.setItemMarcRecordList(itemMarcRecords);
            holdingsMarcRecordList.add(holdingsMarcRecord);
        }
        bibMarcRecord.setHoldingsMarcRecords(holdingsMarcRecordList);
        return bibMarcRecord;
    }

    /**
     * Write marc xml string.
     *
     * @param record the record
     * @return the string
     */
    public String writeMarcXml(Record record) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        MarcWriter marcWriter = new MarcXmlWriter(byteArrayOutputStream, true);
        marcWriter.write(record);
        marcWriter.close();
        String content = new String(byteArrayOutputStream.toByteArray());
        content = content.replaceAll("marcxml:", "");
        return content;
    }

    private static Record getRecordFromContent(byte[] content) {
        MarcReader reader;
        Record record = null;
        InputStream inputStream = new ByteArrayInputStream(content);
        reader = new MarcXmlReader(inputStream);
        while (reader.hasNext()) {
            record = reader.next();
        }
        return record;
    }

    public Map<String, Object> extractXmlAndSetEntityToMap(Record bibRecord, StringBuilder errorMessage, Map<String, Object> map, BibliographicEntity bibliographicEntity) {
        String bibContent = writeMarcXml(bibRecord);
        if (StringUtils.isNotBlank(bibContent)) {
            bibliographicEntity.setContent(bibContent.getBytes());
        } else {
            errorMessage.append(" Bib Content cannot be empty");
        }

        boolean subFieldExistsFor245 = isSubFieldExists(bibRecord, "245");
        if (!subFieldExistsFor245) {
            errorMessage.append(" Atleast one subfield should be there for 245 tag");
        }
        Leader leader = bibRecord.getLeader();
        if (leader != null) {
            String leaderValue = bibRecord.getLeader().toString();
            if (!(StringUtils.isNotBlank(leaderValue) && leaderValue.length() == 24)) {
                errorMessage.append(" Leader Field value should be 24 characters");
            }
        }
        map.put(ScsbConstants.BIBLIOGRAPHIC_ENTITY, bibliographicEntity);
        return map;
    }

    public String convertAndValidateXml(String inputRecords, boolean checkLimit, List<Record> records) {
        try {
            records.addAll(convertMarcXmlToRecord(inputRecords));
            if (checkLimit && records.size() > inputLimit) {
                return ScsbConstants.SUBMIT_COLLECTION_LIMIT_EXCEED_MESSAGE + inputLimit;
            }
        } catch (Exception e) {
            log.info(String.valueOf(e.getCause()));
            log.error(ScsbCommonConstants.LOG_ERROR, e);
            return ScsbConstants.INVALID_MARC_XML_FORMAT_MESSAGE;
        }
        return null;
    }

    public String getDataFieldValueForRecordType(RecordType marcRecord, String field, String ind1, String ind2, String subField) {
        List<String> strings = resolveValueForRecordType(marcRecord, field, ind1, ind2, subField);
        return CollectionUtils.isEmpty(strings) ? "" : strings.get(0);
    }

    private static List<String> resolveValueForRecordType(RecordType marcRecord, String field, String ind1, String ind2, String subField) {
        List<String> values = new ArrayList<>();
        String indicator1 = StringUtils.isNotBlank(ind1) ? String.valueOf(ind1.charAt(0)) : " ";
        String indicator2 = StringUtils.isNotBlank(ind2) ? String.valueOf(ind2.charAt(0)) : " ";
        List<DataFieldType> dataFields = marcRecord.getDatafield();

        for (Iterator<DataFieldType> dataFieldIterator = dataFields.iterator(); dataFieldIterator.hasNext(); ) {
            DataFieldType dataField = dataFieldIterator.next();
            if (dataField != null && dataField.getTag().equals(field) && doIndicatorsMatchForDataFieldType(indicator1, indicator2, dataField)) {
                List<SubfieldatafieldType> subFields = dataField.getSubfield();
                for (Iterator<SubfieldatafieldType> subfieldIterator = subFields.iterator(); subfieldIterator.hasNext(); ) {
                    SubfieldatafieldType subfieldatafieldType = subfieldIterator.next();
                    if (subField != null && subfieldatafieldType.getCode().equals(subField)) {
                        String data = subfieldatafieldType.getCode();
                        if (StringUtils.isNotBlank(data)) {
                            values.add(subfieldatafieldType.getValue());
                        }
                    }
                }
            }
        }
        return values;
    }

    private static boolean doIndicatorsMatchForDataFieldType(String indicator1, String indicator2, DataFieldType dataField) {
        boolean result = true;
        if (StringUtils.isNotBlank(indicator1)) {
            result = dataField.getInd1().equals(String.valueOf(indicator1.charAt(0)));
        }
        if (StringUtils.isNotBlank(indicator2)) {
            result &= dataField.getInd2().equals(String.valueOf(indicator2.charAt(0)));
        }
        return result;
    }

    public String getInd1ForRecordType(RecordType marcRecord, String field, String subField) {
        List<DataFieldType> dataFields = marcRecord.getDatafield();

        for (Iterator<DataFieldType> dataFieldIterator = dataFields.iterator(); dataFieldIterator.hasNext(); ) {
            DataFieldType dataField = dataFieldIterator.next();
            if (dataField != null && dataField.getTag().equals(field)) {
                List<SubfieldatafieldType> subFields = dataField.getSubfield();
                for (Iterator<SubfieldatafieldType> subfieldIterator = subFields.iterator(); subfieldIterator.hasNext(); ) {
                    SubfieldatafieldType subfieldatafieldType = subfieldIterator.next();
                    if (subField != null && subfieldatafieldType.getCode().equals(subField)) {
                        return dataField.getInd1();
                    }
                }
            }
        }
        return null;
    }
}
