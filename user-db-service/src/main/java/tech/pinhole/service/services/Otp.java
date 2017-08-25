package tech.pinhole.service.services;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.time.Instant;

/**
 * @author piyush.goyal
 */
@DataObject(generateConverter = true)
public class Otp {

    private String phoneNumber;

    private String otp;

    private Long creationTime;

    public Otp(final String phoneNumber, final String otp, final Long creationTime) {
        this.phoneNumber = phoneNumber;
        this.creationTime = creationTime;
        this.otp = otp;

    }
    public Otp() {}

    public Otp(JsonObject jsonObject) {
        OtpConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        OtpConverter.toJson(this, json);
        return json;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public Otp setCreationTime(final Long creationTime) {
        this.creationTime = creationTime;
        return this;
    }

    public String getOtp() {
        return otp;
    }

    public Otp setOtp(final String otp) {
        this.otp = otp;
        return this;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Otp setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }


    @Override
    public String toString() {
        return toJson().encodePrettily();
    }
}
