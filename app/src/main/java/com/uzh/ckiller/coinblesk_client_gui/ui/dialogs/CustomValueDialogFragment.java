package com.uzh.ckiller.coinblesk_client_gui.ui.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.uzh.ckiller.coinblesk_client_gui.R;

/**
 * Created by ckiller on 28/02/16.
 */
public class CustomValueDialogFragment extends DialogFragment implements View.OnClickListener {

    private final static String TAG = CustomValueDialogFragment.class.getName();
    public static final String CUSTOMIZE_BUTTON_KEY = "CUSTOMIZE_BUTTON_KEY";
    public static final String MERCHANT_CUSTOM_BUTTONS_PREF_KEY = "MERCHANT_CUSTOM_BUTTONS";
    public static final String DESCRIPTION_IDENTIFIER = "]DESCRIPTION";
    public static final String PRICE_IDENTIFIER = "]PRICE";


    private EditText descriptionEditText;
    private EditText priceEditText;


    public static DialogFragment newInstance(String customizeButton) {
        DialogFragment fragment = new CustomValueDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(CUSTOMIZE_BUTTON_KEY, customizeButton);
        fragment.setArguments(arguments);
        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customize_button_dialog, container);

        this.descriptionEditText = (EditText) view.findViewById(R.id.customize_button_description);
        this.priceEditText = (EditText) view.findViewById(R.id.customize_button_price);

        view.findViewById(R.id.customize_button_cancel).setOnClickListener(this);
        view.findViewById(R.id.customize_button_save).setOnClickListener(this);

        return view;

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.customize_button_save:
                saveInput();
                getDialog().cancel();
                break;

            case R.id.customize_button_cancel:
                getDialog().cancel();
                break;
        }
    }

    private void saveInput() {
        SharedPreferences prefs = getActivity().getSharedPreferences(MERCHANT_CUSTOM_BUTTONS_PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("[" + getArguments().get(CUSTOMIZE_BUTTON_KEY).toString()
                + DESCRIPTION_IDENTIFIER, descriptionEditText.getText().toString());
        editor.putString("[" + getArguments().get(CUSTOMIZE_BUTTON_KEY).toString()
                + PRICE_IDENTIFIER, priceEditText.getText().toString());

        editor.commit();
    }
}
