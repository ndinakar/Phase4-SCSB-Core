package org.recap.service.accession.callable;

import lombok.Data;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.accession.AccessionResponse;
import org.recap.model.jpa.ImsLocationEntity;
import org.recap.model.jpa.ReportDataEntity;
import org.recap.util.AccessionProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Callable;

/**
 * Created by sheiks on 26/05/17.
 */
@Data
@Component
@Scope("prototype")
public class BibDataCallable implements Callable{
  
    @Autowired
    private AccessionProcessService accessionProcessService;
    private AccessionRequest accessionRequest;
    private String owningInstitution;
    private boolean writeToReport;
    private ImsLocationEntity imsLocationEntity;

    @Override
    public Object call() throws Exception {

        List<Map<String, String>> responseMaps = new ArrayList<>();
        List<ReportDataEntity> reportDataEntitys = new ArrayList<>();
        Set<AccessionResponse> accessionResponses = new HashSet<>();
        return accessionProcessService.processRecords(accessionResponses, responseMaps, accessionRequest, reportDataEntitys, owningInstitution, writeToReport,imsLocationEntity);

    }
}
