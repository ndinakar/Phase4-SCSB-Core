package org.recap.util;

import com.csvreader.CsvWriter;
import lombok.extern.slf4j.Slf4j;
import org.recap.model.xl.ItemHoldingData;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by dinakarn on 25/02/22.
 */
@Slf4j
@Component
public class CsvUtil {

    /**
     * Write data row for title exception report.
     *
     * @param itemHoldingData the title exception report
     * @param csvOutput            the csv output
     * @throws IOException the io exception
     */
    public void writeDataRowForItemHoldingReport(ItemHoldingData itemHoldingData, CsvWriter csvOutput) throws IOException {
        csvOutput.write(itemHoldingData.getBarcode());
        csvOutput.write(itemHoldingData.getHoldingId());
        csvOutput.write(itemHoldingData.getScsbHoldingId());
        csvOutput.write(itemHoldingData.getItemId());
        csvOutput.endRecord();
    }


    public void writeHeaderRowForItemHoldingReport(CsvWriter csvOutput) throws IOException {
        csvOutput.write("BARCODE");
        csvOutput.write("OWNING_HOLDING_ID");
        csvOutput.write("SCSB_HOLDING_ID");
        csvOutput.write("ITEM_ID");
        csvOutput.endRecord();
    }

}
