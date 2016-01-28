package com.uzh.ckiller.coinblesk_client_gui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
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

    public static final String ARG_PAGE = "ARG_PAGE";
    KeyboardClicked mCallback;
    View view;
    private int mPage;

    public static KeyboardFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        KeyboardFragment fragment = new KeyboardFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPage = getArguments().getInt(ARG_PAGE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_keyboard, container, false);
//        mCallback.onKeyboardClicked("init");

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

        ImageView ivBackspace = (ImageView) view.findViewById(R.id.amount_backspace_image_view);
        ivBackspace.setOnClickListener(this);

        ImageView ivSwitchCurrencies = (ImageView) view.findViewById(R.id.amount_switch_image_view);
        ivSwitchCurrencies.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.keyboard_first_row_first_col:
                mCallback.onKeyboardClicked("1");
                break;

            case R.id.keyboard_first_row_second_col:
                mCallback.onKeyboardClicked("2");
                break;

            case R.id.keyboard_first_row_third_col:
                mCallback.onKeyboardClicked("3");
                break;

            case R.id.keyboard_second_row_first_col:
                mCallback.onKeyboardClicked("4");
                break;

            case R.id.keyboard_second_row_second_col:
                mCallback.onKeyboardClicked("5");
                break;

            case R.id.keyboard_second_row_third_col:
                mCallback.onKeyboardClicked("6");
                break;

            case R.id.keyboard_third_row_first_col:
                mCallback.onKeyboardClicked("7");
                break;

            case R.id.keyboard_third_row_second_col:
                mCallback.onKeyboardClicked("8");
                break;

            case R.id.keyboard_third_row_third_col:
                mCallback.onKeyboardClicked("9");
                break;

            case R.id.keyboard_fourth_row_first_col:
                mCallback.onKeyboardClicked(".");
                break;

            case R.id.keyboard_fourth_row_second_col:
                mCallback.onKeyboardClicked("0");
                break;

            case R.id.amount_backspace_image_view:
                mCallback.onKeyboardClicked("backspace");
                break;

            case R.id.amount_switch_image_view:
                mCallback.onKeyboardClicked("switch");
                break;

            default:
                break;
        }
    }

    //TODO rausnehmen OnKeyboardClickedListener
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
                    + " must implement KeyboardClicked");
        }
    }

    @Override
    public void onDetach() {
        mCallback = null; // => avoid leaking
        super.onDetach();
    }

    public void updateAmount(SpannableString formattedSpannable, String currency) {

        switch (currency) {
            case "":
                break;
            case "btc":
                final TextView tvLarge = (TextView) view.findViewById(R.id.amount_large_text_view);
                tvLarge.setText(formattedSpannable);
                break;
            case "fiat":
                final TextView tvSmall = (TextView) view.findViewById(R.id.amount_small_text_view);
                tvSmall.setText(formattedSpannable);
                break;

            default:
                break;

        }

    }
}
