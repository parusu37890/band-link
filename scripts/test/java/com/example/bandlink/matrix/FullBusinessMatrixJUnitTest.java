package com.example.bandlink.matrix;

import com.example.bandlink.matrix.adapters.AuthProfileFeedbackAdapter;
import com.example.bandlink.matrix.adapters.PostMessageStateAdapter;
import com.example.bandlink.matrix.adapters.SecuritySearchImageReportAdapter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/** Executes one real-code adapter invocation for every JUnit-eligible inventory row. */
public class FullBusinessMatrixJUnitTest {
    private static final Path INVENTORY = Path.of("docs/test-plan/full-case-inventory.csv");
    private static final Path RESULT = Path.of("target/full-business-matrix-results.csv");

    @TestFactory
    Stream<DynamicTest> everyCaseInvokesItsBusinessAdapter() throws IOException {
        List<ReleaseCase> rows = ReleaseCaseCsv.readJUnitCases(INVENTORY);
        Files.createDirectories(RESULT.getParent());
        Files.writeString(RESULT, "TestID,Feature,Adapter,Status\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return rows.stream().map(row -> DynamicTest.dynamicTest(row.testId() + " " + row.feature()
                + " " + row.operation(), () -> execute(row)));
    }

    private static void execute(ReleaseCase row) throws Exception {
        String adapter = switch (row.feature()) {
            case "Authentication", "Profile", "Feedback" -> {
                AuthProfileFeedbackAdapter.execute(row);
                yield "AuthProfileFeedbackAdapter";
            }
            case "Recruitment-post", "Direct-message" -> {
                PostMessageStateAdapter.execute(row);
                yield "PostMessageStateAdapter";
            }
            case "Search", "Image-storage", "Report-admin", "Security" -> {
                SecuritySearchImageReportAdapter.execute(row);
                yield "SecuritySearchImageReportAdapter";
            }
            default -> throw new AssertionError("No business adapter for " + row.feature());
        };
        append(row, adapter);
    }

    private static synchronized void append(ReleaseCase row, String adapter) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(RESULT, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(csv(row.testId()) + ',' + csv(row.feature()) + ',' + csv(adapter) + ",PASS");
            writer.newLine();
        }
    }

    private static String csv(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
