package com.coinblesk.client.additionalservices;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.coinblesk.client.R;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.coinblesk.json.BaseTO;
import com.coinblesk.json.UserAccountStatusTO;
import com.coinblesk.json.UserAccountTO;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.util.Pair;
import com.coinblesk.util.SerializeUtils;

import java.io.IOException;


import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by draft on 16.06.16.
 */
public class AdditionalServicesUsernameDialog extends DialogFragment {

    private static final String TAG = AdditionalServicesUsernameDialog.class.getName();

    private Runnable runner;
    public void setCloseRunner(Runnable runner) {
        this.runner = runner;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(runner != null) {
            runner.run();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.additional_services_username_password, null);
        final EditText usernameText = (EditText) view.findViewById(R.id.additional_services_username);
        final EditText passwordText = (EditText) view.findViewById(R.id.additional_services_password);


        Log.d(TAG, "onCreateDialog with address=" + usernameText.getText().toString());

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.edit_login)
                .setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new LoginTask(getActivity()).execute(new Pair<String, String>(
                                usernameText.getText().toString(), passwordText.getText().toString()));
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.signup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new SignupTask(getActivity()).execute(new Pair<String, String>(
                                usernameText.getText().toString(), passwordText.getText().toString()));
                    }
                })
                .create();
    }

    private class SignupTask extends AsyncTask<Pair<String,String>, Void, Boolean> {

        final private Activity activity;
        private SignupTask(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected Boolean doInBackground(Pair<String,String>... pairs) {
            CoinbleskWebService coinbleskService = AdditionalServiceUtils.coinbleskService();
            for(Pair<String,String> pair:pairs) {
                UserAccountTO to = new UserAccountTO()
                        .email(pair.element0())
                        .password(pair.element1());
                Call<UserAccountStatusTO> result = coinbleskService.signUp(to);
                try {
                    Response<UserAccountStatusTO> res =result.execute();
                    if(res.body().isSuccess()) {
                        toast("Confirmation email sent");
                    } else {
                        toast("Error:" +res.body().message());
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }

        private void toast(final String text)  {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private class LoginTask extends AsyncTask<Pair<String,String>, Void, Boolean> {

        final private Activity activity;
        private LoginTask(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected Boolean doInBackground(Pair<String,String>... pairs) {
            CoinbleskWebService coinbleskService = AdditionalServiceUtils.coinbleskService();
            for(Pair<String,String> pair:pairs) {
                Call<ResponseBody> response = coinbleskService.login(pair.element0(), pair.element1());
                try {
                    Response<ResponseBody> res = response.execute();
                    if(res.isSuccessful()) {
                        String rawCookie = res.headers().get("Set-Cookie");
                        String cookie = AdditionalServiceUtils.parseCookie(rawCookie);
                        SharedPrefUtils.setJSessionID(activity, cookie);
                        Constants.RETROFIT_SESSION = new Retrofit.Builder()
                                .addConverterFactory(GsonConverterFactory.create(SerializeUtils.GSON))
                                .client(AdditionalServiceUtils.jsessionClient(activity))
                                .baseUrl(Constants.COINBLESK_SERVER_BASE_URL).build();
                        toast( "Logged in successfully");
                    } else {
                        toast( "Password/username incorrect");
                    }
                } catch (IOException e) {
                    toast( "Username/password incorrect");
                    e.printStackTrace();
                }
            }
            return true;
        }

        private void toast(final String text)  {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
