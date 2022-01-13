
package org.recap.service.submitcollection;

import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.junit.Test;
import org.marc4j.MarcReader;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.converter.MarcToBibEntityConverter;
import org.recap.converter.SCSBToBibEntityConverter;
import org.recap.model.jaxb.BibRecord;
import org.recap.model.jaxb.marc.BibRecords;
import org.recap.model.jpa.*;
import org.recap.model.submitcollection.SubmitCollectionResponse;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.jpa.ItemDetailsRepository;
import org.recap.repository.jpa.ReportDetailRepository;
import org.recap.service.common.RepositoryService;
import org.recap.service.common.SetupDataService;
import org.recap.util.CommonUtil;
import org.recap.util.MarcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;


/**
 * Created by premkb on 20/12/16.
 */
@Slf4j
public class SubmitCollectionServiceUT extends BaseTestCaseUT {

    @InjectMocks
    SubmitCollectionService submitCollectionService;

    @Mock
    SubmitCollectionValidationService validationService;

    @Mock
    InstitutionDetailsRepository institutionDetailsRepository;

    @Mock
    RepositoryService repositoryService;

    @Mock
    ItemDetailsRepository itemDetailsRepository;

    @Mock
    MarcUtil marcUtil;

    @Mock
    Record record;

    @Mock
    MarcToBibEntityConverter marcToBibEntityConverter;

    @Mock
    SCSBToBibEntityConverter scsbToBibEntityConverter;

    @Mock
    SubmitCollectionDAOService submitCollectionDAOService;

    @Mock
    SetupDataService setupDataService;

    @Mock
    SubmitCollectionReportHelperService submitCollectionReportHelperService;

    @Mock
    SubmitCollectionHelperService submitCollectionHelperService;

    @Mock
    ReportDetailRepository reportDetailRepository;

    @Mock
    CommonUtil commonUtil;

    @Mock
    BibRecords bibRecords;

    @Mock
    BibRecord bibRecord;

    @Mock
    RestTemplate restTemplate;

    @Mock
    Exchange exchange;

    @Mock
    ExecutorService executorService;

    @Mock
    BibliographicDetailsRepository bibliographicDetailsRepository;

    @Mock
    ApplicationContext applicationContext;

    @Value("${" + PropertyKeyConstants.SCSB_SOLR_DOC_URL + "}")
    private String scsbSolrClientUrl;


    @Value("${" + PropertyKeyConstants.NONHOLDINGID_INSTITUTION + "}")
    private String nonHoldingIdInstitution;



    private String bibMarcContentForPUL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<collection>\n" +
            "   <record>\n" +
            "      <leader>01302cas a2200361 a 4500</leader>\n" +
            "      <controlfield tag=\"001\">202304</controlfield>\n" +
            "      <controlfield tag=\"005\">20160526232735.0</controlfield>\n" +
            "      <controlfield tag=\"008\">830323c19819999iluqx p   gv  0    0eng d</controlfield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"010\">\n" +
            "         <subfield code=\"a\">82640039</subfield>\n" +
            "         <subfield code=\"z\">81640039</subfield>\n" +
            "         <subfield code=\"z\">sn 81001329</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"0\" ind2=\" \" tag=\"022\">\n" +
            "         <subfield code=\"a\">0276-9948</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"035\">\n" +
            "         <subfield code=\"a\">(OCoLC)7466281</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"035\">\n" +
            "         <subfield code=\"a\">(CStRLIN)NJPG83-S372</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"035\">\n" +
            "         <subfield code=\"9\">ABB7255TS-test</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"040\">\n" +
            "         <subfield code=\"a\">NSDP</subfield>\n" +
            "         <subfield code=\"d\">NjP</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"042\">\n" +
            "         <subfield code=\"a\">nsdp</subfield>\n" +
            "         <subfield code=\"a\">lc</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"043\">\n" +
            "         <subfield code=\"a\">n-us-il</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"0\" ind2=\"0\" tag=\"050\">\n" +
            "         <subfield code=\"a\">K25</subfield>\n" +
            "         <subfield code=\"b\">.N63</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\"0\" tag=\"222\">\n" +
            "         <subfield code=\"a\">University of Illinois law review</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"0\" ind2=\"0\" tag=\"245\">\n" +
            "         <subfield code=\"a\">University of Michigan.</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"3\" ind2=\"0\" tag=\"246\">\n" +
            "         <subfield code=\"a\">Law review</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"260\">\n" +
            "         <subfield code=\"a\">Champaign, IL :</subfield>\n" +
            "         <subfield code=\"b\">University of Illinois at Urbana-Champaign, College of Law,</subfield>\n" +
            "         <subfield code=\"c\">c1981-</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"300\">\n" +
            "         <subfield code=\"a\">v. ;</subfield>\n" +
            "         <subfield code=\"c\">27 cm.</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"310\">\n" +
            "         <subfield code=\"a\">5 times a year,</subfield>\n" +
            "         <subfield code=\"b\">2001-&amp;lt;2013&amp;gt;</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"321\">\n" +
            "         <subfield code=\"a\">Quarterly,</subfield>\n" +
            "         <subfield code=\"b\">1981-2000</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"0\" ind2=\" \" tag=\"362\">\n" +
            "         <subfield code=\"a\">Vol. 1981, no. 1-</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"588\">\n" +
            "         <subfield code=\"a\">Title from cover.</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"588\">\n" +
            "         <subfield code=\"a\">Latest issue consulted: Vol. 2013, no. 5.</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\"0\" tag=\"650\">\n" +
            "         <subfield code=\"a\">Law reviews</subfield>\n" +
            "         <subfield code=\"z\">Illinois.</subfield>\n" +
            "         <subfield code=\"0\">(uri)http://id.loc.gov/authorities/subjects/sh2009129243</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"2\" ind2=\" \" tag=\"710\">\n" +
            "         <subfield code=\"a\">University of Illinois at Urbana-Champaign.</subfield>\n" +
            "         <subfield code=\"b\">College of Law.</subfield>\n" +
            "         <subfield code=\"0\">(uri)http://id.loc.gov/authorities/names/n50049213</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\"0\" ind2=\"0\" tag=\"780\">\n" +
            "         <subfield code=\"t\">University of Illinois law forum</subfield>\n" +
            "         <subfield code=\"x\">0041-963X</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"998\">\n" +
            "         <subfield code=\"a\">09/09/94</subfield>\n" +
            "         <subfield code=\"s\">9110</subfield>\n" +
            "         <subfield code=\"n\">NjP</subfield>\n" +
            "         <subfield code=\"w\">DCLC82640039S</subfield>\n" +
            "         <subfield code=\"d\">03/23/83</subfield>\n" +
            "         <subfield code=\"c\">DLJ</subfield>\n" +
            "         <subfield code=\"b\">SZF</subfield>\n" +
            "         <subfield code=\"i\">940909</subfield>\n" +
            "         <subfield code=\"l\">NJPG</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"911\">\n" +
            "         <subfield code=\"a\">19940916</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\" \" tag=\"912\">\n" +
            "         <subfield code=\"a\">19970731060735.8</subfield>\n" +
            "      </datafield>\n" +
            "   </record>\n" +
            "</collection>";

    private String holdingMarcContentForPUL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<collection>\n" +
            "   <record>\n" +
            "      <datafield ind1=\"0\" ind2=\"1\" tag=\"852\">\n" +
            "         <subfield code=\"b\">rcppa</subfield>\n" +
            "         <subfield code=\"h\">K25.xN6</subfield>\n" +
            "      </datafield>\n" +
            "      <datafield ind1=\" \" ind2=\"0\" tag=\"866\">\n" +
            "         <subfield code=\"z\">Subscription cancelled with the last issue of 2013</subfield>\n" +
            "      </datafield>\n" +
            "   </record>\n" +
            "</collection>";

    private String updatedMarcForPUL = "<collection xmlns=\"http://www.loc.gov/MARC21/slim\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd\">\n" +
            "<record>\n" +
            "<leader>01302cas a2200361 a 4500</leader>\n" +
            "<controlfield tag=\"001\">202304</controlfield>\n" +
            "<controlfield tag=\"005\">20160526232735.0</controlfield>\n" +
            "<controlfield tag=\"008\">830323c19819999iluqx p gv 0 0eng d</controlfield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"010\">\n" +
            "<subfield code=\"a\">82640039</subfield>\n" +
            "<subfield code=\"z\">81640039</subfield>\n" +
            "<subfield code=\"z\">sn 81001329</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"0\" ind2=\" \" tag=\"022\">\n" +
            "<subfield code=\"a\">0276-9948</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"035\">\n" +
            "<subfield code=\"a\">(OCoLC)7466281</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"035\">\n" +
            "<subfield code=\"a\">(CStRLIN)NJPG83-S372</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"035\">\n" +
            "<subfield code=\"9\">ABB7255TS</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"040\">\n" +
            "<subfield code=\"a\">NSDP</subfield>\n" +
            "<subfield code=\"d\">NjP</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"042\">\n" +
            "<subfield code=\"a\">nsdp</subfield>\n" +
            "<subfield code=\"a\">lc</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"043\">\n" +
            "<subfield code=\"a\">n-us-il</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"0\" ind2=\"0\" tag=\"050\">\n" +
            "<subfield code=\"a\">K25</subfield>\n" +
            "<subfield code=\"b\">.N63</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\"0\" tag=\"222\">\n" +
            "<subfield code=\"a\">University of Illinois law review</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"0\" ind2=\"0\" tag=\"245\">\n" +
            "<subfield code=\"a\">University of Illinois law review.</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"3\" ind2=\"0\" tag=\"246\">\n" +
            "<subfield code=\"a\">Law review</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"260\">\n" +
            "<subfield code=\"a\">Champaign, IL :</subfield>\n" +
            "<subfield code=\"b\">\n" +
            "University of Illinois at Urbana-Champaign, College of Law,\n" +
            "</subfield>\n" +
            "<subfield code=\"c\">c1981-</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"300\">\n" +
            "<subfield code=\"a\">v. ;</subfield>\n" +
            "<subfield code=\"c\">27 cm.</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"310\">\n" +
            "<subfield code=\"a\">5 times a year,</subfield>\n" +
            "<subfield code=\"b\">2001-2013</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"321\">\n" +
            "<subfield code=\"a\">Quarterly,</subfield>\n" +
            "<subfield code=\"b\">1981-2000</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"0\" ind2=\" \" tag=\"362\">\n" +
            "<subfield code=\"a\">Vol. 1981, no. 1-</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"588\">\n" +
            "<subfield code=\"a\">Title from cover.</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"588\">\n" +
            "<subfield code=\"a\">Latest issue consulted: Vol. 2013, no. 5.</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\"0\" tag=\"650\">\n" +
            "<subfield code=\"a\">Law reviews</subfield>\n" +
            "<subfield code=\"z\">Illinois.</subfield>\n" +
            "<subfield code=\"0\">\n" +
            "(uri)http://id.loc.gov/authorities/subjects/sh2009129243\n" +
            "</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"2\" ind2=\" \" tag=\"710\">\n" +
            "<subfield code=\"a\">University of Illinois at Urbana-Champaign.</subfield>\n" +
            "<subfield code=\"b\">College of Law.</subfield>\n" +
            "<subfield code=\"0\">(uri)http://id.loc.gov/authorities/names/n50049213</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"0\" ind2=\"0\" tag=\"780\">\n" +
            "<subfield code=\"t\">University of Illinois law forum</subfield>\n" +
            "<subfield code=\"x\">0041-963X</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"998\">\n" +
            "<subfield code=\"a\">09/09/94</subfield>\n" +
            "<subfield code=\"s\">9110</subfield>\n" +
            "<subfield code=\"n\">NjP</subfield>\n" +
            "<subfield code=\"w\">DCLC82640039S</subfield>\n" +
            "<subfield code=\"d\">03/23/83</subfield>\n" +
            "<subfield code=\"c\">DLJ</subfield>\n" +
            "<subfield code=\"b\">SZF</subfield>\n" +
            "<subfield code=\"i\">940909</subfield>\n" +
            "<subfield code=\"l\">NJPG</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"911\">\n" +
            "<subfield code=\"a\">19940916</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"912\">\n" +
            "<subfield code=\"a\">19970731060735.0</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\"0\" tag=\"866\">\n" +
            "<subfield code=\"0\">222420</subfield>\n" +
            "<subfield code=\"a\">Vol. 1981, no. 1-v. 2013, no. 5</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\"0\" tag=\"866\">\n" +
            "<subfield code=\"0\">222420</subfield>\n" +
            "<subfield code=\"z\">LACKS: 2012, no. 1</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\"0\" tag=\"866\">\n" +
            "<subfield code=\"0\">222420</subfield>\n" +
            "<subfield code=\"x\">DESIGNATOR: year, no.</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\"0\" tag=\"866\">\n" +
            "<subfield code=\"0\">222420</subfield>\n" +
            "<subfield code=\"z\">Subscription cancelled with the last issue of 2013</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"959\">\n" +
            "<subfield code=\"a\">2000-06-13 00:00:00 -0500</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"0\" ind2=\"1\" tag=\"852\">\n" +
            "<subfield code=\"0\">222420</subfield>\n" +
            "<subfield code=\"b\">rcppa</subfield>\n" +
            "<subfield code=\"h\">K25 .xN5</subfield>\n" +
            "<subfield code=\"t\">1</subfield>\n" +
            "<subfield code=\"x\">tr fr f</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"0\" ind2=\"0\" tag=\"876\">\n" +
            "<subfield code=\"0\">222420</subfield>\n" +
            "<subfield code=\"a\">1110846</subfield>\n" +
            "<subfield code=\"h\"/>\n" +
            "<subfield code=\"j\">Not Charged</subfield>\n" +
            "<subfield code=\"p\">32101062128309</subfield>\n" +
            "<subfield code=\"t\">1</subfield>\n" +
            "<subfield code=\"x\">Open</subfield>\n" +
            "<subfield code=\"z\">PA</subfield>\n" +
            "</datafield>\n" +
            "</record>\n" +
            "</collection>";

    private String updatedMarcForPULWtihNewItem = "<collection xmlns=\"http://www.loc.gov/MARC21/slim\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd\">\n" +
            "<record>\n" +
            "<leader>01302cas a2200361 a 4500</leader>\n" +
            "<controlfield tag=\"001\">202305</controlfield>\n" +
            "<controlfield tag=\"005\">20160526232735.0</controlfield>\n" +
            "<controlfield tag=\"008\">830323c19819999iluqx p gv 0 0eng d</controlfield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"010\">\n" +
            "<subfield code=\"a\">82640039</subfield>\n" +
            "<subfield code=\"z\">81640039</subfield>\n" +
            "<subfield code=\"z\">sn 81001329</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"0\" ind2=\" \" tag=\"022\">\n" +
            "<subfield code=\"a\">0276-9948</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"035\">\n" +
            "<subfield code=\"a\">(OCoLC)7466281</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"035\">\n" +
            "<subfield code=\"a\">(CStRLIN)NJPG83-S372</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"035\">\n" +
            "<subfield code=\"9\">ABB7255TS</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"040\">\n" +
            "<subfield code=\"a\">NSDP</subfield>\n" +
            "<subfield code=\"d\">NjP</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"042\">\n" +
            "<subfield code=\"a\">nsdp</subfield>\n" +
            "<subfield code=\"a\">lc</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"043\">\n" +
            "<subfield code=\"a\">n-us-il</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"0\" ind2=\"0\" tag=\"050\">\n" +
            "<subfield code=\"a\">K25</subfield>\n" +
            "<subfield code=\"b\">.N63</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\"0\" tag=\"222\">\n" +
            "<subfield code=\"a\">University of Illinois law review</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"0\" ind2=\"0\" tag=\"245\">\n" +
            "<subfield code=\"a\">University of Illinois law review.</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"3\" ind2=\"0\" tag=\"246\">\n" +
            "<subfield code=\"a\">Law review</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"260\">\n" +
            "<subfield code=\"a\">Champaign, IL :</subfield>\n" +
            "<subfield code=\"b\">\n" +
            "University of Illinois at Urbana-Champaign, College of Law,\n" +
            "</subfield>\n" +
            "<subfield code=\"c\">c1981-</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"300\">\n" +
            "<subfield code=\"a\">v. ;</subfield>\n" +
            "<subfield code=\"c\">27 cm.</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"310\">\n" +
            "<subfield code=\"a\">5 times a year,</subfield>\n" +
            "<subfield code=\"b\">2001-2013</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"321\">\n" +
            "<subfield code=\"a\">Quarterly,</subfield>\n" +
            "<subfield code=\"b\">1981-2000</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"0\" ind2=\" \" tag=\"362\">\n" +
            "<subfield code=\"a\">Vol. 1981, no. 1-</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"588\">\n" +
            "<subfield code=\"a\">Title from cover.</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"588\">\n" +
            "<subfield code=\"a\">Latest issue consulted: Vol. 2013, no. 5.</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\"0\" tag=\"650\">\n" +
            "<subfield code=\"a\">Law reviews</subfield>\n" +
            "<subfield code=\"z\">Illinois.</subfield>\n" +
            "<subfield code=\"0\">\n" +
            "(uri)http://id.loc.gov/authorities/subjects/sh2009129243\n" +
            "</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"2\" ind2=\" \" tag=\"710\">\n" +
            "<subfield code=\"a\">University of Illinois at Urbana-Champaign.</subfield>\n" +
            "<subfield code=\"b\">College of Law.</subfield>\n" +
            "<subfield code=\"0\">(uri)http://id.loc.gov/authorities/names/n50049213</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"0\" ind2=\"0\" tag=\"780\">\n" +
            "<subfield code=\"t\">University of Illinois law forum</subfield>\n" +
            "<subfield code=\"x\">0041-963X</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"998\">\n" +
            "<subfield code=\"a\">09/09/94</subfield>\n" +
            "<subfield code=\"s\">9110</subfield>\n" +
            "<subfield code=\"n\">NjP</subfield>\n" +
            "<subfield code=\"w\">DCLC82640039S</subfield>\n" +
            "<subfield code=\"d\">03/23/83</subfield>\n" +
            "<subfield code=\"c\">DLJ</subfield>\n" +
            "<subfield code=\"b\">SZF</subfield>\n" +
            "<subfield code=\"i\">940909</subfield>\n" +
            "<subfield code=\"l\">NJPG</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"911\">\n" +
            "<subfield code=\"a\">19940916</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"912\">\n" +
            "<subfield code=\"a\">19970731060735.0</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\"0\" tag=\"866\">\n" +
            "<subfield code=\"0\">222420</subfield>\n" +
            "<subfield code=\"a\">Vol. 1981, no. 1-v. 2013, no. 5</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\"0\" tag=\"866\">\n" +
            "<subfield code=\"0\">222420</subfield>\n" +
            "<subfield code=\"z\">LACKS: 2012, no. 1</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\"0\" tag=\"866\">\n" +
            "<subfield code=\"0\">222420</subfield>\n" +
            "<subfield code=\"x\">DESIGNATOR: year, no.</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\"0\" tag=\"866\">\n" +
            "<subfield code=\"0\">222420</subfield>\n" +
            "<subfield code=\"z\">Subscription cancelled with the last issue of 2013</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\" \" ind2=\" \" tag=\"959\">\n" +
            "<subfield code=\"a\">2000-06-13 00:00:00 -0500</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"0\" ind2=\"1\" tag=\"852\">\n" +
            "<subfield code=\"0\">222420</subfield>\n" +
            "<subfield code=\"b\">rcppa</subfield>\n" +
            "<subfield code=\"h\">K25 .xN5</subfield>\n" +
            "<subfield code=\"t\">1</subfield>\n" +
            "<subfield code=\"x\">tr fr f</subfield>\n" +
            "</datafield>\n" +
            "<datafield ind1=\"0\" ind2=\"0\" tag=\"876\">\n" +
            "<subfield code=\"0\">222420</subfield>\n" +
            "<subfield code=\"a\">1110847</subfield>\n" +
            "<subfield code=\"h\"/>\n" +
            "<subfield code=\"j\">Not Charged</subfield>\n" +
            "<subfield code=\"p\">32101barcode</subfield>\n" +
            "<subfield code=\"t\">1</subfield>\n" +
            "<subfield code=\"x\">Open</subfield>\n" +
            "<subfield code=\"z\">PA</subfield>\n" +
            "</datafield>\n" +
            "</record>\n" +
            "</collection>";

    private String bibMarcContentForNYPL1 =  "<collection xmlns=\"http://www.loc.gov/MARC21/slim\">\n" +
            "               <record>\n" +
            "                  <controlfield tag=\"001\">NYPG001000005-B</controlfield>\n" +
            "                  <controlfield tag=\"005\">20001116192418.8</controlfield>\n" +
            "                  <controlfield tag=\"008\">841106s1970 le b 000 0bara dcam i</controlfield>\n" +
            "                  <datafield ind1=\"1\" ind2=\" \" tag=\"100\">\n" +
            "                     <subfield code=\"a\">Ḥāwī, Īlīyā Salīm.</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\"8\" ind2=\" \" tag=\"952\">\n" +
            "                     <subfield code=\"h\">*OFS 84-1997</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\"1\" ind2=\"0\" tag=\"600\">\n" +
            "                     <subfield code=\"a\">Ḥuṭayʼah, Jarwal ibn Aws,</subfield>\n" +
            "                     <subfield code=\"d\">d. 650?</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\" \" ind2=\" \" tag=\"504\">\n" +
            "                     <subfield code=\"a\">Bibliography: p.221.</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\" \" ind2=\" \" tag=\"546\">\n" +
            "                     <subfield code=\"a\">In English.</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\" \" ind2=\" \" tag=\"260\">\n" +
            "                     <subfield code=\"a\">Bayrūt :</subfield>\n" +
            "                     <subfield code=\"b\">Dār al-Thaqāfah,</subfield>\n" +
            "                     <subfield code=\"c\">1970.</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\"8\" ind2=\" \" tag=\"952\">\n" +
            "                     <subfield code=\"h\">*OFS 84-1997</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\" \" ind2=\" \" tag=\"300\">\n" +
            "                     <subfield code=\"a\">223 p. ;</subfield>\n" +
            "                     <subfield code=\"c\">25cm.</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\"1\" ind2=\"3\" tag=\"245\">\n" +
            "                     <subfield code=\"a\">al-Ḥuṭayʼah :</subfield>\n" +
            "                     <subfield code=\"b\">fī sīratihi wa-nafsīyatihi wa-shiʻrihi /</subfield>\n" +
            "                     <subfield code=\"c\">bi-qalam Īlīyā Ḥāwī.</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\" \" ind2=\" \" tag=\"959\">\n" +
            "                     <subfield code=\"a\">.b10000136</subfield>\n" +
            "                     <subfield code=\"b\">07-18-08</subfield>\n" +
            "                     <subfield code=\"c\">07-29-91</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\" \" ind2=\" \" tag=\"040\">\n" +
            "                     <subfield code=\"a\">NN</subfield>\n" +
            "                     <subfield code=\"c\">NN</subfield>\n" +
            "                     <subfield code=\"d\">WaOLN</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\" \" ind2=\" \" tag=\"997\">\n" +
            "                     <subfield code=\"a\">ho</subfield>\n" +
            "                     <subfield code=\"b\">12-15-00</subfield>\n" +
            "                     <subfield code=\"c\">m</subfield>\n" +
            "                     <subfield code=\"d\">a</subfield>\n" +
            "                     <subfield code=\"e\">-</subfield>\n" +
            "                     <subfield code=\"f\">ara</subfield>\n" +
            "                     <subfield code=\"g\">le</subfield>\n" +
            "                     <subfield code=\"h\">3</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\"0\" ind2=\"0\" tag=\"907\">\n" +
            "                     <subfield code=\"a\">.b100000125</subfield>\n" +
            "                  </datafield>\n" +
            "                  <leader>00000cam 2200217 i 4500</leader>\n" +
            "               </record>\n" +
            "            </collection>\n" ;

    private String holdingContentForNYPL1 =             "               <collection xmlns=\"http://www.loc.gov/MARC21/slim\">\n" +
            "                  <record>\n" +
            "                     <datafield ind1=\"8\" ind2=\" \" tag=\"852\">\n" +
            "                        <subfield code=\"b\">rcma2</subfield>\n" +
            "                        <subfield code=\"h\">*OFS 84-1998</subfield>\n" +
            "                     </datafield>\n" +
            "                     <datafield ind1=\" \" ind2=\" \" tag=\"866\">\n" +
            "                        <subfield code=\"a\" />\n" +
            "                     </datafield>\n" +
            "                  </record>\n" +
            "               </collection>\n";

    private String updatedContentForNYPL1 ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<bibRecords>\n" +
            "   <bibRecord>\n" +
            "      <bib>\n" +
            "         <owningInstitutionId>NYPL</owningInstitutionId>\n" +
            "         <owningInstitutionBibId>.b100000125</owningInstitutionBibId>\n" +
            "         <content>\n" +
            "            <collection xmlns=\"http://www.loc.gov/MARC21/slim\">\n" +
            "               <record>\n" +
            "                  <controlfield tag=\"001\">NYPG001000005-B</controlfield>\n" +
            "                  <controlfield tag=\"005\">20001116192418.8</controlfield>\n" +
            "                  <controlfield tag=\"008\">841106s1970 le b 000 0bara dcam i</controlfield>\n" +
            "                  <datafield ind1=\"1\" ind2=\" \" tag=\"100\">\n" +
            "                     <subfield code=\"a\">Ḥāwī, Īlīyā Salīm.</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\"8\" ind2=\" \" tag=\"952\">\n" +
            "                     <subfield code=\"h\">*OFS 84-1997</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\"1\" ind2=\"0\" tag=\"600\">\n" +
            "                     <subfield code=\"a\">Ḥuṭayʼah, Jarwal ibn Aws,</subfield>\n" +
            "                     <subfield code=\"d\">d. 650?</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\" \" ind2=\" \" tag=\"504\">\n" +
            "                     <subfield code=\"a\">Bibliography: p.221.</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\" \" ind2=\" \" tag=\"546\">\n" +
            "                     <subfield code=\"a\">In Arabic.</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\" \" ind2=\" \" tag=\"260\">\n" +
            "                     <subfield code=\"a\">Bayrūt :</subfield>\n" +
            "                     <subfield code=\"b\">Dār al-Thaqāfah,</subfield>\n" +
            "                     <subfield code=\"c\">1970.</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\"8\" ind2=\" \" tag=\"952\">\n" +
            "                     <subfield code=\"h\">*OFS 84-1997</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\" \" ind2=\" \" tag=\"300\">\n" +
            "                     <subfield code=\"a\">223 p. ;</subfield>\n" +
            "                     <subfield code=\"c\">25cm.</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\"1\" ind2=\"3\" tag=\"245\">\n" +
            "                     <subfield code=\"a\">al-Ḥuṭayʼah :</subfield>\n" +
            "                     <subfield code=\"b\">fī sīratihi wa-nafsīyatihi wa-shiʻrihi /</subfield>\n" +
            "                     <subfield code=\"c\">bi-qalam Īlīyā Ḥāwī.</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\" \" ind2=\" \" tag=\"959\">\n" +
            "                     <subfield code=\"a\">.b10000136</subfield>\n" +
            "                     <subfield code=\"b\">07-18-08</subfield>\n" +
            "                     <subfield code=\"c\">07-29-91</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\" \" ind2=\" \" tag=\"040\">\n" +
            "                     <subfield code=\"a\">NN</subfield>\n" +
            "                     <subfield code=\"c\">NN</subfield>\n" +
            "                     <subfield code=\"d\">WaOLN</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\" \" ind2=\" \" tag=\"997\">\n" +
            "                     <subfield code=\"a\">ho</subfield>\n" +
            "                     <subfield code=\"b\">12-15-00</subfield>\n" +
            "                     <subfield code=\"c\">m</subfield>\n" +
            "                     <subfield code=\"d\">a</subfield>\n" +
            "                     <subfield code=\"e\">-</subfield>\n" +
            "                     <subfield code=\"f\">ara</subfield>\n" +
            "                     <subfield code=\"g\">le</subfield>\n" +
            "                     <subfield code=\"h\">3</subfield>\n" +
            "                  </datafield>\n" +
            "                  <datafield ind1=\"0\" ind2=\"0\" tag=\"907\">\n" +
            "                     <subfield code=\"a\">.b100000125</subfield>\n" +
            "                  </datafield>\n" +
            "                  <leader>00000cam a2200217 i 4500</leader>\n" +
            "               </record>\n" +
            "            </collection>\n" +
            "         </content>\n" +
            "      </bib>\n" +
            "      <holdings>\n" +
            "         <holding>\n" +
            "            <owningInstitutionHoldingsId />\n" +
            "            <content>\n" +
            "               <collection xmlns=\"http://www.loc.gov/MARC21/slim\">\n" +
            "                  <record>\n" +
            "                     <datafield ind1=\"8\" ind2=\" \" tag=\"852\">\n" +
            "                        <subfield code=\"b\">rcma2</subfield>\n" +
            "                        <subfield code=\"h\">*OFS 84-1997</subfield>\n" +
            "                     </datafield>\n" +
            "                     <datafield ind1=\" \" ind2=\" \" tag=\"866\">\n" +
            "                        <subfield code=\"a\" />\n" +
            "                     </datafield>\n" +
            "                  </record>\n" +
            "               </collection>\n" +
            "            </content>\n" +
            "            <items>\n" +
            "               <content>\n" +
            "                  <collection xmlns=\"http://www.loc.gov/MARC21/slim\">\n" +
            "                     <record>\n" +
            "                        <datafield ind1=\" \" ind2=\" \" tag=\"876\">\n" +
            "                           <subfield code=\"p\">33433014514719</subfield>\n" +
            "                           <subfield code=\"h\">In Library Use</subfield>\n" +
            "                           <subfield code=\"a\">.i100000034</subfield>\n" +
            "                           <subfield code=\"j\">Available</subfield>\n" +
            "                           <subfield code=\"t\">1</subfield>\n" +
            "                        </datafield>\n" +
            "                        <datafield ind1=\" \" ind2=\" \" tag=\"900\">\n" +
            "                           <subfield code=\"a\">Open</subfield>\n" +
            "                           <subfield code=\"b\">NA</subfield>\n" +
            "                        </datafield>\n" +
            "                     </record>\n" +
            "                  </collection>\n" +
            "               </content>\n" +
            "            </items>\n" +
            "         </holding>\n" +
            "      </holdings>\n" +
            "   </bibRecord>\n" +
            "</bibRecords>";

    private String dummyBibMarcContent = "<collection>\n" +
            "    <record>\n" +
            "        <leader>01893cam a2200361 a 4500</leader>\n" +
            "        <controlfield tag=\"001\">Dummy</controlfield>\n" +
            "        <controlfield tag=\"005\">dummydummydumm.y</controlfield>\n" +
            "        <controlfield tag=\"008\">dummydummydummydummydummydummydummydummy</controlfield>\n" +
            "        <datafield ind1=\" \" ind2=\" \" tag=\"010\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\" \" ind2=\" \" tag=\"020\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\" \" ind2=\" \" tag=\"020\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\" \" ind2=\" \" tag=\"035\">\n" +
            "            <subfield code=\"a\">(OCoLC)Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\" \" ind2=\" \" tag=\"040\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\" \" ind2=\" \" tag=\"042\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\" \" ind2=\" \" tag=\"043\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\"0\" ind2=\"0\" tag=\"050\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "            <subfield code=\"b\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\"0\" ind2=\"0\" tag=\"245\">\n" +
            "            <subfield code=\"a\">Dummy Title</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\" \" ind2=\" \" tag=\"260\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\" \" ind2=\" \" tag=\"300\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\" \" ind2=\" \" tag=\"500\">\n" +
            "            <subfield code=\"a\">\"Dummy\"</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\" \" ind2=\" \" tag=\"500\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\" \" ind2=\" \" tag=\"504\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\"2\" ind2=\"0\" tag=\"610\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "            <subfield code=\"x\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\"2\" ind2=\"0\" tag=\"610\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "            <subfield code=\"x\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\" \" ind2=\"0\" tag=\"650\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "            <subfield code=\"z\">Dummy</subfield>\n" +
            "            <subfield code=\"x\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\"1\" ind2=\" \" tag=\"700\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\"2\" ind2=\" \" tag=\"710\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "            <subfield code=\"0\">(uri)http://id.loc.gov/authorities/names/no</subfield>\n" +
            "        </datafield>\n" +
            "        <datafield ind1=\" \" ind2=\" \" tag=\"999\">\n" +
            "            <subfield code=\"a\">Dummy</subfield>\n" +
            "        </datafield>\n" +
            "    </record>\n" +
            "</collection>";

    private String dummyHoldingMarcContent = "<collection>\n" +
            "    <record>\n" +
            "        <datafield ind1=\"0\" ind2=\"0\" tag=\"852\">\n" +
            "            <subfield code=\"b\">dummy</subfield>\n" +
            "            <subfield code=\"h\">dummydummydummy</subfield>\n" +
            "        </datafield>\n" +
            "    </record>\n" +
            "</collection>";

    @Test
    public void processForPUL() throws JAXBException {
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity(1,"202304","222420","1110846",1,"32101062128309",bibMarcContentForPUL,holdingMarcContentForPUL, ScsbCommonConstants.INCOMPLETE_STATUS);
        Set<Integer> processedBibIds = new HashSet<>();
        Map<String,String> idMapToRemoveIndex = new HashMap<>();
        Map<String,String> bibIdMapToRemoveIndex = new HashMap<>();
        List<Integer> reportRecordNumList = new ArrayList<>();
        Mockito.when(validationService.validateInstitution(Mockito.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(repositoryService,"institutionDetailsRepository",institutionDetailsRepository);
        Mockito.when(repositoryService.getInstitutionDetailsRepository()).thenCallRealMethod();
        Mockito.when(repositoryService.getReportDetailRepository()).thenCallRealMethod();
        Mockito.when(institutionDetailsRepository.findByInstitutionCode(Mockito.anyString())).thenReturn(getInstitutionEntity());
        ReflectionTestUtils.setField(marcUtil,"inputLimit",1);
        Mockito.when(marcUtil.convertAndValidateXml(Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyList())).thenCallRealMethod();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenCallRealMethod();
        Map responseMap=new HashMap();
        StringBuilder stringBuilder=new StringBuilder();
        responseMap.put("errorMessage",stringBuilder);
        responseMap.put(ScsbCommonConstants.BIBLIOGRAPHICENTITY,savedBibliographicEntity);
        ReflectionTestUtils.setField(repositoryService,"itemDetailsRepository",itemDetailsRepository);
        ReflectionTestUtils.setField(repositoryService,"reportDetailRepository",reportDetailRepository);
        Mockito.when(repositoryService.getItemDetailsRepository()).thenCallRealMethod();
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(savedBibliographicEntity.getItemEntities());
        Mockito.when(marcToBibEntityConverter.convert(any(), any())).thenReturn(responseMap);
        ReflectionTestUtils.setField(submitCollectionDAOService,"repositoryService",repositoryService);
        Mockito.when(submitCollectionDAOService.updateBibliographicEntity(any(),any(),any(),any(),anyBoolean(),any(),any())).thenReturn(savedBibliographicEntity);
        ReflectionTestUtils.setField(submitCollectionDAOService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionDAOService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionDAOService,"submitCollectionReportHelperService",submitCollectionReportHelperService);
        ReflectionTestUtils.setField(submitCollectionDAOService,"marcUtil",marcUtil);
        ReflectionTestUtils.setField(submitCollectionDAOService,"bibliographicDetailsRepository",bibliographicDetailsRepository);
        ReflectionTestUtils.setField(submitCollectionDAOService,"submitCollectionHelperService",submitCollectionHelperService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"repositoryService",repositoryService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"submitCollectionHelperService",submitCollectionHelperService);
        Map<Integer,String> institutionEntityMap=new HashMap<>();
        institutionEntityMap.put(1,"PUL");
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(institutionEntityMap);
        Mockito.when(submitCollectionReportHelperService.buildSubmitCollectionReportInfo(Mockito.anyMap(), any(), any())).thenCallRealMethod();
        Map<String,Map<String,ItemEntity>> fetchedHoldingItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        ItemEntity itemEntity=savedBibliographicEntity.getItemEntities().get(0);
        itemEntity.setUseRestrictions("no");
        itemEntityMap.put("1",itemEntity);
        fetchedHoldingItemMap.put("1",itemEntityMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(any())).thenReturn(fetchedHoldingItemMap);
        ReportEntity savedReportEntity=new ReportEntity();
        savedReportEntity.setId(1);
        List<Future> futures = new ArrayList<>();
        Mockito.when(reportDetailRepository.save(any())).thenReturn(savedReportEntity);
        List<SubmitCollectionResponse>  submitCollectionResponseList = submitCollectionService.process("PUL",updatedMarcForPUL,processedBibIds,Arrays.asList(idMapToRemoveIndex), Arrays.asList(bibIdMapToRemoveIndex), ScsbConstants.REST,reportRecordNumList, true,false,null, null,executorService,futures);
        //assertEquals(ScsbConstants.SUBMIT_COLLECTION_SUCCESS_RECORD,submitCollectionResponseList.get(0).getMessage());
        String updatedBibMarcXML = new String(savedBibliographicEntity.getContent(), StandardCharsets.UTF_8);
        List<Record> bibRecordList = readMarcXml(updatedBibMarcXML);
        assertNotNull(bibRecordList);
        DataField field912 = (DataField)bibRecordList.get(0).getVariableField("912");
        assertEquals("19970731060735.8", field912.getSubfield('a').getData());
        HoldingsEntity holdingsEntity = savedBibliographicEntity.getHoldingsEntities().get(0);
        String updatedHoldingMarcXML = new String(holdingsEntity.getContent(),StandardCharsets.UTF_8);
        List<Record> holdingRecordList = readMarcXml(updatedHoldingMarcXML);
        log.info("updatedHoldingMarcXML-->"+updatedHoldingMarcXML);
        TestCase.assertNotNull(holdingRecordList);
        DataField field852 = (DataField)holdingRecordList.get(0).getVariableField("852");
        assertEquals("K25.xN6", field852.getSubfield('h').getData());
        String callNumber = savedBibliographicEntity.getItemEntities().get(0).getCallNumber();
        assertEquals("K25 .xN5",callNumber);
        Integer collectionGroupId = savedBibliographicEntity.getItemEntities().get(0).getCollectionGroupId();
        assertEquals(new Integer(1),collectionGroupId);
    }

    @Test
    public void generateSubmitCollectionReportFile() {
        List<Integer> reportRecordNumberList=new ArrayList<>();
        reportRecordNumberList.add(1);
        submitCollectionService.generateSubmitCollectionReportFile(reportRecordNumberList);
        assertTrue(true);
    }

    @Test
    public void partialIndexData()  {
        Set<Integer> bibliographicIdList=new HashSet<>();
        Mockito.doReturn(ScsbCommonConstants.SUCCESS).when(restTemplate).postForObject( ArgumentMatchers.anyString(),
                any(),
                ArgumentMatchers.<Class<Map>>any());
        String partialindex=submitCollectionService.partialIndexData(bibliographicIdList);
        assertEquals(ScsbCommonConstants.SUCCESS,partialindex);
    }

    @Test
    public void processForCUL() throws JAXBException {
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity(1,"202304","222420","1110846",1,"32101062128309",bibMarcContentForPUL,holdingMarcContentForPUL, ScsbCommonConstants.INCOMPLETE_STATUS);
        Set<Integer> processedBibIds = new HashSet<>();
        Map<String,String> idMapToRemoveIndex = new HashMap<>();
        Map<String,String> bibIdMapToRemoveIndex = new HashMap<>();
        List<Integer> reportRecordNumList = new ArrayList<>();
        Mockito.when(validationService.validateInstitution(Mockito.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(repositoryService,"institutionDetailsRepository",institutionDetailsRepository);
        Mockito.when(repositoryService.getInstitutionDetailsRepository()).thenCallRealMethod();
        Mockito.when(repositoryService.getReportDetailRepository()).thenCallRealMethod();
        Mockito.when(institutionDetailsRepository.findByInstitutionCode(Mockito.anyString())).thenReturn(getInstitutionEntity());
        ReflectionTestUtils.setField(marcUtil,"inputLimit",1);
        Mockito.when(marcUtil.convertAndValidateXml(Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyList())).thenCallRealMethod();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenCallRealMethod();
        Map responseMap=new HashMap();
        StringBuilder stringBuilder=new StringBuilder();
        responseMap.put("errorMessage",stringBuilder);
        responseMap.put(ScsbCommonConstants.BIBLIOGRAPHICENTITY,savedBibliographicEntity);
        ReflectionTestUtils.setField(repositoryService,"itemDetailsRepository",itemDetailsRepository);
        ReflectionTestUtils.setField(repositoryService,"reportDetailRepository",reportDetailRepository);
        Mockito.when(repositoryService.getItemDetailsRepository()).thenCallRealMethod();
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(savedBibliographicEntity.getItemEntities());
        Mockito.when(marcToBibEntityConverter.convert(any(), any())).thenReturn(responseMap);
        ReflectionTestUtils.setField(submitCollectionDAOService,"repositoryService",repositoryService);
        Mockito.when(submitCollectionDAOService.updateBibliographicEntity(any(),Mockito.anyMap(),Mockito.anyList(), anySet(),Mockito.anyBoolean(), any(), any())).thenReturn(savedBibliographicEntity);
        ReflectionTestUtils.setField(submitCollectionDAOService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionDAOService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionDAOService,"submitCollectionReportHelperService",submitCollectionReportHelperService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"repositoryService",repositoryService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"submitCollectionHelperService",submitCollectionHelperService);
        Map<Integer,String> institutionEntityMap=new HashMap<>();
        institutionEntityMap.put(1,"PUL");
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(institutionEntityMap);
        Mockito.when(submitCollectionReportHelperService.buildSubmitCollectionReportInfo(Mockito.anyMap(), any(), any())).thenCallRealMethod();
        Map<String,Map<String,ItemEntity>> fetchedHoldingItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        ItemEntity itemEntity=savedBibliographicEntity.getItemEntities().get(0);
        itemEntity.setUseRestrictions("no");
        itemEntityMap.put("1",itemEntity);
        fetchedHoldingItemMap.put("1",itemEntityMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(any())).thenReturn(fetchedHoldingItemMap);
        ReportEntity savedReportEntity=new ReportEntity();
        savedReportEntity.setId(1);
        List<Future> futures = new ArrayList<>();
        Mockito.when(reportDetailRepository.save(any())).thenReturn(savedReportEntity);
        List<SubmitCollectionResponse>  submitCollectionResponseList = submitCollectionService.process("PUL",updatedMarcForPUL,processedBibIds,Arrays.asList(idMapToRemoveIndex), Arrays.asList(bibIdMapToRemoveIndex), ScsbConstants.REST,reportRecordNumList, true,false,null, null,executorService,futures);
        assertNotNull(submitCollectionResponseList);

    }

    @Test
    public void processForPULXMLError() throws JAXBException {
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity(1,"202304","222420","1110846",1,"32101062128309",bibMarcContentForPUL,holdingMarcContentForPUL, ScsbCommonConstants.INCOMPLETE_STATUS);
        Set<Integer> processedBibIds = new HashSet<>();
        Map<String,String> idMapToRemoveIndex = new HashMap<>();
        Map<String,String> bibIdMapToRemoveIndex = new HashMap<>();
        List<Integer> reportRecordNumList = new ArrayList<>();
        Mockito.when(validationService.validateInstitution(Mockito.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(repositoryService,"institutionDetailsRepository",institutionDetailsRepository);
        Mockito.when(repositoryService.getInstitutionDetailsRepository()).thenCallRealMethod();
        Mockito.when(repositoryService.getReportDetailRepository()).thenCallRealMethod();
        Mockito.when(institutionDetailsRepository.findByInstitutionCode(Mockito.anyString())).thenReturn(getInstitutionEntity());
        ReflectionTestUtils.setField(marcUtil,"inputLimit",1);
        Mockito.when(marcUtil.convertAndValidateXml(Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyList())).thenCallRealMethod();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenCallRealMethod();
        Map responseMap=new HashMap();
        StringBuilder stringBuilder=new StringBuilder();
        stringBuilder.append("Error while parsing xml for a barcode in submit collection");
        responseMap.put("errorMessage",stringBuilder);
        responseMap.put(ScsbCommonConstants.BIBLIOGRAPHICENTITY,savedBibliographicEntity);
        ReflectionTestUtils.setField(repositoryService,"itemDetailsRepository",itemDetailsRepository);
        ReflectionTestUtils.setField(repositoryService,"reportDetailRepository",reportDetailRepository);
        Mockito.when(repositoryService.getItemDetailsRepository()).thenCallRealMethod();
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(savedBibliographicEntity.getItemEntities());
        Mockito.when(marcToBibEntityConverter.convert(any(), any())).thenReturn(responseMap);
        ReflectionTestUtils.setField(submitCollectionDAOService,"repositoryService",repositoryService);
        Mockito.when(submitCollectionDAOService.updateBibliographicEntity(any(),Mockito.anyMap(),Mockito.anyList(), anySet(),Mockito.anyBoolean(), any(), any())).thenCallRealMethod();
        ReflectionTestUtils.setField(submitCollectionDAOService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionDAOService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionDAOService,"submitCollectionReportHelperService",submitCollectionReportHelperService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"repositoryService",repositoryService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"submitCollectionHelperService",submitCollectionHelperService);
        Map<Integer,String> institutionEntityMap=new HashMap<>();
        institutionEntityMap.put(1,"PUL");
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(institutionEntityMap);
        Mockito.when(submitCollectionReportHelperService.buildSubmitCollectionReportInfo(Mockito.anyMap(), any(), any())).thenCallRealMethod();
        Map<String,Map<String,ItemEntity>> fetchedHoldingItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        ItemEntity itemEntity=savedBibliographicEntity.getItemEntities().get(0);
        itemEntity.setUseRestrictions("no");
        itemEntityMap.put("1",itemEntity);
        fetchedHoldingItemMap.put("1",itemEntityMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(any())).thenReturn(fetchedHoldingItemMap);
        ReportEntity savedReportEntity=new ReportEntity();
        savedReportEntity.setId(1);
        List<Future> futures = new ArrayList<>();
        Mockito.when(reportDetailRepository.save(any())).thenReturn(savedReportEntity);
        Mockito.doCallRealMethod().when(submitCollectionReportHelperService).setSubmitCollectionFailureReportForUnexpectedException(any(),Mockito.anyList(),Mockito.anyString(), any());
        List<SubmitCollectionResponse>  submitCollectionResponseList = submitCollectionService.process("PUL",updatedMarcForPUL,processedBibIds,Arrays.asList(idMapToRemoveIndex), Arrays.asList(bibIdMapToRemoveIndex), ScsbConstants.REST,reportRecordNumList, true,false,null, null,executorService,futures);
        assertEquals("Failed record - Item not updated - "+"Error while parsing xml for a barcode in submit collection",submitCollectionResponseList.get(0).getMessage());
    }

    @Test
    public void processForPULXMLUnknownError() throws JAXBException {
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity(1,"202304","222420","1110846",1,"32101062128309",bibMarcContentForPUL,holdingMarcContentForPUL, ScsbCommonConstants.INCOMPLETE_STATUS);
        Set<Integer> processedBibIds = new HashSet<>();
        Map<String,String> idMapToRemoveIndex = new HashMap<>();
        Map<String,String> bibIdMapToRemoveIndex = new HashMap<>();
        List<Integer> reportRecordNumList = new ArrayList<>();
        Mockito.when(validationService.validateInstitution(Mockito.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(repositoryService,"institutionDetailsRepository",institutionDetailsRepository);
        Mockito.when(repositoryService.getInstitutionDetailsRepository()).thenCallRealMethod();
        Mockito.when(repositoryService.getReportDetailRepository()).thenCallRealMethod();
        Mockito.when(institutionDetailsRepository.findByInstitutionCode(Mockito.anyString())).thenReturn(getInstitutionEntity());
        ReflectionTestUtils.setField(marcUtil,"inputLimit",1);
        Mockito.when(marcUtil.convertAndValidateXml(Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyList())).thenCallRealMethod();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenCallRealMethod();
        Map responseMap=new HashMap();
        responseMap.put("errorMessage",null);
        responseMap.put(ScsbCommonConstants.BIBLIOGRAPHICENTITY,savedBibliographicEntity);
        ReflectionTestUtils.setField(repositoryService,"itemDetailsRepository",itemDetailsRepository);
        ReflectionTestUtils.setField(repositoryService,"reportDetailRepository",reportDetailRepository);
        Mockito.when(repositoryService.getItemDetailsRepository()).thenCallRealMethod();
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(savedBibliographicEntity.getItemEntities());
        Mockito.when(marcToBibEntityConverter.convert(any(), any())).thenReturn(responseMap);
        ReflectionTestUtils.setField(submitCollectionDAOService,"repositoryService",repositoryService);
        Mockito.when(submitCollectionDAOService.updateBibliographicEntity(any(),Mockito.anyMap(),Mockito.anyList(), anySet(),Mockito.anyBoolean(), any(), any())).thenCallRealMethod();
        ReflectionTestUtils.setField(submitCollectionDAOService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionDAOService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionDAOService,"submitCollectionReportHelperService",submitCollectionReportHelperService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"repositoryService",repositoryService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"submitCollectionHelperService",submitCollectionHelperService);
        Map<Integer,String> institutionEntityMap=new HashMap<>();
        institutionEntityMap.put(1,"PUL");
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(institutionEntityMap);
        Mockito.when(submitCollectionReportHelperService.buildSubmitCollectionReportInfo(Mockito.anyMap(), any(), any())).thenCallRealMethod();
        Map<String,Map<String,ItemEntity>> fetchedHoldingItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        ItemEntity itemEntity=savedBibliographicEntity.getItemEntities().get(0);
        itemEntity.setUseRestrictions("no");
        itemEntityMap.put("1",itemEntity);
        fetchedHoldingItemMap.put("1",itemEntityMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(any())).thenReturn(fetchedHoldingItemMap);
        ReportEntity savedReportEntity=new ReportEntity();
        savedReportEntity.setId(1);
        List<Future> futures = new ArrayList<>();
        Mockito.when(reportDetailRepository.save(any())).thenReturn(savedReportEntity);
        Mockito.doCallRealMethod().when(submitCollectionReportHelperService).setSubmitCollectionFailureReportForUnexpectedException(any(),Mockito.anyList(),Mockito.anyString(), any());
        List<SubmitCollectionResponse>  submitCollectionResponseList = submitCollectionService.process("PUL",updatedMarcForPUL,processedBibIds,Arrays.asList(idMapToRemoveIndex), Arrays.asList(bibIdMapToRemoveIndex), ScsbConstants.REST,reportRecordNumList, true,false,null, null,executorService,futures);
        assertEquals("Failed record - Item not updated - ",submitCollectionResponseList.get(0).getMessage());
    }

    @Test
    public void processForPULException() throws JAXBException {
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity(1,"202304","222420","1110846",1,"32101062128309",bibMarcContentForPUL,holdingMarcContentForPUL, ScsbCommonConstants.INCOMPLETE_STATUS);
        Set<Integer> processedBibIds = new HashSet<>();
        Map<String,String> idMapToRemoveIndex = new HashMap<>();
        Map<String,String> bibIdMapToRemoveIndex = new HashMap<>();
        List<Integer> reportRecordNumList = new ArrayList<>();
        Mockito.when(validationService.validateInstitution(Mockito.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(repositoryService,"institutionDetailsRepository",institutionDetailsRepository);
        Mockito.when(repositoryService.getInstitutionDetailsRepository()).thenCallRealMethod();
        Mockito.when(repositoryService.getReportDetailRepository()).thenCallRealMethod();
        Mockito.when(institutionDetailsRepository.findByInstitutionCode(Mockito.anyString())).thenReturn(getInstitutionEntity());
        ReflectionTestUtils.setField(marcUtil,"inputLimit",1);
        Mockito.when(marcUtil.convertAndValidateXml(Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyList())).thenCallRealMethod();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenCallRealMethod();
        Map responseMap=new HashMap();
        responseMap.put("errorMessage",null);
        responseMap.put(ScsbCommonConstants.BIBLIOGRAPHICENTITY,savedBibliographicEntity);
        ReflectionTestUtils.setField(repositoryService,"itemDetailsRepository",itemDetailsRepository);
        ReflectionTestUtils.setField(repositoryService,"reportDetailRepository",reportDetailRepository);
        Mockito.when(repositoryService.getItemDetailsRepository()).thenCallRealMethod();
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(savedBibliographicEntity.getItemEntities());
        Mockito.when(marcToBibEntityConverter.convert(any(), any())).thenThrow(NullPointerException.class);
        ReflectionTestUtils.setField(submitCollectionDAOService,"repositoryService",repositoryService);
        Mockito.when(submitCollectionDAOService.updateBibliographicEntity(any(),Mockito.anyMap(),Mockito.anyList(), anySet(),Mockito.anyBoolean(), any(), any())).thenCallRealMethod();
        ReflectionTestUtils.setField(submitCollectionDAOService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionDAOService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionDAOService,"submitCollectionReportHelperService",submitCollectionReportHelperService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"repositoryService",repositoryService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"submitCollectionHelperService",submitCollectionHelperService);
        Map<Integer,String> institutionEntityMap=new HashMap<>();
        institutionEntityMap.put(1,"PUL");
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(institutionEntityMap);
        Mockito.when(submitCollectionReportHelperService.buildSubmitCollectionReportInfo(Mockito.anyMap(), any(), any())).thenCallRealMethod();
        Map<String,Map<String,ItemEntity>> fetchedHoldingItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        ItemEntity itemEntity=savedBibliographicEntity.getItemEntities().get(0);
        itemEntity.setUseRestrictions("no");
        itemEntityMap.put("1",itemEntity);
        fetchedHoldingItemMap.put("1",itemEntityMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(any())).thenReturn(fetchedHoldingItemMap);
        ReportEntity savedReportEntity=new ReportEntity();
        savedReportEntity.setId(1);
        List<Future> futures = new ArrayList<>();
        Mockito.when(reportDetailRepository.save(any())).thenReturn(savedReportEntity);
        Mockito.doCallRealMethod().when(submitCollectionReportHelperService).setSubmitCollectionFailureReportForUnexpectedException(any(),Mockito.anyList(),Mockito.anyString(), any());
        List<SubmitCollectionResponse>  submitCollectionResponseList = submitCollectionService.process("PUL",updatedMarcForPUL,processedBibIds,Arrays.asList(idMapToRemoveIndex), Arrays.asList(bibIdMapToRemoveIndex), ScsbConstants.REST,reportRecordNumList, true,false,null, null,executorService,futures);
        assertEquals("Failed record - Item not updated - null",submitCollectionResponseList.get(0).getMessage());
    }

    private InstitutionEntity getInstitutionEntity() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setId(1);
        institutionEntity.setInstitutionName("PUL");
        institutionEntity.setInstitutionCode("PUL");
        return institutionEntity;
    }

    @Test
    public void processForPULUpdateIncompleteRecord() throws JAXBException {
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity(1,"202304","222420","1110846",1,"32101062128309",bibMarcContentForPUL,holdingMarcContentForPUL, ScsbCommonConstants.INCOMPLETE_STATUS);
        Set<Integer> processedBibIds = new HashSet<>();
        Map<String,String> idMapToRemoveIndex = new HashMap<>();
        Map<String,String> bibIdMapToRemoveIndex = new HashMap<>();
        List<Integer> reportRecordNumList = new ArrayList<>();
        Mockito.when(validationService.validateInstitution(Mockito.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(repositoryService,"institutionDetailsRepository",institutionDetailsRepository);
        Mockito.when(repositoryService.getInstitutionDetailsRepository()).thenCallRealMethod();
        Mockito.when(repositoryService.getReportDetailRepository()).thenCallRealMethod();
        Mockito.when(institutionDetailsRepository.findByInstitutionCode(Mockito.anyString())).thenReturn(getInstitutionEntity());
        ReflectionTestUtils.setField(marcUtil,"inputLimit",1);
        Mockito.when(marcUtil.convertAndValidateXml(Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyList())).thenCallRealMethod();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenCallRealMethod();
        Map responseMap=new HashMap();
        StringBuilder stringBuilder=new StringBuilder();
        responseMap.put("errorMessage",stringBuilder);
        responseMap.put(ScsbCommonConstants.BIBLIOGRAPHICENTITY,savedBibliographicEntity);
        ReflectionTestUtils.setField(repositoryService,"itemDetailsRepository",itemDetailsRepository);
        ReflectionTestUtils.setField(repositoryService,"reportDetailRepository",reportDetailRepository);
        Mockito.when(repositoryService.getItemDetailsRepository()).thenCallRealMethod();
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(savedBibliographicEntity.getItemEntities());
        Mockito.when(marcToBibEntityConverter.convert(any(), any())).thenReturn(responseMap);
        ReflectionTestUtils.setField(submitCollectionDAOService,"repositoryService",repositoryService);
        Mockito.when(submitCollectionDAOService.updateBibliographicEntity(any(),Mockito.anyMap(),Mockito.anyList(), anySet(),Mockito.anyBoolean(), any(), any())).thenCallRealMethod();
        ReflectionTestUtils.setField(submitCollectionDAOService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionDAOService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionDAOService,"submitCollectionReportHelperService",submitCollectionReportHelperService);
        ReflectionTestUtils.setField(submitCollectionDAOService,"marcUtil",marcUtil);
        ReflectionTestUtils.setField(submitCollectionDAOService,"submitCollectionHelperService",submitCollectionHelperService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"repositoryService",repositoryService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"submitCollectionHelperService",submitCollectionHelperService);
        Map<Integer,String> institutionEntityMap=new HashMap<>();
        institutionEntityMap.put(1,"PUL");
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(institutionEntityMap);
        Mockito.when(submitCollectionReportHelperService.buildSubmitCollectionReportInfo(Mockito.anyMap(), any(), any())).thenCallRealMethod();
        Map<String,Map<String,ItemEntity>> fetchedHoldingItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        itemEntityMap.put("1",savedBibliographicEntity.getItemEntities().get(0));
        fetchedHoldingItemMap.put("1",itemEntityMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(any())).thenReturn(fetchedHoldingItemMap);
        ReportEntity savedReportEntity=new ReportEntity();
        savedReportEntity.setId(1);
        List<Future> futures = new ArrayList<>();
        Mockito.when(reportDetailRepository.save(any())).thenReturn(savedReportEntity);
        ReflectionTestUtils.setField(submitCollectionDAOService,"bibliographicDetailsRepository",bibliographicDetailsRepository);
        List<SubmitCollectionResponse>  submitCollectionResponseList = submitCollectionService.process("PUL",updatedMarcForPUL,processedBibIds,Arrays.asList(idMapToRemoveIndex), Arrays.asList(bibIdMapToRemoveIndex), ScsbConstants.REST,reportRecordNumList, true,false,null, null,executorService,futures);
        assertNotNull(submitCollectionResponseList);
    }

    @Test
    public void processForPULRejectedRecord() throws JAXBException {
        Set<Integer> processedBibIds = new HashSet<>();
        Map<String,String> idMapToRemoveIndex = new HashMap<>();
        Map<String,String> bibIdMapToRemoveIndex = new HashMap<>();
        List<Integer> reportRecordNumList = new ArrayList<>();
        Mockito.when(validationService.validateInstitution(Mockito.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(marcUtil,"inputLimit",1);
        List<Future> futures = new ArrayList<>();
        Mockito.when(marcUtil.convertAndValidateXml(Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyList())).thenReturn(ScsbConstants.SUBMIT_COLLECTION_REJECTION_RECORD);
        Mockito.when(institutionDetailsRepository.findByInstitutionCode(Mockito.anyString())).thenReturn(getInstitutionEntity());
        ReflectionTestUtils.setField(repositoryService,"institutionDetailsRepository",institutionDetailsRepository);
        Mockito.when(repositoryService.getInstitutionDetailsRepository()).thenCallRealMethod();
        List<SubmitCollectionResponse>  submitCollectionResponseList = submitCollectionService.process("PUL",updatedMarcForPUL,processedBibIds,Arrays.asList(idMapToRemoveIndex),Arrays.asList(bibIdMapToRemoveIndex), ScsbConstants.REST,reportRecordNumList, true,false,null, exchange,executorService,futures);
        assertEquals(ScsbConstants.SUBMIT_COLLECTION_REJECTION_RECORD,submitCollectionResponseList.get(0).getMessage());
    }

    @Test
    public void processException() throws JAXBException {
        Set<Integer> processedBibIds = new HashSet<>();
        Map<String,String> idMapToRemoveIndex = new HashMap<>();
        Map<String,String> bibIdMapToRemoveIndex = new HashMap<>();
        List<Integer> reportRecordNumList = new ArrayList<>();
        Mockito.when(validationService.validateInstitution(Mockito.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(marcUtil,"inputLimit",1);
        Mockito.when(marcUtil.convertAndValidateXml(Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyList())).thenReturn(ScsbConstants.SUBMIT_COLLECTION_INTERNAL_ERROR);
        Mockito.when(institutionDetailsRepository.findByInstitutionCode(Mockito.anyString())).thenReturn(getInstitutionEntity());
        ReflectionTestUtils.setField(repositoryService,"institutionDetailsRepository",institutionDetailsRepository);
        Mockito.when(repositoryService.getInstitutionDetailsRepository()).thenCallRealMethod();
        List<Future> futures = new ArrayList<>();
        Mockito.doThrow(NullPointerException.class).when(submitCollectionReportHelperService).setSubmitCollectionReportInfoForInvalidXml(Mockito.anyString(),Mockito.anyList(),Mockito.anyString());
        List<SubmitCollectionResponse>  submitCollectionResponseList = submitCollectionService.process("PUL",updatedMarcForPUL,processedBibIds,Arrays.asList(idMapToRemoveIndex),Arrays.asList(bibIdMapToRemoveIndex), ScsbConstants.REST,reportRecordNumList, true,false,null, null,executorService,futures);
        assertEquals(ScsbConstants.SUBMIT_COLLECTION_INTERNAL_ERROR,submitCollectionResponseList.get(0).getMessage());
    }

    @Test
    public void processUnknownInstitution() throws JAXBException {
         Set<Integer> processedBibIds = new HashSet<>();
        Map<String,String> idMapToRemoveIndex = new HashMap<>();
        Map<String,String> bibIdMapToRemoveIndex = new HashMap<>();
        List<Integer> reportRecordNumList = new ArrayList<>();
        List<Future> futures = new ArrayList<>();
        List<SubmitCollectionResponse>  submitCollectionResponseList = submitCollectionService.process("PUL",updatedMarcForPUL,processedBibIds,Arrays.asList(idMapToRemoveIndex),Arrays.asList(bibIdMapToRemoveIndex), ScsbConstants.REST,reportRecordNumList, true,false,null, null,executorService,futures);
        assertEquals("Please provide valid institution code",submitCollectionResponseList.get(0).getMessage());
    }

    @Test
    public void processForNYPL() throws JAXBException {
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity(3,".b100000125","123",".i100000034",1,"33433014514719",bibMarcContentForNYPL1,holdingContentForNYPL1, ScsbCommonConstants.INCOMPLETE_STATUS);
        Set<Integer> processedBibIds = new HashSet<>();
        Map<String,String> idMapToRemoveIndex = new HashMap<>();
        Map<String,String> bibIdMapToRemoveIndex = new HashMap<>();
        List<Integer> reportRecordNumList = new ArrayList<>();
        Mockito.when(validationService.validateInstitution(Mockito.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(repositoryService,"institutionDetailsRepository",institutionDetailsRepository);
        Mockito.when(repositoryService.getInstitutionDetailsRepository()).thenCallRealMethod();
        Mockito.when(repositoryService.getReportDetailRepository()).thenCallRealMethod();
        Mockito.when(institutionDetailsRepository.findByInstitutionCode(Mockito.anyString())).thenReturn(getInstitutionEntity());
        ReflectionTestUtils.setField(marcUtil,"inputLimit",1);
        ReflectionTestUtils.setField(submitCollectionService,"inputLimit",1);
        Mockito.when(marcUtil.convertAndValidateXml(Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyList())).thenCallRealMethod();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenCallRealMethod();
        Map responseMap=new HashMap();
        StringBuilder stringBuilder=new StringBuilder();
        responseMap.put("errorMessage",stringBuilder);
        responseMap.put(ScsbCommonConstants.BIBLIOGRAPHICENTITY,savedBibliographicEntity);
        ReflectionTestUtils.setField(repositoryService,"itemDetailsRepository",itemDetailsRepository);
        ReflectionTestUtils.setField(repositoryService,"reportDetailRepository",reportDetailRepository);
        Mockito.when(repositoryService.getItemDetailsRepository()).thenCallRealMethod();
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(savedBibliographicEntity.getItemEntities());
        Mockito.when(scsbToBibEntityConverter.convert(any(), any())).thenReturn(responseMap);
        ReflectionTestUtils.setField(submitCollectionDAOService,"repositoryService",repositoryService);
        Mockito.when(submitCollectionDAOService.updateBibliographicEntity(any(),Mockito.anyMap(),Mockito.anyList(), anySet(),Mockito.anyBoolean(), any(), any())).thenCallRealMethod();
        ReflectionTestUtils.setField(submitCollectionDAOService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionDAOService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionDAOService,"submitCollectionReportHelperService",submitCollectionReportHelperService);
        ReflectionTestUtils.setField(submitCollectionDAOService,"marcUtil",marcUtil);
        ReflectionTestUtils.setField(submitCollectionDAOService,"bibliographicDetailsRepository",bibliographicDetailsRepository);
        ReflectionTestUtils.setField(submitCollectionDAOService,"submitCollectionHelperService",submitCollectionHelperService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"repositoryService",repositoryService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"submitCollectionHelperService",submitCollectionHelperService);
        Map<Integer,String> institutionEntityMap=new HashMap<>();
        institutionEntityMap.put(1,"NYPL");
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(institutionEntityMap);
        Mockito.when(submitCollectionReportHelperService.buildSubmitCollectionReportInfo(Mockito.anyMap(), any(), any())).thenCallRealMethod();
        Map<String,Map<String,ItemEntity>> fetchedHoldingItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        ItemEntity itemEntity=savedBibliographicEntity.getItemEntities().get(0);
        itemEntity.setUseRestrictions("no");
        itemEntityMap.put("1",itemEntity);
        fetchedHoldingItemMap.put("1",itemEntityMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(any())).thenReturn(fetchedHoldingItemMap);
        ReportEntity savedReportEntity=new ReportEntity();
        savedReportEntity.setId(1);
        List<Future> futures = new ArrayList<>();
        Mockito.when(reportDetailRepository.save(any())).thenReturn(savedReportEntity);
        Mockito.when(commonUtil.extractBibRecords(any())).thenCallRealMethod();
        List<SubmitCollectionResponse>  submitCollectionResponseList = submitCollectionService.process("NYPL",updatedContentForNYPL1,processedBibIds,Arrays.asList(idMapToRemoveIndex),Arrays.asList(bibIdMapToRemoveIndex), ScsbConstants.REST,reportRecordNumList, true,false,null, null,executorService,futures);
       // assertEquals(ScsbConstants.SUBMIT_COLLECTION_SUCCESS_RECORD,submitCollectionResponseList.get(0).getMessage());
    }
    @Test
    public void processForNYPLBib() throws JAXBException {
        BibliographicEntity savedBibliographicEntity = getBibliographicEntity(3,".b100000125","123",".i100000034",1,"33433014514719",bibMarcContentForNYPL1,holdingContentForNYPL1, ScsbCommonConstants.INCOMPLETE_STATUS);
        Set<Integer> processedBibIds = new HashSet<>();
        Map<String,String> idMapToRemoveIndex = new HashMap<>();
        Map<String,String> bibIdMapToRemoveIndex = new HashMap<>();
        List<Integer> reportRecordNumList = new ArrayList<>();
        Mockito.when(validationService.validateInstitution(Mockito.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(repositoryService,"institutionDetailsRepository",institutionDetailsRepository);
        Mockito.when(repositoryService.getInstitutionDetailsRepository()).thenCallRealMethod();
        Mockito.when(repositoryService.getReportDetailRepository()).thenCallRealMethod();
        Mockito.when(institutionDetailsRepository.findByInstitutionCode(Mockito.anyString())).thenReturn(getInstitutionEntity());
        ReflectionTestUtils.setField(marcUtil,"inputLimit",1);
        ReflectionTestUtils.setField(submitCollectionService,"inputLimit",1);
        Mockito.when(marcUtil.convertAndValidateXml(Mockito.anyString(),Mockito.anyBoolean(),Mockito.anyList())).thenCallRealMethod();
        Mockito.when(marcUtil.convertMarcXmlToRecord(Mockito.anyString())).thenCallRealMethod();
        Map responseMap=new HashMap();
        StringBuilder stringBuilder=new StringBuilder();
        responseMap.put("errorMessage",stringBuilder);
        responseMap.put(ScsbCommonConstants.BIBLIOGRAPHICENTITY,savedBibliographicEntity);
        ReflectionTestUtils.setField(repositoryService,"itemDetailsRepository",itemDetailsRepository);
        ReflectionTestUtils.setField(repositoryService,"reportDetailRepository",reportDetailRepository);
        Mockito.when(repositoryService.getItemDetailsRepository()).thenCallRealMethod();
        Mockito.when(itemDetailsRepository.findByBarcodeInAndOwningInstitutionId(Mockito.anyList(),Mockito.anyInt())).thenReturn(savedBibliographicEntity.getItemEntities());
        Mockito.when(scsbToBibEntityConverter.convert(any(), any())).thenReturn(responseMap);
        ReflectionTestUtils.setField(submitCollectionDAOService,"repositoryService",repositoryService);
        Mockito.when(submitCollectionDAOService.updateBibliographicEntity(any(),Mockito.anyMap(),Mockito.anyList(), anySet(),Mockito.anyBoolean(), any(), any())).thenReturn(savedBibliographicEntity);
        ReflectionTestUtils.setField(submitCollectionDAOService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"nonHoldingIdInstitution",nonHoldingIdInstitution);
        ReflectionTestUtils.setField(submitCollectionDAOService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionDAOService,"submitCollectionReportHelperService",submitCollectionReportHelperService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"repositoryService",repositoryService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"setupDataService",setupDataService);
        ReflectionTestUtils.setField(submitCollectionReportHelperService,"submitCollectionHelperService",submitCollectionHelperService);
        Map<Integer,String> institutionEntityMap=new HashMap<>();
        institutionEntityMap.put(1,"NYPL");
        Mockito.when(setupDataService.getInstitutionIdCodeMap()).thenReturn(institutionEntityMap);
        Mockito.when(submitCollectionReportHelperService.buildSubmitCollectionReportInfo(Mockito.anyMap(), any(), any())).thenCallRealMethod();
        Map<String,Map<String,ItemEntity>> fetchedHoldingItemMap = new HashMap<>();
        Map<String,ItemEntity> itemEntityMap = new HashMap<>();
        ItemEntity itemEntity=savedBibliographicEntity.getItemEntities().get(0);
        itemEntity.setUseRestrictions("no");
        itemEntityMap.put("1",itemEntity);
        fetchedHoldingItemMap.put("1",itemEntityMap);
        Mockito.when(submitCollectionHelperService.getHoldingItemIdMap(any())).thenReturn(fetchedHoldingItemMap);
        ReportEntity savedReportEntity=new ReportEntity();
        savedReportEntity.setId(1);
        List<Future> futures = new ArrayList<>();
        Mockito.when(reportDetailRepository.save(any())).thenReturn(savedReportEntity);
        Mockito.when(commonUtil.extractBibRecords(any())).thenCallRealMethod();
        List<SubmitCollectionResponse>  submitCollectionResponseList = submitCollectionService.process("NYPL",updatedContentForNYPL1,processedBibIds,Arrays.asList(idMapToRemoveIndex),Arrays.asList(bibIdMapToRemoveIndex), ScsbConstants.REST,reportRecordNumList, true,false,null, null,executorService,futures);
        assertNotNull(submitCollectionResponseList);
    }

    @Test
    public void processForNYPLLimitExceeded() throws JAXBException {
        Set<Integer> processedBibIds = new HashSet<>();
        Map<String,String> idMapToRemoveIndex = new HashMap<>();
        Map<String,String> bibIdMapToRemoveIndex = new HashMap<>();
        List<Integer> reportRecordNumList = new ArrayList<>();
        Mockito.when(validationService.validateInstitution(Mockito.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(repositoryService,"institutionDetailsRepository",institutionDetailsRepository);
        Mockito.when(repositoryService.getInstitutionDetailsRepository()).thenCallRealMethod();
        Mockito.when(repositoryService.getReportDetailRepository()).thenCallRealMethod();
        Mockito.when(institutionDetailsRepository.findByInstitutionCode(Mockito.anyString())).thenReturn(getInstitutionEntity());
        ReflectionTestUtils.setField(marcUtil,"inputLimit",0);
        ReflectionTestUtils.setField(submitCollectionService,"inputLimit",0);
        Mockito.when(commonUtil.extractBibRecords(any())).thenReturn(bibRecords);
        List<BibRecord> bibRecordList=new ArrayList<>();
        bibRecordList.add(bibRecord);
        List<Future> futures = new ArrayList<>();
        Mockito.when(bibRecords.getBibRecordList()).thenReturn(bibRecordList);
        List<SubmitCollectionResponse>  submitCollectionResponseList = submitCollectionService.process("NYPL",updatedContentForNYPL1,processedBibIds,Arrays.asList(idMapToRemoveIndex),Arrays.asList(bibIdMapToRemoveIndex), ScsbConstants.REST,reportRecordNumList, true,false,null, exchange,executorService,futures);
        assertTrue(submitCollectionResponseList.get(0).getMessage().contains(ScsbConstants.SUBMIT_COLLECTION_LIMIT_EXCEED_MESSAGE));
    }

    @Test
    public void processForNYPLJAXBException() throws JAXBException {
        Set<Integer> processedBibIds = new HashSet<>();
        Map<String,String> idMapToRemoveIndex = new HashMap<>();
        Map<String,String> bibIdMapToRemoveIndex = new HashMap<>();
        List<Integer> reportRecordNumList = new ArrayList<>();
        Mockito.when(validationService.validateInstitution(Mockito.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(repositoryService,"institutionDetailsRepository",institutionDetailsRepository);
        Mockito.when(repositoryService.getInstitutionDetailsRepository()).thenCallRealMethod();
        Mockito.when(repositoryService.getReportDetailRepository()).thenCallRealMethod();
        Mockito.when(institutionDetailsRepository.findByInstitutionCode(Mockito.anyString())).thenReturn(getInstitutionEntity());
        ReflectionTestUtils.setField(marcUtil,"inputLimit",0);
        ReflectionTestUtils.setField(submitCollectionService,"inputLimit",0);
        Mockito.when(commonUtil.extractBibRecords(any())).thenThrow(JAXBException.class);
        List<Future> futures = new ArrayList<>();
        List<SubmitCollectionResponse>  submitCollectionResponseList = submitCollectionService.process("NYPL",updatedContentForNYPL1,processedBibIds,Arrays.asList(idMapToRemoveIndex),Arrays.asList(bibIdMapToRemoveIndex), ScsbConstants.REST,reportRecordNumList, true,false,null, exchange,executorService,futures);
        assertEquals(ScsbConstants.INVALID_SCSB_XML_FORMAT_MESSAGE,submitCollectionResponseList.get(0).getMessage());
    }
    @Test
    public void removeSolrIndex() throws JAXBException {
        List<Map<String,String> >idMapToRemoveIndexList=new ArrayList<>();
        Map<String,String> response=new HashMap<>();
        idMapToRemoveIndexList.add(response);
        submitCollectionService.removeSolrIndex(idMapToRemoveIndexList);
        submitCollectionService.removeBibFromSolrIndex(idMapToRemoveIndexList);
        Set<Integer> bibliographicIdList=new HashSet<>();
        bibliographicIdList.add(1);
        String index=submitCollectionService.indexData(bibliographicIdList);
        String indexDataUsingOwningInstBibId=submitCollectionService.indexDataUsingOwningInstBibId(new ArrayList<>(),1);
        assertNull(index);
        assertNull(indexDataUsingOwningInstBibId);
    }

    private BibliographicEntity getBibliographicEntity(Integer owningInstitutionId, String owningInstitutionBibId, String owningInstitutionHoldingsId,
                                                       String owningInstitutionItemId, Integer itemAvailabilityStatusId, String itemBarcode, String bibMarcContent , String holdingMarcContent, String catalogingStatus){
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setId(1);
        bibliographicEntity.setContent(bibMarcContent.getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setCreatedBy("etl");
        bibliographicEntity.setLastUpdatedBy("etl");
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setOwningInstitutionBibId(owningInstitutionBibId);
        bibliographicEntity.setOwningInstitutionId(owningInstitutionId);
        bibliographicEntity.setCatalogingStatus(catalogingStatus);
        List<BibliographicEntity> bibliographicEntitylist = new LinkedList(Arrays.asList(bibliographicEntity));

        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setContent(holdingMarcContent.getBytes());
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setCreatedBy("etl");
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setOwningInstitutionId(owningInstitutionId);
        holdingsEntity.setLastUpdatedBy("etl");
        holdingsEntity.setOwningInstitutionHoldingsId(owningInstitutionHoldingsId);
        List<HoldingsEntity> holdingsEntitylist = new LinkedList(Arrays.asList(holdingsEntity));

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setCallNumberType("0");
        itemEntity.setCallNumber("K25 .xN5");
        itemEntity.setCreatedDate(new Date());
        itemEntity.setCreatedBy("submitCollection");
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setLastUpdatedBy("submitCollection");
        itemEntity.setBarcode(itemBarcode);
        itemEntity.setOwningInstitutionItemId(owningInstitutionItemId);
        itemEntity.setOwningInstitutionId(owningInstitutionId);
        itemEntity.setCollectionGroupId(1);
        itemEntity.setCustomerCode("PA");
        itemEntity.setItemAvailabilityStatusId(itemAvailabilityStatusId);
        itemEntity.setDeleted(false);
        itemEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        itemEntity.setCatalogingStatus(catalogingStatus);
        List<ItemEntity> itemEntitylist = new LinkedList(Arrays.asList(itemEntity));



        holdingsEntity.setBibliographicEntities(bibliographicEntitylist);
        holdingsEntity.setItemEntities(itemEntitylist);
        bibliographicEntity.setHoldingsEntities(holdingsEntitylist);
        bibliographicEntity.setItemEntities(itemEntitylist);
        itemEntity.setHoldingsEntities(holdingsEntitylist);
        itemEntity.setBibliographicEntities(bibliographicEntitylist);

        return bibliographicEntity;
    }
    private List<Record> readMarcXml(String marcXmlString) {
        List<Record> recordList = new ArrayList<>();
        InputStream in = new ByteArrayInputStream(marcXmlString.getBytes());
        MarcReader reader = new MarcXmlReader(in);
        while (reader.hasNext()) {
            Record record = reader.next();
            recordList.add(record);
            log.info(record.toString());
        }
        return recordList;
    }
}

