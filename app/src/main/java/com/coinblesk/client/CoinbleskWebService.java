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

import com.coinblesk.dto.ForexDTO;
import com.coinblesk.dto.KeyExchangeRequestDTO;
import com.coinblesk.dto.KeyExchangeResponseDTO;
import com.coinblesk.dto.SignedDTO;
import com.coinblesk.json.v1.*;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * @author Andreas Albrecht
 * @author Alessandro De Carli
 * @author Thomas Bocek
 *
 */
public interface CoinbleskWebService {

    @POST("payment/key-exchange")
    Call<KeyExchangeResponseDTO> keyExchange(@Body KeyExchangeRequestDTO keyExchangeRequestDTO);

    @POST("payment/createTimeLockedAddress")
    Call<SignedDTO> createTimeLockedAddress(@Body SignedDTO request);

    @GET("forex/exchange-rate/bitcoin/bitstamp/current/{symbol}")
    Call<ForexDTO> exchangeRateBTC(@Path("symbol") String symbol);

    @POST("payment/virtualbalance")
    Call<SignedDTO> virtualBalance(@Body SignedDTO request);


    // new endpoints
    @POST("full-payment/sign")
    @Headers("Accept: application/vnd.coinblesk.v4+json")
    Call<SignTO> sign(@Body SignTO signTO);

    @POST("full-payment/verify")
    @Headers("Accept: application/vnd.coinblesk.v4+json")
    Call<VerifyTO> verify(@Body VerifyTO verifyTO);



    @POST("v1/payment/signverify")
    @Headers("Accept: application/vnd.coinblesk.v4+json")
    Call<SignVerifyTO> signVerify(@Body SignVerifyTO signVerify);



    // login and user related
    @POST("login")
    @FormUrlEncoded
    @Headers("Accept: application/vnd.coinblesk.v4+json")
    Call<ResponseBody> login(@Field("username")String username, @Field("password") String password);

    @POST("v1/user/create")
    @Headers("Accept: application/vnd.coinblesk.v4+json")
    Call<UserAccountStatusTO> signUp(@Body UserAccountTO request);

    @GET("v1/user/auth/get")
    @Headers("Accept: application/vnd.coinblesk.v4+json")
    Call<UserAccountTO> getAccount();

    @POST("v1/user/auth/transfer-p2sh")
    @Headers("Accept: application/vnd.coinblesk.v4+json")
    Call<UserAccountTO> transferToP2SH(@Body BaseTO request);

    @GET("v1/user/auth/logout")
    @Headers("Accept: application/vnd.coinblesk.v4+json")
    Call<UserAccountStatusTO> logout();

    @GET("v1/user/forgot/{email}")
    @Headers("Accept: application/vnd.coinblesk.v4+json")
    Call<UserAccountStatusTO> forgot(@Path("email") String email);

    @POST("v1/user/auth/change-password")
    @Headers("Accept: application/vnd.coinblesk.v4+json")
    Call<UserAccountStatusTO> changePassword(@Body UserAccountTO request);
}