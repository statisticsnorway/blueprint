package no.ssb.dapla.blueprint.rest;

import io.helidon.config.Config;
import io.helidon.webserver.ServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;

public class GithubHookVerifier {

    private static final String HMAC_SHA1 = "HmacSHA1";
    private static final Logger logger = LoggerFactory.getLogger(GithubHookService.class);
    private static final int SIGNATURE_LENGTH = 45;
    private static final String SHA_PREFIX = "sha1=";
    private static final String HEX_CHARS = "0123456789abcdef";
    private static final String HEADER_NAME = "X-Hub-Signature";

    private final SecretKeySpec signingKey;
    private final Mac mac;

    // package private for tests.
    GithubHookVerifier(String secret) throws NoSuchAlgorithmException {
        this.mac = Mac.getInstance(HMAC_SHA1);
        signingKey = new SecretKeySpec(Objects.requireNonNull(secret).getBytes(), HMAC_SHA1);
    }

    public GithubHookVerifier(Config config) throws NoSuchAlgorithmException {
        this(config.get("github.secret").asString().get());
    }

    private Mac getThreadSafeMac() {
        try {
            return (Mac) mac.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    private CharSequence encodeHex(byte[] bytes) {
        char[] hash = new char[2 * bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            hash[2 * i] = HEX_CHARS.charAt((bytes[i] & 0xf0) >> 4);
            hash[2 * i + 1] = HEX_CHARS.charAt(bytes[i] & 0x0f);
        }
        return new String(hash);
    }

    Optional<String> getSignature(ServerRequest request) {
        return request.headers()
                .value(HEADER_NAME)
                .filter(value -> value.length() == SIGNATURE_LENGTH);
    }

    public Boolean checkSignature(String signature, byte[] bytes) throws InvalidKeyException {
        final var threadSafeMac = getThreadSafeMac();
        threadSafeMac.init(signingKey);
        final String expected = SHA_PREFIX + encodeHex(threadSafeMac.doFinal(bytes));
        return expected.equals(signature);
    }

    public Boolean checkSignature(ServerRequest request, byte[] bytes) {
        try {
            Optional<String> signature = getSignature(request);
            if (!signature.isPresent()) {
                return false;
            }
            return checkSignature(signature.get(), bytes);
        } catch (Exception e) {
            return false;
        }
    }
}
