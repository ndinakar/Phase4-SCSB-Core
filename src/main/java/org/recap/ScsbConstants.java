package org.recap;

import java.util.Arrays;
import java.util.List;

/**
 * Created by premkb on 19/8/16.
 */
public final class ScsbConstants {

    public static final String EDD_REQUEST = "EDD";
    public static final String REFILED_REQUEST = "REFILED";
    public static final String REQUEST_ACCESSION_RECONCILIATION_MAIL_QUEUE = "AccessionReconciliation";
    public static final String REQUEST_INITIAL_DATA_LOAD = "requestInitialDataLoad";
    public static final String SUBMIT_COLLECTION_EXCEPTION = "Exception";
    public static final String SUBJECT_FOR_SUBMIT_COL_EXCEPTION = "Exception Occured during Submit collection";
    public static final String DELETED_RECORDS_EMAIL_TEMPLATE = "deleted_records_email_body.vm";
    public static final String EMAIL_Q = "scsbactivemq:queue:CoreEmailQ";
    public static final String EMAIL_ROUTE_ID = "RequestRecallEmailRouteId";
    public static final String FORMAT_MARC = "marc";
    public static final String FORMAT_SCSB = "scsb";
    public static final String SUBMIT_COLLECTION_COMPLETION_QUEUE_FROM = "scsbactivemq:queue:submitCollectionCompletionFromQueue";
    public static final String SUBMIT_COLLECTION_COMPLETION_QUEUE_TO = "scsbactivemq:queue:submitCollectionCompletionToQueue";
    public static final String SEND_EMAIL_FOR_EMPTY_DIRECTORY = "sendEmailForEmptyDirectory";
    public static final String ITEM_STATUS_AVAILABLE = "Available";
    public static final String INVALID_SCSB_XML_FORMAT_MESSAGE = "Invalid SCSB xml format";
    public static final String INVALID_MARC_XML_FORMAT_MESSAGE = "Invalid Marc xml format";
    public static final String INVALID_MARC_XML_FORMAT_IN_SCSBXML_MESSAGE = "Invalid Marc xml content with in SCSB xml";
    public static final String SUBMIT_COLLECTION_INTERNAL_ERROR = "Internal error occured during submit collection";
    public static final String SUBMIT_COLLECTION_LIMIT_EXCEED_MESSAGE = "Maximum allowed input record is ";
    public static final String BIBRECORD_TAG = "<bibRecords>";
    public static final String SUBMIT_COLLECTION = "submitCollection";
    public static final String SUBMIT_COLLECTION_CGD_UPDATED = "submitCollection-cgd-updated";
    public static final String SUBMIT_COLLECTION_FOR_NO_FILES = "submitCollectionForNoFiles";
    public static final String BIBLIOGRAPHIC_ENTITY = "bibliographicEntity";
    public static final String GUEST_USER = "Guest";
    public static final String REQUEST_ITEM_AVAILABILITY_STATUS_UPDATE = "RequestItem AvailabilityStatus Change";
    public static final String REQUEST_ITEM_AVAILABILITY_STATUS_DATA_ROLLBACK = "2 - 1";
    public static final String UPDATE_ITEM_STATUS_SOLR = "/updateItem/updateItemAvailablityStatus";
    public static final String UPDATE_ITEM_STATUS_SOLR_PARAM_ITEM_ID = "itemBarcode";
    public static final String REQUEST_STATUS_EXCEPTION = "EXCEPTION";
    public static final String GFA_STATUS_INCOMING_ON_WORK_ORDER = "INC ON WO:";
    public static final String GFA_STATUS_OUT_ON_EDD_WORK_ORDER = "OUT ON EDD WO:";
    public static final String GFA_STATUS_REACC_ON_WORK_ORDER = "REACC ON WO:";
    public static final String GFA_STATUS_REFILE_ON_WORK_ORDER = "REFILE ON WO:";
    public static final String GFA_STATUS_SCH_ON_EDD_WORK_ORDER = "SCH ON EDD WO:";
    public static final String GFA_STATUS_VER_ON_EDD_WORK_ORDER = "VER ON EDD WO:";
    public static final String GFA_STATUS_IN = "IN";

    public static final String GFA_STATUS_NOT_ON_FILE = "NOT ON FILE";
    public static final String GFA_STATUS_OUT_ON_RETRIVAL_WORK_ORDER = "OUT ON RET WO:";
    public static final String GFA_STATUS_PW_INDIRECT_WORK_ORDER = "PWI ON WO:";
    public static final String GFA_STATUS_PW_DIRECT_WORK_ORDER = "PWD ON WO:";
    public static final String GFA_STATUS_SCH_ON_RET_WORK_ORDER = "SCH ON RET WO:";
    public static final String GFA_STATUS_SCH_ON_REFILE_WORK_ORDER = "SCH ON REFILE WO:";
    public static final String GFA_STATUS_VER_ON_REFILE_WORK_ORDER = "VER ON REFILE WO:";
    public static final String GFA_STATUS_VER_ON_PW_INDIRECT_WORK_ORDER = "VER ON PWI WO:";
    public static final String GFA_STATUS_VER_ON_PW_DIRECT_WORK_ORDER = "VER ON PWD WO:";
    public static final String GFA_STATUS_VER_ON_RET_WORK_ORDER = "VER ON RET WO:";
    public static final String GFA_STATUS_VER_ON_WORK_ORDER = "VER ON WO:";
    public static final String PROTECTED = "protection";
    public static final String NOT_PROTECTED = "no_protection";
    public static final String ACCESSION_RECONCILIATION_DIRECT_ROUTE = "accessionReconciliationDirectRoute";
    public static final String CGD_PROTECTED_ROUTE_ID = "CgdProtectedRouteId";
    public static final String CGD_NOT_PROTECTED_ROUTE_ID = "CgdNotProtectedRouteId";
    public static final String DUPLICATE_BARCODE_ENTRY = "Duplicate entry in the same accession request";
    public static final String ACCESSION_REPORT_Q = "scsbactivemq:queue:accesionReportQ";
    public static final String ACCESSION_REPORT_ROUTE_ID = "accessionReportQRoute";
    public static final String CGG_TYPE = "cgdType";
    public static final String CAMEL_FILE_NAME_ONLY = "CamelFileNameOnly" ;
    public static final String IMS_LOCATION = "ImsLocation";

    public static class SERVICEPATH {
        public static final String CHECKIN_ITEM = "requestItem/checkinItem";
        public static final String REFILE_ITEM_IN_ILS = "requestItem/refileItemInILS";
        public static final String REFILE_ITEM = "requestItem/refile";
    }

    //Accession
    public static final String BULK_ACCESSION_SUMMARY = "BULK_ACCESSION_SUMMARY";
    public static final String ACCESSION_SUMMARY = "ACCESSION_SUMMARY";
    public static final String ACCESSION_JOB_FAILURE = "Exception occurred in SCSB Ongoing Accession Job";
    public static final String ONGOING_ACCESSION_LIMIT_EXCEED_MESSAGE = "Input limit exceeded, maximum allowed input limit is ";
    public static final String PENDING = "pending";
    public static final String PROCESSING = "Processing";
    public static final String MARC_FORMAT_PARSER_ERROR = "Unable to parse input";
    public static final String INVALID_MARC_XML_ERROR_MSG = "Unable to parse input, xml is having invalid marc tag";
    public static final String OWNING_INST = "owningInstitution";
    public static final String ITEM_ALREADY_ACCESSIONED = "Item already accessioned - Existing item details : ";
    public static final String OWN_INST_BIB_ID = " OwningInstBibId-";
    public static final String OWN_INST_HOLDING_ID = " OwningInstHoldingId-";
    public static final String OWN_INST_ITEM_ID = " OwningInstItemId-";
    public static final String REACCESSION = "re-accession";
    public static final String ITEM_ISDELETED_TRUE_TO_FALSE = "Item isdeleted true to false";
    public static final String ACCESSION_SAVE_SUCCESS_STATUS = "The accession request is successfully processed.";
    public static final String ACCESSION_SAVE_FAILURE_STATUS = "Failed to process accession request.";
    public static final String ITEM_BARCODE_EMPTY = "Item Barcode is Blank.";
    public static final String CUSTOMER_CODE_EMPTY = "Customer Code is Blank.";
    public static final String INVALID_IMS_LOCACTION_CODE = "Invalid ims location code";
    public static final String IMS_LOCACTION_CODE_IS_BLANK = "Ims location code is blank";
    public static final String OWNING_INST_EMPTY = "Owning Institution is Blank.";
    public static final String INVALID_BARCODE_LENGTH="Barcode length should not exceed 45 characters";
    public static final String INVALID_BOUNDWITH_RECORD = "Bound-with item having invalid data";
    public static final String UNKNOWN_INSTITUTION = "UN";
    public static final String ONGOING_ACCESSION_REPORT = "Ongoing_Accession_Report";
    public static final String ACCESSION_DUMMY_RECORD = "Dummy record created";
    public static final String SUCCESS_INCOMPLETE_RECORD = "Success - Incomplete record";
    public static final String INCOMPLETE_RESPONSE = "incompleteResponse";
    public static final String ITEM_BARCODE_ALREADY_ACCESSIONED_MSG = "Unavailable barcode from partner is already accessioned";
    public static final String FAILED = "Failed";
    public static final String DUMMY_CALL_NUMBER_TYPE = "dummycallnumbertype";
    public static final String DUMMY_BIB_CONTENT_XML = "dummybibcontent.xml";
    public static final String DUMMY_HOLDING_CONTENT_XML = "dummyholdingcontent.xml";
    public static final String EMAIL_BODY_FOR = "emailBodyFor";
    public static final String SUBMIT_COLLECTION_SUCCESS_LIST = "submitCollectionSuccessList";
    public static final String SUBMIT_COLLECTION_FAILURE_LIST = "submitCollectionFailureList";
    public static final String SUBMIT_COLLECTION_REJECTION_LIST = "submitCollectionRejectionList";
    public static final String SUBMIT_COLLECTION_EXCEPTION_LIST = "submitCollectionExceptionList";
    public static final String SUBMIT_COLLECTION_EXCEPTION_RECORD = "Exception record - Item is unavailable in scsb to update";
    public static final String SUBMIT_COLLECTION_DEACCESSION_EXCEPTION_RECORD = "Exception record - Item not updated, it is a deaccessioned item";
    public static final String SUBMIT_COLLECTION_REJECTION_RECORD = "Rejection record - Only use restriction and cgd not updated because the item is in use";
    public static final String SUBMIT_COLLECTION_SUCCESS_RECORD = "Success record";
    public static final String SUBMIT_COLLECTION_FAILED_RECORD = "Failed record";
    public static final String REST = "rest-api";
    public static final String SUBMIT_COLLECTION_EMAIL_BODY_VM = "submit_collection_email_body.vm";
    public static final String SUBMIT_COLLECTION_EMAIL_BODY_FOR_EMPTY_DIRECTORY_VM = "submit_collection_email_body_for_emptyDirectory.vm";
    public static final String PROCESS_INPUT = "processInput";
    public static final String PROCESS_INPUT_FAILURE = "processInputForFailure";
    public static final String SUBMIT_COLLECTION_COMPLETE_RECORD_UPDATE = "Complete item record info updated through submit collection";
    public static final String SUBMIT_COLLECTION_DUMMY_RECORD_UPDATE = "Dummy item record removed and actual record added through submit collection";
    public static final String SUBMIT_COLLECTION_CGD_UPDATED_RECORD = "CGD info updated from {0} to {1} through submit collection";
    public static final String COUNT_OF_PURGED_EXCEPTION_REQUESTS = "countOfPurgedExceptionRequests";
    public static final String USE_RESTRICTION_UNAVAILABLE = "use restriction is unavailable in the input xml";
    public static final String RECORD_INCOMPLETE = "Record continue to be incomplete because ";
    public static final String DELETED_RECORDS_SUCCESS_MSG = "Deleted records completed successfully";
    public static final String DELETED_RECORDS_FAILURE_MSG = "Deleted records failed due to unexpected error";
    public static final String DELETED_STATUS_NOT_REPORTED = "Not Reported";
    public static final String DELETED_STATUS_REPORTED = "Reported";
    public static final String DELETED_MAIL_TO = "DELETED_MAIl_TO";
    public static final String EMAIL_SUBJECT_DELETED_RECORDS = "List of Deleted Records";
    public static final String EMAIL_DELETED_RECORDS_DISPLAY_MESSAGE = "Total No. of Records Deleted : ";
    public static final String DAILY_RECONCILIATION = "DailyReconciliation";

    //Daily Reconciliation
    public static final String DAILY_RR_S3_ROUTE_ID = "DailyReconciliationS3Route";
    public static final String DAILY_RR_FS_ROUTE_ID = "DailyReconciliationFsRoute";
    public static final String DAILY_RR_FS_OPTIONS = "?delete=true";
    public static final String DAILY_RR_FS_FILE = "file:";
    public static final String DAILY_RR_LAS = "LAS";
    public static final String DAILY_RR_SCSB = "SCSB";
    public static final String DAILY_RR_COMPARISON = "Comparison";
    public static final String DAILY_RR_FILE_DATE_FORMAT = "yyyyMMdd_HHmmss";
    public static final String DAILY_RR = "DailyReconciliation_";
    public static final String DATE_CELL_STYLE_FORMAT = "MM/dd/yyyy HH:mm:ss.S";
    public static final String DAILY_RR_REQUEST_ID = "RequestId";
    public static final String DAILY_RR_BARCODE = "Barcode";
    public static final String DAILY_RR_CUSTOMER_CODE = "CustomerCode";
    public static final String DAILY_RR_STOP_CODE = "StopCode";
    public static final String DAILY_RR_PATRON_ID = "PatronId";
    public static final String DAILY_RR_CREATED_DATE = "CreatedDate";
    public static final String DAILY_RR_LAST_UPDATED_DATE = "LastUpdatedDate";
    public static final String DAILY_RR_REQUESTING_INST = "RequestingInstitution";
    public static final String DAILY_RR_OWNING_INSTITUTION = "OwningInstitution";
    public static final String DAILY_RR_DELIVERY_METHOD = "DeliveryMethod";
    public static final String DAILY_RR_STATUS = "Status";
    public static final String DAILY_RR_MATCHED = "Matched";
    public static final String DAILY_RR_MISMATCH = "Mismatch";
    public static final String DAILY_RR_LAS_NOT_GIVEN_STATUS = "LASNotGivenStatus";
    public static final String DAILY_RR_NOT_IN_SCSB = "NotInScsb";
    //status Reconciliation
    public static final String STATUS_RECONCILIATION_REPORT = "scsbactivemq:queue:statusReconciliationReportQ";
    public static final String STATUS_RECONCILIATION_REPORT_ID = "statusReconciliationReportRoute";
    public static final String STATUS_RECONCILIATION_FAILURE_REPORT = "scsbactivemq:queue:statusReconciliationFailureReportQ";
    public static final String STATUS_RECONCILIATION_FAILURE_REPORT_ID = "statusReconciliationFailureReportRoute";
    public static final String COMPLETE = "Complete";
    public static final String ACCESSION_RECONCILIATION_S3_ROUTE_ID = "accessionReconciliationS3Route";
    public static final String ACCESSION_RECONCILIATION_FS_ROUTE_ID = "accessionReconciliationFsRoute";
    public static final String ACCESSION_RECONCILATION_FILE_NAME = "AccessionReconcilation";
    public static final String ACCESSION_RECONCILATION_SOLR_CLIENT_URL = "accessionReconcilationService/startAccessionReconcilation";
    public static final String SUBMIT_COLLECTION_COMPLETED_ROUTE = "submitCollectionCompletedRoute";
    public static final String SUBMIT_COLLECTION_CAUGHT_EXCEPTION_METHOD = "caughtException";
    public static final String SUBMIT_COLLECTION_EXCEPTION_BODY_VM = "submit_collection_exception_body.vm";
    public static final String DELETED_MAIL_QUEUE = "deletedRecordsMailSendQueue";
    public static final String COUNT_OF_PURGED_ACCESSION_REQUESTS = "countOfPurgedAccessionRequests";
    public static final String GFA_MULTIPLE_ITEM_STATUS_URL = "gfaService/multipleItemsStatusCheck";
    public static final String STATUS_RECONCILIATION_CHANGE_LOG_OPERATION_TYPE = "StatusReconciliation-ItemAvailablityStatusChange";
    public static final String FOR = "for";
    public static final String STATUS_RECONCILIATION = "StatusReconciliation";
    public static final String STATUS_RECONCILIATION_FAILURE = "StatusReconciliationFailure";
    public static final String CAMEL_SPLIT_INDEX = "CamelSplitIndex";
    public static final String ITEM_BARCODE_NOT_FOUND = "ITEM_BARCODE_NOT_FOUND";
    public static final String CAMEL_SPLIT_COMPLETE = "CamelSplitComplete";
    public static final String DIRECT = "direct:";
    public static final String PROCESS_DAILY_RECONCILIATION = "processDailyReconciliation";
    public static final String ACCESSION_RECONCILIATION_HEADER = "Barcodes not present in SCSB";
    public static final String BARCODE_RECONCILIATION_FILE_DATE_FORMAT = "yyyyMMdd";
    public static final String ACCESSION_JOB_INITIATE_ROUTE_ID = "scsbactivemq:queue:accessionInitiateRoute";
    public static final String SUBMIT_COLLECTION_JOB_INITIATE_ROUTE_ID = "scsbactivemq:queue:submitCollectionInitiateRoute";
    public static final String BARCODE_NOT_FOUND_OR_COULD_NOT_FIND_LAS_ITEM_STATUS = "Barcode not found in LAS or Could not find LAS Item Status";
    public static final String CUSTOMER_CODE_HEADER = "Customer Code mentioned in LAS";
    public static final String TAB = "\t";
    public static final String NEW_LINE = "\n";
    public static final String ITEM_STATUS_NOT_AVAILABLE = "Not Available";
    private ScsbConstants() {
    }

    public static final String FAILURE_BIB_REASON = "ReasonForFailureBib";
    public static final String FAILURE_ITEM_REASON = "ReasonForFailureItem";
    public static final String EXCEPTION = "exception->";
    public static final String SCSB_CAMEL_S3_TO_ENDPOINT = "aws-s3://{{scsbBucketName}}?autocloseBody=false&region={{awsRegion}}&accessKey=RAW({{awsAccessKey}})&secretKey=RAW({{awsAccessSecretKey}})";
    public static final String CAMEL_AWS_KEY = "CamelAwsS3Key";
    public static final String ACCESSION_CAUGHT_EXCEPTION_METHOD = "caughtException";
    public static final String EMAIL_FOR = "emailFor";
    public static final String ACCESSION_DIRECT_ROUTE_FOR_EXCEPTION = "direct:AccessionException";

    public static final String BIB_ID_LIST = "BibIdList";
    public static final String CHANGED_TO_AVAILABLE = "Changed To Available";
    public static final String UNCHANGED = "Unchanged";
    public static final String UNKNOWN_CODE = "Unknown Code";
    public static final String UNKNOWN_IMS_STATUS = "Unknown IMS Status";
    public static final String FAILED_TO_GET_IMS_ITEM_STATUS = "Failed to get IMS Item Status";
    public static final String UNUSUAL = "THIS IS UNUSUAL";

    public static final String YES = "Yes";
    public static final String NO = "No";
    public static final String NUMBER_PATTERN="[^0-9]";

    public static final String LOG_MA_QUALIFIER_UPDATE = "Number of Bib Ids Updated with MA Qualifier to {} : {}";
    public static final String LOG_MATCH_ID_QUALIFIER_UPDATE = "Matching Id - {}, Update MA Qualifier to {}, Collected {} Bib Ids: {}";
    public static final String CSV_EXTENSION = ".csv";
    public static final String DATE_FORMAT_FOR_REPORTS = "yyyyMMdd_HHmmss";
    public static final String EXCEPTION_MSG = "EXCEPTION OCCURED WHILE UPDATING ITEMHLODINGS DATA";
    public static final String ITEMHLODINGS_APPEND = "ITEM_HOLDINGS_DATA-";
}
