package tech.pinhole.service.verticle;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Properties;
import java.util.concurrent.Future;

/**
 * Created by tosheer.kalra on 03/03/2017.
 */
public class MessageSubscriberVerticle extends BaseMicroserviceVerticle {

    private static final String MQTT_SERVER_URI = "mqtt.serverURI";
    private static final String MQTT_ALBUM_TOPIC = "mqtt.album.topic";
    private static final String MQTT_ALBUM_QOS = "mqtt.album.qos";
    private static final String KAFKA_SERVER_URI = "kafka.serverURI";
    private static final String KAFKA_ALBUM_TOPIC = "kafka.album.topic";

    @Override
    public void start() throws Exception {

        final JsonObject config = config();

        MqttClient client = new MqttClient(
                config.getString(MQTT_SERVER_URI), RandomStringUtils.randomNumeric(8), new MemoryPersistence());
        client.connect();

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getString(KAFKA_SERVER_URI));
        props.put(ProducerConfig.ACKS_CONFIG, Integer.toString(1));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000);
        final Producer<String, String> producerKafka = new KafkaProducer<>(props);

        client.subscribe(config.getString(MQTT_ALBUM_TOPIC), config.getInteger(MQTT_ALBUM_QOS),
                (topic, mqttMessage) -> {
                    System.out.println(mqttMessage);
                    final Future<RecordMetadata> send = producerKafka.send(
                            new ProducerRecord<>(config.getString(KAFKA_ALBUM_TOPIC), topic, mqttMessage.toString()));
                    System.out.println(send.get().topic());
                });
    }

}
