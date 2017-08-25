package tech.pinhole.service.services;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A service interface managing otp.
 * <p>
 * This service is an event bus service (aka. service proxy).
 * </p>
 *
 * @author tosheer.kalra
 */
@VertxGen
@ProxyGen
public interface OtpService {

    /**
     * Initialize the persistence.
     * @param resultHandler the result handler will be called as soon as the initialization has been accomplished.
     *                      The async result indicates whether the operation was successful or not.
     * @return Should return for adhering to fluent.
     */
    @Fluent
    OtpService initializePersistence(Handler<AsyncResult<Void>> resultHandler);

    /**
     * Add a otp to the persistence.
     *
     * @param otp       a otp entity that we want to add
     * @param resultHandler the result handler will be called as soon as the otp has been added. The async
     *                      result indicates whether the operation was successful or not.
     */
    @Fluent
    OtpService addOtp(Otp otp, Handler<AsyncResult<Void>> resultHandler);

    /**
     * Retrieve last added otp in for passed phone number from persistence.
     *
     * @param phoneNumber       phonenumber for searching otp.
     * @param resultHandler the result handler will be called when last otp entry for passed  phone number is found or
     *                      no entry is found.
     */
    @Fluent
    OtpService retrieveLastOtp(String phoneNumber, Handler<AsyncResult<Otp>> resultHandler);

}
