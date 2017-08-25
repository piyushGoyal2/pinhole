package tech.pinhole.service.services;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.List;

/**
 * A service interface managing user accounts.
 * <p>
 * This service is an event bus service (aka. service proxy).
 * </p>
 *
 * @author tosheer.kalra
 */
@VertxGen
@ProxyGen
public interface AccountService {

    /**
     * Initialize the persistence.
     *
     * @param resultHandler the result handler will be called as soon as the initialization has been accomplished. The async result indicates
     *                      whether the operation was successful or not.
     */
    @Fluent
    AccountService initializePersistence(Handler<AsyncResult<Void>> resultHandler);

    /**
     * Add a account to the persistence.
     *
     * @param account       a account entity that we want to add.
     * @param resultHandler the result handler will be called as soon as the account has been added. The async result indicates
     *                      whether the operation was successful or not.
     */
    @Fluent
    AccountService addAccount(Account account, Handler<AsyncResult<Void>> resultHandler);

    /**
     * Retrieve the user account with certain `id`.
     *
     * @param id            user account id
     * @param resultHandler the result handler will be called as soon as the user has been retrieved. The async result indicates
     *                      whether the operation was successful or not.
     */
    @Fluent
    AccountService retrieveAccount(String id, Handler<AsyncResult<Account>> resultHandler);

    /**
     * Retrieve the user account with certain `username`.
     *
     * @param phoneNumber      phoneNumber
     * @param resultHandler the result handler will be called as soon as the user has been retrieved. The async result indicates
     *                      whether the operation was successful or not.
     */
    @Fluent
    AccountService retrieveByPhoneNumber(String phoneNumber, Handler<AsyncResult<Account>> resultHandler);

    /**
     * Retrieve the user account with certain `email`.
     *
     * @param email      user email.
     * @param resultHandler the result handler will be called as soon as the user has been retrieved. The async result indicates
     *                      whether the operation was successful or not.
     */
    @Fluent
    AccountService retrieveByEmail(String email, Handler<AsyncResult<Account>> resultHandler);

    /**
     * Retrieve all user accounts.
     *
     * @param resultHandler the result handler will be called as soon as the users have been retrieved. The async result indicates
     *                      whether the operation was successful or not.
     */
    @Fluent
    AccountService retrieveAllAccounts(Handler<AsyncResult<List<Account>>> resultHandler);

    /**
     * Confirm the account and set the confirmation date.
     *
     * @param otp otp which is to be validated.
     * @param resultHandler the result handler will be called as soon as the account has been updated. The async result indicates
     *                      whether the operation was successful or not.
     */
    @Fluent
    AccountService confirmUserPhoneNumber(Otp otp, Handler<AsyncResult<Void>> resultHandler);

    /**
     * Confirm customer email.
     * @param emailConfirm email token which is to validated.
     * @param resultHandler the result handler will be called as soon as the account has been updated. The async result indicates
     *                      whether the operation was successful or not.
     * @return
     */
    @Fluent
    AccountService confirmUserEmail(EmailToken emailConfirm, Handler<AsyncResult<Void>> resultHandler);

    /**
     * Delete a user account from the persistence
     *
     * @param id            user account id
     * @param resultHandler the result handler will be called as soon as the user has been removed. The async result indicates
     *                      whether the operation was successful or not.
     */
    @Fluent
    AccountService deleteAccount(String id, Handler<AsyncResult<Void>> resultHandler);

    /**
     * Delete all user accounts from the persistence
     *
     * @param resultHandler the result handler will be called as soon as the users have been removed. The async result indicates
     *                      whether the operation was successful or not.
     */
    @Fluent
    AccountService deleteAllAccounts(Handler<AsyncResult<Void>> resultHandler);

    /**
     * Authenticate email and password with persistence.
     *
     * @param email user email id.
     * @param password user password.
     * @param resultHandler the result handler will be called as soon as the user has been has been authenticated.
     */
    @Fluent
    AccountService authemnticateAccountForEmail(String email, String password, Handler<AsyncResult<Account>> resultHandler);


    /**
     * Authenticate phonenumber and password with persistence.
     *
     * @param phonenumber user phonenumber.
     * @param password user password.
     * @param resultHandler the result handler will be called as soon as the user has been has been authenticated.
     */
    @Fluent
    AccountService authemnticateAccountForPhoneNumber(String phonenumber, String password, Handler<AsyncResult<Account>> resultHandler);

}
