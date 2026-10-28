package com.doulgs.testyourluck.webhook;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Substitui {{ chave }} pelo valor da variável; ausentes viram "" e aspas são escapadas. */
public final class TemplateRenderer {

    private static final Pattern VAR = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*}}");

    private TemplateRenderer() {
    }

    public static String render(String template, Map<String, String> vars) {
        if (template == null) {
            return "";
        }
        Matcher m = VAR.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = vars.get(m.group(1));
            String safe = value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
            m.appendReplacement(out, Matcher.quoteReplacement(safe));
        }
        m.appendTail(out);
        return out.toString();
    }
}
