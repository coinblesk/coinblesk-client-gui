/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.coinblesk.client;

import android.content.*;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.coinblesk.client.ui.OnKeyboardListener;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.coinblesk.client.utils.UIUtils;
import com.coinblesk.client.ui.dialogs.CustomValueDialog;
import com.coinblesk.client.config.Constants;
import com.coinblesk.payments.WalletService;
import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by ckiller on 24/01/16.
 */

public abstract class KeyboardFragment extends Fragment implements View.OnClickListener, OnKeyboardListener, CustomValueDialog.CustomValueListener {
    private final static String TAG = KeyboardFragment.class.getSimpleName();

    private final static String KEY_AMOUNT = "amount";
    private final static String KEY_SUM = "sum";
    private final static String KEY_IS_BITCOIN_LARGE_AMOUNT = "isBitcoinLargeAmount";

    private String amountString = "0";
    private String sumString = "";

    private ExchangeRate exchangeRate = new ExchangeRate(Fiat.parseFiat("CHF", "430"));
    private boolean isBitcoinLargeAmount = true;

    protected abstract DialogFragment getDialogFragment();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        Log.d(TAG, "onCreate");
        isBitcoinLargeAmount = SharedPrefUtils.getPrimaryBalance(getContext()).equals("Bitcoin");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        UIUtils.formatConnectionIcon(this.getContext(), nfcIcon, AppConstants.NFC_ACTIVATED);
        UIUtils.formatConnectionIcon(this.getContext(), bluetoothIcon, AppConstants.BT_ACTIVATED);
        UIUtils.formatConnectionIcon(this.getContext(), wifiIcon, AppConstants.WIFIDIRECT_ACTIVATED);

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

    @Override
    public void onActivityCreated(Bundle inState) {
        super.onActivityCreated(inState);

        if (inState != null) {
            if (inState.containsKey(KEY_AMOUNT)) {
                amountString = inState.getString(KEY_AMOUNT, "0");
            }
            if (inState.containsKey(KEY_SUM)) {
                sumString = inState.getString(KEY_SUM, "");
            }
            if (inState.containsKey(KEY_IS_BITCOIN_LARGE_AMOUNT)) {
                isBitcoinLargeAmount = inState.getBoolean(KEY_IS_BITCOIN_LARGE_AMOUNT, true);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (amountString != null) {
            outState.putString(KEY_AMOUNT, amountString);
        }
        if (sumString != null) {
            outState.putString(KEY_SUM, sumString);
        }
        outState.putBoolean(KEY_IS_BITCOIN_LARGE_AMOUNT, isBitcoinLargeAmount);
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
        view.findViewById(R.id.key_backspace).setOnClickListener(this);
        view.findViewById(R.id.key_accept).setOnClickListener(this);
        view.findViewById(R.id.key_clear).setOnClickListener(this);
        view.findViewById(R.id.key_subtotal).setOnClickListener(this);
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

            case R.id.key_subtotal:
                if (this.amountString.equals("0") && this.sumString.length() > 2) {
                    this.amountString = UIUtils.getSum(sumString);
                    updateAmount();
                }
                break;

            case R.id.key_backspace:
                if (this.amountString.length() == 1) {
                    this.amountString = "0";
                } else {
                    this.amountString = this.amountString.substring(0, this.amountString.length() - 1);
                }
                this.updateAmount();
                break;

            case R.id.key_clear:
                this.sumString = "";
                this.amountString = "0";
                updateAmount();
                ((TextView) this.getView().findViewById(R.id.sum_values_text_view)).setText("");

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
            return UIUtils.getValue(this.amountString, this.getContext());
        } else {
            return exchangeRate.fiatToCoin(this.getFiat());
        }
    }

    protected Fiat getFiat() {
        if (!isBitcoinLargeAmount) {
            return Fiat.parseFiat(exchangeRate.fiat.currencyCode, this.amountString);
        } else {
            return exchangeRate.coinToFiat(UIUtils.getValue(this.amountString, this.getContext()));
        }
    }

    private void updateAmount() {
        final TextView smallTextView = (TextView) this.getView().findViewById(R.id.amount_small_text_view);
        final TextView largeTextView = (TextView) this.getView().findViewById(R.id.amount_large_text_view);

        largeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, UIUtils.getLargeTextSize(this.getContext(), amountString.length()));

        final Coin coin = this.getCoin();
        final Fiat fiat = this.getFiat();

        String coinDenomination = SharedPrefUtils.getBitcoinScalePrefix(getContext());
        if (isBitcoinLargeAmount) {
            largeTextView.setText(UIUtils.toLargeSpannable(getContext(), amountString, coinDenomination));
            smallTextView.setText(UIUtils.toSmallSpannable(fiat.toPlainString(), exchangeRate.fiat.getCurrencyCode()));
        } else {
            largeTextView.setText(UIUtils.toLargeSpannable(getContext(), amountString, exchangeRate.fiat.getCurrencyCode()));
            smallTextView.setText(UIUtils.toSmallSpannable(UIUtils.scaleCoin(coin, coinDenomination), coinDenomination));
        }
    }

    protected void setAmountByCoin(Coin newAmount) {
        if (isBitcoinLargeAmount) {
            amountString = UIUtils.coinToAmount(newAmount, getContext());
        } else {
            amountString = exchangeRate.coinToFiat(newAmount).toPlainString();
        }
        updateAmount();
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this.getActivity(), WalletService.class);
        this.getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        //this.updateAmount();
    }

    @Override
    public void onDigit(int digit) {

        if (isBitcoinLargeAmount) {
            this.onCoinDigit(digit);
        } else {
            this.onFiatDigit(digit);
        }

    }

    public void onFiatDigit(int digit) {
        final int fractionalLength = UIUtils.getFractionalLengthFromString(this.amountString);
        final int integerLength = UIUtils.getIntegerLengthFromString(this.amountString);
        final boolean isDecimal = UIUtils.isDecimal(this.amountString);

        if (isDecimal) {
            if (fractionalLength < AppConstants.FIAT_DECIMAL_THRESHOLD) {
                this.amountString += digit;
                this.amountString = new BigDecimal(amountString).toString();
                this.updateAmount();
            }
        }

        if (!isDecimal) {
            if (integerLength < AppConstants.MAXIMUM_FIAT_AMOUNT_LENGTH) {
                this.amountString += digit;
                this.amountString = new BigDecimal(amountString).toString();
                this.updateAmount();
            }
        }
    }

    public void onCoinDigit(int digit) {
        final String coinDenom = SharedPrefUtils.getBitcoinScalePrefix(getContext());
        final int decimalThreshold = UIUtils.getDecimalThreshold(coinDenom);
        final int fractionalLength = UIUtils.getFractionalLengthFromString(amountString);
        final int integerLength = UIUtils.getIntegerLengthFromString(amountString);

        final boolean isDecimal = UIUtils.isDecimal(amountString);

        if (isDecimal) {
            if (fractionalLength < decimalThreshold) {
                this.amountString += digit;
                this.amountString = new BigDecimal(amountString).toString();
                this.updateAmount();
            }
        }

        if (!isDecimal) {
            if (integerLength < AppConstants.MAXIMUM_COIN_AMOUNT_LENGTH) {
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
        if (getCoin().isPositive()) {
            DialogFragment fragment = getDialogFragment();
            if (fragment != null) {
                fragment.show(this.getFragmentManager(), "keyboard_dialog_fragment");
            }
        }
    }


    @Override
    public void onPlus(String value) {
        if (UIUtils.stringIsNotZero(this.amountString)) {
            // check if it's the first summand
            if (this.sumString.length() == 0) {
                this.sumString += value;
                ((TextView) this.getView().findViewById(R.id.sum_values_text_view)).setText((sumString));
            } else {
                this.sumString += ("+" + value);
                ((TextView) this.getView().findViewById(R.id.sum_values_text_view)).setText((sumString + "=" + UIUtils.getSum(sumString)));
            }
        }

        this.amountString = "0";
        updateAmount();
    }

    public void onCustom(int customKey) {
        if (SharedPrefUtils.isCustomButtonEmpty(getContext(), Integer.toString(customKey))) {
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

                if (this.sumString.length() == 0) {
                    this.sumString += UIUtils.getCustomButton(this.getContext(), (Integer.toString(customKey))).get(1);
                    ((TextView) this.getView().findViewById(R.id.sum_values_text_view)).setText((sumString));
                } else {
                    this.sumString += ("+" + UIUtils.getCustomButton(this.getContext(), (Integer.toString(customKey))).get(1));
                    ((TextView) this.getView().findViewById(R.id.sum_values_text_view)).setText((sumString + "=" + UIUtils.getSum(sumString)));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error onCustom " + customKey + ": ", e);
            }
        }
    }

    protected void initCustomButton(String customKey) {

        List<String> contentList = UIUtils.getCustomButton(getContext(), customKey);

        if (contentList != null) {
            View button = null;
            switch (customKey) {
                case "1":
                    button = getActivity().findViewById(R.id.key_custom_one);
                    break;
                case "2":
                    button = getActivity().findViewById(R.id.key_custom_two);
                    break;
                case "3":
                    button = getActivity().findViewById(R.id.key_custom_three);
                    break;
                case "4":
                    button = getActivity().findViewById(R.id.key_custom_four);
                    break;
                case "5":
                    button = getActivity().findViewById(R.id.key_custom_five);
                    break;
                case "6":
                    button = getActivity().findViewById(R.id.key_custom_six);
                    break;
                case "7":
                    button = getActivity().findViewById(R.id.key_custom_seven);
                    break;
                case "8":
                    button = getActivity().findViewById(R.id.key_custom_eight);
                    break;
                default:
                    button = null;
            }

            if (button != null) {
                String buttonText = UIUtils.formatCustomButton(contentList.get(0), contentList.get(1));
                ((TextView) button).setText(buttonText);
            }
        }
    }

    private void initCustomButtons(String customKey) {
        if (!SharedPrefUtils.isCustomButtonEmpty(getContext(), customKey)) {
            this.initCustomButton(customKey);
        }
    }

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private final BroadcastReceiver exchangeRateChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            exchangeRate = walletServiceBinder.getExchangeRate();
            updateAmount();
        }
    };

    private final BroadcastReceiver instantPaymentSuccessListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playNotificationSound();
            Snackbar.make(getView(), UIUtils.toFriendlySnackbarString(getContext(), getResources().getString(R.string.instant_payment_success_message)), Snackbar.LENGTH_LONG).show();
        }
    };

    private final BroadcastReceiver insufficientFundsListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playNotificationSound();
            Snackbar.make(getView(), UIUtils.toFriendlySnackbarString(getContext(), getResources().getString(R.string.insufficient_funds)), Snackbar.LENGTH_LONG).show();
        }
    };

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getContext(), notification);
            r.play();
        } catch (Exception e) {
            Log.e(TAG, "Error playing notification.", e);
        }
    }

    private final BroadcastReceiver instantPaymentErrorListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String errMsg;
            if (intent.hasExtra(Constants.ERROR_MESSAGE_KEY)) {
                errMsg = intent.getExtras().getString(
                        Constants.ERROR_MESSAGE_KEY, "");
            } else {
                errMsg = "";
            }
            String msg = getString(R.string.instant_payment_error_message, errMsg);
            Log.d(TAG, msg);
            Snackbar.make(getView(), UIUtils.toFriendlySnackbarString(getContext(), msg), Snackbar.LENGTH_LONG)
                    .show();
        }
    };

    private WalletService.WalletServiceBinder walletServiceBinder;

    @Override
    public void onStop() {
        super.onStop();
        this.getActivity().unbindService(serviceConnection);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(exchangeRateChangeListener);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(instantPaymentErrorListener);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(instantPaymentSuccessListener);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(insufficientFundsListener);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            exchangeRate = walletServiceBinder.getExchangeRate();
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(exchangeRateChangeListener, new IntentFilter(Constants.EXCHANGE_RATE_CHANGED_ACTION));
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(instantPaymentSuccessListener, new IntentFilter(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION));
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(instantPaymentErrorListener, new IntentFilter(Constants.INSTANT_PAYMENT_FAILED_ACTION));
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(insufficientFundsListener, new IntentFilter(Constants.WALLET_INSUFFICIENT_BALANCE_ACTION));
            updateAmount();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */


    protected WalletService.WalletServiceBinder getWalletServiceBinder() {
        return walletServiceBinder;
    }
}
