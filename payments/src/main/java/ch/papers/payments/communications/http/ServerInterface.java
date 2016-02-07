package ch.papers.payments.communications.http;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

import ch.papers.payments.models.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 06/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public interface ServerInterface {

    @POST("user/register/")
    Call<User> createUser(@Body User user);

    @POST("refund/")
    Call<TransactionSignature> createRefund(@Body Transaction refundTransaction, @Body TransactionSignature clientSignature);

    @POST("multisig/")
    Call<Transaction> createMultisig(@Body Transaction multisigTransaction);

    @POST("transaction/")
    Call<TransactionSignature> createTransaction(@Body Address recipient, @Body Coin amount);

}
