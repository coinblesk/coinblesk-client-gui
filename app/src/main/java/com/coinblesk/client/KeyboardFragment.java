package com.coinblesk.client;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.coinblesk.client.coinblesk_client_gui.R;
import com.coinblesk.client.helpers.UIUtils;
import com.coinblesk.client.ui.dialogs.CustomValueDialog;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import java.math.BigDecimal;
import java.util.List;

import com.coinblesk.payments.Constants;
import com.coinblesk.payments.WalletService;

/**
 * Created by ckiller on 24/01/16.
 */

public abstract class KeyboardFragment extends Fragment implements View.OnClickListener, OnKeyboardListener, CustomValueDialog.CustomValueListener {
    private String amountString = "0";
    private String sumString = "";

    private ExchangeRate exchangeRate = new ExchangeRate(Fiat.parseFiat("CHF", "430"));
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
                this.amountString = "0";
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

        if (this.isBitcoinLargeAmount) {
            largeTextView.setText(UIUtils.toLargeSpannable(this.getContext(), this.amountString, UIUtils.getCoinDenomination(this.getContext())));
            smallTextView.setText(UIUtils.toSmallSpannable(fiat.toPlainString(), this.exchangeRate.fiat.getCurrencyCode()));
        } else {
            largeTextView.setText(UIUtils.toLargeSpannable(this.getContext(), this.amountString, this.exchangeRate.fiat.getCurrencyCode()));
            smallTextView.setText(UIUtils.toSmallSpannable(UIUtils.scaleCoin(coin,
                            UIUtils.getCoinDenomination(this.getContext())),
                    UIUtils.getCoinDenomination(this.getContext())));
        }
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
        final String coinDenom = UIUtils.getCoinDenomination(this.getContext());
        final int decimalThreshold = UIUtils.getDecimalThreshold(coinDenom);
        final int fractionalLength = UIUtils.getFractionalLengthFromString(this.amountString);
        final int integerLength = UIUtils.getIntegerLengthFromString(this.amountString);

        final boolean isDecimal = UIUtils.isDecimal(this.amountString);

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
        if (this.getCoin().isPositive()) {
            this.getDialogFragment().show(this.getFragmentManager(), "keyboard_dialog_fragment");
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

                if (this.sumString.length() == 0) {
                    this.sumString += UIUtils.getCustomButton(this.getContext(), (Integer.toString(customKey))).get(1);
                    ((TextView) this.getView().findViewById(R.id.sum_values_text_view)).setText((sumString));
                } else {
                    this.sumString += ("+" + UIUtils.getCustomButton(this.getContext(), (Integer.toString(customKey))).get(1));
                    ((TextView) this.getView().findViewById(R.id.sum_values_text_view)).setText((sumString + "=" + UIUtils.getSum(sumString)));
                }
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

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private final BroadcastReceiver exchangeRateChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            exchangeRate = walletServiceBinder.getExchangeRate();
            updateAmount();
        }
    };

//    UIUtils.toFriendlySnackbarString(getApplicationContext(),getResources()
//            .getString(R.string.snackbar_address_copied)

    private final BroadcastReceiver instantPaymentSuccessListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getContext(), notification);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Snackbar.make(getView(), UIUtils.toFriendlySnackbarString(getContext(), getResources().getString(R.string.instant_payment_success_message)), Snackbar.LENGTH_LONG).show();
        }
    };

    private final BroadcastReceiver instantPaymentErrorListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Snackbar.make(getView(), UIUtils.toFriendlySnackbarString(getContext(), String.format(getResources().getString(R.string.instant_payment_error_message), intent.getExtras().getString(Constants.ERROR_MESSAGE_KEY, ""))), Snackbar.LENGTH_LONG).show();
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
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            exchangeRate = walletServiceBinder.getExchangeRate();
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(exchangeRateChangeListener, new IntentFilter(Constants.EXCHANGE_RATE_CHANGED_ACTION));
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(instantPaymentSuccessListener, new IntentFilter(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION));
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(instantPaymentErrorListener, new IntentFilter(Constants.INSTANT_PAYMENT_FAILED_ACTION));
            walletServiceBinder.fetchExchangeRate();
            updateAmount();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */

}
