package com.uzh.ckiller.coinblesk_client_gui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by ckiller on 24/01/16.
 */
public class KeyboardFragment extends Fragment implements View.OnClickListener {

    KeyboardClicked mCallback;
    View view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.send_keyboard, container, false);
        mCallback.onKeyboardClicked("init");

        // Numbers 0 through 9
        TextView tvOne = (TextView) view.findViewById(R.id.keyboard_first_row_first_col);
        tvOne.setOnClickListener(this);
        TextView tvTwo = (TextView) view.findViewById(R.id.keyboard_first_row_second_col);
        tvTwo.setOnClickListener(this);
        TextView tvThree = (TextView) view.findViewById(R.id.keyboard_first_row_third_col);
        tvThree.setOnClickListener(this);
        TextView tvFour = (TextView) view.findViewById(R.id.keyboard_second_row_first_col);
        tvFour.setOnClickListener(this);
        TextView tvFive = (TextView) view.findViewById(R.id.keyboard_second_row_second_col);
        tvFive.setOnClickListener(this);
        TextView tvSix = (TextView) view.findViewById(R.id.keyboard_second_row_third_col);
        tvSix.setOnClickListener(this);
        TextView tvSeven = (TextView) view.findViewById(R.id.keyboard_third_row_first_col);
        tvSeven.setOnClickListener(this);
        TextView tvEight = (TextView) view.findViewById(R.id.keyboard_third_row_second_col);
        tvEight.setOnClickListener(this);
        TextView tvNine = (TextView) view.findViewById(R.id.keyboard_third_row_third_col);
        tvNine.setOnClickListener(this);
        TextView tvDot = (TextView) view.findViewById(R.id.keyboard_fourth_row_first_col);
        tvDot.setOnClickListener(this);
        TextView tvZero = (TextView) view.findViewById(R.id.keyboard_fourth_row_second_col);
        tvZero.setOnClickListener(this);

        ImageView ivBackspace = (ImageView) view.findViewById(R.id.send_keyboard_backspace);
        ivBackspace.setOnClickListener(this);

        ImageView ivSwitchCurrencies = (ImageView) view.findViewById(R.id.send_keyboard_switch_currencies);
        ivSwitchCurrencies.setOnClickListener(this);

        // Special characters (ImageViews)

        return view;
    }

    // TODO Move to Main Activity
/*    private void initAmount() {

        final TextView tvSmall = (TextView) this.getView().findViewById(R.id.send_keyboard_amount_small);
        final TextView tvLarge = (TextView) this.getView().findViewById(R.id.send_keyboard_amount_large);

        if (mDisplayBitcoinMode) {
            tvLarge.setText(formatCurrency(getBitcoinAmount()));
            tvSmall.setText(formatCurrency(getFiatAmount()));
        } else {
            tvLarge.setText(formatCurrency(getFiatAmount()));
            tvSmall.setText(formatCurrency(getBitcoinAmount()));
        }
    }*/


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.keyboard_first_row_first_col:
                Toast.makeText(getActivity(), R.string.keypad_first_row_first_col, Toast.LENGTH_LONG).show();
                mCallback.onKeyboardClicked("1");
//                mCallback.sendBundle(createBundle("keypad_first_row_first_col"));
                break;

            case R.id.keyboard_first_row_second_col:
                Toast.makeText(getActivity(), R.string.keypad_first_row_second_col, Toast.LENGTH_LONG).show();
                mCallback.onKeyboardClicked("2");
//                mCallback.sendBundle(createBundle("keypad_first_row_second_col"));
                break;

            case R.id.keyboard_first_row_third_col:
                Toast.makeText(getActivity(), R.string.keypad_first_row_third_col, Toast.LENGTH_LONG).show();
                mCallback.onKeyboardClicked("3");
//                mCallback.sendBundle(createBundle("keypad_first_row_third_col"));
                break;

            case R.id.keyboard_second_row_first_col:
                Toast.makeText(getActivity(), R.string.keypad_second_row_first_col, Toast.LENGTH_LONG).show();
                mCallback.onKeyboardClicked("4");
//                mCallback.sendBundle(createBundle("keypad_second_row_first_col"));
                break;

            case R.id.keyboard_second_row_second_col:
                Toast.makeText(getActivity(), R.string.keypad_second_row_second_col, Toast.LENGTH_LONG).show();
                mCallback.onKeyboardClicked("5");
//                mCallback.sendBundle(createBundle("keypad_second_row_second_col"));
                break;

            case R.id.keyboard_second_row_third_col:
                Toast.makeText(getActivity(), R.string.keypad_second_row_third_col, Toast.LENGTH_LONG).show();
                mCallback.onKeyboardClicked("6");
//                mCallback.sendBundle(createBundle("keypad_second_row_third_col"));
                break;

            case R.id.keyboard_third_row_first_col:
                Toast.makeText(getActivity(), R.string.keypad_third_row_first_col, Toast.LENGTH_LONG).show();
                mCallback.onKeyboardClicked("7");
//                mCallback.sendBundle(createBundle("keypad_third_row_first_col"));
                break;

            case R.id.keyboard_third_row_second_col:
                Toast.makeText(getActivity(), R.string.keypad_third_row_second_col, Toast.LENGTH_LONG).show();
                mCallback.onKeyboardClicked("8");
//                mCallback.sendBundle(createBundle("keypad_third_row_second_col"));
                break;

            case R.id.keyboard_third_row_third_col:
                Toast.makeText(getActivity(), R.string.keypad_third_row_third_col, Toast.LENGTH_LONG).show();
                mCallback.onKeyboardClicked("9");
//                mCallback.sendBundle(createBundle("keypad_third_row_third_col"));
                break;

            case R.id.keyboard_fourth_row_first_col:
                Toast.makeText(getActivity(), R.string.keypad_fourth_row_first_col, Toast.LENGTH_LONG).show();
                mCallback.onKeyboardClicked(".");
//                mCallback.sendBundle(createBundle("keypad_fourth_row_first_col"));
                break;

            case R.id.keyboard_fourth_row_second_col:
                Toast.makeText(getActivity(), R.string.keypad_fourth_row_second_col, Toast.LENGTH_LONG).show();
                mCallback.onKeyboardClicked("0");
//                mCallback.sendBundle(createBundle("keypad_fourth_row_second_col"));
                break;

            case R.id.send_keyboard_backspace:
                Toast.makeText(getActivity(), "Backspace", Toast.LENGTH_LONG).show();
                mCallback.onKeyboardClicked("backspace");
                break;

            case R.id.send_keyboard_switch_currencies:
                Toast.makeText(getActivity(), "Switch Currencies", Toast.LENGTH_LONG).show();
                mCallback.onKeyboardClicked("switch");
                break;

            default:
                break;
        }
    }

    public interface KeyboardClicked {
        public void onKeyboardClicked(String string);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (KeyboardClicked) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement KeypadClicked");
        }
    }

    @Override
    public void onDetach() {
        mCallback = null; // => avoid leaking
        super.onDetach();
    }

    public void updateAmount(SpannableString string, boolean value) {

        if (value) {
            final TextView tvLarge = (TextView) view.findViewById(R.id.send_keyboard_amount_large);
            tvLarge.setText(string);
        } else {
            final TextView tvSmall = (TextView) view.findViewById(R.id.send_keyboard_amount_small);
            tvSmall.setText(string);
        }

    }
}
