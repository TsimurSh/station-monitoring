package pl.goeuropa.station.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;

@Slf4j
@Service
public class HmacVerificationService {

    private static final String HMAC_ALGO = "HmacSHA256";

    private final byte[] secretBytes;
    private final long timeWindowMs;
    private final Clock clock;

    @Autowired
    public HmacVerificationService(
            @Value("${api.security.hmac.secret}") String secret,
            @Value("${api.security.hmac.time-window-ms:60000}") long timeWindowMs) {
        this(secret, timeWindowMs, Clock.systemUTC());
    }

    HmacVerificationService(String secret, long timeWindowMs, Clock clock) {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.timeWindowMs = timeWindowMs;
        this.clock = clock;
    }

    public boolean verify(String signature, String timestamp) {
        if (signature == null || timestamp == null
                || signature.isBlank() || timestamp.isBlank()) {
            log.debug("HMAC verification failed: missing header");
            return false;
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            log.debug("HMAC verification failed: timestamp is not a number");
            return false;
        }

        if (Math.abs(clock.millis() - ts) > timeWindowMs) {
            log.debug("HMAC verification failed: timestamp outside ±{}ms window", timeWindowMs);
            return false;
        }

        String expected = hmacHex(timestamp);
        byte[] a = signature.getBytes(StandardCharsets.UTF_8);
        byte[] b = expected.getBytes(StandardCharsets.UTF_8);
        return a.length == b.length && MessageDigest.isEqual(a, b);
    }

    private String hmacHex(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGO));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }
}
