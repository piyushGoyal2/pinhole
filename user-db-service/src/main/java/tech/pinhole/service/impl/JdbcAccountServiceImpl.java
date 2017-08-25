package tech.pinhole.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.pinhole.service.dao.JdbcRepositoryWrapper;
import tech.pinhole.service.services.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JDBC implementation of {@link AccountService}.
 *
 * @author piyush.goyal
 */
public class JdbcAccountServiceImpl extends JdbcRepositoryWrapper implements AccountService {

    private static final Logger logger = LoggerFactory.getLogger(JdbcAccountServiceImpl.class);
    private static final String VERTX_CONFIG_ACCOUNT_OTP_TTL = "account.otp.ttl";
    private static final String VERTX_CONFIG_ACCOUNT_EMAIL_TOKEN_TTL = "account.email.token.ttl";

    private OtpService otpService;

    private EmailTokenService emailTokenService;

    private Long accountOtpTTL;

    private Long accountEmailTokenTTL;

    public JdbcAccountServiceImpl(Vertx vertx, JsonObject config) {
        super(vertx, config);
        accountOtpTTL = config.getLong(VERTX_CONFIG_ACCOUNT_OTP_TTL, 120000L);
        accountEmailTokenTTL = config.getLong(VERTX_CONFIG_ACCOUNT_EMAIL_TOKEN_TTL, 120000L);
        otpService = new JdbcOtpServiceImpl(vertx, config);
        emailTokenService = new JdbcEmailTokenServiceImpl(vertx, config);
    }

    @Override
    public AccountService initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
        client.getConnection(connHandler(resultHandler, connection -> {
            connection.execute(CREATE_STATEMENT, r -> {
                resultHandler.handle(r);
                connection.close();
            });
        }));
        return this;
    }

    @Override
    public AccountService addAccount(Account account, Handler<AsyncResult<Void>> resultHandler) {
        logger.debug("Add account request for account: {}", account);
        JsonArray params = new JsonArray()
                .add(account.getPhoneNumber())
                .add(account.getEmail())
                .add(account.getFirstName())
                .add(account.getLastName())
                .add(account.getPassword());
        this.executeNoResult(params, INSERT_STATEMENT, resultHandler);
        return this;
    }

    @Override
    public AccountService retrieveAccount(String id, Handler<AsyncResult<Account>> resultHandler) {
        logger.debug("Retrieve account request for account id: {}", id);
        JsonArray params = new JsonArray().add(id);
        this.retrieveOne(params, FETCH_STATEMENT)
                .map(option -> option.map(Account::new).orElse(null))
                .setHandler(resultHandler);
        return this;
    }

    @Override
    public AccountService retrieveByPhoneNumber(String username, Handler<AsyncResult<Account>> resultHandler) {
        logger.debug("Retrieve account request for account phone: {}", username);
        JsonArray params = new JsonArray().add(username);
        this.retrieveOne(params, FETCH_BY_PHONE_NUMBER_STATEMENT)
                .map(option -> option.map(Account::new).orElse(null))
                .setHandler(resultHandler);
        return this;
    }

    @Override
    public AccountService retrieveByEmail(String email, Handler<AsyncResult<Account>> resultHandler) {
        logger.debug("Retrieve account request for account email: {}", email);
        JsonArray params = new JsonArray().add(email);
        this.retrieveOne(params, FETCH_BY_EMAIl_STATEMENT)
                .map(option -> option.map(Account::new).orElse(null))
                .setHandler(resultHandler);
        return this;
    }

    @Override
    public AccountService retrieveAllAccounts(Handler<AsyncResult<List<Account>>> resultHandler) {
        this.retrieveAll(FETCH_ALL_STATEMENT)
                .map(rawList -> rawList.stream()
                        .map(Account::new)
                        .collect(Collectors.toList()))
                .setHandler(resultHandler);
        return this;
    }

    @Override
    public AccountService confirmUserPhoneNumber(Otp otp, Handler<AsyncResult<Void>> resultHandler) {
        logger.debug("Account confirmation request for account with phonenumber: {}", otp.getPhoneNumber());
        Future<Otp> lastOtpRetrievedFuture = Future.future();
        otpService.retrieveLastOtp(otp.getPhoneNumber(), lastOtpRetrievedFuture.completer());
        lastOtpRetrievedFuture.setHandler(otpAsyncResult -> {
            if (otpAsyncResult.failed()) {
                resultHandler.handle(Future.failedFuture(otpAsyncResult.cause()));
            } else {
                long currentTime = Instant.now().toEpochMilli();
                if (StringUtils.equalsIgnoreCase(otpAsyncResult.result().getOtp(), otp.getOtp())
                        && (otp.getCreationTime() + accountOtpTTL >= currentTime)) {
                    JsonArray params = new JsonArray()
                            .add(true)
                            .add(currentTime)
                            .add(otp.getPhoneNumber());
                    this.executeNoResult(params, UPDATE_PHONENUMBER_STATEMENT, resultHandler);
                } else {
                    logger.info("Account confirmation request failed for account with otp: {}", otp);
                    resultHandler.handle(Future.failedFuture("Otp does not match."));
                }
            }
        });
        return this;
    }

    @Override
    public AccountService confirmUserEmail(EmailToken emailToken, Handler<AsyncResult<Void>> resultHandler) {
        logger.debug("Email verification request for account with email: {}", emailToken.getEmail());
        Future<EmailToken> lastTokenRetrievedFuture = Future.future();
        emailTokenService.retrieveLastToken(emailToken.getEmail(), lastTokenRetrievedFuture.completer());
        lastTokenRetrievedFuture.setHandler(tokenAsyncResult -> {

            if (tokenAsyncResult.failed()) {
                resultHandler.handle(Future.failedFuture(tokenAsyncResult.cause()));
            } else {
                if (StringUtils.equalsIgnoreCase(tokenAsyncResult.result().getToken(), emailToken.getToken())
                        && (emailToken.getCreationTime() + accountEmailTokenTTL >= Instant.now().toEpochMilli())) {
                    JsonArray params = new JsonArray()
                            .add(true)
                            .add(emailToken.getEmail());
                    this.executeNoResult(params, UPDATE_EMAIL_CONFIRM_STATEMENT, resultHandler);
                } else {
                    logger.info("Email verification request failed for account with email token :{}", emailToken);
                    resultHandler.handle(Future.failedFuture("token does not match."));
                }
            }
        });
        return this;
    }

    @Override
    public AccountService deleteAccount(String id, Handler<AsyncResult<Void>> resultHandler) {
        logger.debug("Delete account request for account with id: {}", id);
        this.removeOne(id, DELETE_STATEMENT, resultHandler);
        return this;
    }

    @Override
    public AccountService deleteAllAccounts(Handler<AsyncResult<Void>> resultHandler) {
        logger.debug("Delete all account request received.");
        this.removeAll(DELETE_ALL_STATEMENT, resultHandler);
        return this;
    }

    @Override
    public AccountService authemnticateAccountForEmail(
            String email, String password, Handler<AsyncResult<Account>> resultHandler) {
        logger.debug("Authentication request received for account with email {}.", email);
        JsonArray params = new JsonArray().add(email).add(password);
        this.retrieveOne(params, AUTHETICATE_USER_WITH_EMAIL_STATEMENT)
                .map(option -> option.map(account -> {
                    logger.debug("Account found {}.", account);
                    return new Account(account);
                }).orElse(null))
                .setHandler(resultHandler);
        return this;
    }

    @Override
    public AccountService authemnticateAccountForPhoneNumber(
            String phonenumber, String password, Handler<AsyncResult<Account>> resultHandler) {
        logger.debug("Authentication request received for account with phonenumber {}.", phonenumber);
        JsonArray params = new JsonArray().add(phonenumber).add(password);
        this.retrieveOne(params, AUTHETICATE_USER_WITH_PHONENUMBER_STATEMENT)
                .map(option -> option.map(account -> {
                    logger.debug("Account found {}.", account);
                    return new Account(account);
                }).orElse(null))
                .setHandler(resultHandler);
        return this;
    }

    // SQL statement

    private static final String CREATE_STATEMENT = "CREATE TABLE IF NOT EXISTS `user_account` (\n" +
        "  `id` INT NOT NULL AUTO_INCREMENT,\n" +
        "  `phoneNumber` varchar(20) NOT NULL,\n" +
        "  `email` varchar(45) NOT NULL,\n" +
        "  `firstName` varchar(255) NOT NULL,\n" +
        "  `lastName` varchar(255) NOT NULL,\n" +
        "  `password` varchar(255) NOT NULL,\n" +
        "  `isPhoneNumberConfirmed` tinyint(1) NOT NULL DEFAULT 0,\n" +
        "  `isEmailConfirmed` tinyint(1) NOT NULL DEFAULT 0,\n" +
        "  `confirmationDate` bigint(20),\n" +
        "  PRIMARY KEY (`id`),\n" +
        "  UNIQUE KEY `phone_UNIQUE` (`phoneNumber`) )";
    private static final String INSERT_STATEMENT = "INSERT INTO user_account (phoneNumber, email, firstName, lastName, password" +
            ") VALUES (?, ?, ?, ?, ?)";
    private static final String FETCH_STATEMENT = "SELECT * FROM user_account WHERE id = ?";
    private static final String FETCH_BY_PHONE_NUMBER_STATEMENT = "SELECT * FROM user_account WHERE phoneNumber = ?";
    private static final String FETCH_BY_EMAIl_STATEMENT = "SELECT * FROM user_account WHERE email = ?";
    private static final String FETCH_ALL_STATEMENT = "SELECT * FROM user_account";
    private static final String UPDATE_PHONENUMBER_STATEMENT = "UPDATE user_account\n" +
            "SET isPhoneNumberConfirmed = ?,\n" +
            "confirmationDate = ?\n" +
            "WHERE phoneNumber = ?";
    private static final String UPDATE_EMAIL_CONFIRM_STATEMENT = "UPDATE user_account\n" +
            "SET isEmailConfirmed = ?\n" +
            "WHERE email = ?";
    private static final String DELETE_STATEMENT = "DELETE FROM user_account WHERE id = ?";
    private static final String DELETE_ALL_STATEMENT = "DELETE FROM user_account";

    private static final String AUTHETICATE_USER_WITH_PHONENUMBER_STATEMENT =
            "SELECT * FROM user_account WHERE phoneNumber = ? AND password = ?;";

    private static final String AUTHETICATE_USER_WITH_EMAIL_STATEMENT =
            "SELECT * FROM user_account WHERE email = ? AND password = ?;";

}
