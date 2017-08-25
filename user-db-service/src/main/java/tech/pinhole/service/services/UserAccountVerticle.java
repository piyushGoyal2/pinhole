package tech.pinhole.service.services;

import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ProxyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.pinhole.service.constant.PinholeServiceAddressConstants;
import tech.pinhole.service.constant.PinholeServiceNameConstants;
import tech.pinhole.service.impl.JdbcAccountServiceImpl;
import tech.pinhole.service.impl.JdbcEmailTokenServiceImpl;
import tech.pinhole.service.impl.JdbcOtpServiceImpl;
import tech.pinhole.service.rest.api.RestUserAccountVerticle;
import tech.pinhole.service.verticle.BaseMicroserviceVerticle;

import java.util.ArrayList;
import java.util.List;


/**
 * Verticle which act as a layer on top of User Account DB through which other microservice do DB operations.
 * @author tosheer.kalra
 */
public class UserAccountVerticle extends BaseMicroserviceVerticle {

    private static final Logger logger = LoggerFactory.getLogger(UserAccountVerticle.class);

    private AccountService accountService;
    private OtpService otpService;
    private EmailTokenService emailTokenService;

    @Override
    public void start(Future<Void> future) throws Exception {
        super.start();

        // create the service instance
        accountService = new JdbcAccountServiceImpl(vertx, config());
        otpService = new JdbcOtpServiceImpl(vertx, config());
        emailTokenService = new JdbcEmailTokenServiceImpl(vertx, config());

        // register the service proxy on event bus
        ProxyHelper.registerService(AccountService.class, vertx, accountService,
                PinholeServiceAddressConstants.USER_ACCOUNT_DB_EVENT_BUS_ADDRESS);
        ProxyHelper.registerService(OtpService.class, vertx, otpService,
                PinholeServiceAddressConstants.USER_OTP_DB_EVENT_BUS_ADDRESS);
        ProxyHelper.registerService(EmailTokenService.class, vertx, emailTokenService,
                PinholeServiceAddressConstants.USER_EMAIL_CONFIRM_DB_EVENT_BUS_ADDRESS);

        // publish the service and REST endpoint in the discovery infrastructure
        Future<Void> accountEventBusServicefuture = publishEventBusService(PinholeServiceNameConstants.
                        USER_ACCOUNT_DB_EVENT_BUS_SERVICE, PinholeServiceAddressConstants.USER_ACCOUNT_DB_EVENT_BUS_ADDRESS,
                AccountService.class);
        Future<Void> otpEventBusServicefuture = publishEventBusService(PinholeServiceNameConstants.
                        USER_OTP_DB_EVENT_BUS_SERVICE, PinholeServiceAddressConstants.USER_OTP_DB_EVENT_BUS_ADDRESS,
                OtpService.class);
        Future<Void> confirmEmailEventBusServicefuture = publishEventBusService(PinholeServiceNameConstants.
                        USER_EMAIL_TOKEN_DB_EVENT_BUS_SERVICE, PinholeServiceAddressConstants.USER_EMAIL_CONFIRM_DB_EVENT_BUS_ADDRESS,
                EmailTokenService.class);

        List<Future> futuresList = new ArrayList<>();
        futuresList.add(accountEventBusServicefuture);
        futuresList.add(otpEventBusServicefuture);
        futuresList.add(confirmEmailEventBusServicefuture);
        futuresList.add(deployRestVerticle());
        futuresList.add(initAccountDataBase());
        futuresList.add(initOtpDataBase());
        futuresList.add(initConfirmEmailDataBase());

        CompositeFuture.all(futuresList)
                .setHandler(compositeFutureAsyncResult -> {
                    if (compositeFutureAsyncResult.failed()) {
                        logger.error("Future in start failed!! because: {}", compositeFutureAsyncResult.cause());
                        future.fail(compositeFutureAsyncResult.cause());
                    } else {
                        logger.info("Successful completion of verticle deployment!!");
                        future.complete();
                    }

                });
    }

    private Future<Void> initAccountDataBase() {
        Future<Void> initFuture = Future.future();
        accountService.initializePersistence(initFuture.completer());
        return initFuture;
    }

    private Future<Void> initOtpDataBase() {
        Future<Void> initFuture = Future.future();
        otpService.initializePersistence(initFuture.completer());
        return initFuture;
    }

    private Future<Void> initConfirmEmailDataBase() {
        Future<Void> initFuture = Future.future();
        emailTokenService.initializePersistence(initFuture.completer());
        return initFuture;
    }

    private Future<Void> deployRestVerticle() {
        Future<String> future = Future.future();
        vertx.deployVerticle(new RestUserAccountVerticle(),
                new DeploymentOptions().setConfig(config()),
                future.completer());
        return future.map(r -> null);
    }
}
