package tech.pinhole.service.verticle;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.base.Function;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Created by tosheer.kalra on 4/8/17.
 */
public class UserAuthorizerVerticle extends BaseMicroserviceVerticle {


    private static final String USER_AUTHORIZATION_TABLE = "user_authorization";
    private static final String CASSANDRA_HOSTNAME = "cassandra.hostname";
    private static final String CASSANDRA_DB_KEYSPACE = "cassandra.db.keyspace";
    private static final String WEBHOOKS_PORT = "webhooks.port";
    private static final String WEBHOOK_ENDPOINT_AUTH_PUBLISH = "/auth/publish";
    private static final String WEBHOOK_ENDPOINT_AUTH_SUBSCRIBE = "/auth/subscribe";
    private static final String WEBHOOK_ENDPOINT_AUTH_REGISTER = "/auth/register";
    private static final String WEBHOOK_ENDPOINT_UNSUBSCRIBE = "/unsubscribe";
    private static final String MQTT_TOPIC = "topic";
    private static final String MQTT_TOPICS = "topics";
    private static final String MQTT_MESSAGE_PAYLOAD = "payload";
    private static final String MQTT_MESSAGE_PAYLOAD_EVENT_TYPE = "event_type";
    private static final String MQTT_MESSAGE_CREATE_ALBUM = "create_album";
    private static final String RESPONSE_RESULT_OK = "{'result': 'ok'}";
    private static final String RESPONSE_RESULT_ERROR = "{'result': 'error'}";
    private static final String USER_AUTHORIZATION_COLUMN_STATUS = "status";
    private static final String USER_AUTHORIZATION_COLUMN_USERID = "userid";
    private static final String MQTT_MESSAGE_PAYLOAD_CLIENT_ID = "client_id";
    private static final String USER_AUTHORIZATION_COLUMN_ALBUMID = "albumid";
    private static final String MQTT_MESSAGE_PAYLOAD_ALBUM_ID = "album_id";
    private static final String MQTT_MESSAGE_PAYLOAD_ALBUM_OWNER = "album_owner";
    private static final String USER_AUTHORIZATION_COLUMN_OPERATION_TIMESTAMP = "operationTimestamp";
    private static final String PINHOLE_ALBUMS_PREFIX = "/pinhole/albums/";
    private static final String USER_AUTHORIZATION_COLUMN_STATUS_VALUE = "yes";

    ListenableFuture<Session> session;


    private String keyspace;

    @Override
    public void start(Future<Void> future) throws Exception {
        keyspace = config().getString(CASSANDRA_DB_KEYSPACE);
        super.start();
        Cluster cluster = Cluster.builder()
                .addContactPoint(config().getString(CASSANDRA_HOSTNAME))
                .build();
        session = cluster.connectAsync(keyspace);
        startWeebhooksEndpoints(http -> completeStartup(http, future));
    }

    private void startWeebhooksEndpoints(Handler<AsyncResult<HttpServer>> next) {

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        //TODO remove this code.
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader( HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
                    .end("Welcome");
        });


        router.post(WEBHOOK_ENDPOINT_AUTH_PUBLISH).handler(this::authOnPublish);
        router.post(WEBHOOK_ENDPOINT_AUTH_SUBSCRIBE).handler(this::authOnSubscribe);
        router.post(WEBHOOK_ENDPOINT_AUTH_REGISTER).handler(this::authOnRegister);
        router.post(WEBHOOK_ENDPOINT_UNSUBSCRIBE).handler(this::onUnsubscribe);

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        config().getInteger("http.port", config().getInteger(WEBHOOKS_PORT)),
                        next::handle
                );
    }


    private void authOnPublish(RoutingContext routingContext) {

        final JsonObject bodyAsJson = routingContext.getBodyAsJson();

        String topic = bodyAsJson.getString(MQTT_TOPIC);
        String decodePayload = new String(Base64.getDecoder().decode(bodyAsJson.getString(MQTT_MESSAGE_PAYLOAD)));
        JsonObject payloadJson = new JsonObject(decodePayload);
        String event_type = payloadJson.getString(MQTT_MESSAGE_PAYLOAD_EVENT_TYPE);

        if (topic != null ) {
            if (MQTT_MESSAGE_CREATE_ALBUM.equalsIgnoreCase(event_type)) {
                addOwner(payloadJson);
                createSuccessResponse(routingContext);
                return;
            }

            Statement retrieveStatus = QueryBuilder.select(USER_AUTHORIZATION_COLUMN_STATUS)
                    .from(keyspace, USER_AUTHORIZATION_TABLE)
                    .where(QueryBuilder.eq(USER_AUTHORIZATION_COLUMN_USERID, bodyAsJson.getString(MQTT_MESSAGE_PAYLOAD_CLIENT_ID)))
                    .and(QueryBuilder.eq(USER_AUTHORIZATION_COLUMN_ALBUMID, topic));

            AsyncFunction<Session, ResultSet> asyncFunction = session -> session.executeAsync(retrieveStatus);
            ListenableFuture<ResultSet> resultSetFuture = Futures.transformAsync(session, asyncFunction);

            Function<ResultSet, Boolean> function = resultSet -> {
                if(!resultSet.iterator().hasNext()) {
                    return false;
                } else {
                    return true;
                }
            };
            ListenableFuture<Boolean> status = Futures.transform(resultSetFuture, function);

            // Use a callback to perform an action once the future is complete:
            Futures.addCallback(status, new FutureCallback<Boolean>() {
                public void onSuccess(Boolean status) {
                    if (status) {
                        createSuccessResponse(routingContext);
                        return;
                    } else {
                        createFailureResponse(routingContext);
                        return;
                    }
                }
                public void onFailure(Throwable t) {
                    createFailureResponse(routingContext);
                    return;
                }
            });

        } else {
            createFailureResponse(routingContext);
        }

    }

    private void authOnSubscribe(RoutingContext routingContext) {

        final JsonObject bodyAsJson = routingContext.getBodyAsJson();
        System.out.println("Auth On Subscribe " + bodyAsJson);
        System.out.println("topics " + bodyAsJson.getJsonArray(MQTT_TOPICS));
        JsonArray topics = bodyAsJson.getJsonArray(MQTT_TOPICS);

        if (topics != null) {
            JsonObject jsonObject = topics.getJsonObject(0);
            String topic = jsonObject.getString(MQTT_TOPIC);
            System.out.println("topic " + topic);
            Statement retrieveStatus = QueryBuilder.select(USER_AUTHORIZATION_COLUMN_STATUS)
                    .from(keyspace, USER_AUTHORIZATION_TABLE)
                    .where(QueryBuilder.eq(USER_AUTHORIZATION_COLUMN_USERID, bodyAsJson.getString(MQTT_MESSAGE_PAYLOAD_CLIENT_ID)))
                    .and(QueryBuilder.eq(USER_AUTHORIZATION_COLUMN_ALBUMID, topic));

            System.out.println(retrieveStatus.toString());

            AsyncFunction<Session, ResultSet> asyncFunction = session -> session.executeAsync(retrieveStatus);
            ListenableFuture<ResultSet> resultSetFuture = Futures.transformAsync(session, asyncFunction);

            Function<ResultSet, Boolean> function = resultSet -> {
                if (!resultSet.iterator().hasNext()) {
                    return false;
                } else {
                    return true;
                }
            };
            ListenableFuture<Boolean> status = Futures.transform(resultSetFuture, function);

            // Use a callback to perform an action once the future is complete:
            Futures.addCallback(status, new FutureCallback<Boolean>() {
                public void onSuccess(Boolean status) {
                    if (status) {
                        createSuccessResponse(routingContext);
                    } else {
                        createFailureResponse(routingContext);
                    }
                }
                public void onFailure(Throwable t) {
                    createFailureResponse(routingContext);
                }
            });

        } else {
            createFailureResponse(routingContext);
        }

    }

    private void onUnsubscribe(RoutingContext routingContext) {

        final JsonObject bodyAsJson = routingContext.getBodyAsJson();
        JsonArray topics = bodyAsJson.getJsonArray(MQTT_TOPICS);
        if (topics != null) {
            String topic = topics.getString(0);
            Statement deleteStatement = QueryBuilder.delete().from(keyspace, USER_AUTHORIZATION_TABLE)
                    .where(QueryBuilder.eq(USER_AUTHORIZATION_COLUMN_ALBUMID, topic)).and(QueryBuilder.eq(USER_AUTHORIZATION_COLUMN_USERID, bodyAsJson.getString(MQTT_MESSAGE_PAYLOAD_CLIENT_ID)));
            System.out.println(deleteStatement.toString());

            AsyncFunction<Session, ResultSet> asyncFunction = session -> session.executeAsync(deleteStatement);
            ListenableFuture<ResultSet> resultSetFuture = Futures.transformAsync(session, asyncFunction);

            Function<ResultSet, Boolean> function = resultSet -> {
                if(!resultSet.iterator().hasNext()) {
                    return false;
                } else {
                    return true;
                }
            };
            ListenableFuture<Boolean> status = Futures.transform(resultSetFuture, function);

            // Use a callback to perform an action once the future is complete:
            Futures.addCallback(status, new FutureCallback<Boolean>() {
                public void onSuccess(Boolean status) {
                    if (status) createSuccessResponse(routingContext);
                    else {
                        createFailureResponse(routingContext);
                    }
                }
                public void onFailure(Throwable t) {
                    createFailureResponse(routingContext);
                }
            });

        } else {
            createFailureResponse(routingContext);
        }

    }

    private void authOnRegister(RoutingContext routingContext) {

        final JsonObject bodyAsJson = routingContext.getBodyAsJson();
        System.out.println("Auth On register" + bodyAsJson);
        createSuccessResponse(routingContext);
        /*final JsonObject bodyAsJson = routingContext.getBodyAsJson();
        System.out.println("On Subscribe " + bodyAsJson);
        String jwtToken = bodyAsJson.getString("password");


        // Create a JWT Auth Provider
        JWTAuth jwt = JWTAuth.create(vertx, new JsonObject()
                .put("keyStore", new JsonObject()
                        .put("type", "jceks")
                        .put("path", "keystore.jceks")
                        .put("password", "secret")));

        JsonObject authInfo = new JsonObject().put("jwt", jwtToken);

        jwt.authenticate(authInfo, res -> {
            if (res.succeeded()) {
                final HashMap<String, String> map = new HashMap<>();
                map.put("result", "ok");
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encode(map));
            } else {
                final HashMap<String, String> map = new HashMap<>();
                map.put("result", "error");
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encode(map));
            }});*/

    }


    private void createSuccessResponse(RoutingContext routingContext) {
        routingContext.response()
                .putHeader( HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
                .end(RESPONSE_RESULT_OK);
    }

    private void createFailureResponse(RoutingContext routingContext) {
        routingContext.response()
                .putHeader( HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
                .end(RESPONSE_RESULT_ERROR);
    }

    private void addOwner(JsonObject payloadJson) {

        String album_owner = payloadJson.getString(MQTT_MESSAGE_PAYLOAD_ALBUM_OWNER);
        String album_id = payloadJson.getString(MQTT_MESSAGE_PAYLOAD_ALBUM_ID);

        final Insert insert = QueryBuilder.insertInto(keyspace, USER_AUTHORIZATION_TABLE)
                .value(USER_AUTHORIZATION_COLUMN_USERID, album_owner)
                .value(USER_AUTHORIZATION_COLUMN_ALBUMID, PINHOLE_ALBUMS_PREFIX + album_id)
                .value(USER_AUTHORIZATION_COLUMN_OPERATION_TIMESTAMP, LocalDateTime.now().toString())
                .value(USER_AUTHORIZATION_COLUMN_STATUS, USER_AUTHORIZATION_COLUMN_STATUS_VALUE);

        AsyncFunction<Session, ResultSet> asyncFunction = session -> session.executeAsync(insert.toString());
        ListenableFuture<ResultSet> resultSetFuture = Futures.transformAsync(session, asyncFunction);
        Function<ResultSet, String> function = resultSet -> resultSet.toString();
        ListenableFuture<String> insertResult = Futures.transform(resultSetFuture, function);


        Futures.addCallback(insertResult, new FutureCallback<String>() {
            public void onSuccess(String version) {
                System.out.printf("Insert successful to cassandra");
            }

            public void onFailure(Throwable t) {
                System.out.printf("Insert failed : %s%n", t.getMessage());
            }
        });

    }

    private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
        if (http.succeeded()) {
            fut.complete();
        } else {
            fut.fail(http.cause());
        }
    }

}
