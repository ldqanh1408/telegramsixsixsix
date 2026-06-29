package com.lede.telegrambots.application.port.out;

/**
 * Outbound port for verifying an inbound webhook signature against a shared secret.
 *
 * <p>Consumers depend on this interface so the verification algorithm can be swapped or
 * test-doubled without touching webhook-handling code (Dependency Inversion).</p>
 */
public interface WebhookSignatureVerifier {

    /**
     * @param secret          per-bot shared secret; blank/null disables verification
     * @param signatureHeader value of the signature header sent by the provider
     * @param body            raw request body bytes
     * @return {@code true} if verification is disabled or the signature matches
     */
    boolean verify(String secret, String signatureHeader, byte[] body);
}
