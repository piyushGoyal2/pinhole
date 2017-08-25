package tech.pinhole.service.verticle.worker;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import tech.pinhole.service.constant.PinholeServiceAddressConstants;
import tech.pinhole.service.services.Otp;

import java.time.Instant;

/**
 * Worker verticle for sending sms for confirming user's phone number.
 * Created by tosheer.kalra on 15/02/2017.
 */
public class SendOtp extends AbstractVerticle {


    private static final String TWILIO_ACCOUNT_SID = "twilio.account.sid";
    private static final String TWILIO_AUTH_TOKEN = "twilio.auth.token";
    private static final String TWILIO_SENDER_PHONE_NUMBER = "twilio.sender.phone.number";

    @Override
    public void start() throws Exception {

        final JsonObject config = config();

        Twilio.init(config.getString(TWILIO_ACCOUNT_SID), config.getString(TWILIO_AUTH_TOKEN));
        String messageSenderNumber = config.getString(TWILIO_SENDER_PHONE_NUMBER);

        vertx.eventBus().consumer(PinholeServiceAddressConstants.NOTIFICATION_SEND_OTP_MESSAGE_SOURCE_ADDRESS, message -> {
            final String generatedOtp = RandomStringUtils.randomNumeric(4);
            final String smsText = "Your otp is " + generatedOtp;
            System.out.println(smsText);
            final String phoneNumber = (String)message.body();
            Otp otp = new Otp().setPhoneNumber(phoneNumber).setCreationTime(Instant.now().toEpochMilli()).setOtp(generatedOtp);
            //Message.creator(
              //      new PhoneNumber(phoneNumber), new PhoneNumber(messageSenderNumber), smsText).create();
            final String encode = Json.encode(otp);
            message.reply(encode);
        });
    }
}