package com.coinblesk.client.additionalservices;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.coinblesk.client.R;
import com.coinblesk.json.v1.UserAccountTO;
import com.coinblesk.util.Pair;

/**
 * Created by draft on 16.06.16.
 */
public class AdditionalServicesUsernameDialog extends DialogFragment {

    private static final String TAG = AdditionalServicesUsernameDialog.class.getName();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        new AdditionalServicesTasks.GetAccountTask(getActivity()).execute();
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        UserAccountTO userAccountTO = (UserAccountTO) getArguments().getSerializable("");
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.additional_services_username_password, null);
        final EditText usernameText = (EditText) view.findViewById(R.id.additional_services_username);
        final EditText passwordText = (EditText) view.findViewById(R.id.additional_services_password);
        final EditText passwordText2 = (EditText) view.findViewById(R.id.additional_services_second_password);
        final TextInputLayout layout = (TextInputLayout) view.findViewById(R.id.additional_services_second_password_layout);
        final boolean isLoggedin = userAccountTO != null && userAccountTO.isSuccess();
        if (isLoggedin) {
            usernameText.setText(userAccountTO.email());
        }

        final CheckBox checkBoxSingle = (CheckBox) view.findViewById(R.id.additional_services_checkBox1);
        checkBoxSingle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // checkbox status is changed from uncheck to checked.
                if (!isChecked) {
                    // show password
                    passwordText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                } else {
                    // hide password
                    passwordText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
            }
        });

        final CheckBox checkBoxBoth = (CheckBox) view.findViewById(R.id.additional_services_checkBox2);
        checkBoxBoth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // checkbox status is changed from uncheck to checked.
                if (!isChecked) {
                    // show password
                    passwordText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    passwordText2.setTransformationMethod(PasswordTransformationMethod.getInstance());
                } else {
                    // hide password
                    passwordText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    passwordText2.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
            }
        });
        final CheckBox checkBoxForgot = (CheckBox) view.findViewById(R.id.additional_services_button);
        if (isLoggedin) {
            usernameText.setText(userAccountTO.email());
            checkBoxForgot.setVisibility(View.INVISIBLE);
        }



        Log.d(TAG, "onCreateDialog with address=" + usernameText.getText().toString());

        final AlertDialog d = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.login_signup)
                .setView(view)
                .setPositiveButton(isLoggedin? R.string.additional_services_change : R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(isLoggedin? R.string.logout : R.string.signup, null)
                .create();

        checkBoxForgot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if(checked) {
                    passwordText.setVisibility(View.INVISIBLE);
                    checkBoxSingle.setVisibility(View.INVISIBLE);
                    //TODO remove PW text
                } else {
                    passwordText.setVisibility(View.VISIBLE);
                    checkBoxSingle.setVisibility(View.VISIBLE);

                }

                d.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(checked ? View.INVISIBLE: View.VISIBLE);
            }
        });

        final View.OnClickListener okClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(checkBoxForgot.isChecked()) {
                    //send new password
                    new AdditionalServicesTasks.ForgotTask(getActivity(), new AdditionalServicesTasks.OnTaskCompleted() {
                        @Override
                        public void onTaskCompleted(boolean success, String message) {
                            if (success) {
                                toastAndQuit(R.string.additional_services_forgot_success, d);
                            } else {
                                toast(R.string.additional_services_forgot_error, message);
                            }
                        }
                    }).execute(usernameText.getText().toString());
                } else if (isLoggedin) {
                    //change password
                    if(layout.getVisibility() == View.GONE) {
                        layout.setVisibility(View.VISIBLE);
                        checkBoxSingle.setVisibility(View.GONE);
                        checkBoxForgot.setVisibility(View.GONE);
                        d.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
                    } else if(!passwordText.getText().toString().equals(passwordText2.getText().toString())) {
                        toast(R.string.additional_services_password_mismatch);
                    }
                    else {
                        new AdditionalServicesTasks.ChangePassword(getActivity(), new AdditionalServicesTasks.OnTaskCompleted() {
                            @Override
                            public void onTaskCompleted(boolean success, String message) {
                                if (success) {
                                    toastAndQuit(R.string.additional_services_change_success, d);
                                } else {
                                    toast(R.string.additional_services_change_error, message);
                                }
                            }
                        }).execute(new Pair<String, String>(usernameText.getText().toString(), passwordText.getText().toString()));
                    }
                }
                else {
                    //login
                    new AdditionalServicesTasks.LoginTask(getActivity(), new AdditionalServicesTasks.OnTaskCompleted() {
                        @Override
                        public void onTaskCompleted(boolean success, String message) {
                            if (success) {
                                toastAndQuit(R.string.additional_services_login_success, d);
                            } else {
                                if (message == null) {
                                    toast(R.string.additional_services_login_user_password_incorrect);
                                } else {
                                    toast(R.string.additional_services_login_error, message);
                                }
                            }
                        }
                    }).execute(new Pair<String, String>(
                            usernameText.getText().toString(), passwordText.getText().toString()));
                }
            }
        };

        if (isLoggedin) {

            d.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    Button b = d.getButton(AlertDialog.BUTTON_NEUTRAL);
                    b.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AdditionalServicesTasks.LogoutTask(getActivity(), new AdditionalServicesTasks.OnTaskCompleted() {
                                @Override
                                public void onTaskCompleted(boolean success, String message) {
                                    if (success) {
                                        getArguments().putSerializable("", null);
                                        toastAndQuit(R.string.additional_services_logout_success, d);
                                    } else {
                                        toast(R.string.additional_services_logout_error, message);
                                    }
                                }
                            }).execute();

                        }
                    });

                    Button b1 = d.getButton(AlertDialog.BUTTON_POSITIVE);
                    b1.setOnClickListener(okClickListener);
                }
            });
            return d;
        } else {
            d.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    Button b = d.getButton(AlertDialog.BUTTON_NEUTRAL);
                    b.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            if(layout.getVisibility() == View.GONE) {
                                layout.setVisibility(View.VISIBLE);
                                checkBoxSingle.setVisibility(View.GONE);
                                checkBoxForgot.setVisibility(View.GONE);
                                d.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
                            } else if(!passwordText.getText().toString().equals(passwordText2.getText().toString())) {
                                toast(R.string.additional_services_password_mismatch);
                            }
                            else {
                                new AdditionalServicesTasks.SignupTask(getActivity(), new AdditionalServicesTasks.OnTaskCompleted() {
                                    @Override
                                    public void onTaskCompleted(boolean success, String message) {
                                        if (success) {
                                            toastAndQuit(R.string.additional_services_signup_success, d);
                                        } else {
                                            toast(R.string.additional_services_signup_error, message);
                                        }
                                    }
                                }).execute(new Pair<String, String>(
                                        usernameText.getText().toString(), passwordText.getText().toString()));
                            }

                        }
                    });
                    Button b1 = d.getButton(AlertDialog.BUTTON_POSITIVE);
                    b1.setOnClickListener(okClickListener);
                }
            });
            return d;
        }
    }

    private void toastAndQuit(final int text, final AlertDialog d) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String resolved = getActivity().getResources().getString(text);
                Toast.makeText(getActivity(), resolved, Toast.LENGTH_LONG).show();
                d.dismiss();
            }
        });
    }

    private void toast(final int text) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String resolved = getActivity().getResources().getString(text);
                Toast.makeText(getActivity(), resolved, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void toastAndQuit(final int text, final String msg, final AlertDialog d) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String resolved = getActivity().getResources().getString(text);
                Toast.makeText(getActivity(), resolved + msg, Toast.LENGTH_LONG).show();
                d.dismiss();
            }
        });
    }

    private void toast(final int text, final String msg) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String resolved = getActivity().getResources().getString(text);
                Toast.makeText(getActivity(), resolved + msg, Toast.LENGTH_LONG).show();
            }
        });
    }

}
