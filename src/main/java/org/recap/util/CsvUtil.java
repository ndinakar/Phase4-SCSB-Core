package org.recap.util;

import com.csvreader.CsvWriter;
import lombok.extern.slf4j.Slf4j;
import org.recap.model.xl.ItemHoldingData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Created by dinakarn on 25/02/22.
 */
@Slf4j
@Component
public class CsvUtil {
    private static final Logger logger = LoggerFactory.getLogger(CsvUtil.class);
    /**
     * Write data row for title exception report.
     *
     * @param itemHoldingData the title exception report
     * @param csvOutput            the csv output
     * @throws IOException the io exception
     */
    public void writeDataRowForItemHoldingReport(List<ItemHoldingData> itemHoldingDataList, CsvWriter csvOutput) throws IOException {
        for(ItemHoldingData itemHoldingData: itemHoldingDataList) {
            logger.info("DATA:" + itemHoldingData.getBarcode() + " " + itemHoldingData.getHoldingId() + " " + itemHoldingData.getScsbHoldingId() + " " + itemHoldingData.getItemId());
            csvOutput.write(itemHoldingData.getBarcode());
            csvOutput.write(itemHoldingData.getHoldingId());
            csvOutput.write(itemHoldingData.getScsbHoldingId());
            csvOutput.write(itemHoldingData.getItemId());
            csvOutput.endRecord();
        }
    }


    public void writeHeaderRowForItemHoldingReport(CsvWriter csvOutput) throws IOException {
        csvOutput.write("BARCODE");
        csvOutput.write("OWNING_HOLDING_ID");
        csvOutput.write("SCSB_HOLDING_ID");
        csvOutput.write("ITEM_ID");
        csvOutput.endRecord();
    }

}
