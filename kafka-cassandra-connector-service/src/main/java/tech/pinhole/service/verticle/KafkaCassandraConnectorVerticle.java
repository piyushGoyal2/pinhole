package tech.pinhole.service.verticle;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;

/**
 * @author tosheer.kalra
 */
public class KafkaCassandraConnectorVerticle extends BaseMicroserviceVerticle {


    @Override
    public void start(Future<Void> future) throws Exception {
        deployMessageSubscriberVerticle().setHandler(compositeFutureAsyncResult -> {
            if (compositeFutureAsyncResult.failed()) {
                future.fail(compositeFutureAsyncResult.cause());
            } else {
                future.complete();
            }
        });
    }

    private Future<Void> deployMessageSubscriberVerticle() {
        Future<String> future = Future.future();
        vertx.deployVerticle(new KafkaConsumerVerticle(),
                new DeploymentOptions().setConfig(config()), future.completer());
        return future.map(r -> null);
    }
}
