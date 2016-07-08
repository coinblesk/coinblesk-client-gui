/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.coinblesk.client;

import com.coinblesk.json.v1.*;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * @author Andreas Albrecht
 * @author Alessandro De Carli
 *
 */
public interface CoinbleskWebService {

    @POST("payment/key-exchange")
    Call<KeyTO> keyExchange(@Body KeyTO keyTO);

    // new endpoints
    @POST("full-payment/sign")
    Call<SignTO> sign(@Body SignTO signTO);

    @POST("full-payment/verify")
    Call<VerifyTO> verify(@Body VerifyTO verifyTO);

    // CLTV
    @POST("v1/payment/createTimeLockedAddress")
    Call<TimeLockedAddressTO> createTimeLockedAddress(@Body TimeLockedAddressTO request);

    @POST("v1/payment/signverify")
    Call<SignVerifyTO> signVerify(@Body SignVerifyTO signVerify);

    // exchange Rate
    @GET("wallet/exchangeRate/EUR")
    Call<ExchangeRateTO> eurToUsd();

    @GET("wallet/exchangeRate/CHF")
    Call<ExchangeRateTO> chfToUsd();

    @POST("v1/version")
    Call<VersionTO> version(@Body VersionTO request);

    // login and user related
    @POST("login")
    @FormUrlEncoded
    Call<ResponseBody> login(@Field("username")String username, @Field("password") String password);

    @POST("v1/user/create")
    Call<UserAccountStatusTO> signUp(@Body UserAccountTO request);

    @GET("v1/user/auth/get")
    Call<UserAccountTO> getAccount();

    @POST("v1/user/auth/transfer-p2sh")
    Call<UserAccountTO> transferToP2SH(@Body BaseTO request);

    @GET("v1/user/auth/logout")
    Call<UserAccountStatusTO> logout();

}