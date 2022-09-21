package org.recap.camel.dailyreconciliation;

import com.amazonaws.services.s3.AmazonS3;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbConstants;
import org.recap.ScsbCommonConstants;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.jpa.RequestItemEntity;
import org.recap.model.csv.DailyReconcilationRecord;
import org.recap.repository.jpa.ItemDetailsRepository;
import org.recap.repository.jpa.RequestItemDetailsRepository;
import org.recap.util.PropertyUtil;
import org.recap.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

/**
 * The type Daily reconcilation processor.
 */

@Slf4j
@Data
@Service
@Scope("prototype")
public class DailyReconciliationProcessor {


    @Autowired
    private ItemDetailsRepository itemDetailsRepository;

    @Autowired
    private RequestItemDetailsRepository requestItemDetailsRepository;

    @Value("${" + PropertyKeyConstants.DAILY_RECONCILIATION_FILE + "}")
    private String filePath;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private PropertyUtil propertyUtil;

    @Autowired
    AmazonS3 awsS3Client;

    private String imsLocationCode;
    private String imsAvailableCodes;
    private String imsNotAvailableCodes;

    public DailyReconciliationProcessor(String imsLocationCode) {
        this.imsLocationCode = imsLocationCode;
    }

    /**
     * Process input for daily reconciliation report.
     *
     * @param exchange the exchange
     */
    public void processInput(Exchange exchange) {
        try {
            imsAvailableCodes = propertyUtil.getPropertyByImsLocationAndKey(imsLocationCode, PropertyKeyConstants.IMS.IMS_AVAILABLE_ITEM_STATUS_CODES);
            imsNotAvailableCodes = propertyUtil.getPropertyByImsLocationAndKey(imsLocationCode, PropertyKeyConstants.IMS.IMS_NOT_AVAILABLE_ITEM_STATUS_CODES);
            String queueableCodes = propertyUtil.getPropertyByImsLocationAndKey(imsLocationCode, PropertyKeyConstants.IMS.IMS_REQUESTABLE_NOT_RETRIEVABLE_ITEM_STATUS_CODES);
            if (StringUtils.isNotBlank(queueableCodes)) {
                imsAvailableCodes = imsAvailableCodes + "," + queueableCodes;
            }
            String xmlFileName = exchange.getIn().getHeader(ScsbConstants.CAMEL_AWS_KEY).toString();
            log.info("{} LAS File Processing: {}", imsLocationCode, xmlFileName);
            List<DailyReconcilationRecord> dailyReconcilationRecordList = null;
            if (exchange.getIn().getBody() instanceof DailyReconcilationRecord) {
                dailyReconcilationRecordList = new ArrayList<>();
            } else {
                dailyReconcilationRecordList = (List<DailyReconcilationRecord>) exchange.getIn().getBody();
            }
            try (XSSFWorkbook xssfWorkbook = new XSSFWorkbook()) {
                XSSFSheet lasSheet = xssfWorkbook.createSheet(imsLocationCode + "_" + ScsbConstants.DAILY_RR_LAS);
                xssfWorkbook.setSheetOrder(imsLocationCode + "_" + ScsbConstants.DAILY_RR_LAS, 0);
                int i = 0;
                setColumnWidthForSheet(lasSheet);
                CellStyle cellStyle = xssfWorkbook.createCellStyle();
                cellStyle.setAlignment(HorizontalAlignment.LEFT);
                log.info("started creating las sheet for : {}", imsLocationCode);
                for (DailyReconcilationRecord dailyReconcilationRecord : dailyReconcilationRecordList) {
                    XSSFRow row = lasSheet.createRow(i);
                    createCellForEqualRow(row, xssfWorkbook, dailyReconcilationRecord.getCustomerCode(), cellStyle, dailyReconcilationRecord.getRequestId(), 0, dailyReconcilationRecord.getBarcode(), 1, 2, dailyReconcilationRecord.getStopCode(), 3, dailyReconcilationRecord.getPatronId(), 4, dailyReconcilationRecord.getCreateDate(), 5);
                    createCellForEqualRow(row, xssfWorkbook, dailyReconcilationRecord.getOwningInst(), cellStyle, dailyReconcilationRecord.getLastUpdatedDate(), 6, dailyReconcilationRecord.getRequestingInst(), 7, 8, dailyReconcilationRecord.getDeliveryMethod(), 9, dailyReconcilationRecord.getStatus(), 10, dailyReconcilationRecord.getErrorCode(), 11);
                    createCell(xssfWorkbook, row,cellStyle, dailyReconcilationRecord.getErrorNote(), 12);
                    i++;                         
                }
                log.info("completed creating las sheet for : {}", imsLocationCode);
                Sheet readLasSheet = xssfWorkbook.getSheetAt(0);
                XSSFSheet scsbSheet = xssfWorkbook.createSheet(ScsbConstants.DAILY_RR_SCSB);
                xssfWorkbook.setSheetOrder(ScsbConstants.DAILY_RR_SCSB, 1);
                createHeader(scsbSheet);
                XSSFCellStyle dateCellStyle = getXssfCellStyleForDate(xssfWorkbook);
                log.info("started creating scsb sheet");
                for (int j = 1; j <= readLasSheet.getLastRowNum(); j++) {
                    readValuesFromLasSheet(xssfWorkbook, readLasSheet, scsbSheet, dateCellStyle, j);
                }
                log.info("completed creating scsb sheet");
                compareLasAndScsbSheets(xssfWorkbook,cellStyle);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ScsbConstants.DAILY_RR_FILE_DATE_FORMAT);
                log.info("File Path: {}, Ims : {}", filePath, imsLocationCode);
                FileOutputStream fileOutputStream = new FileOutputStream(filePath + "/" + imsLocationCode + "/" + ScsbConstants.DAILY_RR + simpleDateFormat.format(new Date()) + ".xlsx");
                xssfWorkbook.write(fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
                log.info("total number of sheets created {}",xssfWorkbook.getNumberOfSheets());
                camelContext.getRouteController().startRoute(ScsbConstants.DAILY_RR_FS_ROUTE_ID + imsLocationCode);
                log.info("started {}", ScsbConstants.DAILY_RR_FS_ROUTE_ID + imsLocationCode);
                String bucketName = exchange.getIn().getHeader("CamelAwsS3BucketName").toString();
                if (awsS3Client.doesObjectExist(bucketName, xmlFileName)) {
                    String basepath = xmlFileName.substring(0, xmlFileName.lastIndexOf('/'));
                    String fileName = xmlFileName.substring(xmlFileName.lastIndexOf('/'));
                    awsS3Client.copyObject(bucketName, xmlFileName, bucketName, basepath + "/.done-" + imsLocationCode + fileName);
                    awsS3Client.deleteObject(bucketName, xmlFileName);
                }
            }
            log.info("{} LAS File Processed: {}",imsLocationCode, xmlFileName);
        }
        catch (Exception e){
            log.error(ScsbCommonConstants.LOG_ERROR, e);
        }

    }

    private void readValuesFromLasSheet(XSSFWorkbook xssfWorkbook, Sheet readLasSheet, XSSFSheet scsbSheet, XSSFCellStyle dateCellStyle, int j) {
        Row row = readLasSheet.getRow(j);
        Cell requestId = row.getCell(0);
        if (requestId != null) {
            buildRequestsRows(xssfWorkbook,scsbSheet,dateCellStyle,j,requestId.getStringCellValue());
        } else {
            Cell barcode = row.getCell(1);
            if (barcode != null) {
                buildDeacessionRows(xssfWorkbook,scsbSheet,dateCellStyle,j,barcode.getStringCellValue());
            }
        }
    }

    /**
     * Create cell in the xssf workbook to produce daily reconciliation report.
     *
     * @param xssfWorkbook the xssf workbook
     * @param row          the row
     * @param cellValue    the cell value
     * @param cellNum      the cell num
     */
    public void createCell(XSSFWorkbook xssfWorkbook, XSSFRow row,CellStyle cellStyle, String cellValue, int cellNum) {
        if (StringUtils.isNotBlank(cellValue)){
            XSSFCell cell = row.createCell(cellNum);
            cell.setCellValue(cellValue);
            cell.setCellStyle(cellStyle);
        }
    }

    /**
     * Build requests rows for daily reconciliation report.
     *
     * @param xssfWorkbook  the xssf workbook
     * @param xssfSheet     the xssf sheet
     * @param dateCellStyle the date cell style
     * @param rowNum        the row num
     * @param requestId     the request id
     */
    public void buildRequestsRows(XSSFWorkbook xssfWorkbook, XSSFSheet xssfSheet, XSSFCellStyle dateCellStyle, int rowNum, String requestId){
        XSSFRow row = xssfSheet.createRow(rowNum);
        CellStyle cellStyle = xssfWorkbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.LEFT);
        if(StringUtils.isNotBlank(requestId)){
            Optional<RequestItemEntity> requestItemEntity = requestItemDetailsRepository.findById(Integer.valueOf(requestId));
            if(requestItemEntity.isPresent()){
                ItemEntity itemEntity = requestItemEntity.get().getItemEntity();
                createCell(xssfWorkbook, row,cellStyle, String.valueOf(requestItemEntity.get().getId()), 0);
                createCell(xssfWorkbook, row,cellStyle, itemEntity.getBarcode(), 1);
                createCell(xssfWorkbook, row,cellStyle, itemEntity.getCustomerCode(), 2);
                createCell(xssfWorkbook, row,cellStyle, requestItemEntity.get().getStopCode(), 3);
                createCell(xssfWorkbook, row,cellStyle, requestItemEntity.get().getPatronId(), 4);
                getCreatedAndLastUpdatedDate(dateCellStyle, requestItemEntity.get().getCreatedDate(), requestItemEntity.get().getLastUpdatedDate(), row);
                createCell(xssfWorkbook, row,cellStyle, String.valueOf(requestItemEntity.get().getInstitutionEntity().getInstitutionCode()), 7);
                createCell(xssfWorkbook, row,cellStyle, String.valueOf(itemEntity.getInstitutionEntity().getInstitutionCode()), 8);
                createCell(xssfWorkbook, row,cellStyle, requestItemEntity.get().getRequestTypeEntity().getRequestTypeCode(), 9);
                createCell(xssfWorkbook, row,cellStyle, itemEntity.getItemStatusEntity().getStatusCode(), 10);

            }
        }
    }

    /**
     * Build deacession rows daily reconciliation report.R
     *
     * @param xssfWorkbook  the xssf workbook
     * @param xssfSheet     the xssf sheet
     * @param dateCellStyle the date cell style
     * @param rowNum        the row num
     * @param barcode       the barcode
     */
    public void buildDeacessionRows(XSSFWorkbook xssfWorkbook,XSSFSheet xssfSheet, XSSFCellStyle dateCellStyle,int rowNum,String barcode){
        List<ItemEntity> itemEntityList = itemDetailsRepository.findByBarcode(barcode);
        CellStyle cellStyle = xssfWorkbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.LEFT);
        if(itemEntityList != null){
            for (ItemEntity itemEntity : itemEntityList) {
                XSSFRow row = xssfSheet.createRow(rowNum);
                createCell(xssfWorkbook, row,cellStyle, itemEntity.getBarcode(), 1);
                createCell(xssfWorkbook, row,cellStyle, itemEntity.getCustomerCode(), 2);
                createCell(xssfWorkbook, row,cellStyle, itemEntity.getInstitutionEntity().getInstitutionCode(), 8);
                createCell(xssfWorkbook, row,cellStyle, itemEntity.getItemStatusEntity().getStatusCode(),10);
                getCreatedAndLastUpdatedDate(dateCellStyle, itemEntity.getCreatedDate(),itemEntity.getLastUpdatedDate(), row);
            }
        }

    }

    private static void getCreatedAndLastUpdatedDate(XSSFCellStyle dateCellStyle, Date createdDate, Date lastUpdatedDate, XSSFRow row) {
        XSSFCell createdDateCell = row.createCell(5);
        createdDateCell.setCellValue(createdDate);
        createdDateCell.setCellStyle(dateCellStyle);
        XSSFCell lastupdatedDateCell = row.createCell(6);
        lastupdatedDateCell.setCellValue(lastUpdatedDate);
        lastupdatedDateCell.setCellStyle(dateCellStyle);
    }

    /**
     * Gets xssf cell style for date field in the xssf workbook to produce daily reconciliation report.
     *
     * @param xssfWorkbook the xssf workbook
     * @return the xssf cell style for date
     */
    public XSSFCellStyle getXssfCellStyleForDate(XSSFWorkbook xssfWorkbook) {
        XSSFCreationHelper createHelper = xssfWorkbook.getCreationHelper();
        XSSFCellStyle cellStyle = xssfWorkbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.LEFT);
        cellStyle.setDataFormat(createHelper.createDataFormat().getFormat(ScsbConstants.DATE_CELL_STYLE_FORMAT));
        return cellStyle;
    }

    private static void createHeader(XSSFSheet xssfSheet) {
        XSSFRow row = xssfSheet.createRow(0);
        row.createCell(0).setCellValue(ScsbConstants.DAILY_RR_REQUEST_ID);
        row.createCell(1).setCellValue(ScsbConstants.DAILY_RR_BARCODE);
        row.createCell(2).setCellValue(ScsbConstants.DAILY_RR_CUSTOMER_CODE);
        row.createCell(3).setCellValue(ScsbConstants.DAILY_RR_STOP_CODE);
        row.createCell(4).setCellValue(ScsbConstants.DAILY_RR_PATRON_ID);
        row.createCell(5).setCellValue(ScsbConstants.DAILY_RR_CREATED_DATE);
        row.createCell(6).setCellValue(ScsbConstants.DAILY_RR_LAST_UPDATED_DATE);
        row.createCell(7).setCellValue(ScsbConstants.DAILY_RR_REQUESTING_INST);
        row.createCell(8).setCellValue(ScsbConstants.DAILY_RR_OWNING_INSTITUTION);
        row.createCell(9).setCellValue(ScsbConstants.DAILY_RR_DELIVERY_METHOD);
        row.createCell(10).setCellValue(ScsbConstants.DAILY_RR_STATUS);
        setColumnWidthForSheet(xssfSheet);
    }

    private static void setColumnWidthForSheet(XSSFSheet xssfSheet) {
        xssfSheet.setColumnWidth(1, 4000);
        xssfSheet.setColumnWidth(2, 3000);
        xssfSheet.setColumnWidth(4, 3000);
        xssfSheet.setColumnWidth(5, 5000);
        xssfSheet.setColumnWidth(6, 4000);
        xssfSheet.setColumnWidth(7, 4000);
        xssfSheet.setColumnWidth(8, 4000);
        xssfSheet.setColumnWidth(9, 3000);
        xssfSheet.setColumnWidth(10, 6000);
    }

    /**
     * Compare las and scsb sheets.
     *
     * @param xssfWorkbook the xssf workbook
     */
    public void compareLasAndScsbSheets(XSSFWorkbook xssfWorkbook,CellStyle cellStyle) {
            log.info("started comparing las and scsb sheets");
            XSSFSheet sheet1 = xssfWorkbook.getSheetAt(0);
            XSSFSheet sheet2 = xssfWorkbook.getSheetAt(1);
            XSSFSheet sheet3 = xssfWorkbook.createSheet(ScsbConstants.DAILY_RR_COMPARISON);
            xssfWorkbook.setSheetOrder(ScsbConstants.DAILY_RR_COMPARISON,2);
            createHeaderForCompareSheet(sheet3);
            compareTwoSheets(sheet1, sheet2, sheet3,xssfWorkbook,cellStyle);
            log.info("completed comparing las and scsb sheets");
    }

    /**
     * Compare the given las and scsb sheets.
     *
     * @param sheet1       the sheet 1
     * @param sheet2       the sheet 2
     * @param sheet3       the sheet 3
     * @param xssfWorkbook the xssf workbook
     */
    public void compareTwoSheets(XSSFSheet sheet1, XSSFSheet sheet2, XSSFSheet sheet3,XSSFWorkbook xssfWorkbook,CellStyle cellStyle) {
        int firstRow1 = 1;
        int lastRow1 = sheet1.getLastRowNum();
        int createRowSheet3 = 2;
        log.info("started row wise comparison");
        for (int i = firstRow1; i <= lastRow1; i++) {
            XSSFRow row1 = sheet1.getRow(i);
            XSSFRow row2 = sheet2.getRow(i);
            XSSFRow row3 = sheet3.createRow(createRowSheet3);
            createRowSheet3++;
            compareTwoRows(row1, row2, row3,xssfWorkbook,cellStyle);
        }
        log.info("completed row wise comparison");
    }

    /**
     * Compare two rows for the given sheets.
     *
     * @param row1         the row 1
     * @param row2         the row 2
     * @param row3         the row 3
     * @param xssfWorkbook the xssf workbook
     */
    public void compareTwoRows(XSSFRow row1, XSSFRow row2, XSSFRow row3,XSSFWorkbook xssfWorkbook,CellStyle cellStyle) {
        Cell sheet1RequestId = null;
        Cell sheet1Barcode = null;
        Cell sheet1Status = null;
        Cell sheet2RequestId = null;
        Cell sheet2Barcode = null;
        Cell sheet2Status = null;
        String sheet1LasStatus = null;
        String[] sheet1 = new String[3];
        String[] sheet2 = new String[3];
        if (row1 != null){
            sheet1RequestId = getRowValuesForCompare(row1,0);
            sheet1Barcode = getRowValuesForCompare(row1,1);
            sheet1Status = getRowValuesForCompare(row1,10);
        }
        if(row2 != null){
            sheet2RequestId = getRowValuesForCompare(row2,0);
            sheet2Barcode =  getRowValuesForCompare(row2,1);
            sheet2Status = getRowValuesForCompare(row2,10);
        }
        if (checkCellIsNotEmpty(sheet1RequestId)){
            sheet1[0] = sheet1RequestId.getStringCellValue();
        }
        if (checkCellIsNotEmpty(sheet1Barcode)){
            sheet1[1] = sheet1Barcode.getStringCellValue().toUpperCase();
        }
        if (checkCellIsNotEmpty(sheet2RequestId)){
            sheet2[0] = sheet2RequestId.getStringCellValue();
        }
        if (checkCellIsNotEmpty(sheet2Barcode)){
            sheet2[1]=sheet2Barcode.getStringCellValue();
        }
        if (checkCellIsNotEmpty(sheet1Status)){
            sheet1LasStatus = getLasStatusForCompare(sheet1Status, sheet1);
        }
        if(checkCellIsNotEmpty(sheet2Status)){
            sheet2[2] = sheet2Status.getStringCellValue();
        }
        boolean equalRow = Arrays.equals(sheet1, sheet2);
        buildComparisionSheet(row3, xssfWorkbook, sheet1LasStatus, sheet1, sheet2, equalRow,cellStyle);
    }

    private void buildComparisionSheet(XSSFRow row3, XSSFWorkbook xssfWorkbook, String sheet1LasStatus, String[] sheet1, String[] sheet2, boolean equalRow,CellStyle cellStyle) {
        if (equalRow){
            createCellForEqualRow(row3, xssfWorkbook, sheet1LasStatus, cellStyle, sheet1[0], 0, sheet1[1], 1, 2, sheet2[0], 3, sheet2[1], 4, sheet2[2], 5);
            createCell(xssfWorkbook,row3,cellStyle, ScsbConstants.DAILY_RR_MATCHED,6);
        }
        else {
            createCellForEqualRow(row3, xssfWorkbook, sheet1LasStatus, cellStyle, sheet1[0], 0, sheet1[1], 1, 2, sheet2[0], 3, sheet2[1], 4, sheet2[2], 5);
            if (StringUtils.isBlank(sheet1LasStatus) && StringUtils.isNotBlank(sheet2[2])){
                createCellForNotEqualCells(xssfWorkbook,row3, ScsbConstants.DAILY_RR_LAS_NOT_GIVEN_STATUS,6);
            }
            else if (StringUtils.isBlank(sheet2[0])&&StringUtils.isBlank(sheet2[1])&&StringUtils.isBlank(sheet2[2])){
                createCellForNotEqualCells(xssfWorkbook,row3, ScsbConstants.DAILY_RR_NOT_IN_SCSB,6);
            }
            else {
                createCellForNotEqualCells(xssfWorkbook,row3, ScsbConstants.DAILY_RR_MISMATCH,6);
            }
        }
    }

    private void createCellForEqualRow(XSSFRow row3, XSSFWorkbook xssfWorkbook, String sheet1LasStatus, CellStyle cellStyle, String s, int i, String s2, int i2, int i3, String s3, int i4, String s4, int i5, String s5, int i6) {
        createCell(xssfWorkbook, row3, cellStyle, s, i);
        createCell(xssfWorkbook, row3, cellStyle, s2, i2);
        createCell(xssfWorkbook, row3, cellStyle, sheet1LasStatus, i3);
        createCell(xssfWorkbook, row3, cellStyle, s3, i4);
        createCell(xssfWorkbook, row3, cellStyle, s4, i5);
        createCell(xssfWorkbook, row3, cellStyle, s5, i6);
    }

    private String getLasStatusForCompare(Cell sheet1Status, String[] sheet1) {
        String sheet1LasStatus;
        sheet1LasStatus = sheet1Status.getStringCellValue();
        String[] lasAvailableStatusList = imsAvailableCodes.split(",");
        String[] lasNotAvailableStatusList = imsNotAvailableCodes.split(",");
        boolean statusFound = false;
        for (String lasAvailableStatus : lasAvailableStatusList) {
            if(StringUtils.startsWithIgnoreCase(sheet1LasStatus,lasAvailableStatus)){
                sheet1[2] = ScsbCommonConstants.AVAILABLE;
                statusFound = true;
                break;
            }
        }
        if(!statusFound){
            for (String lasNotAvailableStatus : lasNotAvailableStatusList) {
                if(StringUtils.startsWithIgnoreCase(sheet1LasStatus,lasNotAvailableStatus)){
                    sheet1[2] = ScsbCommonConstants.NOT_AVAILABLE;
                    break;
                }
            }
        }
        return sheet1LasStatus;
    }

    private static void createCellForNotEqualCells(XSSFWorkbook xssfWorkbook, XSSFRow row, String cellValue, int cellNum) {
        if (StringUtils.isNotBlank(cellValue)){
            XSSFCell cell = row.createCell(cellNum);
            cell.setCellValue(cellValue);
            CellStyle cellStyle = xssfWorkbook.createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.LEFT);
            Font font = xssfWorkbook.createFont();
            font.setColor(IndexedColors.RED.getIndex());
            font.setFontHeightInPoints((short)10);
            cellStyle.setFont(font);
            cell.setCellStyle(cellStyle);
        }
    }

    private static boolean checkCellIsNotEmpty(Cell cell){
        boolean cellContainsValue = false;
        if (cell != null){
            cellContainsValue = true;
        }
        return  cellContainsValue;
    }

    /**
     * Get cell values for the given row.
     *
     * @param row     the row
     * @param cellNum the cell num
     * @return the cell
     */
    public Cell getRowValuesForCompare(Row row,int cellNum){
            return row.getCell(cellNum);
    }

    private void createHeaderForCompareSheet(XSSFSheet xssfSheet) {
        XSSFRow row = xssfSheet.createRow(0);
        row.createCell(0).setCellValue(ScsbConstants.DAILY_RR_LAS);
        row.createCell(3).setCellValue(ScsbConstants.DAILY_RR_SCSB);
        XSSFRow row1 = xssfSheet.createRow(1);
        row1.createCell(0).setCellValue(ScsbConstants.DAILY_RR_REQUEST_ID);
        row1.createCell(1).setCellValue(ScsbConstants.DAILY_RR_BARCODE);
        row1.createCell(2).setCellValue(ScsbConstants.DAILY_RR_STATUS);
        row1.createCell(3).setCellValue(ScsbConstants.DAILY_RR_REQUEST_ID);
        row1.createCell(4).setCellValue(ScsbConstants.DAILY_RR_BARCODE);
        row1.createCell(5).setCellValue(ScsbConstants.DAILY_RR_STATUS);
        xssfSheet.setColumnWidth(0, 3000);
        xssfSheet.setColumnWidth(1, 4000);
        xssfSheet.setColumnWidth(2, 7500);
        xssfSheet.setColumnWidth(3, 3000);
        xssfSheet.setColumnWidth(4, 4000);
        xssfSheet.setColumnWidth(5, 5000);
        xssfSheet.setColumnWidth(6, 7000);
        log.info("created headers for comparison sheets");
    }
}
