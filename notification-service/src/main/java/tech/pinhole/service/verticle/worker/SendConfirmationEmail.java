package tech.pinhole.service.verticle.worker;

import com.sendgrid.*;
import com.twilio.Twilio;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import tech.pinhole.service.constant.PinholeServiceAddressConstants;
import tech.pinhole.service.services.EmailToken;
import tech.pinhole.service.services.Otp;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * Worker verticle for sending email address confirm email.
 * Created by tosheer.kalra on 15/02/2017.
 */
public class SendConfirmationEmail extends AbstractVerticle {

    private static final String EMAIL_FROM_ACCOUNT = "email.from";
    private static final String EMAIL_SUBJECT = "email.subject";
    private static final String SENDGRID_API_KEY = "sendgrid.api.key";

    @Override
    public void start() throws Exception {

        final JsonObject config = config();

        Email from = new Email(config.getString(EMAIL_FROM_ACCOUNT));
        String subject = config.getString(EMAIL_SUBJECT);
        SendGrid sg = new SendGrid(config.getString(SENDGRID_API_KEY));

        vertx.eventBus().consumer(PinholeServiceAddressConstants.NOTIFICATION_SEND_CONFIRMATION_EMAIL_MESSAGE_SOURCE_ADDRESS, message -> {

            final String email = (String)message.body();
            Email to = new Email(email);

            UUID uuid = UUID.randomUUID();
            Content content = new Content("text/plain", "Please click here for confirming your email address : " + uuid.toString());
            Mail mail = new Mail(from, subject, to, content);

            Request request = new Request();
            try {
                request.method = Method.POST;
                request.endpoint = "mail/send";
                request.body = mail.build();
                Response response = sg.api(request);
                final String encode =
                        Json.encode(new EmailToken().setEmail(email)
                                .setCreationTime(Instant.now().toEpochMilli())
                                .setToken(uuid.toString()));
                message.reply(encode);
            } catch (IOException ex) {

            }

        });
    }
}