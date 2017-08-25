package tech.pinhole.service.services;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A service interface managing email validation tokens.
 * <p>
 * This service is an event bus service (aka. service proxy).
 * </p>
 *
 * @author tosheer.kalra
 */
@VertxGen
@ProxyGen
public interface EmailTokenService {

    /**
     * Initialize the persistence.
     * @param resultHandler the result handler will be called as soon as the initialization has been accomplished.
     *                      The async result indicates whether the operation was successful or not.
     * @return Should return for adhering to fluent.
     */
    @Fluent
    EmailTokenService initializePersistence(Handler<AsyncResult<Void>> resultHandler);

    /**
     * Add a email token to the persistence.
     *
     * @param emailToken a email token entity that we want to add.
     * @param resultHandler the result handler will be called as soon as the email token has been added. The async
     *                      result indicates whether the operation was successful or not.
     */
    @Fluent
    EmailTokenService addToken(EmailToken emailToken, Handler<AsyncResult<Void>> resultHandler);

    /**
     * Retrieve last added email token in for passed email from persistence.
     *
     * @param email       email for searching email token.
     * @param resultHandler the result handler will be called when last email token entry for passed email is found or
     *                      no entry is found.
     */
    @Fluent
    EmailTokenService retrieveLastToken(String email, Handler<AsyncResult<EmailToken>> resultHandler);

}
