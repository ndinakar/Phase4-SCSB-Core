package org.recap.util;

import lombok.Data;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.StringUtils;
import org.marc4j.marc.Record;
import org.recap.ScsbConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by rajeshbabuk on 17/Sep/2021
 */
@Data
@Service
public class BibJSONUtil extends MarcUtil {




    private List<String> nonHoldingInstitutions;


    private ProducerTemplate producerTemplate;


    /**
     * Gets lccn value from the record.
     *
     * @param record the record
     * @return the lccn value
     */
    public String getLCCNValue(Record record) {
        String lccnValue = getDataFieldValue(record, "010", null, null, "a");
        if (lccnValue != null) {
            lccnValue = lccnValue.trim();
        }
        return lccnValue;
    }

    /**
     * This method is used to get OCLC numbers from the records.
     * @param record
     * @param institutionCode
     * @return
     */
    public List<String> getOCLCNumbers(Record record, String institutionCode) {
        List<String> oclcNumbers = new ArrayList<>();
        List<String> oclcNumberList = getMultiDataFieldValues(record, "035", null, null, "a");
        for (String oclcNumber : oclcNumberList) {
            if (StringUtils.isNotBlank(oclcNumber) && oclcNumber.contains("OCoLC")) {
                String modifiedOclc = oclcNumber.replaceAll(ScsbConstants.NUMBER_PATTERN, "");
                modifiedOclc = StringUtils.stripStart(modifiedOclc, "0");
                oclcNumbers.add(modifiedOclc);
            }
        }
        if (CollectionUtils.isEmpty(oclcNumbers) && StringUtils.isNotBlank(institutionCode) && nonHoldingInstitutions.contains(institutionCode)) {
            String oclcTag = getControlFieldValue(record, "003");
            if (StringUtils.isNotBlank(oclcTag) && "OCoLC".equalsIgnoreCase(oclcTag)) {
                oclcTag = getControlFieldValue(record, "001");
            }
            oclcTag = StringUtils.stripStart(oclcTag, "0");
            if (StringUtils.isNotBlank(oclcTag)) {
                oclcNumbers.add(oclcTag);
            }
        }
        return oclcNumbers;
    }

    /**
     * Gets isbn number list from the record.
     *
     * @param record the record
     * @return the list
     */
    public List<String> getISBNNumber(Record record){
        List<String> isbnNumbers = new ArrayList<>();
        List<String> isbnNumberList = getMultiDataFieldValues(record,"020", null, null, "a");
        for(String isbnNumber : isbnNumberList){
            isbnNumbers.add(isbnNumber.replaceAll(ScsbConstants.NUMBER_PATTERN, ""));
        }
        return isbnNumbers;
    }

    /**
     * Get issn number list from the record.
     *
     * @param record the record
     * @return the list
     */
    public List<String> getISSNNumber(Record record){
        List<String> issnNumbers = new ArrayList<>();
        List<String> issnNumberList = getMultiDataFieldValues(record,"022", null, null, "a");
        for(String issnNumber : issnNumberList){
            issnNumbers.add(issnNumber.replaceAll(ScsbConstants.NUMBER_PATTERN, ""));
        }
        return issnNumbers;
    }


    /**
     * This method gets the title from marc record.
     *
     * @param marcRecord the marc record
     * @return the title
     */
    public String getTitle(Record marcRecord) {
        StringBuilder title=new StringBuilder();
        title.append(getDataFieldValueStartsWith(marcRecord, "245", Arrays.asList('a', 'b','n','p')) + " ");
        title.append(getDataFieldValueStartsWith(marcRecord, "246", Arrays.asList('a', 'b')) + " ");
        title.append(getDataFieldValueStartsWith(marcRecord, "130", Collections.singletonList('a')) + " ");
        title.append(getDataFieldValueStartsWith(marcRecord, "730", Collections.singletonList('a')) + " ");
        title.append(getDataFieldValueStartsWith(marcRecord, "740", Collections.singletonList('a')) + " ");
        title.append(getDataFieldValueStartsWith(marcRecord, "830", Collections.singletonList('a'))+ " ");
        return title.toString();
    }
}
