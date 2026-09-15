package com.example.bandlink.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/**
 * NFT-004, against the real disposable QA database. Deliberately NOT class-level @Transactional
 * (same reasoning as PostConcurrencyIntegrationTest): the two uploads below run on their own worker
 * threads with their own independent DB transactions, the same shape as two browser tabs racing to
 * fill the last image slot on the same post. Cleanup is done for real via JdbcTemplate and the
 * filesystem in @BeforeEach/@AfterEach rather than relying on test-transaction rollback.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${QA_RELEASE_JDBC_URL:jdbc:postgresql://localhost:5432/band_link_release_test}",
        "spring.datasource.username=${QA_RELEASE_DB_USERNAME:postgres}",
        "spring.datasource.password=${QA_RELEASE_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "QA_RELEASE_IT", matches = "true")
class PostImageConcurrencyIntegrationTest {
    private static final String U14 = "qa-release-images@example.test";
    private static final long POST_ID = 920009L;
    // Fixture PNG bytes (a single transparent pixel), same as the one already relied on by
    // ReleaseApiIntegrationTest#sec011 - small, real, and passes ImageStorageService.valid().
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII=");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private final Path uploadsRoot = Paths.get(
            System.getenv().getOrDefault("BANDLINK_UPLOAD_DIR", "uploads")).toAbsolutePath().normalize();

    private Row removedThirdImage;
    private Set<Long> baselineImageIds;
    private Set<String> filesBefore;

    private record Row(long id, long postId, String imageUrl, int sortOrder, Timestamp createdAt) {}

    @BeforeEach
    void bringPostDownToTwoImagesAndSnapshotTheUploadsDirectory() throws IOException {
        assertEquals("band_link_release_test", jdbc.queryForObject("select current_database()", String.class));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from qa_release_fixture where singleton=true and suite='Band Link disposable release QA'",
                Integer.class));
        // The seed fixture gives P009 3 images already (its cap); NFT-004's stated precondition is
        // 2, so a single concurrent upload has a real free slot to race over. Removing the row (not
        // its file) is enough - the physical fixture file it pointed to is never touched or checked.
        var top = jdbc.queryForObject(
                "select id, post_id, image_url, sort_order, created_at from post_images "
                        + "where post_id=? order by sort_order desc limit 1",
                (rs, i) -> new Row(rs.getLong("id"), rs.getLong("post_id"), rs.getString("image_url"),
                        rs.getInt("sort_order"), rs.getTimestamp("created_at")),
                POST_ID);
        removedThirdImage = top;
        jdbc.update("delete from post_images where id=?", top.id());
        baselineImageIds = new HashSet<>(jdbc.queryForList("select id from post_images where post_id=?", Long.class, POST_ID));
        assertEquals(2, baselineImageIds.size(), "precondition: P009 must have exactly 2 images before the race");
        filesBefore = listUploadsRootFiles();
    }

    @AfterEach
    void undoWhateverTheRaceCommittedAndRestoreTheSeededThirdImage() throws IOException {
        List<Long> newIds = jdbc.queryForList("select id from post_images where post_id=?", Long.class, POST_ID)
                .stream().filter(id -> !baselineImageIds.contains(id)).toList();
        for (Long id : newIds) jdbc.update("delete from post_images where id=?", id);
        jdbc.update("insert into post_images(id, post_id, image_url, sort_order, created_at) values (?,?,?,?,?)",
                removedThirdImage.id(), removedThirdImage.postId(), removedThirdImage.imageUrl(),
                removedThirdImage.sortOrder(), removedThirdImage.createdAt());
        // Delete every file this test run wrote to the uploads directory, successful or not - none
        // of them are part of the seeded fixture the rest of the suite depends on.
        Set<String> filesAfter = listUploadsRootFiles();
        filesAfter.removeAll(filesBefore);
        for (String name : filesAfter) Files.deleteIfExists(uploadsRoot.resolve(name));
    }

    @Test
    void nft004_concurrentUploadsNeverExceedThreeImagesAndLeaveNoOrphanFile() throws Exception {
        List<MvcResult> results = fireConcurrently();
        long created = results.stream().filter(r -> r.getResponse().getStatus() == 201).count();
        long rejected = results.stream().filter(r -> r.getResponse().getStatus() >= 400 && r.getResponse().getStatus() < 500).count();
        long serverErrors = results.stream().filter(r -> r.getResponse().getStatus() >= 500).count();

        assertEquals(0, serverErrors, "neither request may surface a raw 500");
        assertEquals(1, created, "exactly one of the two concurrent uploads should fill the last slot");
        assertEquals(1, rejected, "the loser must get a 4xx RuleViolation, not a 500 or a silent 4th image");

        int finalCount = jdbc.queryForObject("select count(*) from post_images where post_id=?", Integer.class, POST_ID);
        assertEquals(3, finalCount, "total images must never exceed the 3-image cap");

        // Orphan check: the only new file the uploads directory may contain after the race is the
        // one file backing the single successful DB row - proving the loser's DataIntegrityViolation
        // catch in PostImageService actually deleted the file it had already written to disk.
        List<String> newDbUrls = jdbc.queryForList(
                "select image_url from post_images where post_id=? and id not in ("
                        + baselineImageIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")",
                String.class, POST_ID);
        assertEquals(1, newDbUrls.size());
        String expectedFileName = newDbUrls.get(0).substring(newDbUrls.get(0).lastIndexOf('/') + 1);

        Set<String> filesAfter = listUploadsRootFiles();
        filesAfter.removeAll(filesBefore);
        assertEquals(Set.of(expectedFileName), filesAfter,
                "uploads directory must contain exactly the winning image's file - no orphan from the loser");
    }

    private Set<String> listUploadsRootFiles() throws IOException {
        if (!Files.isDirectory(uploadsRoot)) return new HashSet<>();
        try (var stream = Files.list(uploadsRoot)) {
            return stream.filter(Files::isRegularFile).map(p -> p.getFileName().toString())
                    .collect(Collectors.toCollection(HashSet::new));
        }
    }

    private List<MvcResult> fireConcurrently() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try {
            Callable<MvcResult> a = () -> upload(barrier, "race-a.png");
            Callable<MvcResult> b = () -> upload(barrier, "race-b.png");
            List<Future<MvcResult>> futures = pool.invokeAll(List.of(a, b));
            return List.of(futures.get(0).get(), futures.get(1).get());
        } finally {
            pool.shutdown();
        }
    }

    private MvcResult upload(CyclicBarrier barrier, String filename) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "image/png", PNG);
        barrier.await();
        return mvc.perform(multipart("/api/posts/{postId}/images", POST_ID).file(file)
                        .with(user(U14).roles("USER")).with(csrf()))
                .andReturn();
    }
}
