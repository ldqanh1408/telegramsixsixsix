package com.lede.telegrambots.github;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class SignatureVerifier {

    private static final String ALGO = "HmacSHA256";
    private static final String PREFIX = "sha256=";

    private SignatureVerifier() {}

    /**
     * Verify GitHub's X-Hub-Signature-256 against the raw request body using the shared secret.
     * Returns true if the secret is empty (verification disabled) OR the signature matches.
     */
    public static boolean verify(String secret, String signatureHeader, byte[] body) {
        if (secret == null || secret.isBlank()) {
            return true; // disabled
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
