package org.recap.model.xl;

import com.poiji.annotation.ExcelCell;
import lombok.Data;

@Data
public class ItemReader {
    @ExcelCell(0)
    private String barcode;

    @ExcelCell(1)
    private String institution;

}
