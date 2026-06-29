package com.lede.telegrambots.infrastructure.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HmacSha256SignatureVerifierTest {

    private final HmacSha256SignatureVerifier verifier = new HmacSha256SignatureVerifier();

    @Test
    void blankSecretDisablesVerification() {
        assertTrue(verifier.verify(null, null, "body".getBytes(StandardCharsets.UTF_8)));
        assertTrue(verifier.verify("", "sha256=whatever", "body".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void acceptsValidSignature() throws Exception {
        String secret = "topsecret";
        byte[] body = "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8);
        String header = "sha256=" + hmac(secret, body);

        assertTrue(verifier.verify(secret, header, body));
    }

    @Test
    void rejectsTamperedBody() throws Exception {
        String secret = "topsecret";
        byte[] body = "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8);
        String header = "sha256=" + hmac(secret, body);

        byte[] tampered = "{\"hello\":\"evil\"}".getBytes(StandardCharsets.UTF_8);
        assertFalse(verifier.verify(secret, header, tampered));
    }

    @Test
    void rejectsMissingOrMalformedHeader() {
        byte[] body = "x".getBytes(StandardCharsets.UTF_8);
        assertFalse(verifier.verify("secret", null, body));
        assertFalse(verifier.verify("secret", "md5=abc", body));
    }

    private static String hmac(String secret, byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] out = mac.doFinal(body);
        StringBuilder sb = new StringBuilder(out.length * 2);
        for (byte b : out) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
