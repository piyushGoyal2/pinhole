package tech.pinhole.service.verticle;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWT;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.auth.jwt.impl.JWTAuthProviderImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.servicediscovery.types.HttpEndpoint;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.pinhole.service.constant.PinholeServiceNameConstants;
import tech.pinhole.service.services.AccountService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.regex.Pattern;

/**
 *
 * API gateway for every call.
 * @author tosheer.kalra
 */
public class APIGatewayVerticle extends RestAPIVerticle {

    private static final Logger logger = LoggerFactory.getLogger(APIGatewayVerticle.class);

    /**
     * Patten for finding the JWT token in authorization header.
     */
    private static final Pattern BEARER = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);

    /**
     * Vertx config constant for API gateway hostname.
     */
    private static final String API_GATEWAY_HTTP_ADDRESS = "api.gateway.http.address";

    /**
     * Default fallback port for api gateway.
     */
    private static final int DEFAULT_PORT = 8787;
    private static final String API_GATEWAY_HTTP_PORT = "api.gateway.http.port";

    private JWTAuth jwtAuth;

    private JWT jwt;

    private AccountService accountService;

    @Override
    public void start(Future<Void> future) throws Exception {
        super.start();

        JsonObject config = new JsonObject()
                .put("keyStore", new JsonObject()
                        .put("type", "jceks")
                        .put("path", "keystore.jceks")
                        .put("password", "secret"));

        this.jwtAuth = JWTAuth.create(vertx, config);

        final JsonObject keyStore = config.getJsonObject("keyStore");

        KeyStore ks = KeyStore.getInstance(keyStore.getString("type", "jceks"));

        // synchronize on the class to avoid the case where multiple file accesses will overlap
        synchronized (JWTAuthProviderImpl.class) {
            final Buffer keystore = vertx.fileSystem().readFileBlocking(keyStore.getString("path"));

            try (InputStream in = new ByteArrayInputStream(keystore.getBytes())) {
                ks.load(in, keyStore.getString("password").toCharArray());
            }
        }

        this.jwt = new JWT(ks, keyStore.getString("password").toCharArray());

        // get HTTP host and port from configuration, or use default value
        final String host = config().getString(API_GATEWAY_HTTP_ADDRESS, "localhost");
        final int port = config().getInteger(API_GATEWAY_HTTP_PORT, DEFAULT_PORT);

        Future<HttpServer> httpServerFuture = configureHTTPServer(host, port);
        Future<Void> apiGatewayPublishedFuture = httpServerFuture.compose(server ->
                    publishApiGateway(host, port));

        apiGatewayPublishedFuture.setHandler(compositeAsyncHandler -> {
            if(compositeAsyncHandler.failed()) {
                System.out.println("failed!! because: " + compositeAsyncHandler.cause());
                future.fail(compositeAsyncHandler.cause());
            } else {
                System.out.println("Successful completion of future!!");
                future.complete();
            }
        });


    }

    private Future<HttpServer> configureHTTPServer(String host, int port) {
        Future<HttpServer> future = Future.future();

        // Use a Vert.x Web router for this REST API.
        Router router = Router.router(vertx);

        // cookie and session handler
        enableLocalSession(router);

        // body handler
        router.route().handler(BodyHandler.create());

        // version handler
        router.get("/api/v").handler(this::apiVersion);
        router.post("/login").handler(this::loginEntryHandler);
        router.post("/user").handler(this::registrationHandler);
        router.get("/user/confirmEmail").handler(this::confirmEmail);
        router.post("/user/confirmOTP").handler(this::confirmOtp);
        router.get("/resendOtp").handler(this::confirmEmail);
        router.get("/resendEmail").handler(this::confirmEmail);

        router.route().handler(JWTAuthHandler.create(jwtAuth));
        router.get("/logout").handler(this::logutHandler);
        router.get("/user").handler(this::userInfoHandler);

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(port, host, ar -> {
                    if (ar.succeeded()) {
                        publishApiGateway(host, port);
                        future.complete();
                        logger.info("API Gateway is running on port " + port);
                        // publish log
                        publishGatewayLog("api_gateway_init_success:" + port);
                    } else {
                        future.fail(ar.cause());
                    }
                });

        return future;
    }

    private void confirmEmail(RoutingContext context) {
        HttpServerResponse response = context.response();
        HttpServerRequest request = context.request();
        Future<HttpClient> future = Future.future();
        HttpEndpoint.getClient(discovery,
                new JsonObject().put("name", PinholeServiceNameConstants.REGISTRATION_HTTP_SERVICE),
                future.completer());

        Handler<AsyncResult<HttpClient>> asyncHttpClient = httpClientAsyncResult -> {
            if (httpClientAsyncResult.failed()) {
                response.setStatusCode(500).end("An error occurred while getting client for user service");
            } else {
                HttpClient httpClient = httpClientAsyncResult.result();
                HttpClientRequest confirmEmailRequest = httpClient.get(request.uri(),
                        findUserResponse -> {
                            response.setStatusCode(findUserResponse.statusCode()).end();
                        }).exceptionHandler(event -> {
                    response.setStatusCode(500).end("Service error while authenticating customer");
                }).setTimeout(10000);

                request.headers().forEach(
                        header -> confirmEmailRequest.putHeader(header.getKey(), header.getValue()));
                confirmEmailRequest.end();
            }
        };
        future.setHandler(asyncHttpClient);


    }

    private void confirmOtp(RoutingContext context) {
        HttpServerResponse response = context.response();
        @Nullable Buffer body = context.getBody();
        Future<HttpClient> future = Future.future();

        HttpEndpoint.getClient(discovery,
                new JsonObject().put("name", PinholeServiceNameConstants.REGISTRATION_HTTP_SERVICE),
                future.completer());

        Handler<AsyncResult<HttpClient>> asyncHttpClient = httpClientAsyncResult -> {
            if(httpClientAsyncResult.failed()) {
                response.setStatusCode(500).end("An error occurred while getting client for user service");
            } else {
                HttpClient httpClient = httpClientAsyncResult.result();
                HttpClientRequest authorization = httpClient.post("/confirmOtp", confirmOtpResponse -> {
                    response.setStatusCode(confirmOtpResponse.statusCode()).end();
                }).exceptionHandler(event -> {
                    response.setStatusCode(500).end("Service error while authenticating customer");
                });

                context.request().headers().forEach(header -> {
                    authorization.putHeader(header.getKey(), header.getValue());
                });
                authorization.write(body).end();

            }
        };
        future.setHandler(asyncHttpClient);
    }

    private void apiVersion(RoutingContext context) {
        HttpServerResponse response = context.response();

        logger.info("Inside logging.");

        response.putHeader("Authorization","Bearer " +
                jwtAuth.generateToken(new JsonObject(), new JWTOptions().setExpiresInSeconds(60L)));
        response
                .end(new JsonObject().put("version", "v1").encodePrettily());

    }

    private void publishGatewayLog(String info) {
        JsonObject message = new JsonObject()
                .put("info", info)
                .put("time", System.currentTimeMillis());
        publishLogEvent("gateway", message);
    }


    private void loginEntryHandler(RoutingContext context) {

        HttpServerResponse response = context.response();
        @Nullable Buffer body = context.getBody();
        System.out.println("body" + body.toString());
        Future<HttpClient> future = Future.future();

        HttpEndpoint.getClient(discovery,
                new JsonObject().put("name", PinholeServiceNameConstants.REGISTRATION_HTTP_SERVICE),
                future.completer());

        Handler<AsyncResult<HttpClient>> asyncHttpClient = httpClientAsyncResult -> {
            if(httpClientAsyncResult.failed()) {
                response.setStatusCode(500).end("An error occurred while getting client for authentication service");
            } else {
                HttpClient httpClient = httpClientAsyncResult.result();
                HttpClientRequest authorization = httpClient.post("/login", findUserResponse -> {
                    if (findUserResponse.statusCode() == 200) {
                        findUserResponse.bodyHandler(responseAsString -> {
                            JsonObject entries = new JsonObject(responseAsString.toString());
                            response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
                            JsonObject payload = new JsonObject()
                                    .put("sub", entries.getLong("id"))
                                    .put("exp", 1747055313);
                            String s = jwtAuth.generateToken(
                                    payload,
                                    new JWTOptions().setExpiresInMinutes(4320L).setIssuer("pinhole.tech"));
                            response.putHeader(HttpHeaders.AUTHORIZATION, "Bearer " +
                                    s);
                            response.setStatusCode(200).end();
                        });
                    } else {
                        response.setStatusCode(findUserResponse.statusCode()).end();
                    }
                }).exceptionHandler(event -> {
                    response.setStatusCode(500).end("Service error while authenticating customer");
                }).setTimeout(10000);

                context.request().headers().forEach(header -> {
                    authorization.putHeader(header.getKey(), header.getValue());
                });
                authorization.write(body).end();

            }
        };
        future.setHandler(asyncHttpClient);
    }

    private void logutHandler(RoutingContext context) {
        context.response().putHeader(HttpHeaders.AUTHORIZATION, StringUtils.EMPTY).setStatusCode(200).end();
    }

    private void registrationHandler(RoutingContext context) {
        HttpServerResponse response = context.response();
        @Nullable Buffer body = context.getBody();
        System.out.println("body" + body.toString());
        Future<HttpClient> future = Future.future();

        HttpEndpoint.getClient(discovery,
                new JsonObject().put("name", PinholeServiceNameConstants.REGISTRATION_HTTP_SERVICE),
                future.completer());

        Handler<AsyncResult<HttpClient>> asyncHttpClient = httpClientAsyncResult -> {
            if(httpClientAsyncResult.failed()) {
                response.setStatusCode(500).end("An error occurred while getting client for user service");
            } else {
                HttpClient httpClient = httpClientAsyncResult.result();
                HttpClientRequest authorization = httpClient.post("/registerUser", registerUserResponse -> {
                    response.setStatusCode(registerUserResponse.statusCode()).end();
                }).exceptionHandler(event -> {
                    response.setStatusCode(500).end("Service error while authenticating customer");
                });

                context.request().headers().forEach(header -> {
                    authorization.putHeader(header.getKey(), header.getValue());
                });
                authorization.write(body).end();

            }
        };
        future.setHandler(asyncHttpClient);
    }

    private void userInfoHandler(RoutingContext context) {
        HttpServerResponse response = context.response();
        HttpServerRequest request = context.request();

        Long customerId = getCustomerId(request);

        if (customerId != 0) {

            Future<HttpClient> future = Future.future();
            HttpEndpoint.getClient(discovery,
                    new JsonObject().put("name", PinholeServiceNameConstants.REGISTRATION_HTTP_SERVICE),
                    future.completer());

            Handler<AsyncResult<HttpClient>> asyncHttpClient = httpClientAsyncResult -> {
                if (httpClientAsyncResult.failed()) {
                    response.setStatusCode(500).end("An error occurred while getting client for user service");
                } else {
                    HttpClient httpClient = httpClientAsyncResult.result();
                    HttpClientRequest userInfoRequest = httpClient.get("/user?id=" + customerId,
                            findUserResponse -> {
                                if (findUserResponse.statusCode() == 200) {
                                    findUserResponse.bodyHandler(responseAsString -> {
                                        response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
                                        response.setStatusCode(200).end(responseAsString.toString());
                                    });
                                } else {
                                    response.setStatusCode(findUserResponse.statusCode()).end();
                                }
                    }).exceptionHandler(event -> {
                        response.setStatusCode(500).end("Service error while authenticating customer");
                    }).setTimeout(10000);

                    request.headers().forEach(
                            header -> userInfoRequest.putHeader(header.getKey(), header.getValue()));
                    userInfoRequest.end();
                }
            };
            future.setHandler(asyncHttpClient);
        }
    }

    private Long getCustomerId(HttpServerRequest request) {

        Long customerId = 0L;
        final String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);
        if (authorization != null) {
            String[] parts = authorization.split(" ");
            if (parts.length == 2) {
                if (BEARER.matcher(parts[0]).matches()) {
                    JsonObject decode = jwt.decode(parts[1]);
                    customerId = decode.getLong("sub");
                }
            }

        }
        return customerId;
    }

}
