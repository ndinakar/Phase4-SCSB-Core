package org.recap.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.camel.accessionreconciliation.BarcodeReconciliationRouteBuilder;
import org.recap.repository.jpa.ImsLocationDetailsRepository;
import org.recap.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by akulak on 24/5/17.
 */

@Slf4j
@RestController
@RequestMapping("/accessionReconciliation")
public class AccessionReconcilationJobController {

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    ImsLocationDetailsRepository imsLocationDetailsRepository;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    CamelContext camelContext;

    @Value("${" + PropertyKeyConstants.S3_ACCESSION_RECONCILIATION_DIR + "}")
    private String accessionReconciliationPath;

    @Value("${" + PropertyKeyConstants.ACCESSION_RECONCILIATION_FILEPATH + "}")
    private String accessionReconciliationFilePath;

    @Value("${" + PropertyKeyConstants.S3_ACCESSION_RECONCILIATION_PROCESSED_DIR + "}")
    private String accessionReconciliationProcessedPath;

    /**
     * This method is used for generating report by, comparing each LAS(ReCAP/HD) barcodes and SCSB barcodes. The LAS barcodes are send to SCSB as CVS files, in specific FTP folder.
     * The barcodes are physically separated by institution. This method will initiate the comparison of all the three institution at the same time.
     *
     * @return String
     * @throws Exception
     */
    @PostMapping(value = "/startAccessionReconciliation")
    public String startAccessionReconciliation() throws Exception {
        log.info("Before accession reconciliation process : {}", camelContext.getRoutes().size());
        log.info("Starting Accession Reconciliation Routes");
        List<String> imsLocationCodesExceptUN = imsLocationDetailsRepository.findAllImsLocationCodeExceptUN();
        List<String> allInstitutionCodesExceptSupportInstitution = commonUtil.findAllInstitutionCodesExceptSupportInstitution();
        for (String imsLocation : imsLocationCodesExceptUN) {
            for (String institution : allInstitutionCodesExceptSupportInstitution) {
                camelContext.addRoutes(new BarcodeReconciliationRouteBuilder(applicationContext, camelContext,
                        institution, imsLocation, accessionReconciliationPath, accessionReconciliationFilePath, accessionReconciliationProcessedPath));
            }
        }
        for (String imsLocation : imsLocationCodesExceptUN) {
            for (String institution : allInstitutionCodesExceptSupportInstitution) {
                camelContext.getRouteController().startRoute(imsLocation + institution + ScsbConstants.ACCESSION_RECONCILIATION_S3_ROUTE_ID);
            }
        }
        log.info("After accession reconciliation process : {}", camelContext.getRoutes().size());
        return ScsbCommonConstants.SUCCESS;
    }
}
