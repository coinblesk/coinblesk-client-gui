package com.uzh.ckiller.coinblesk_client_gui.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.gson.Gson;
import com.uzh.ckiller.coinblesk_client_gui.R;

import java.util.Arrays;
import java.util.List;

/**
 * Created by ckiller on 28/02/16.
 */
public class CustomValueDialog extends Dialog implements View.OnClickListener {

    private final static String TAG = CustomValueDialog.class.getName();
    public static final String MERCHANT_CUSTOM_BUTTONS_PREF_KEY = "MERCHANT_CUSTOM_BUTTONS";

    private EditText descriptionEditText;
    private EditText priceEditText;
    private String customizeButton;

    private CustomValueListener customValueListener;

    public CustomValueDialog(Context context, String customizeButton) {
        super(context);
        this.customizeButton = customizeButton;
        this.customValueListener = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_customize_button_dialog);

        this.descriptionEditText = (EditText) findViewById(R.id.customize_button_description);
        this.priceEditText = (EditText) findViewById(R.id.customize_button_price);

        findViewById(R.id.customize_button_cancel).setOnClickListener(this);
        findViewById(R.id.customize_button_save).setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.customize_button_save:
                saveInput();
                super.cancel();
                break;

            case R.id.customize_button_cancel:
                super.cancel();
                break;
        }
    }

    private void saveInput() {
        SharedPreferences prefs = getContext().getSharedPreferences(MERCHANT_CUSTOM_BUTTONS_PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        List<String> customButton = Arrays.asList(descriptionEditText.getText().toString(),
                priceEditText.getText().toString());
        Gson gson = new Gson();
        String jsonCustomButton = gson.toJson(customButton);
        editor.putString(customizeButton, jsonCustomButton);
        editor.commit();

        if(customValueListener!=null){
            customValueListener.onSharedPrefsUpdated(customizeButton);
        }

    }

    public interface CustomValueListener{
        public void onSharedPrefsUpdated(String customKey);
    }

    public void setCustomValueListener(CustomValueListener listener) {
        this.customValueListener = listener;
    }
}
