package tech.pinhole.service.services;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * User account data object
 *
 * @author tosheer.kalra
 */
@DataObject(generateConverter = true)
public class Account {

  private Long id;
  private String phoneNumber;
  private String email;
  private String firstName;
  private String lastName;
  private boolean phoneNumberConfirmed;
  private boolean emailConfirmed;
  private Long confirmationDate;
  private String password;

  public Account() {
    // Empty constructor
  }

  public Account(JsonObject json) {
    AccountConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    AccountConverter.toJson(this, json);
    return json;
  }


  public Long getId() {
    return id;
  }

  public Account setId(Long id) {
    this.id = id;
    return this;
  }

  public String getEmail() {
    return email;
  }

  public Account setEmail(String email) {
    this.email = email;
    return this;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public Account setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
    return this;
  }

  public boolean isPhoneNumberConfirmed() {
    return phoneNumberConfirmed;
  }

  public Account setPhoneNumberConfirmed(boolean phoneNumberConfirmed) {
    this.phoneNumberConfirmed = phoneNumberConfirmed;
    return this;
  }

  public boolean isEmailConfirmed() {
    return emailConfirmed;
  }

  public Account setEmailConfirmed(boolean emailConfirmed) {
    this.emailConfirmed = emailConfirmed;
    return this;
  }

  public Long getConfirmationDate() {
    return confirmationDate;
  }

  public Account setConfirmationDate(Long confirmationDate) {
    this.confirmationDate = confirmationDate;
    return this;
  }

  public String getFirstName() {
    return firstName;
  }

  public Account setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public Account setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public Account setPassword(String password) {
    this.password = password;
    return this;
  }


  @Override
  public String toString() {
    return toJson().encodePrettily();
  }
}
