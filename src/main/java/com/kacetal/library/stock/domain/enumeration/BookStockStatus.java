package com.kacetal.library.stock.domain.enumeration;

/**
 * The BookStockStatus enumeration.
 */
public enum BookStockStatus {

    AVAILABLE("available"), OUT_OF_STOCK("out_of_stock"), OUT_OF_BORROW("out_of_borrow");

    private final String errorKey;

    BookStockStatus(final String errorKey) {
        this.errorKey = errorKey;
    }

    public String errorKey() {
        return errorKey;
    }
}
