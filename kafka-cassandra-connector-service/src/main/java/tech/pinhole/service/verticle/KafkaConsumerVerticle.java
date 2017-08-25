package tech.pinhole.service.verticle;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Properties;

/**
 * Created by tosheer.kalra on 03/03/2017.
 */
public class KafkaConsumerVerticle extends BaseMicroserviceVerticle {

    private static final String KAFKA_SERVER_URI = "kafka.serverURI";
    private static final String KAFKA_ALBUM_TOPIC = "kafka.album.topic";
    private static final String KAFKA_GROUP_ID = "kafka.group.id";

    @Override
    public void start() throws Exception {


        final JsonObject config = config();

        Cluster cluster = Cluster.builder()                                                    // (1)
                .addContactPoint("127.0.0.1")
                .build();
        ListenableFuture<Session> session = cluster.connectAsync("pinhole");

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getString(KAFKA_SERVER_URI));
        final String groupId = config.getString(KAFKA_GROUP_ID);
        if (StringUtils.isNotBlank(groupId)) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        }
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        KafkaConsumer<String, String> consumer = KafkaConsumer.create(this.vertx, props);

        consumer.handler(stringStringKafkaConsumerRecord -> {
            System.out.println(stringStringKafkaConsumerRecord.value());
            ListenableFuture<ResultSet> resultSet = Futures.transformAsync(session,
                    session1 -> session1.executeAsync("in"));

            // Use transform with a simple Function to apply a synchronous computation on the result:
            ListenableFuture<String> version = Futures.transform(resultSet,
                    (Function<ResultSet, String>) rs -> rs.one().getString("release_version"));

            // Use a callback to perform an action once the future is complete:
            Futures.addCallback(version, new FutureCallback<String>() {
                public void onSuccess(String version) {
                    System.out.printf("Cassandra version: %s%n", version);
                }

                public void onFailure(Throwable t) {
                    System.out.printf("Failed to retrieve the version: %s%n",
                            t.getMessage());
                }
            });

        });
        consumer.subscribe(config.getString(KAFKA_ALBUM_TOPIC), ar -> {
            if (ar.succeeded()) {
                System.out.println("subscribed");
            } else {
                System.out.println("Could not subscribe " + ar.cause().getMessage());
            }
        });

    }
}
