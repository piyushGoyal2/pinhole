package tech.pinhole.service.verticle;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.LocalSessionStore;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * An abstract base verticle that provides several helper methods for REST API.
 *
 * @author piyush.goyal
 */
public abstract class RestAPIVerticle extends BaseMicroserviceVerticle {

  /**
   * Create http server for the REST service.
   *
   * @param router router instance
   * @param host   http host
   * @param port   http port
   * @return async result of the procedure
   */
  protected Future<Void> createHttpServer(Router router, String host, int port) {
    Future<HttpServer> httpServerFuture = Future.future();
    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(port, host, httpServerFuture.completer());
    return httpServerFuture.map(r -> null);
  }


  /**
   * Enable local session storage in requests.
   *
   * @param router router instance
   */
  protected void enableLocalSession(Router router) {
    router.route().handler(CookieHandler.create());
    router.route().handler(SessionHandler.create(
      LocalSessionStore.create(vertx, "shopping.user.session")));
  }

  // helper result handler within a request context

  /**
   * This method generates handler for async methods in REST APIs.
   */
  protected <T> Handler<AsyncResult<T>> resultHandler(RoutingContext context, Handler<T> handler) {
    return res -> {
      if (res.succeeded()) {
        handler.handle(res.result());
      } else {
        internalError(context, res.cause());
        res.cause().printStackTrace();
      }
    };
  }

  /**
   * This method generates handler for async methods in REST APIs.
   * Use the result directly and invoke `toString` as the response. The content type is JSON.
   */
  protected <T> Handler<AsyncResult<T>> resultHandler(RoutingContext context) {
    return ar -> {
      if (ar.succeeded()) {
        T res = ar.result();
        context.response()
          .putHeader("content-type", "application/json")
          .end(res == null ? "{}" : res.toString());
      } else {
        internalError(context, ar.cause());
        ar.cause().printStackTrace();
      }
    };
  }

  /**
   * This method generates handler for async methods in REST APIs.
   * Use the result directly and use given {@code converter} to convert result to string
   * as the response. The content type is JSON.
   *
   * @param context   routing context instance
   * @param converter a converter that converts result to a string
   * @param <T>       result type
   * @return generated handler
   */
  protected <T> Handler<AsyncResult<T>> resultHandler(RoutingContext context, Function<T, String> converter) {
    return ar -> {
      if (ar.succeeded()) {
        T res = ar.result();
        if (res == null) {
          serviceUnavailable(context, "invalid_result");
        } else {
          context.response()
            .putHeader("content-type", "application/json")
            .end(converter.apply(res));
        }
      } else {
        internalError(context, ar.cause());
        ar.cause().printStackTrace();
      }
    };
  }

  /**
   * This method generates handler for async methods in REST APIs.
   * The result requires non-empty. If empty, return <em>404 Not Found</em> status.
   * The content type is JSON.
   *
   * @param context routing context instance
   * @param <T>     result type
   * @return generated handler
   */
  protected <T> Handler<AsyncResult<T>> resultHandlerNonEmpty(RoutingContext context) {
    return ar -> {
      if (ar.succeeded()) {
        T res = ar.result();
        if (res == null) {
          notFound(context);
        } else {
          context.response()
            .putHeader("content-type", "application/json")
            .end(res.toString());
        }
      } else {
        internalError(context, ar.cause());
        ar.cause().printStackTrace();
      }
    };
  }

  /**
   * This method generates handler for async methods in REST APIs.
   * The result is not needed. Only the state of the async result is required.
   *
   * @param context routing context instance
   * @param result  result content
   * @param status  status code
   * @return generated handler
   */
  protected Handler<AsyncResult<Void>> resultVoidHandler(RoutingContext context, JsonObject result, int status) {
    return ar -> {
      if (ar.succeeded()) {
        context.response()
          .setStatusCode(status == 0 ? 200 : status)
          .putHeader("content-type", "application/json")
          .end(result.encodePrettily());
      } else {
        internalError(context, ar.cause());
        ar.cause().printStackTrace();
      }
    };
  }

  protected Handler<AsyncResult<Void>> resultVoidHandler(RoutingContext context, int status) {
    return ar -> {
      if (ar.succeeded()) {
        context.response()
          .setStatusCode(status == 0 ? 200 : status)
          .putHeader("content-type", "application/json")
          .end();
      } else {
        internalError(context, ar.cause());
        ar.cause().printStackTrace();
      }
    };
  }


  protected void notFound(RoutingContext context) {
    context.response().setStatusCode(404)
      .putHeader("content-type", "application/json")
      .end(new JsonObject().put("message", "not_found").encodePrettily());
  }

  protected void internalError(RoutingContext context, Throwable ex) {
    context.response().setStatusCode(500)
      .putHeader("content-type", "application/json")
      .end(new JsonObject().put("error", ex.getMessage()).encodePrettily());
  }

  protected void serviceUnavailable(RoutingContext context, String cause) {
    context.response().setStatusCode(503)
      .putHeader("content-type", "application/json")
      .end(new JsonObject().put("error", cause).encodePrettily());
  }

}
