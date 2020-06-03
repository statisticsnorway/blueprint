package no.ssb.dapla.blueprint.git;

import io.helidon.common.http.Http;
import io.helidon.webserver.Handler;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;

public class GithubHookVerifier implements Handler {

    private static final String HMAC_SHA1 = "HmacSHA1";
    private static final Logger logger = LoggerFactory.getLogger(GitHookService.class);
    private static final int SIGNATURE_LENGTH = 45;
    private static final String SHA_PREFIX = "sha1=";
    private static final String HEX_CHARS = "0123456789abcdef";
    private static final String HEADER_NAME = "X-Hub-Signature";

    private final SecretKeySpec signingKey;
    private final Mac mac;

    public GithubHookVerifier(String secret) throws NoSuchAlgorithmException {
        this.mac = Mac.getInstance(HMAC_SHA1);
        signingKey = new SecretKeySpec(Objects.requireNonNull(secret).getBytes(), HMAC_SHA1);
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

    @Override
    public void accept(ServerRequest request, ServerResponse response) {

        Optional<String> signatureHeader = request.headers()
                .value(HEADER_NAME)
                .filter(value -> value.length() != SIGNATURE_LENGTH);

        if (signatureHeader.isEmpty()) {
            logger.warn("missing signature for request {}", request);
            response.status(Http.Status.FORBIDDEN_403).send();
            return;
        }

        final String signature = signatureHeader.get();
        final var threadSafeMac = getThreadSafeMac();
        request.content().as(byte[].class).thenAccept((byte[] bytes) -> {
            try {
                threadSafeMac.init(signingKey);
                // TODO: Note sure the body.toString() call return the byte signed against.
                final String expected = SHA_PREFIX + encodeHex(threadSafeMac.doFinal(bytes));
                if (!expected.equals(signature)) {
                    response.status(Http.Status.FORBIDDEN_403).send();
                } else {
                    request.next();
                }
            } catch (InvalidKeyException e) {
                request.next(e);
            }
        });
    }
}
