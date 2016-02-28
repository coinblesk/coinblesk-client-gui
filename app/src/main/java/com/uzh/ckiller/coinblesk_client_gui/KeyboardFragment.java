package com.uzh.ckiller.coinblesk_client_gui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.uzh.ckiller.coinblesk_client_gui.helpers.SpannableStringFormatter;
import com.uzh.ckiller.coinblesk_client_gui.ui.dialogs.CustomValueDialog;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import java.math.BigDecimal;
import java.security.Key;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ckiller on 24/01/16.
 */

public abstract class KeyboardFragment extends Fragment implements View.OnClickListener, OnKeyboardListener, CustomValueDialog.CustomValueListener {
    SwipeRefreshLayout mSwipeRefreshLayout;
    private Handler handler = new Handler();
    private final static String TAG = KeyboardFragment.class.getName();


    public static final String MERCHANT_CUSTOM_BUTTONS_PREF_KEY = "MERCHANT_CUSTOM_BUTTONS";

    private String amountString = "0";
    private String sumAmountString = "0";
    private SharedPreferences prefs;
    private SpannableStringFormatter spannableStringFormatter;

    // TODO: get current exchange rate from net, largeamount settings from prefs, prefered fiat from prefs.
    private String currencyCode = "CHF";
    private ExchangeRate exchangeRate = new ExchangeRate(Fiat.parseFiat(currencyCode, "430"));
    private boolean isBitcoinLargeAmount = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_keyboard, container, false);

        final int screenLayout = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        switch (screenLayout) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                initLarge(view);
                break;
            default:
                initStandard(view);
                break;
        }

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.keyboard_swipe_refresh_layout);
        try {
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    //TODO: Refresh Exchange Rate Here
                    handler.post(refreshing);
                }
            });

            // sets the colors used in the refresh animation
            mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.material_lime_A100,
                    R.color.material_lime_A400, R.color.material_lime_A400);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        this.onKeyboardListener = this;
        return view;
    }

    private void initStandard(View view) {

        // Numbers '0 1 2 3 4 5 6 7 8 9,' and '.'
        view.findViewById(R.id.key_one).setOnClickListener(this);
        view.findViewById(R.id.key_two).setOnClickListener(this);
        view.findViewById(R.id.key_three).setOnClickListener(this);
        view.findViewById(R.id.key_four).setOnClickListener(this);
        view.findViewById(R.id.key_five).setOnClickListener(this);
        view.findViewById(R.id.key_six).setOnClickListener(this);
        view.findViewById(R.id.key_seven).setOnClickListener(this);
        view.findViewById(R.id.key_eight).setOnClickListener(this);
        view.findViewById(R.id.key_nine).setOnClickListener(this);
        view.findViewById(R.id.key_dot).setOnClickListener(this);
        view.findViewById(R.id.key_zero).setOnClickListener(this);
        view.findViewById(R.id.key_accept).setOnClickListener(this);

        // Backspace Button
        view.findViewById(R.id.amount_backspace_image_view).setOnClickListener(this);

        // Switch Currency Button
        view.findViewById(R.id.amount_switch_image_view).setOnClickListener(this);
    }

    private void initLarge(View view) {
        // Numbers '00 0 1 2 3 4 5 6 7 8 9 . + x  clear'
        view.findViewById(R.id.key_one).setOnClickListener(this);
        view.findViewById(R.id.key_two).setOnClickListener(this);
        view.findViewById(R.id.key_three).setOnClickListener(this);
        view.findViewById(R.id.key_four).setOnClickListener(this);
        view.findViewById(R.id.key_five).setOnClickListener(this);
        view.findViewById(R.id.key_six).setOnClickListener(this);
        view.findViewById(R.id.key_seven).setOnClickListener(this);
        view.findViewById(R.id.key_eight).setOnClickListener(this);
        view.findViewById(R.id.key_nine).setOnClickListener(this);
        view.findViewById(R.id.key_dot).setOnClickListener(this);
        view.findViewById(R.id.key_zero).setOnClickListener(this);
        view.findViewById(R.id.key_zero_zero).setOnClickListener(this);
        view.findViewById(R.id.key_accept).setOnClickListener(this);
        view.findViewById(R.id.key_clear).setOnClickListener(this);
        view.findViewById(R.id.key_multiply).setOnClickListener(this);
        view.findViewById(R.id.key_plus).setOnClickListener(this);

        // Backspace Button
        view.findViewById(R.id.amount_backspace_image_view).setOnClickListener(this);
        // Switch Currency Button
        view.findViewById(R.id.amount_switch_image_view).setOnClickListener(this);

        // Customize Buttons for Merchant Mode
        view.findViewById(R.id.key_custom_one).setOnClickListener(this);
        view.findViewById(R.id.key_custom_two).setOnClickListener(this);
        view.findViewById(R.id.key_custom_three).setOnClickListener(this);
        view.findViewById(R.id.key_custom_four).setOnClickListener(this);
        view.findViewById(R.id.key_custom_five).setOnClickListener(this);
        view.findViewById(R.id.key_custom_six).setOnClickListener(this);
        view.findViewById(R.id.key_custom_seven).setOnClickListener(this);
        view.findViewById(R.id.key_custom_eight).setOnClickListener(this);

    }

    private OnKeyboardListener onKeyboardListener;


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.key_one:
                onKeyboardListener.onDigit(1);
                break;

            case R.id.key_two:
                onKeyboardListener.onDigit(2);
                break;

            case R.id.key_three:
                onKeyboardListener.onDigit(3);
                break;

            case R.id.key_four:
                onKeyboardListener.onDigit(4);
                break;

            case R.id.key_five:
                onKeyboardListener.onDigit(5);
                break;

            case R.id.key_six:
                onKeyboardListener.onDigit(6);
                break;

            case R.id.key_seven:
                onKeyboardListener.onDigit(7);
                break;

            case R.id.key_eight:
                onKeyboardListener.onDigit(8);
                break;

            case R.id.key_nine:
                onKeyboardListener.onDigit(9);
                break;

            case R.id.key_dot:
                onKeyboardListener.onDot();
                break;

            case R.id.key_zero:
                onKeyboardListener.onDigit(0);
                break;

            case R.id.amount_backspace_image_view:
                if (this.amountString.length() == 1) {
                    this.amountString = "0";
                } else {
                    this.amountString = this.amountString.substring(0, this.amountString.length() - 1);
                }
                this.updateAmount();
                break;

            case R.id.amount_switch_image_view:
                this.isBitcoinLargeAmount = !this.isBitcoinLargeAmount;
                this.updateAmount();
                break;

            case R.id.key_accept:
                onKeyboardListener.onEnter();
                break;

            case R.id.key_plus:
                // TODO Add 'adding' funct, appending to input values
                break;

            case R.id.key_multiply:
                // TODO Add multiply funct.
                break;

            case R.id.key_clear:
                // TODO Add clear funct.
                // TODO Remove input values as well
                // TODO Clear all preferences


                break;

            // Customize Buttons for Merchant Mode
            case R.id.key_custom_one:
                onKeyboardListener.onCustom(1);
                break;
            case R.id.key_custom_two:
                onKeyboardListener.onCustom(2);
                break;
            case R.id.key_custom_three:
                onKeyboardListener.onCustom(3);
                break;
            case R.id.key_custom_four:
                onKeyboardListener.onCustom(4);
                break;
            case R.id.key_custom_five:
                onKeyboardListener.onCustom(5);
                break;
            case R.id.key_custom_six:
                onKeyboardListener.onCustom(6);
                break;
            case R.id.key_custom_seven:
                onKeyboardListener.onCustom(7);
                break;
            case R.id.key_custom_eight:
                onKeyboardListener.onCustom(8);
                break;
        }

    }


    protected Coin getCoin() {
        if (isBitcoinLargeAmount) {
            return Coin.parseCoin(this.amountString);
        } else {
            return exchangeRate.fiatToCoin(this.getFiat());
        }
    }

    protected Fiat getFiat() {
        if (!isBitcoinLargeAmount) {
            return Fiat.parseFiat(currencyCode, this.amountString);
        } else {
            return exchangeRate.coinToFiat(this.getCoin());
        }
    }

    private void updateAmount() {
        spannableStringFormatter = new SpannableStringFormatter(this.getActivity());
        final Coin coin = this.getCoin();
        final Fiat fiat = this.getFiat();

        final TextView smallTextView = (TextView) this.getView().findViewById(R.id.amount_small_text_view);
        final TextView largeTextView = (TextView) this.getView().findViewById(R.id.amount_large_text_view);
        if (this.isBitcoinLargeAmount) {
            largeTextView.setText(spannableStringFormatter.toLargeSpannable(this.amountString, "BTC"));
            smallTextView.setText(spannableStringFormatter.toSmallSpannable(fiat.toPlainString(), "CHF"));
        } else {
            largeTextView.setText(spannableStringFormatter.toLargeSpannable(this.amountString, "CHF"));
            smallTextView.setText(spannableStringFormatter.toSmallSpannable(coin.toPlainString(), "BTC"));
        }
    }

    // Code partly from https://yassirh.com/2014/05/how-to-use-swiperefreshlayout-the-right-way/
    // and here: http://stackoverflow.com/a/28173911

    private final Runnable refreshing = new Runnable() {
        public void run() {
            try {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        this.updateAmount();
    }

    @Override
    public void onDigit(int digit) {
        this.amountString += digit;
        this.amountString = new BigDecimal(amountString).toString();
        this.updateAmount();
    }

    @Override
    public void onDot() {
        if (!this.amountString.contains(".")) {
            this.amountString += ".";
        }
        this.updateAmount();
    }

    @Override
    public void onEnter() {
        if (this.getCoin().isPositive()) {
            this.getDialogFragment().show(this.getFragmentManager(), "keyboard_dialog_fragment");
        }
    }

    protected abstract DialogFragment getDialogFragment();

    public void onCustom(int customKey) {

        this.prefs = getActivity().getSharedPreferences(MERCHANT_CUSTOM_BUTTONS_PREF_KEY, Context.MODE_PRIVATE);
        if (this.prefs.contains(Integer.toString(customKey))) {
            // Add amount to sum of input values

        } else {
            CustomValueDialog cvd = new CustomValueDialog(getContext(), Integer.toString(customKey));
            cvd.setCustomValueListener(new CustomValueDialog.CustomValueListener() {
                @Override
                public void onSharedPrefsUpdated(String customKey) {
                   updateCustomButton(customKey);
                }
            });
            cvd.show();
        }
    }

    public void updateCustomButton(String customKey) {
        String json = prefs.getString(customKey, null);
        Gson gson = new Gson();

        String[] customButtonContent = gson.fromJson(json, String[].class);
        List<String> customButtonContentList;
        customButtonContentList = Arrays.asList(customButtonContent);

        switch (customKey) {
            case "1":
                ((TextView)this.getActivity().findViewById(R.id.key_custom_one))
                        .setText(spannableStringFormatter.toFriendlyCustomButtonString(customButtonContentList.get(0),customButtonContentList.get(1)));
                break;
            case "2":
                ((TextView)this.getActivity().findViewById(R.id.key_custom_two))
                        .setText(spannableStringFormatter.toFriendlyCustomButtonString(customButtonContentList.get(0), customButtonContentList.get(1)));
                break;
            case "3":
                ((TextView)this.getActivity().findViewById(R.id.key_custom_three))
                        .setText(spannableStringFormatter.toFriendlyCustomButtonString(customButtonContentList.get(0),customButtonContentList.get(1)));
                break;
            case "4":
                ((TextView)this.getActivity().findViewById(R.id.key_custom_four))
                        .setText(spannableStringFormatter.toFriendlyCustomButtonString(customButtonContentList.get(0),customButtonContentList.get(1)));
                break;
            case "5":
                ((TextView)this.getActivity().findViewById(R.id.key_custom_five))
                        .setText(spannableStringFormatter.toFriendlyCustomButtonString(customButtonContentList.get(0), customButtonContentList.get(1)));
                break;
            case "6":
                ((TextView)this.getActivity().findViewById(R.id.key_custom_six))
                        .setText(spannableStringFormatter.toFriendlyCustomButtonString(customButtonContentList.get(0),customButtonContentList.get(1)));
                break;
            case "7":
                ((TextView)this.getActivity().findViewById(R.id.key_custom_seven))
                        .setText(spannableStringFormatter.toFriendlyCustomButtonString(customButtonContentList.get(0),customButtonContentList.get(1)));
                break;
            case "8":
                ((TextView)this.getActivity().findViewById(R.id.key_custom_eight))
                        .setText(spannableStringFormatter.toFriendlyCustomButtonString(customButtonContentList.get(0),customButtonContentList.get(1)));
                break;
        }

    }


}