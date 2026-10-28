package com.doulgs.testyourluck.shared.util;

public final class Numbers {

    private Numbers() {
    }

    /** Percentual com uma casa decimal; 0 quando o total é 0. */
    public static double pct(double part, double total) {
        return total <= 0 ? 0 : Math.round(part / total * 1000) / 10.0;
    }
}
