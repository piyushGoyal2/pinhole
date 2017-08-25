package tech.pinhole.service.verticle;

import io.vertx.core.Vertx;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for {@link NotificationVerticle}.
 */
public class NotificationVerticleTest {


    private static Vertx vertx = Vertx.vertx();

    private String encodedUser;

    @BeforeClass
    public static void bootApp() throws InterruptedException {

       /* Record record = MessageSource.createRecord("test", "test.one");
        publish(record);

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                                .put(SendOtp.TWILIO_ACCOUNT_SID, "ACa0c3e2bf89a92d349afdcc99e32698b6")
                                .put(SendOtp.TWILIO_AUTH_TOKEN, "b0465fa0d167cfa4cfcf508e86d1a028")
                                .put(SendOtp.TWILIO_SENDER_PHONE_NUMBER, "+16084926582")
                );
        final CountDownLatch latch = new CountDownLatch(1);
        vertx.deployVerticle(NotificationVerticle.class.getName(), options, res -> {
            latch.countDown();
        });

        latch.await();*/
    }

    @After
    public void tearDown() throws Exception {
        /*AtomicBoolean completed = new AtomicBoolean();
        vertx.close((v) -> completed.set(true));
        await().untilAtomic(completed, is(true));*/
    }


    @Before
    public void createEventData() {
       /* encodedUser = "+918860005121";*/

    }

    @Test
    public void testGetAndAdd() throws Exception {
        /*vertx.eventBus().send(EventBusTopics.ACCOUNT_SERVICE_USER_CREATED, encodedUser
                *//*,result -> {
                    assertEquals("DONE", result.result().body());
                }*//*
        );*/

    }

    /*private Future<Void> publish(Record record) {
        if (discovery == null) {
            try {
                start();
            } catch (Exception e) {
                throw new IllegalStateException("Cannot create discovery service");
            }
        }

        Future<Void> future = Future.future();
        // publish the service
        discovery.publish(record, ar -> {
            if (ar.succeeded()) {
                registeredRecords.add(record);
                future.complete();
            } else {
                future.fail(ar.cause());
            }
        });

        return future;
    }*/

}