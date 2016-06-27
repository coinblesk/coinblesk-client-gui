package com.coinblesk.client.additionalservices;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.coinblesk.client.R;
import com.coinblesk.json.UserAccountStatusTO;
import com.coinblesk.json.UserAccountTO;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.util.Pair;

import java.io.IOException;


import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by draft on 16.06.16.
 */
public class AdditionalServicesUsernameDialog extends DialogFragment {

    private static final String TAG = AdditionalServicesUsernameDialog.class.getName();

    private AdditionalServicesActivity.AdditionalServiceGUIState listener;
    private Activity parent;
    public AdditionalServicesUsernameDialog setData(Activity parent, AdditionalServicesActivity.AdditionalServiceGUIState listener) {
        this.listener = listener;
        this.parent = parent;
        return this;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        new AdditionalServicesTasks.GetAccountTask(listener).execute();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.additional_services_username_password, null);
        final EditText usernameText = (EditText) view.findViewById(R.id.additional_services_username);
        final EditText passwordText = (EditText) view.findViewById(R.id.additional_services_password);
        final boolean isLoggedin = listener != null && listener.userAccountTO() != null && listener.userAccountTO().isSuccess();
        if(isLoggedin) {
            usernameText.setText(listener.userAccountTO().email());
            passwordText.setText("******");
        }


        Log.d(TAG, "onCreateDialog with address=" + usernameText.getText().toString());

        DialogInterface.OnClickListener okClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new AdditionalServicesTasks.LoginTask(getActivity(), new AdditionalServicesTasks.OnTaskCompleted() {
                    @Override
                    public void onTaskCompleted(boolean success, String message) {
                        if(success) {
                            toast(R.string.additional_services_login_success);
                        } else {
                            if(message == null) {
                                toast(R.string.additional_services_login_user_password_incorrect);
                            } else {
                                toast(R.string.additional_services_login_error, message);
                            }
                        }
                    }
                }).execute(new Pair<String, String>(
                        usernameText.getText().toString(), passwordText.getText().toString()));
            }
        };

        if(isLoggedin) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.login_signup)
                    .setView(view)
                    .setPositiveButton(R.string.ok, okClickListener)
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.logout, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new AdditionalServicesTasks.LogoutTask(getActivity(), new AdditionalServicesTasks.OnTaskCompleted() {
                                @Override
                                public void onTaskCompleted(boolean success, String message) {
                                    if(success) {
                                        toast(R.string.additional_services_logout_success);
                                    } else {
                                        toast(R.string.additional_services_logout_error, message);
                                    }
                                }
                            }).execute();
                        }
                    })
                    .create();
        } else {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.login_logout)
                    .setView(view)
                    .setPositiveButton(R.string.ok, okClickListener)
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.signup, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new AdditionalServicesTasks.SignupTask(getActivity(), new AdditionalServicesTasks.OnTaskCompleted() {
                                @Override
                                public void onTaskCompleted(boolean success, String message) {
                                    if(success) {
                                        toast(R.string.additional_services_signup_success);
                                    } else {
                                        toast(R.string.additional_services_signup_error, message);
                                    }
                                }
                            }).execute(new Pair<String, String>(
                                    usernameText.getText().toString(), passwordText.getText().toString()));
                        }
                    })
                    .create();
        }
    }

    private void toast(final int text)  {
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String resolved = parent.getResources().getString(text);
                Toast.makeText(parent, resolved, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void toast(final int text, final String msg)  {
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String resolved = parent.getResources().getString(text);
                Toast.makeText(parent, resolved + msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
