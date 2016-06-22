package com.coinblesk.client.additionalservices;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.coinblesk.client.R;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.coinblesk.json.UserAccountStatusTO;
import com.coinblesk.json.UserAccountTO;
import com.coinblesk.payments.communications.http.CoinbleskWebService;

import java.io.IOException;


import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by draft on 16.06.16.
 */
public class AdditionalServicesUsernameDialog extends DialogFragment {

    private static final String TAG = AdditionalServicesUsernameDialog.class.getName();
    /*@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.additional_services_username_password, container);
        //mEditText = (EditText) view.findViewById(R.id.txt_your_name);
        //getDialog().setTitle("Hello");

        return view;
    }*/

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
                        CoinbleskWebService coinbleskService = AdditionalServiceUtils.coinbleskService(AdditionalServicesUsernameDialog.this.getActivity());
                        Call<ResponseBody> response = coinbleskService.login(usernameText.getText().toString(), passwordText.getText().toString());
                        try {
                            Response<ResponseBody> res = response.execute();
                            String rawCookie = res.headers().get("Set-Cookie");
                            String cookie = AdditionalServiceUtils.parseCookie(rawCookie);
                            SharedPrefUtils.setJSessionID(AdditionalServicesUsernameDialog.this.getActivity(), cookie);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.signup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CoinbleskWebService coinbleskService = AdditionalServiceUtils.coinbleskService(AdditionalServicesUsernameDialog.this.getActivity());
                        UserAccountTO to = new UserAccountTO()
                                .email(usernameText.getText().toString())
                                .password(passwordText.getText().toString());
                        Call<UserAccountStatusTO> result = coinbleskService.signUp(to);
                        try {
                            Response<UserAccountStatusTO> res =result.execute();
                            if(res.body().isSuccess()) {
                                Toast.makeText(AdditionalServicesUsernameDialog.this.getActivity(), "Confirmation email sent", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(AdditionalServicesUsernameDialog.this.getActivity(), "Error:" +res.body().message(), Toast.LENGTH_LONG).show();
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .create();
    }


}
