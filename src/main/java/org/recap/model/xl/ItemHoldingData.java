package org.recap.model.xl;

import lombok.Data;

@Data
public class ItemHoldingData {
    private String barcode;
    private String holdingId;
    private String scsbHoldingId;
    private String itemId;
}
