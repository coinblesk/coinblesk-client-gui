package com.coinblesk.client.additionalservices;

import android.content.Context;

import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.util.SerializeUtils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by draft on 22.06.16.
 */
public class AdditionalServiceUtils {
    public static CoinbleskWebService coinbleskService() {
        return Constants.RETROFIT_SESSION.create(CoinbleskWebService.class);
    }

    public static final OkHttpClient jsessionClient(Context context) {
        final OkHttpClient okHttpClient = new OkHttpClient();
        String jSessionID = SharedPrefUtils.getJSessionID(context);
        return okHttpClient.newBuilder().addInterceptor(new HeaderInterceptor(jSessionID)).build();
    };

    private static class HeaderInterceptor implements Interceptor {
        final private String jSessionID;
        public HeaderInterceptor(String jSessionID) {
            this.jSessionID = jSessionID;
        }

        @Override
        public okhttp3.Response intercept(Chain chain)
                throws IOException {
            Request request = chain.request();
            request = request.newBuilder()
                    .addHeader("Cookie", jSessionID)
                    .build();
            okhttp3.Response response = chain.proceed(request);
            return response;
        }
    }

    //TODO: check bounds
    public static String parseCookie(String rawCookie) {
        int indexStart = rawCookie.indexOf("JSESSIONID=");
        int indexStop = rawCookie.indexOf(";", indexStart);
        return rawCookie.substring(indexStart, indexStop);
    }
}
