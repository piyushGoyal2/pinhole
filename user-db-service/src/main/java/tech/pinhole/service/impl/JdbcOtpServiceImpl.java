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
import tech.pinhole.service.services.Otp;
import tech.pinhole.service.services.OtpService;

/**
 * Created by piyush.goyal on 2/18/17.
 */
public class JdbcOtpServiceImpl extends JdbcRepositoryWrapper implements OtpService {

    private static final Logger logger = LoggerFactory.getLogger(JdbcEmailTokenServiceImpl.class);

    public JdbcOtpServiceImpl(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    @Override
    public OtpService initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
        client.getConnection(connHandler(resultHandler, connection -> {
            connection.execute(CREATE_STATEMENT, r -> {
                resultHandler.handle(r);
                connection.close();
            });
        }));
        return this;
    }

    @Override
    public OtpService addOtp(Otp otp, Handler<AsyncResult<Void>> resultHandler) {
        logger.debug("Add otp request received with otp: {}", otp);
        JsonArray params = new JsonArray().add(otp.getPhoneNumber())
                .add(otp.getOtp())
                .add(otp.getCreationTime());
        this.executeNoResult(params, INSERT_STATEMENT, resultHandler);
        return this;
    }

    @Override
    public OtpService retrieveLastOtp(String phoneNumber, Handler<AsyncResult<Otp>> resultHandler) {
        logger.debug("Retrieve last otp request for phonenumber: {}", phoneNumber);
        JsonArray params = new JsonArray().add(phoneNumber);
        this.retrieveOne(params, FIND_USER_LAST_OTP)
                .map(option -> option.map(otp -> {
                    logger.debug("Retrieve last otp found email token: {}", otp);
                    return new Otp(otp);
                }).orElse(null))
                .setHandler(resultHandler);
        return this;
    }


    private static final String CREATE_STATEMENT = "CREATE TABLE IF NOT EXISTS `user_otp` (\n" +
            "  `phoneNumber` varchar(20) NOT NULL,\n" +
            "  `otp` varchar(45) NOT NULL,\n" +
            "  `creationTime` bigint(20) NOT NULL,\n" +
            "  PRIMARY KEY (`phoneNumber`),\n" +
            "  UNIQUE KEY `phone_UNIQUE` (`phoneNumber`) )";
    private static final String INSERT_STATEMENT = "INSERT INTO user_otp (phoneNumber, otp, creationTime) VALUES (?, ?, ?)";

    private static final String FIND_USER_LAST_OTP = "SELECT * FROM user_otp WHERE phoneNumber = ? ORDER BY creationTime DESC LIMIT 1";

}
