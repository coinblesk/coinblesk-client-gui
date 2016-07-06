package com.coinblesk.client.additionalservices;

import android.app.Activity;
import android.content.Context;

import com.coinblesk.client.CoinbleskApp;
import com.coinblesk.client.config.AppConfig;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.coinblesk.client.CoinbleskWebService;
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
    private static Retrofit RETROFIT_SESSION = null;

    public static CoinbleskWebService coinbleskService() {
        return RETROFIT_SESSION.create(CoinbleskWebService.class);
    }

    public static void setSessionID(Activity activity, String cookie) {
        if(cookie != null) {
            SharedPrefUtils.setJSessionID(activity, cookie);
        }
        AppConfig appConfig = ((CoinbleskApp) activity.getApplication()).getAppConfig();
        if("".equals(cookie)) {
            RETROFIT_SESSION = new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create(SerializeUtils.GSON))
                    .baseUrl(appConfig.getCoinbleskServerUrl()).build();
        } else {
            RETROFIT_SESSION = new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create(SerializeUtils.GSON))
                    .client(jsessionClient(activity))
                    .baseUrl(appConfig.getCoinbleskServerUrl()).build();
        }
    }

    private static final OkHttpClient jsessionClient(Context context) {
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

    public static String parseCookie(String rawCookie) {
        int indexStart = rawCookie.indexOf("JSESSIONID=");
        if(indexStart >= 0) {
            int indexStop = rawCookie.indexOf(";", indexStart);
            if(indexStop >=0 && indexStop > indexStart) {
                return rawCookie.substring(indexStart, indexStop);
            }
        }
        return "";
    }
}
