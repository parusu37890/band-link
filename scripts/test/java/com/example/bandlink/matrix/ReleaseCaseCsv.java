package com.example.bandlink.matrix;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ReleaseCaseCsv {
    private ReleaseCaseCsv() {}

    public static List<ReleaseCase> readJUnitCases(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) throw new IOException("Inventory is empty: " + path);
        List<String> header = parse(lines.getFirst());
        header.set(0, header.getFirst().replace("\uFEFF", ""));
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.size(); i++) index.put(header.get(i), i);
        String[] required = {"TestID", "RequirementID", "Layer", "Priority", "Feature", "UserState",
                "InputState", "DataState", "Operation", "NetworkState", "Expected", "DBCheck", "APICheck", "UICheck"};
        for (String name : required) if (!index.containsKey(name)) throw new IOException("Missing column: " + name);

        List<ReleaseCase> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int line = 1; line < lines.size(); line++) {
            if (lines.get(line).isBlank()) continue;
            List<String> values = parse(lines.get(line));
            String feature = value(values, index, "Feature");
            if ("Non-functional".equals(feature)) continue;
            ReleaseCase row = new ReleaseCase(
                    value(values, index, "TestID"), value(values, index, "RequirementID"),
                    value(values, index, "Layer"), value(values, index, "Priority"), feature,
                    value(values, index, "UserState"), value(values, index, "InputState"),
                    value(values, index, "DataState"), value(values, index, "Operation"),
                    value(values, index, "NetworkState"), value(values, index, "Expected"),
                    value(values, index, "DBCheck"), value(values, index, "APICheck"), value(values, index, "UICheck"));
            if (!ids.add(row.testId())) throw new IOException("Duplicate TestID: " + row.testId());
            result.add(row);
        }
        if (result.size() != 14_227) throw new IOException("Expected 14227 JUnit rows, got " + result.size());
        return result;
    }

    private static String value(List<String> values, Map<String, Integer> index, String name) {
        int column = index.get(name);
        return column < values.size() ? values.get(column) : "";
    }

    private static List<String> parse(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                result.add(value.toString());
                value.setLength(0);
            } else {
                value.append(c);
            }
        }
        result.add(value.toString());
        return result;
    }
}
