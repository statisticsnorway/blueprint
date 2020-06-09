package no.ssb.dapla.blueprint.git;

import no.ssb.dapla.blueprint.HelidonConfigExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(HelidonConfigExtension.class)
class GithubHookVerifierTest {

    private static final String EMPTY_BODY = "sha1=e3466381c9276bc328beb656a2afae7ae784b5ec";
    private static final String EMPTY_OBJECT_BODY = "sha1=5c522b95dd7d86574927070c42555291490bfdf3";
    private static final String SECRET = "hook-secret";

    @Test
    void testEmptyBody() throws NoSuchAlgorithmException, InvalidKeyException {
        GithubHookVerifier verifier = new GithubHookVerifier(SECRET);
        assertThat(verifier.checkSignature(EMPTY_BODY, "".getBytes())).isTrue();
    }

    @Test
    void testEmptyObject() throws NoSuchAlgorithmException, InvalidKeyException {
        GithubHookVerifier verifier = new GithubHookVerifier(SECRET);
        assertThat(verifier.checkSignature(EMPTY_OBJECT_BODY, "{}".getBytes())).isTrue();
    }
}