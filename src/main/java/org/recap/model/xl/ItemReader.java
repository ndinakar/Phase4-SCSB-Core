package org.recap.model.xl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poiji.annotation.ExcelCell;
import lombok.Data;

@Data
public class ItemReader {
    @ExcelCell(0)
    private String barcode;

    @ExcelCell(1)
    private String institution;


    @Override
    public String toString() {
        return (this.barcode+", "+this.institution);
    }
}
