package com.coinblesk.payments.communications.http;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 22/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */

import com.coinblesk.json.*;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface CoinbleskWebService {

    @POST("payment/key-exchange")
    Call<KeyTO> keyExchange(@Body KeyTO keyTO);

    @POST("payment/refund")
    Call<RefundTO> refund(@Body RefundTO refundTO);

    // new endpoints
    @POST("full-payment/sign")
    Call<SignTO> sign(@Body SignTO signTO);

    @POST("full-payment/verify")
    Call<VerifyTO> verify(@Body VerifyTO verifyTO);

    // CLTV
    @POST("v3/payment/createTimeLockedAddress")
    Call<TimeLockedAddressTO> createTimeLockedAddress(@Body TimeLockedAddressTO request);

    @POST("v3/payment/signverify")
    Call<SignTO> signTx(@Body SignTO sign);

    // exchange Rate
    @GET("wallet/exchangeRate/EUR")
    Call<ExchangeRateTO> eurToUsd();

    @GET("wallet/exchangeRate/CHF")
    Call<ExchangeRateTO> chfToUsd();

}
