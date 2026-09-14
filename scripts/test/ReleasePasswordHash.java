import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Fixture preparation, not a test. Run by Runa with the project's dependency classpath. */
class ReleasePasswordHash {
    public static void main(String[] args) {
        String value = System.getenv("QA_RELEASE_PASSWORD");
        if (value == null || value.length() < 8 || value.length() > 72) {
            throw new IllegalArgumentException("Set a disposable QA_RELEASE_PASSWORD of 8..72 characters");
        }
        System.out.print(new BCryptPasswordEncoder().encode(value));
    }
}
