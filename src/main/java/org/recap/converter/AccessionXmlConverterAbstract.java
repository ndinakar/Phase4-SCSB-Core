package org.recap.converter;

import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.util.CommonUtil;
import org.recap.util.DBReportUtil;
import org.recap.util.MarcUtil;
import org.recap.util.PropertyUtil;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AccessionXmlConverterAbstract implements AccessionXmlToBibEntityConverterInterface {

    @Autowired
    public MarcUtil marcUtil;

    @Autowired
    public DBReportUtil dbReportUtil;

    @Autowired
    public CommonUtil commonUtil;

    @Autowired
    public PropertyUtil propertyUtil;

    @Autowired
    public BibliographicDetailsRepository bibliographicDetailsRepository;

    public abstract boolean isFormat(String format);
}
