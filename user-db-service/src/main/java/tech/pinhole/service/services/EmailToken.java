package tech.pinhole.service.services;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * @author piyush.goyal
 */
@DataObject(generateConverter = true)
public class EmailToken {

    private String email;

    private String token;

    private Long creationTime;

    public EmailToken(final String email, final String token, final Long creationTime) {
        this.email = email;
        this.creationTime = creationTime;
        this.token = token;

    }
    public EmailToken() {}

    public EmailToken(JsonObject jsonObject) {
        EmailTokenConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        EmailTokenConverter.toJson(this, json);
        return json;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public EmailToken setCreationTime(final Long creationTime) {
        this.creationTime = creationTime;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public EmailToken setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getToken() {
        return token;
    }

    public EmailToken setToken(String token) {
        this.token = token;
        return this;
    }

    @Override
    public String toString() {
        return toJson().encodePrettily();
    }
}
