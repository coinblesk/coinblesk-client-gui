package com.uzh.ckiller.coinblesk_client_gui;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.uzh.ckiller.coinblesk_client_gui.helpers.IPreferenceStrings;
import com.uzh.ckiller.coinblesk_client_gui.helpers.UIUtils;
import com.uzh.ckiller.coinblesk_client_gui.ui.dialogs.CustomValueDialog;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by ckiller on 24/01/16.
 */

public abstract class KeyboardFragment extends Fragment implements View.OnClickListener, OnKeyboardListener, CustomValueDialog.CustomValueListener, IPreferenceStrings {
    private String amountString = "0";
    private String sumString = "";

    // TODO: get current exchange rate from net, largeamount settings from prefs, prefered fiat from prefs.
    private String currencyCode = "CHF";
    private ExchangeRate exchangeRate = new ExchangeRate(Fiat.parseFiat(currencyCode, "430"));
    private boolean isBitcoinLargeAmount = true;

    protected abstract DialogFragment getDialogFragment();

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

        final ImageView nfcIcon = (ImageView) view.findViewById(R.id.nfc_balance);
        final ImageView bluetoothIcon = (ImageView) view.findViewById(R.id.bluetooth_balance);
        final ImageView wifiIcon = (ImageView) view.findViewById(R.id.wifidirect_balance);

        UIUtils.formatConnectionIcon(this.getContext(), nfcIcon, NFC_ACTIVATED);
        UIUtils.formatConnectionIcon(this.getContext(), bluetoothIcon, BT_ACTIVATED);
        UIUtils.formatConnectionIcon(this.getContext(), wifiIcon, WIFIDIRECT_ACTIVATED);

        this.onKeyboardListener = this;
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final int screenLayout = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE || screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            this.initCustomButtons("1");
            this.initCustomButtons("2");
            this.initCustomButtons("3");
            this.initCustomButtons("4");
            this.initCustomButtons("5");
            this.initCustomButtons("6");
            this.initCustomButtons("7");
            this.initCustomButtons("8");
        }

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
                onPlus(amountString);
                break;

            case R.id.key_multiply:
                // TODO Add multiply funct.
                break;

            case R.id.key_clear:
                this.sumString = "";
                this.amountString = "0";
                updateAmount();
                updateSum();
                break;

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
            return UIUtils.formatCoin(this.getContext(), this.amountString, false);
        } else {
            return UIUtils.formatCoin(this.getContext(), exchangeRate.fiatToCoin(this.getFiat()).toPlainString(), true);
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
        final TextView smallTextView = (TextView) this.getView().findViewById(R.id.amount_small_text_view);
        final TextView largeTextView = (TextView) this.getView().findViewById(R.id.amount_large_text_view);

        largeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, UIUtils.getLargeTextSize(this.getContext(), amountString.length()));

        final Coin coin = this.getCoin();
        final Fiat fiat = this.getFiat();

        if (this.isBitcoinLargeAmount) {
            largeTextView.setText(UIUtils.toLargeSpannable(this.getContext(), this.amountString, UIUtils.getCoinDenomination(this.getContext())));
            smallTextView.setText(UIUtils.toSmallSpannable(fiat.toPlainString(), "CHF"));
        } else {
            largeTextView.setText(UIUtils.toLargeSpannable(this.getContext(), this.amountString, "CHF"));
            smallTextView.setText(UIUtils.toSmallSpannable(coin.toPlainString(), UIUtils.getCoinDenomination(this.getContext())));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        this.updateAmount();
    }

    @Override
    public void onDigit(int digit) {

        final String coinDenom = UIUtils.getCoinDenomination(this.getContext());
        final int decimalThreshold = UIUtils.getDecimalThreshold(coinDenom);
        final int fractionalLength = UIUtils.getFractionalLengthFromString(this.amountString);
        final int integerLength = UIUtils.getIntegerLengthFromString(this.amountString);

        final boolean isDecimal = UIUtils.isDecimal(amountString);

        if(isDecimal){
            if (fractionalLength < decimalThreshold) {
                this.amountString += digit;
                this.amountString = new BigDecimal(amountString).toString();
                this.updateAmount();
            }
        }

        if(!isDecimal){
            if (integerLength < MAXIMUM_AMOUNT_LENGTH) {
                this.amountString += digit;
                this.amountString = new BigDecimal(amountString).toString();
                this.updateAmount();
            }
        }

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

    private boolean isBitcoinLargeAmount() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        String isLargeAmount = prefs.getString(PRIMARY_BALANCE_PREF_KEY, null);
        boolean isLarge = true;
        switch (isLargeAmount) {
            case BTC_AS_PRIMARY:
                isLarge = true;
                break;
            case FIAT_AS_PRIMARY:
                isLarge = false;
                break;
        }
        return isLarge;
    }

    @Override
    public void onPlus(String value) {
        if (this.sumString.length() == 0) {
            this.sumString += value;
        } else {
            this.sumString += ("+" + value);
        }
        this.amountString = "0";
        updateAmount();
        this.updateSum();
    }


    private void updateSum() {
        final TextView sumTextView = (TextView) this.getView().findViewById(R.id.sum_values_text_view);
        sumTextView.setText((sumString + "=" + UIUtils.appendResult(sumString)));
    }


    public void onCustom(int customKey) {

        if (UIUtils.isCustomButtonEmpty(this.getContext(), Integer.toString(customKey))) {
            CustomValueDialog cvd = new CustomValueDialog(getContext(), Integer.toString(customKey));
            cvd.setCustomValueListener(new CustomValueDialog.CustomValueListener() {
                @Override
                public void onSharedPrefsUpdated(String customKey) {
                    initCustomButton(customKey);
                }
            });
            cvd.show();
        } else {
            try {
                this.onPlus(UIUtils.getCustomButton(this.getContext(), (Integer.toString(customKey))).get(1));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void initCustomButton(String customKey) {

        List<String> contentList = UIUtils.getCustomButton(this.getContext(), customKey);

        if (contentList != null) {
            switch (customKey) {
                case "1":
                    ((TextView) this.getActivity().findViewById(R.id.key_custom_one))
                            .setText(UIUtils.formatCustomButton(contentList.get(0), contentList.get(1)));
                    break;
                case "2":
                    ((TextView) this.getActivity().findViewById(R.id.key_custom_two))
                            .setText(UIUtils.formatCustomButton(contentList.get(0), contentList.get(1)));
                    break;
                case "3":
                    ((TextView) this.getActivity().findViewById(R.id.key_custom_three))
                            .setText(UIUtils.formatCustomButton(contentList.get(0), contentList.get(1)));
                    break;
                case "4":
                    ((TextView) this.getActivity().findViewById(R.id.key_custom_four))
                            .setText(UIUtils.formatCustomButton(contentList.get(0), contentList.get(1)));
                    break;
                case "5":
                    ((TextView) this.getActivity().findViewById(R.id.key_custom_five))
                            .setText(UIUtils.formatCustomButton(contentList.get(0), contentList.get(1)));
                    break;
                case "6":
                    ((TextView) this.getActivity().findViewById(R.id.key_custom_six))
                            .setText(UIUtils.formatCustomButton(contentList.get(0), contentList.get(1)));
                    break;
                case "7":
                    ((TextView) this.getActivity().findViewById(R.id.key_custom_seven))
                            .setText(UIUtils.formatCustomButton(contentList.get(0), contentList.get(1)));
                    break;
                case "8":
                    ((TextView) this.getActivity().findViewById(R.id.key_custom_eight))
                            .setText(UIUtils.formatCustomButton(contentList.get(0), contentList.get(1)));
                    break;
            }
        }

    }

    private void initCustomButtons(String customKey) {
        if (UIUtils.isCustomButtonEmpty(this.getContext(), customKey) == false) {
            this.initCustomButton(customKey);
        }
    }

}
