package com.doulgs.testyourluck.catalog;

public enum StockStatus {
    OK, BAIXO, ESGOTADO;

    public static StockStatus of(int quantity, int minStock) {
        if (quantity <= 0) {
            return ESGOTADO;
        }
        return quantity <= minStock ? BAIXO : OK;
    }
}
