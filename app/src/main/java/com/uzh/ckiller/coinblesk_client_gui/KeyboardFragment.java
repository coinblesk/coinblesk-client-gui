package com.uzh.ckiller.coinblesk_client_gui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by ckiller on 24/01/16.
 */
public class KeyboardFragment extends Fragment implements View.OnClickListener{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.send_keyboard, container, false);

        setInitBalance(view);

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

        // Special characters (ImageViews)

        return view;
    }

    private void setInitBalance(View view) {

     /*   // 1 Get view references
        final TextView tvSmallBalance = (TextView) view.findViewById(R.id.send_amount_small);
        final TextView tvLargeBalance = (TextView) view.findViewById(R.id.send_amount_large);

        // 2 Get Balance
        // TODO instead of dummy data, get the real balance here.
        setBitcoinSendAmount("0.00");
        setFiatSendAmount("0.00");

        // 3 Set the small balance & Format the SpannableStrings for the large one
        // TODO Feed the Balance into a Method to format properly
        tvLargeBalance.setText(formatInitBalance(bitcoinSendAmount));
        tvSmallBalance.setText(fiatSendAmount);*/

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.keyboard_first_row_first_col:
                Toast.makeText(getActivity(), R.string.keypad_first_row_first_col, Toast.LENGTH_LONG).show();
//                mCallback.sendBundle(createBundle("keypad_first_row_first_col"));
                break;

            case R.id.keyboard_first_row_second_col:
                Toast.makeText(getActivity(), R.string.keypad_first_row_second_col, Toast.LENGTH_LONG).show();
//                mCallback.sendBundle(createBundle("keypad_first_row_second_col"));
                break;

            case R.id.keyboard_first_row_third_col:
                Toast.makeText(getActivity(), R.string.keypad_first_row_third_col, Toast.LENGTH_LONG).show();
//                mCallback.sendBundle(createBundle("keypad_first_row_third_col"));
                break;

            case R.id.keyboard_second_row_first_col:
                Toast.makeText(getActivity(), R.string.keypad_second_row_first_col, Toast.LENGTH_LONG).show();
//                mCallback.sendBundle(createBundle("keypad_second_row_first_col"));
                break;

            case R.id.keyboard_second_row_second_col:
                Toast.makeText(getActivity(), R.string.keypad_second_row_second_col, Toast.LENGTH_LONG).show();
//                mCallback.sendBundle(createBundle("keypad_second_row_second_col"));
                break;

            case R.id.keyboard_second_row_third_col:
                Toast.makeText(getActivity(), R.string.keypad_second_row_third_col, Toast.LENGTH_LONG).show();
//                mCallback.sendBundle(createBundle("keypad_second_row_third_col"));
                break;

            case R.id.keyboard_third_row_first_col:
                Toast.makeText(getActivity(), R.string.keypad_third_row_first_col, Toast.LENGTH_LONG).show();
//                mCallback.sendBundle(createBundle("keypad_third_row_first_col"));
                break;

            case R.id.keyboard_third_row_second_col:
                Toast.makeText(getActivity(), R.string.keypad_third_row_second_col, Toast.LENGTH_LONG).show();
//                mCallback.sendBundle(createBundle("keypad_third_row_second_col"));
                break;

            case R.id.keyboard_third_row_third_col:
                Toast.makeText(getActivity(), R.string.keypad_third_row_third_col, Toast.LENGTH_LONG).show();
//                mCallback.sendBundle(createBundle("keypad_third_row_third_col"));
                break;

            case R.id.keyboard_fourth_row_first_col:
                Toast.makeText(getActivity(), R.string.keypad_fourth_row_first_col, Toast.LENGTH_LONG).show();
//                mCallback.sendBundle(createBundle("keypad_fourth_row_first_col"));
                break;

            case R.id.keyboard_fourth_row_second_col:
                Toast.makeText(getActivity(), R.string.keypad_fourth_row_second_col, Toast.LENGTH_LONG).show();
//                mCallback.sendBundle(createBundle("keypad_fourth_row_second_col"));
                break;

            default:
                break;
        }
    }

}
