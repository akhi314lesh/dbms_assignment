package com.smarthome.smart_home_backend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for InfluxDB 2.x "annotated CSV" query responses.
 *
 * A Flux response can contain several tables (one per series / per yield),
 * each introduced by annotation rows (#group, #datatype, #default) and its own
 * header row. Tables with different schemas are separated by a blank line.
 * Every data row is returned as a column -> string map; the "result" column
 * (the yield name) is kept so callers can tell multiple yields apart.
 */
public final class FluxCsvParser {

    private FluxCsvParser() {}

    public static List<Map<String, String>> parse(String csv) {
        List<Map<String, String>> rows = new ArrayList<>();
        if (csv == null || csv.isBlank()) return rows;

        List<String> header = null;
        List<String> defaults = null;
        boolean expectHeader = true;

        for (String rawLine : csv.split("\r?\n")) {
            if (rawLine.isEmpty() || rawLine.isBlank()) {
                // Blank line = end of a table block; next non-annotation line is a new header.
                expectHeader = true;
                header = null;
                defaults = null;
                continue;
            }
            if (rawLine.startsWith("#")) {
                if (rawLine.startsWith("#default")) {
                    defaults = splitCsvLine(rawLine);   // e.g. "#default,_result,,," -> yield name
                }
                expectHeader = true;
                continue;
            }
            List<String> cells = splitCsvLine(rawLine);
            if (expectHeader || header == null) {
                header = cells;
                expectHeader = false;
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < header.size() && i < cells.size(); i++) {
                String col = header.get(i);
                if (col.isEmpty()) continue;           // leading annotation column
                String val = cells.get(i);
                if (val.isEmpty() && defaults != null && i < defaults.size() && !defaults.get(i).isEmpty()) {
                    val = defaults.get(i);
                }
                row.put(col, val);
            }
            if (!row.isEmpty()) rows.add(row);
        }
        return rows;
    }

    /** RFC 4180 style split: supports quoted cells containing commas and doubled quotes. */
    static List<String> splitCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
