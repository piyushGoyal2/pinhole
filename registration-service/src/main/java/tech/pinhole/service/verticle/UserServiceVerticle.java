package tech.pinhole.service.verticle;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.servicediscovery.types.EventBusService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import tech.pinhole.service.constant.PinholeServiceAddressConstants;
import tech.pinhole.service.constant.PinholeServiceNameConstants;
import tech.pinhole.service.services.*;

import java.time.Instant;

/**
 * @author piyush.goyal
 */
public class UserServiceVerticle extends BaseMicroserviceVerticle {

    private static final String USER_ACCOUNT_HTTP_ADDRESS = "user.service.http.address";
    private static final String USER_ACCOUNT_HTTP_PORT = "user.service.http.port";
    private AccountService accountService;

    private String host;
    private int port;

    @Override
    public void start(Future<Void> future) throws Exception {

        super.start();
        host = config().getString(USER_ACCOUNT_HTTP_ADDRESS, "0.0.0.0");
        port = config().getInteger(USER_ACCOUNT_HTTP_PORT, 9091);

        Future<HttpServer> httpServerFuture = configureHTTPServer(port);

        Future<Void> httpServicePublishedFuture = httpServerFuture.compose(server ->
                publishHttpEndpoint(PinholeServiceNameConstants.REGISTRATION_HTTP_SERVICE, host, port));

        Future<AccountService> accountServiceFuture = getAccountServiceProxy();

        Future<Void> messageSourceForOtpConfirmation = createMessageSourceForPhoneNumberConfirmation();
        Future<Void> messageSourceForEmailConfirmation = createMessageSourceForEmailConfirmation();

        CompositeFuture.all(
                httpServicePublishedFuture,
                accountServiceFuture,
                messageSourceForEmailConfirmation,
                messageSourceForOtpConfirmation)
                .setHandler(compositeAsyncHandler -> {

            if(compositeAsyncHandler.failed()) {
                System.out.println("Future in start failed!! because: " + compositeAsyncHandler.cause());
                future.fail(compositeAsyncHandler.cause());
            } else {
                accountService = accountServiceFuture.result();
                System.out.println("Successful completion of future!!");
                future.complete();
            }
        });
    }

    private Future<AccountService> getAccountServiceProxy() {

        Future<AccountService> future = Future.future();
        EventBusService.getServiceProxyWithJsonFilter(discovery,
                new JsonObject().put("name", PinholeServiceNameConstants.USER_ACCOUNT_DB_EVENT_BUS_SERVICE),
                AccountService.class,
                future.completer()
        );
       return future;
    }

    private Future<Void> createMessageSourceForPhoneNumberConfirmation() {

        return publishMessageSource(PinholeServiceNameConstants.REGISTRATION_USER_PHONE_CREATED_MESSAGE_SOURCE_SERVICE,
                PinholeServiceAddressConstants.REGISTRATION_USER_PHONE_CREATED_MESSAGE_SOURCE_ADDRESS);

    }

    private Future<Void> createMessageSourceForEmailConfirmation() {

        return publishMessageSource(PinholeServiceNameConstants.REGISTRATION_USER_EMAIL_CREATED_MESSAGE_SOURCE_SERVICE,
                PinholeServiceAddressConstants.REGISTRATION_USER_EMAIL_CREATED_MESSAGE_SOURCE_ADDRESS);

    }

    private Future<HttpServer> configureHTTPServer(final int port) {
        Future<HttpServer> future = Future.future();

        // Use a Vert.x Web router for this REST API.
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.get("/user").handler(this::findUser);
        router.post("/registerUser").handler(this::registerUser);
        router.post("/confirmOtp").handler(this::confirmUserWithOtp);
        router.get("/user/confirmEmail").handler(this::confirmUserWithEmail);
        router.get("/resendOtp").handler(this::resendOtp);
        router.get("/resendEmail").handler(this::resendConfirmationEmail);
        router.post("/login").handler(this::authenticateUser);

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(port, future.completer());

        return future;
    }

    private void registerUser(final RoutingContext context) {

        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        @Nullable String phoneNumber = request.getFormAttribute("phoneNumber");
        @Nullable String email = request.getFormAttribute("email");
        @Nullable String password = request.getFormAttribute("password");
        @Nullable String firstName = request.getFormAttribute("firstName");
        @Nullable String lastName = request.getFormAttribute("lastName");

        Account account = new Account().setPassword(password)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPhoneNumber(phoneNumber);

        Future<Void> accountAddedFuture = Future.future();

        if (StringUtils.isNoneBlank(phoneNumber, password)) {
            if (StringUtils.isNotBlank(email)) {
                account.setEmail(email);
            }
            accountService.addAccount(account, accountAddedFuture.completer());
            if (accountAddedFuture.failed()) {
                System.err.println("Register user failed because:" + accountAddedFuture.cause());
                response.setStatusCode(500).setStatusMessage("Register user failed because:"
                        + accountAddedFuture.cause()).end();
            } else {
                System.err.println("Register success.");
                //create the body with phone number, date etc..!! and send as message.
                this.vertx.eventBus().send(
                        PinholeServiceAddressConstants.REGISTRATION_USER_PHONE_CREATED_MESSAGE_SOURCE_ADDRESS, account.getPhoneNumber());
                if (StringUtils.isNotBlank(email)) {
                    this.vertx.eventBus().send(
                            PinholeServiceAddressConstants.REGISTRATION_USER_EMAIL_CREATED_MESSAGE_SOURCE_ADDRESS, account.getEmail());
                }
                System.err.println("Time response sent  " + Instant.now().toEpochMilli());
                context.response().setStatusCode(201).setStatusMessage("User registered successfully..").end();
            }
        } else {
            response.setStatusCode(400).setStatusMessage("Parameter missing"
                    + accountAddedFuture.cause()).end();
        }

    }

    private void findUser(final RoutingContext context) {

        final String id = context.request().getParam("id");
        final String phoneNumber = context.request().getParam("phoneNumber");
        final String email = context.request().getParam("email");

        System.out.println(id);
        Future<Account> accountRetrieval = Future.future();
        if (StringUtils.isNotBlank(id)) {
            accountService.retrieveAccount(id, accountRetrieval.completer());
        } else if (StringUtils.isNotBlank(phoneNumber)){
            accountService.retrieveByPhoneNumber(phoneNumber, accountRetrieval.completer());
        } else if (StringUtils.isNotBlank(email)){
            accountService.retrieveByEmail(email, accountRetrieval.completer());
        }

        HttpServerResponse response = context.response();

        Handler<AsyncResult<Account>> asyncResultHandler = event -> {
            if(event.failed()) {
                response.setStatusCode(400).end();
            } else {
                response.setStatusCode(200).end(Json.encode(event.result()));
            }
        };

        accountRetrieval.setHandler(asyncResultHandler);
    }

    private void confirmUserWithOtp(final RoutingContext context) {

        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        @Nullable String phoneNumber = request.getFormAttribute("phoneNumber");
        @Nullable String otpAsString = request.getFormAttribute("otp");

        Otp otp = new Otp()
                .setCreationTime(Instant.now().toEpochMilli())
                .setOtp(otpAsString)
                .setPhoneNumber(phoneNumber);

        Future<Void> accountConfirmedFuture = Future.future();
        accountService.confirmUserPhoneNumber(otp, accountConfirmedFuture);

        Handler<AsyncResult<Void>> asyncResultHandler = event -> {
            if(event.failed()) {
                response.setStatusCode(400).end();
            } else {
                response.setStatusCode(200).end();
            }
        };

        accountConfirmedFuture.setHandler(asyncResultHandler);
    }

    private void confirmUserWithEmail(final RoutingContext context) {

        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String email = request.getParam("email");
        String token = request.getParam("token");

        EmailToken emailToken = new EmailToken()
                .setCreationTime(Instant.now().toEpochMilli())
                .setToken(token)
                .setEmail(email);

        Future<Void> emailConfirmationFuture = Future.future();

        accountService.confirmUserEmail(emailToken, emailConfirmationFuture);
        Handler<AsyncResult<Void>> asyncResultHandler = event -> {
            if(event.failed()) {
                response.setStatusCode(400).end();
            } else {
                response.setStatusCode(200).end();
            }
        };

        emailConfirmationFuture.setHandler(asyncResultHandler);
    }

    private void resendOtp(final RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String phoneNumber = request.getParam("phoneNumber");
        this.vertx.eventBus().send(
                PinholeServiceAddressConstants.REGISTRATION_USER_PHONE_CREATED_MESSAGE_SOURCE_ADDRESS, phoneNumber);
        response.setStatusCode(200).end();

    }

    private void resendConfirmationEmail(final RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String email = request.getParam("email");
        this.vertx.eventBus().send(
                PinholeServiceAddressConstants.REGISTRATION_USER_EMAIL_CREATED_MESSAGE_SOURCE_ADDRESS, email);
        response.setStatusCode(200).end();
    }

    private void authenticateUser(final RoutingContext context) {

        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        @Nullable String username = request.getFormAttribute("username");
        @Nullable String password = request.getFormAttribute("password");

        Future<Account> accountRetrieval = Future.future();
        if (StringUtils.isNoneBlank(username, password)) {
            EmailValidator emailValidator = EmailValidator.getInstance();
            if (emailValidator.isValid(username)) {
                accountService.authemnticateAccountForEmail(username, password, accountRetrieval.completer());
            } else {
                accountService.authemnticateAccountForPhoneNumber(username, password, accountRetrieval.completer());
            }

            Handler<AsyncResult<Account>> asyncResultHandler = event -> {
                if(event.failed()) {
                    response.setStatusCode(404).end();
                } else {
                    String encode = Json.encode(event.result());
                    response.setStatusCode(200).end(encode);
                }
            };
            accountRetrieval.setHandler(asyncResultHandler);
        } else {
            response.setStatusCode(400).end();
        }

    }
}
