package org.recap.util;


import lombok.extern.slf4j.Slf4j;
import org.recap.model.xl.ItemHoldingData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
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
    public void writeDataRowForItemHoldingReport(List<ItemHoldingData> itemHoldingDataList, FileWriter csvOutput,StringBuilder data) throws IOException {
        data.append("BARCODE, OWNING_HOLDING_ID, SCSB_HOLDING_ID, ITEM_ID");
        data.append("\n");
        for(ItemHoldingData itemHoldingData: itemHoldingDataList) {
            logger.info("DATA:" + itemHoldingData.getBarcode() + " " + itemHoldingData.getHoldingId() + " " + itemHoldingData.getScsbHoldingId() + " " + itemHoldingData.getItemId());
            data.append(itemHoldingData.getBarcode() + ", " + itemHoldingData.getHoldingId() + ", " + itemHoldingData.getScsbHoldingId() + ", " + itemHoldingData.getItemId());
            data.append("\n");
        }
    }


}
