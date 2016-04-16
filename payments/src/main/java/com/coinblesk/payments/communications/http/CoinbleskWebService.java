package com.coinblesk.payments.communications.http;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 22/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */

import com.coinblesk.json.KeyTO;
import com.coinblesk.json.RefundTO;
import com.coinblesk.json.SignTO;
import com.coinblesk.json.VerifyTO;
import com.coinblesk.payments.models.ExchangeTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface CoinbleskWebService {
    @GET("admin/info")
    Call<String> info();

    @GET("admin/remove-burned")
    Call<String> resetBurned();

    @POST("payment/key-exchange")
    Call<KeyTO> keyExchange(@Body KeyTO keyTO);

    @POST("payment/refund")
    Call<RefundTO> refund(@Body RefundTO refundTO);

    // new endpoints
    @POST("full-payment/sign")
    Call<SignTO> sign(@Body SignTO signTO);

    @POST("full-payment/verify")
    Call<VerifyTO> verify(@Body VerifyTO verifyTO);

    // exchange Rate
    @GET("wallet/exchangeRate/EUR")
    Call<ExchangeTO> eurToChf();

    @GET("wallet/exchangeRate/USD")
    Call<ExchangeTO> usdToChf();
}
