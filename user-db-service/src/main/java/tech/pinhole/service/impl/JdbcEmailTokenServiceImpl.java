package tech.pinhole.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.pinhole.service.dao.JdbcRepositoryWrapper;
import tech.pinhole.service.services.Account;
import tech.pinhole.service.services.EmailToken;
import tech.pinhole.service.services.EmailTokenService;
import tech.pinhole.service.services.Otp;

/**
 * Created by piyush.goyal on 2/18/17.
 */
public class JdbcEmailTokenServiceImpl extends JdbcRepositoryWrapper implements EmailTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JdbcEmailTokenServiceImpl.class);

    public JdbcEmailTokenServiceImpl(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    @Override
    public EmailTokenService initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
        client.getConnection(connHandler(resultHandler, connection -> {
            connection.execute(CREATE_STATEMENT, r -> {
                resultHandler.handle(r);
                connection.close();
            });
        }));
        return this;
    }

    @Override
    public EmailTokenService addToken(EmailToken emailToken, Handler<AsyncResult<Void>> resultHandler) {
        logger.debug("Add email token request for email token: {}", emailToken);
        JsonArray params = new JsonArray().add(emailToken.getEmail())
                .add(emailToken.getToken())
                .add(emailToken.getCreationTime());
        this.executeNoResult(params, INSERT_STATEMENT, resultHandler);
        return this;
    }

    @Override
    public EmailTokenService retrieveLastToken(String email, Handler<AsyncResult<EmailToken>> resultHandler) {
        logger.debug("Retrieve last email token request for email: {}", email);
        JsonArray params = new JsonArray().add(email);
        this.retrieveOne(params, FIND_USER_LAST_TOKEN)
                .map(option -> option.map(emailToken -> {
                    logger.debug("Retrieve last email token found email token: {}", emailToken);
                    return new EmailToken(emailToken);
                }).orElse(null))
                .setHandler(resultHandler);
        return this;
    }


    private static final String CREATE_STATEMENT = "CREATE TABLE IF NOT EXISTS `user_email_token` (\n" +
            "  `email` varchar(20) NOT NULL,\n" +
            "  `token` varchar(45) NOT NULL,\n" +
            "  `creationTime` bigint(20) NOT NULL,\n" +
            "  PRIMARY KEY (`email`),\n" +
            "  UNIQUE KEY `phone_UNIQUE` (`email`) )";
    private static final String INSERT_STATEMENT = "INSERT INTO user_email_token (email, token, creationTime) VALUES (?, ?, ?)";
    private static final String FIND_USER_LAST_TOKEN = "SELECT * FROM user_email_token WHERE email = ? ORDER BY creationTime DESC LIMIT 1";

}
