package com.lede.telegrambots.infrastructure.security;

import com.lede.telegrambots.application.port.out.WebhookSignatureVerifier;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * GitHub-flavoured HMAC-SHA256 implementation of the {@link WebhookSignatureVerifier} port.
 *
 * <p>Verifies GitHub's {@code X-Hub-Signature-256} header against the raw body using the per-bot
 * secret, with a constant-time comparison to avoid timing attacks.</p>
 */
@Component
public class HmacSha256SignatureVerifier implements WebhookSignatureVerifier {

    private static final String ALGO = "HmacSHA256";
    private static final String PREFIX = "sha256=";

    @Override
    public boolean verify(String secret, String signatureHeader, byte[] body) {
        if (secret == null || secret.isBlank()) {
            return true; // verification disabled for this bot
        }
        if (signatureHeader == null || !signatureHeader.startsWith(PREFIX)) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGO));
            byte[] computed = mac.doFinal(body);
            String expected = PREFIX + toHex(computed);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
