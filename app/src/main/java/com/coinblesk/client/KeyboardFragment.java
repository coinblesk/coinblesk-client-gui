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

import android.app.DialogFragment;
import android.app.Fragment;
import android.content.*;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.coinblesk.client.ui.dialogs.CurrencyDialogFragment;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.coinblesk.client.utils.UIUtils;
import com.coinblesk.client.ui.dialogs.CustomValueDialog;
import com.coinblesk.client.config.Constants;
import com.coinblesk.payments.WalletService;
import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import java.util.List;

/**
 * Created by ckiller on 24/01/16.
 */

public abstract class KeyboardFragment extends Fragment implements View.OnClickListener, CustomValueDialog.CustomValueListener {
    private final static String TAG = KeyboardFragment.class.getSimpleName();

    private static final int MAXIMUM_COIN_AMOUNT_LENGTH = 7;

    private final static String KEY_AMOUNT_BTC = "amount_btc";
    private final static String KEY_AMOUNT_FIAT = "amount_fiat";

    //private final static String KEY_SUM = "sum";
    private final static String KEY_IS_BITCOIN_LARGE_AMOUNT = "isBitcoinLargeAmount";
    private final static String KEY_DIGIT_COUNTER = "digitCounter";
    private final static String KEY_DOT_AT_POS = "dotAtPos";

    private final static int FIAT_SCALE = 10000;


    private long amountBTC = 0;
    private long amountFiat = 0;
    //private long sumBTC = 0;
    //private long sumFiat = 0;
    private int digitCounter = 0;
    private int dotAtPos = -1;

    private ExchangeRate exchangeRate = new ExchangeRate(Fiat.parseFiat("CHF", "660"));
    private boolean isBitcoinPrimary = true;

    protected abstract DialogFragment getDialogFragment();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        Log.d(TAG, "onCreate");
        isBitcoinPrimary = SharedPrefUtils.getPrimaryBalance(getActivity()).equals("bitcoin");
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

        TextView t1 = (TextView) view.findViewById(R.id.amount_large_text_view);
        TextView t2 = (TextView) view.findViewById(R.id.amount_large_text_currency);
        TextView t3 = (TextView) view.findViewById(R.id.amount_small_text_view);
        TextView t4 = (TextView) view.findViewById(R.id.amount_small_text_currency);

        View.OnLongClickListener listener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                DialogFragment fragment = CurrencyDialogFragment.newInstance();
                if (fragment != null) {
                    fragment.show(KeyboardFragment.this.getFragmentManager(), TAG);
                }
                return true;
            }
        };

        t1.setOnLongClickListener(listener);
        t2.setOnLongClickListener(listener);
        t3.setOnLongClickListener(listener);
        t4.setOnLongClickListener(listener);

        UIUtils.refreshConnectionIconStatus(getActivity(), view);

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
            if (inState.containsKey(KEY_AMOUNT_BTC)) {
                amountBTC = inState.getLong(KEY_AMOUNT_BTC, 0);
            }
            if (inState.containsKey(KEY_AMOUNT_FIAT)) {
                amountFiat = inState.getLong(KEY_AMOUNT_FIAT, 0);
            }
            if (inState.containsKey(KEY_IS_BITCOIN_LARGE_AMOUNT)) {
                isBitcoinPrimary = inState.getBoolean(KEY_IS_BITCOIN_LARGE_AMOUNT, true);
            }
            if (inState.containsKey(KEY_DIGIT_COUNTER)) {
                digitCounter = inState.getInt(KEY_DIGIT_COUNTER, 0);
            }
            if (inState.containsKey(KEY_IS_BITCOIN_LARGE_AMOUNT)) {
                dotAtPos = inState.getInt(KEY_DOT_AT_POS, -1);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(KEY_AMOUNT_BTC, amountBTC);
        outState.putLong(KEY_AMOUNT_FIAT, amountFiat);
        outState.putBoolean(KEY_IS_BITCOIN_LARGE_AMOUNT, isBitcoinPrimary);
        outState.putInt(KEY_DIGIT_COUNTER, digitCounter);
        outState.putInt(KEY_DOT_AT_POS, dotAtPos);
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
        setCustomButtonClickListeners(view.findViewById(R.id.key_custom_one));
        setCustomButtonClickListeners(view.findViewById(R.id.key_custom_two));
        setCustomButtonClickListeners(view.findViewById(R.id.key_custom_three));
        setCustomButtonClickListeners(view.findViewById(R.id.key_custom_four));
        setCustomButtonClickListeners(view.findViewById(R.id.key_custom_five));
        setCustomButtonClickListeners(view.findViewById(R.id.key_custom_six));
        setCustomButtonClickListeners(view.findViewById(R.id.key_custom_seven));
        setCustomButtonClickListeners(view.findViewById(R.id.key_custom_eight));
    }

    private void setCustomButtonClickListeners(View v) {
        if (v != null) {
            v.setOnClickListener(this);
            v.setOnLongClickListener(onCustomLongClickListener);
        }
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.key_one:
                onDigit(1);
                break;

            case R.id.key_two:
                onDigit(2);
                break;

            case R.id.key_three:
                onDigit(3);
                break;

            case R.id.key_four:
                onDigit(4);
                break;

            case R.id.key_five:
                onDigit(5);
                break;

            case R.id.key_six:
                onDigit(6);
                break;

            case R.id.key_seven:
                onDigit(7);
                break;

            case R.id.key_eight:
                onDigit(8);
                break;

            case R.id.key_nine:
                onDigit(9);
                break;

            case R.id.key_zero:
                onDigit(0);
                break;

            case R.id.key_dot:
                dotAtPos = digitCounter;
                break;

            case R.id.amount_switch_image_view:
                if(isBitcoinPrimary) {
                    amountFiat = (long) (amountBTC / (UIUtils.scale(getActivity()) / (double)FIAT_SCALE));
                    this.isBitcoinPrimary = false;
                } else {
                    amountBTC = (long) (amountFiat * (UIUtils.scale(getActivity()) / (double)FIAT_SCALE));
                    this.isBitcoinPrimary = true;
                }
                break;

            case R.id.key_accept:
                onEnter();
                break;

            case R.id.key_plus:
                //TODO
                break;

            case R.id.key_subtotal:
                //TODO
                break;

            case R.id.amount_backspace_image_view:
            case R.id.key_backspace:
                if(digitCounter == 0) {
                    break;
                }
                if(isBitcoinPrimary) {
                    int scale = UIUtils.scale(getActivity());
                    amountBTC = backspace(amountBTC, scale, digitCounter, dotAtPos);
                } else {
                    int scale = FIAT_SCALE;
                    amountFiat = backspace(amountFiat, scale, digitCounter, dotAtPos);
                }
                this.digitCounter--;
                //remove the dot
                if(digitCounter <= dotAtPos) {
                    dotAtPos = -1;
                }
                break;

            case R.id.key_clear:
                this.amountBTC = 0;
                this.amountFiat = 0;
                this.digitCounter = 0;
                this.dotAtPos = -1;
                break;

            case R.id.key_custom_one:
                onCustom(1);
                break;
            case R.id.key_custom_two:
                onCustom(2);
                break;
            case R.id.key_custom_three:
                onCustom(3);
                break;
            case R.id.key_custom_four:
                onCustom(4);
                break;
            case R.id.key_custom_five:
                onCustom(5);
                break;
            case R.id.key_custom_six:
                onCustom(6);
                break;
            case R.id.key_custom_seven:
                onCustom(7);
                break;
            case R.id.key_custom_eight:
                onCustom(8);
                break;
        }
        updateAmount();

    }

    private static long backspace(long amount, int scale, int digitCounter, int dotAtPos) {
        if(digitCounter == 1) {
            amount = 0;
        } else {
            if(dotAtPos < 0 || digitCounter - dotAtPos <= 0) {
                //before the dot
                amount = (amount / (scale *10) ) * scale;
            } else {
                //after the dot
                scale /= UIUtils.pow(10, digitCounter - dotAtPos - 1);
                amount = (amount / (scale) ) * scale;
            }
        }
        return amount;
    }

    public Coin coin() {
        return Coin.valueOf(amountBTC);
    }

    private Coin coinConvert() {
        if(isBitcoinPrimary) {
            return Coin.valueOf(amountBTC);
        } else {
            //convert first
            String  currency = SharedPrefUtils.getCurrency(getActivity());
            Fiat fiat = Fiat.valueOf(currency, amountFiat);
            if(exchangeRate.fiat.currencyCode.equals(fiat.currencyCode)) {
                //exchange rate is set from the service, which may not have stared yet
                Coin retVal = exchangeRate.fiatToCoin(fiat);
                amountBTC = retVal.value;
                return retVal;
            }
            return Coin.ZERO;
        }
    }

    private Fiat fiat() {
        if(isBitcoinPrimary) {
            //convert first
            return exchangeRate.coinToFiat(Coin.valueOf(amountBTC));
        } else {
            String  currency = SharedPrefUtils.getCurrency(getActivity());
            Fiat fiat = Fiat.valueOf(currency, amountFiat);
            return fiat;
        }
    }

    public KeyboardFragment coin(Coin coin) {
        this.amountBTC = coin.value;
        //now we need to calculate the digit and dot counter
        String formatted = UIUtils.formater2(getActivity()).decimalMark('.').noCode().minDecimals(0).optionalDecimals(1,1).format(coin).toString();
        formatted = trimLeadingZeros(formatted);
        digitCounter = formatted.length();
        dotAtPos = formatted.indexOf(".");
        if(dotAtPos >=0) {
            digitCounter--;
        }
        this.amountFiat = exchangeRate.coinToFiat(coin).value;

        updateAmount();
        return this;
    }

    public KeyboardFragment btcPrimary() {
        if(!isBitcoinPrimary) {
            amountBTC = (long) (amountFiat * (UIUtils.scale(getActivity()) / (double)FIAT_SCALE));
            this.isBitcoinPrimary = true;
            updateAmount();
        }
        return this;
    }

    private static String trimLeadingZeros(String source) {
        for (int i = 0; i < source.length(); ++i) {
            char c = source.charAt(i);
            if (c != '0' && !Character.isSpaceChar(c))
                return source.substring(i);
        }
        return source;
    }

    private void updateAmount() {

        refreshConnectionIcons();
        final TextView smallTextView = (TextView) this.getView().findViewById(R.id.amount_small_text_view);
        final TextView smallTextCurrency = (TextView) this.getView().findViewById(R.id.amount_small_text_currency);
        final TextView largeTextView = (TextView) this.getView().findViewById(R.id.amount_large_text_view);
        final TextView largeTextCurrency = (TextView) this.getView().findViewById(R.id.amount_large_text_currency);

        String formattedBTC = UIUtils.formater(getActivity()).format(coinConvert(), 0, 1, 1);
        //convert as it is satoshi to get the same format as for BTC
        long value = (long) (fiat().value * (UIUtils.scale(getActivity()) / (double)FIAT_SCALE));
        String formattedFiat = UIUtils.formater(getActivity()).format(Coin.valueOf(value), 0, 1, 1);

        if (largeTextView != null && largeTextCurrency!= null && smallTextView!=null && smallTextCurrency!=null) {

            if (isBitcoinPrimary) {
                largeTextView.setText(formattedBTC);
                largeTextCurrency.setText(UIUtils.getMoneyFormat(getActivity()).code());
                smallTextView.setText(formattedFiat);
                smallTextCurrency.setText(exchangeRate.fiat.getCurrencyCode());
            } else {
                largeTextView.setText(formattedFiat);
                largeTextCurrency.setText(exchangeRate.fiat.getCurrencyCode());
                smallTextView.setText(formattedBTC);
                smallTextCurrency.setText(UIUtils.getMoneyFormat(getActivity()).code());
            }
        }
    }



    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this.getActivity(), WalletService.class);
        this.getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }


    private void onDigit(int digit) {
        if(digitCounter > MAXIMUM_COIN_AMOUNT_LENGTH) {
            return;
        }
        if (isBitcoinPrimary) {
            int scale = UIUtils.scale(getActivity());
            long amount = digit(amountBTC, digit, scale, digitCounter, dotAtPos);
            if(amount < 0) {
                return;
            }
            amountBTC = amount;
        }  else {
            int scale = FIAT_SCALE;
            long amount = digit(amountFiat, digit, scale, digitCounter, dotAtPos);
            if(amount < 0) {
                return;
            }
            amountFiat = amount;
        }
        digitCounter++;
    }

    private static long digit(long amount, int digit, int scale, int digitCounter, int dotAtPos) {
        if(dotAtPos < 0) {
            //before the dot

            //first number has to have the right scale
            if (digitCounter == 0) {
                amount *= scale;
            } else {
                //subsequent numbers, scale by 10
                amount *= 10;
            }
        } else {
            //after the dot
            int pos = digitCounter - dotAtPos;
            if(pos >= 2) {
                return -1;
            }
            scale /= UIUtils.pow(10, pos + 1);
        }
        amount += digit * scale;
        return amount;
    }

    private void onEnter() {
        if (coinConvert().isPositive()) {
            DialogFragment fragment = getDialogFragment();
            if (fragment != null) {
                fragment.show(this.getFragmentManager(), "keyboard_dialog_fragment");
            }
        }
    }


    public void onCustom(int customKey) {
        /*if (!SharedPrefUtils.isCustomButtonEmpty(getContext(), Integer.toString(customKey))) {
            try {
                String price = null;

                List<String> btnProperties = UIUtils.getCustomButton(getContext(), (Integer.toString(customKey)));
                if (btnProperties != null && btnProperties.size() == 2) {
                    price = btnProperties.get(1);
                }

                TextView sumValues = getSumValuesTextView();
                if (price == null || sumValues == null) {
                    return;
                }

                if (sumString.length() == 0) {
                    sumString += price;
                    sumValues.setText((sumString));
                } else {
                    sumString += ("+" + price);
                    sumValues.setText((sumString + "=" + UIUtils.getSum(sumString)));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error onCustom " + customKey + ": ", e);
            }
        }*/
    }

    private final View.OnLongClickListener onCustomLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            switch (v.getId()) {
                case R.id.key_custom_one:
                    onCustomLong(1);
                    return true;
                case R.id.key_custom_two:
                    onCustomLong(2);
                    return true;
                case R.id.key_custom_three:
                    onCustomLong(3);
                    return true;
                case R.id.key_custom_four:
                    onCustomLong(4);
                    return true;
                case R.id.key_custom_five:
                    onCustomLong(5);
                    return true;
                case R.id.key_custom_six:
                    onCustomLong(6);
                    return true;
                case R.id.key_custom_seven:
                    onCustomLong(7);
                    return true;
                case R.id.key_custom_eight:
                    onCustomLong(8);
                    return true;
                default:
                    return false;
            }
        }
    };

    private void onCustomLong(int customKey) {
        CustomValueDialog cvd = new CustomValueDialog(getActivity(), Integer.toString(customKey));
        cvd.setCustomValueListener(new CustomValueDialog.CustomValueListener() {
            @Override
            public void onSharedPrefsUpdated(String customKey) {
                initCustomButton(customKey);
            }
        });
        cvd.show();
    }

    protected void initCustomButton(String customKey) {

        List<String> contentList = UIUtils.getCustomButton(getActivity(), customKey);

        if (getView() != null && contentList != null) {
            View button = null;
            switch (customKey) {
                case "1":
                    button = getView().findViewById(R.id.key_custom_one);
                    break;
                case "2":
                    button = getView().findViewById(R.id.key_custom_two);
                    break;
                case "3":
                    button = getView().findViewById(R.id.key_custom_three);
                    break;
                case "4":
                    button = getView().findViewById(R.id.key_custom_four);
                    break;
                case "5":
                    button = getView().findViewById(R.id.key_custom_five);
                    break;
                case "6":
                    button = getView().findViewById(R.id.key_custom_six);
                    break;
                case "7":
                    button = getView().findViewById(R.id.key_custom_seven);
                    break;
                case "8":
                    button = getView().findViewById(R.id.key_custom_eight);
                    break;
                default:
                    button = null;
            }

            if (button != null && contentList.size() == 2) {
                String buttonText = UIUtils.formatCustomButton(contentList.get(0), contentList.get(1));
                ((TextView) button).setText(buttonText);
            }
        }
    }

    private void initCustomButtons(String customKey) {
        if (!SharedPrefUtils.isCustomButtonEmpty(getActivity(), customKey)) {
            this.initCustomButton(customKey);
        }
    }

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private final BroadcastReceiver exchangeRateChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String symbol = intent.getStringExtra(Constants.EXCHANGE_RATE_SYMBOL);
            if(symbol != null && !symbol.isEmpty()) {
                walletServiceBinder.setCurrency(symbol);
            }
            //exchangeRate = walletServiceBinder.getExchangeRate();

            updateAmount();
        }
    };

    private final BroadcastReceiver instantPaymentSuccessListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playNotificationSound();
            Snackbar.make(getView(), UIUtils.toFriendlySnackbarString(getActivity(), getResources().getString(R.string.instant_payment_success_message)), Snackbar.LENGTH_LONG).show();
        }
    };

    private final BroadcastReceiver insufficientFundsListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playNotificationSound();
            Snackbar.make(getView(), UIUtils.toFriendlySnackbarString(getActivity(), getResources().getString(R.string.insufficient_funds)), Snackbar.LENGTH_LONG).show();
        }
    };

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getActivity(), notification);
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
            Snackbar.make(getView(), UIUtils.toFriendlySnackbarString(getActivity(), msg), Snackbar.LENGTH_LONG)
                    .show();
        }
    };

    private WalletService.WalletServiceBinder walletServiceBinder;

    @Override
    public void onStop() {
        super.onStop();
        this.getActivity().unbindService(serviceConnection);
        LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(getActivity());
        broadcaster.unregisterReceiver(exchangeRateChangeListener);
        broadcaster.unregisterReceiver(instantPaymentErrorListener);
        broadcaster.unregisterReceiver(instantPaymentSuccessListener);
        broadcaster.unregisterReceiver(insufficientFundsListener);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            //exchangeRate = walletServiceBinder.getExchangeRate();
            LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(getActivity());
            broadcaster.registerReceiver(exchangeRateChangeListener, new IntentFilter(Constants.EXCHANGE_RATE_CHANGED_ACTION));
            broadcaster.registerReceiver(instantPaymentSuccessListener, new IntentFilter(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION));
            broadcaster.registerReceiver(instantPaymentErrorListener, new IntentFilter(Constants.INSTANT_PAYMENT_FAILED_ACTION));
            broadcaster.registerReceiver(insufficientFundsListener, new IntentFilter(Constants.WALLET_INSUFFICIENT_BALANCE_ACTION));
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

    @Override
    public void onResume() {
        super.onResume();
        updateAmount();
    }

    private void refreshConnectionIcons() {
        final View view = getView();
        if (view != null) {
            UIUtils.refreshConnectionIconStatus(getActivity(), view);
        }
    }
}
