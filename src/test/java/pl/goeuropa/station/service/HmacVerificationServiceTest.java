package pl.goeuropa.station.service;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class HmacVerificationServiceTest {

    private static final String SECRET = "test-hmac-secret";
    private static final long WINDOW_MS = 60_000L;
    private static final long FIXED_NOW_MS = 1_700_000_000_000L;

    private final Clock fixedClock = Clock.fixed(
            Instant.ofEpochMilli(FIXED_NOW_MS), ZoneId.of("UTC"));
    private final HmacVerificationService service =
            new HmacVerificationService(SECRET, WINDOW_MS, fixedClock);

    @Test
    void verify_returnsTrue_whenSignatureAndTimestampFresh() {
        String ts = String.valueOf(FIXED_NOW_MS);
        String sig = sign(SECRET, ts);

        assertThat(service.verify(sig, ts)).isTrue();
    }

    @Test
    void verify_returnsTrue_whenTimestampWithinWindowPast() {
        String ts = String.valueOf(FIXED_NOW_MS - 30_000L);
        String sig = sign(SECRET, ts);

        assertThat(service.verify(sig, ts)).isTrue();
    }

    @Test
    void verify_returnsTrue_whenTimestampWithinWindowFuture() {
        String ts = String.valueOf(FIXED_NOW_MS + 30_000L);
        String sig = sign(SECRET, ts);

        assertThat(service.verify(sig, ts)).isTrue();
    }

    @Test
    void verify_returnsFalse_whenTimestampOlderThanWindow() {
        String ts = String.valueOf(FIXED_NOW_MS - WINDOW_MS - 1L);
        String sig = sign(SECRET, ts);

        assertThat(service.verify(sig, ts)).isFalse();
    }

    @Test
    void verify_returnsFalse_whenTimestampInFutureBeyondWindow() {
        String ts = String.valueOf(FIXED_NOW_MS + WINDOW_MS + 1L);
        String sig = sign(SECRET, ts);

        assertThat(service.verify(sig, ts)).isFalse();
    }

    @Test
    void verify_returnsFalse_whenSignatureWrong() {
        String ts = String.valueOf(FIXED_NOW_MS);
        String bogus = sign("other-secret", ts);

        assertThat(service.verify(bogus, ts)).isFalse();
    }

    @Test
    void verify_returnsFalse_whenSignatureLengthDiffers() {
        String ts = String.valueOf(FIXED_NOW_MS);

        assertThat(service.verify("abc", ts)).isFalse();
    }

    @Test
    void verify_returnsFalse_whenTimestampNotANumber() {
        String ts = "not-a-number";
        String sig = sign(SECRET, ts);

        assertThat(service.verify(sig, ts)).isFalse();
    }

    @Test
    void verify_returnsFalse_whenSignatureNull() {
        assertThat(service.verify(null, String.valueOf(FIXED_NOW_MS))).isFalse();
    }

    @Test
    void verify_returnsFalse_whenTimestampNull() {
        assertThat(service.verify(sign(SECRET, "x"), null)).isFalse();
    }

    @Test
    void verify_returnsFalse_whenSignatureBlank() {
        assertThat(service.verify("   ", String.valueOf(FIXED_NOW_MS))).isFalse();
    }

    @Test
    void verify_returnsFalse_whenTimestampBlank() {
        assertThat(service.verify(sign(SECRET, "x"), "")).isFalse();
    }

    private static String sign(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
