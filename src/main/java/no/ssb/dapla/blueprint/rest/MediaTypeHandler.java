package no.ssb.dapla.blueprint.rest;

import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.webserver.Handler;
import io.helidon.webserver.HttpException;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inspired by {@link io.helidon.webserver.RequestPredicate}.
 * <p>
 * Associate a handler with one or more {@link MediaType}s.
 */
public class MediaTypeHandler implements Handler {

    private final Map<MediaType, Handler> handlers = new LinkedHashMap<>();
    private Handler fallback = (req, res) -> req.next();

    private MediaTypeHandler() {
    }

    /**
     * Creates new empty {@link MediaTypeHandler} instance.
     *
     * @return new empty handler.
     */
    public static MediaTypeHandler create() {
        return new MediaTypeHandler();
    }

    /**
     * Associate a handler with one or more {@link MediaType}s.
     *
     * @param handler    the handler to associate with the mediaTypes.
     * @param mediaTypes the mediaTypes to associate with the handler.
     * @return this handler.
     */
    public MediaTypeHandler accept(Handler handler, MediaType... mediaTypes) {
        for (MediaType mediaType : mediaTypes) {
            var previousHandler = handlers.put(mediaType, handler);
            if (previousHandler != null) {
                throw new IllegalStateException(String.format(
                        "the media type [%s] was already handled by %s", mediaType, previousHandler));
            }
        }
        return this;
    }

    /**
     * Set this handler to fail with HTTP error code 406 if no media type matches.
     *
     * @return this handler.
     */
    public MediaTypeHandler orFail() {
        this.fallback = (req, res) -> {
            req.next(new HttpException("invalid content-type", Http.Status.NOT_ACCEPTABLE_406));
        };
        return this;
    }


    @Override
    public void accept(ServerRequest req, ServerResponse res) {
        for (MediaType acceptedType : req.headers().acceptedTypes()) {
            if (handlers.containsKey(acceptedType)) {
                handlers.get(acceptedType).accept(req, res);
                return;
            }
        }
        this.fallback.accept(req, res);
    }
}
