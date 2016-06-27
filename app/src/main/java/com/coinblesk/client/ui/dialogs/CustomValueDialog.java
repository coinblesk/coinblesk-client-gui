/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.coinblesk.client.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.coinblesk.client.R;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;

/**
 * Created by ckiller on 28/02/16.
 */
public class CustomValueDialog extends Dialog implements View.OnClickListener {

    private final static String TAG = CustomValueDialog.class.getName();

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

        List<String> customButton = Arrays.asList(
                descriptionEditText.getText().toString(),
                priceEditText.getText().toString());
        Gson gson = new Gson();
        String jsonCustomButton = gson.toJson(customButton);
        SharedPrefUtils.setCustomButton(getContext(), customizeButton, jsonCustomButton);
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
