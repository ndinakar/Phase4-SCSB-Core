package org.recap.service.deletedrecords;

import lombok.extern.slf4j.Slf4j;
import org.recap.ScsbConstants;
import org.recap.repository.jpa.DeletedRecordsRepository;
import org.recap.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by sudhishk on 2/6/17.
 */
@Slf4j
@Service
public class DeletedRecordsService {



    @Autowired
    private DeletedRecordsRepository deletedRecordsRepository;

    @Autowired
    private EmailService emailService;

    /**
     * @return boolean
     */
    public boolean deletedRecords() {
        boolean bReturnMsg = false;

        try {
            long lCountDeleted = deletedRecordsRepository.countByDeletedReportedStatus(ScsbConstants.DELETED_STATUS_NOT_REPORTED);
            log.info("Count : {}", lCountDeleted);
            if (lCountDeleted > 0) {
                // Change Status
                int statusChange = deletedRecordsRepository.updateDeletedReportedStatus(ScsbConstants.DELETED_STATUS_REPORTED, ScsbConstants.DELETED_STATUS_NOT_REPORTED);
                log.info("Delete Count : {}" , statusChange);
                // Send Email
                emailService.sendEmail(ScsbConstants.EMAIL_DELETED_RECORDS_DISPLAY_MESSAGE + lCountDeleted, "", ScsbConstants.DELETED_MAIL_TO, ScsbConstants.EMAIL_SUBJECT_DELETED_RECORDS);
            } else {
                log.info("No records to delete" );
            }
            bReturnMsg = true;
        } catch (Exception ex) {
            log.error("", ex);
        }
        return bReturnMsg;
    }
}
