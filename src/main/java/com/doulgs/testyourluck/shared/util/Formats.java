package com.doulgs.testyourluck.shared.util;

import java.time.ZoneId;

public final class Formats {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private Formats() {
    }

    public static String cpf(String digits) {
        if (digits == null || digits.length() != 11) {
            return digits == null ? "" : digits;
        }
        
        return digits.substring(0, 3) + "." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-" + digits.substring(9);
    }

    public static String phone(String d) {
        if (d == null || d.isEmpty()) {
            return "";
        }
        if (d.length() <= 2) {
            return "(" + d;
        }
        if (d.length() <= 7) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2);
        }
        return "(" + d.substring(0, 2) + ") " + d.substring(2, 7) + "-" + d.substring(7);
    }
}
