package tech.pinhole.service.verticle;

import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.servicediscovery.types.MessageSource;
import tech.pinhole.service.constant.PinholeServiceAddressConstants;
import tech.pinhole.service.constant.PinholeServiceNameConstants;
import tech.pinhole.service.services.EmailToken;
import tech.pinhole.service.services.EmailTokenService;
import tech.pinhole.service.services.Otp;
import tech.pinhole.service.services.OtpService;
import tech.pinhole.service.verticle.worker.SendConfirmationEmail;
import tech.pinhole.service.verticle.worker.SendOtp;

/**
 *
 * Vertx verticle whose responsibility is to send notification to user when needed.
 *
 * @author tosheer.kalra
 */
public class NotificationVerticle extends BaseMicroserviceVerticle {

    private EventBus eventBus;
    private OtpService otpService;
    private EmailTokenService emailTokenService;

    @Override
    public void start(Future<Void> future) throws Exception {

        super.start();
        this.eventBus = vertx.eventBus();

        // Deploy worker verticle as these notification sending api are blocking and can block vertx event loop.
        vertx.deployVerticle(new SendOtp(), new DeploymentOptions().setWorker(true).setConfig(config()));
        vertx.deployVerticle(new SendConfirmationEmail(), new DeploymentOptions().setWorker(true).setConfig(config()));

        Future<OtpService> getOtpServiceProxy = getOtpEventBusService();
        Future<EmailTokenService> getEmailTokenServiceProxy = getEmailTokenEventBusService();

        Future<MessageConsumer<String>> messageConsumerForOTPFuture = configureMessageConsumerForOTP();
        Future<MessageConsumer<String>> messageConsumerForEmailFuture = configureMessageConsumerForEmail();

        CompositeFuture.all(getEmailTokenServiceProxy,
                getOtpServiceProxy,
                messageConsumerForOTPFuture,
                messageConsumerForEmailFuture).setHandler(compositeFutureAsyncResult -> {

            if(compositeFutureAsyncResult.failed()) {
                System.out.println("Future in start failed!! because: " + compositeFutureAsyncResult.cause());
                future.fail(compositeFutureAsyncResult.cause());
            } else {
                System.out.println("Successful completion of future!!");
                otpService = getOtpServiceProxy.result();
                emailTokenService = getEmailTokenServiceProxy.result();
                messageConsumerForOTPFuture.result().handler(this::handleMessageToSendOtp);
                messageConsumerForEmailFuture.result().handler(this::handleMessageToConfirmationEmail);
                future.complete();
            }
        });
    }

    /**
     * Returns otp token persistence service from event bus.
     * @return otp token persistence service.
     */
    private Future<OtpService> getOtpEventBusService() {

        Future<OtpService> otpEventBusFuture = Future.future();

        EventBusService.getServiceProxyWithJsonFilter(discovery,
                new JsonObject().put("name", PinholeServiceNameConstants.USER_OTP_DB_EVENT_BUS_SERVICE),
                OtpService.class, otpEventBusFuture.completer()
        );

        return otpEventBusFuture;
    }

    /**
     * Returns email token persistence service from event bus.
     * @return email token persistence service.
     */
    private Future<EmailTokenService> getEmailTokenEventBusService() {

        Future<EmailTokenService> emailTokenServiceFuture = Future.future();

        EventBusService.getServiceProxyWithJsonFilter(discovery,
                new JsonObject().put("name", PinholeServiceNameConstants.USER_EMAIL_TOKEN_DB_EVENT_BUS_SERVICE),
                EmailTokenService.class, emailTokenServiceFuture.completer()
        );

        return emailTokenServiceFuture;
    }

    /**
     * Register a message consumer which listens to message over event bus for sending the otp notification.
     * @return registered future which need to execute once a message is received for sending otp notification..
     */
    private Future<MessageConsumer<String>> configureMessageConsumerForOTP() {

        Future<MessageConsumer<String>> messageConsumerForOTPFuture = Future.future();
        MessageSource.getConsumer(discovery,
                new JsonObject().put("name", PinholeServiceNameConstants.REGISTRATION_USER_PHONE_CREATED_MESSAGE_SOURCE_SERVICE),
                messageConsumerForOTPFuture.completer());
        return messageConsumerForOTPFuture;
    }

    /**
     * Register a message consumer which listens to message over event bus for sending the email notification.
     * @return registered future which need to execute once a message is received for sending email notification.
     */
    private Future<MessageConsumer<String>> configureMessageConsumerForEmail() {

        Future<MessageConsumer<String>> messageConsumerForOTPFuture = Future.future();
        MessageSource.getConsumer(discovery,
                new JsonObject().put("name", PinholeServiceNameConstants.REGISTRATION_USER_EMAIL_CREATED_MESSAGE_SOURCE_SERVICE),
                messageConsumerForOTPFuture.completer());
        return messageConsumerForOTPFuture;
    }

    /**
     * Async handler which is to executed once a message for sending a otp is received over event bus.
     * @param message message for sending a otp is received over event bus.
     */
    private void handleMessageToSendOtp(Message<String> message) {

        Future<Message<String>> otpSentToUserFuture = Future.future();

        // Sends message to OTP message sender worker thread.
        eventBus.send(PinholeServiceAddressConstants.NOTIFICATION_SEND_OTP_MESSAGE_SOURCE_ADDRESS, message.body(),
                otpSentToUserFuture.completer());

        otpSentToUserFuture.setHandler(otpSent -> {
            Future<Void> otpResult = Future.future();
            if (otpSent.failed()) {
                System.out.println("An error occured while sending OTP");
            } else {
                System.out.println("Return message is" + otpSent.result());
                String otpAsString = otpSentToUserFuture.result().body();
                final Otp otp = Json.decodeValue(otpAsString, Otp.class);
                otpService.addOtp(otp, otpResult.completer());
            }
            otpResult.setHandler(result -> {
                if(result.failed()) {
                    System.out.println("Not able send and persist OTP successfully");
                } else {
                    System.out.println("OTP successfully sent and persisted");
                }
            });
        });
    }

    /**
     * Async handler which is to executed once a message for sending a email conformation is received over event bus.
     * @param message message for sending a email conformation is received over event bus.
     */
    private void handleMessageToConfirmationEmail(Message<String> message) {

        Future<Message<String>> emailTokenToUserFuture = Future.future();
        eventBus.send(PinholeServiceAddressConstants.NOTIFICATION_SEND_CONFIRMATION_EMAIL_MESSAGE_SOURCE_ADDRESS, message.body(),
                emailTokenToUserFuture.completer());

        emailTokenToUserFuture.setHandler(emailTokenSent -> {
            Future<Void> emailTokenPersistenceResult = Future.future();
            if (emailTokenSent.failed()) {
                System.out.println("An error occured while sending email token");
            } else {
                System.out.println("Return message is" + emailTokenSent.result());
                String emailTokenAsString = emailTokenToUserFuture.result().body();
                final EmailToken emailToken = Json.decodeValue(emailTokenAsString, EmailToken.class);
                emailTokenService.addToken(emailToken, emailTokenPersistenceResult.completer());
            }
            emailTokenPersistenceResult.setHandler(result -> {
                if(result.failed()) {
                    System.out.println("Not able send and persist Email token successfully");
                } else {
                    System.out.println("Email token successfully sent and persisted");
                }
            });
        });
    }

}
