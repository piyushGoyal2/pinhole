package tech.pinhole.service.rest.api;

import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.pinhole.service.constant.PinholeServiceNameConstants;
import tech.pinhole.service.services.*;
import tech.pinhole.service.impl.JdbcAccountServiceImpl;
import tech.pinhole.service.impl.JdbcOtpServiceImpl;
import tech.pinhole.service.verticle.RestAPIVerticle;

/**
 * @author piyush.goyal
 */
public class RestUserAccountVerticle extends RestAPIVerticle {

    private static final Logger logger = LoggerFactory.getLogger(UserAccountVerticle.class);
    private static final String VERTX_CONFIG_USER_ACCOUNT_SERVICE_REST_HOST = "user.account.service.rest.host";
    private static final String VERTX_CONFIG_USER_ACCOUNT_SERVICE_REST_PORT = "user.account.service.rest.port";

    private AccountService accountService;
    private OtpService otpService;

    private String host;
    private int port;

    @Override
    public void start(Future<Void> future) throws Exception {

        super.start();
        host = config().getString(VERTX_CONFIG_USER_ACCOUNT_SERVICE_REST_HOST, "0.0.0.0");
        port = config().getInteger(VERTX_CONFIG_USER_ACCOUNT_SERVICE_REST_PORT, 9090);

        accountService = new JdbcAccountServiceImpl(vertx, config());
        otpService = new JdbcOtpServiceImpl(vertx, config());

        Future<Void> httpServerFuture = configureHTTPServer();
        Future<Void> httpEndpointReady = httpServerFuture.compose(server ->
                publishHttpEndpoint(PinholeServiceNameConstants.USER_DB_HTTP_SERVICE, host, port));

        httpEndpointReady.setHandler(httpEndPointHandler -> {

            if (httpEndPointHandler.failed()) {
                logger.error("Future in start for RestUserAccountVerticle failed!! because: {}",
                        httpEndPointHandler.cause());
                future.fail(httpEndPointHandler.cause());
            } else {
                logger.info("Successful completion of RestUserAccountVerticle future!!");
                future.complete();
            }
        });

    }

    private Future<Void> configureHTTPServer() {
        // Use a Vert.x Web router for this REST API.
        Router router = Router.router(vertx);
        router.post("/registerUser").handler(this::createUser);
        router.get("/getUser").handler(this::retrieveOperations);
        router.post("/confirmUser").handler(this::confirmUser);
        router.post("/addOtp").handler(this::addOtp);

        return createHttpServer(router, host, port);
    }


    private void createUser(RoutingContext context) {

        Account account = new Account(context.getBodyAsJson());
        accountService.addAccount(account, resultVoidHandler(context, 201));
    }

    private void retrieveOperations(RoutingContext context) {
        String phone = context.request().getParam("phone");
        accountService.retrieveByPhoneNumber(phone, resultHandlerNonEmpty(context));
    }

    private void confirmUser(RoutingContext context) {
        //to be implememented.
        context.response().setStatusCode(200).end("Yet to be impleme");
    }

    private void addOtp(RoutingContext context) {
        Otp otp = new Otp(context.getBodyAsJson());
        otpService.addOtp(otp, resultVoidHandler(context, 201));
    }
}
