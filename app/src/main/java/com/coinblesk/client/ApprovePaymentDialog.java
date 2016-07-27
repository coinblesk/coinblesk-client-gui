package com.coinblesk.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.coinblesk.client.additionalservices.AdditionalServicesActivity;
import com.coinblesk.client.additionalservices.AdditionalServicesTasks;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.UIUtils;
import com.coinblesk.payments.WalletService;
import com.coinblesk.util.Pair;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;

/**
 * Created by draft on 16.06.16.
 */
public class ApprovePaymentDialog extends DialogFragment {

    private static final String TAG = ApprovePaymentDialog.class.getName();

    private WalletService.WalletServiceBinder walletService;

    private TextView amountText;
    private TextView feeText;
    private String address;
    private Coin amount;
    private String rawAmount;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.request_payment, null);

        address = getArguments().getString(Constants.PAYMENT_REQUEST_ADDRESS);
        rawAmount = getArguments().getString(Constants.PAYMENT_REQUEST_AMOUNT);
        long l = Long.parseLong(rawAmount);
        amount = Coin.valueOf(l);


        amountText = (TextView) view.findViewById(R.id.authview_amount_content);
        final TextView addressView = (TextView) view.findViewById(R.id.authview_address_content);
        feeText = (TextView) view.findViewById(R.id.authview_fee_content);

        addressView.setText(addressView.getText() + address);


        refreshAmountAndFee();

        Log.d(TAG, "onCreateDialog with address=" + address+ "/"+amount);

        final Context context = getActivity().getBaseContext();



        final AlertDialog d = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.request_payment)
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Constants.PAYMENT_REQUEST_APPROVED);
                        intent.putExtra(Constants.PAYMENT_REQUEST_ADDRESS, address);
                        intent.putExtra(Constants.PAYMENT_REQUEST_AMOUNT, rawAmount);
                        LocalBroadcastManager
                                .getInstance(context)
                                .sendBroadcast(intent);
                        ProgressBar pb = (ProgressBar)d.findViewById(R.id.progressBar);
                        pb.setVisibility(View.VISIBLE);
                        //Dismiss once everything is OK.
                        //d.dismiss();
                    }
                });
            }
        });


        return d;
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        Intent walletServiceIntent = new Intent(getActivity(), WalletService.class);
        getActivity().bindService(walletServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        setCancelable(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "destroyed approve Dialog");
        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        getActivity().unbindService(serviceConnection);
    }

    private void refreshAmountAndFee() {
        ExchangeRate exchangeRate = null;
        Address addressTo = null;
        Coin fee = null;
        if (walletService != null) {
            exchangeRate = walletService.getExchangeRate();
            try {
                addressTo = Address.fromBase58(walletService.getNetworkParameters(), address);
            } catch (Exception e) { /* ignore, not valid address */ }
            fee = walletService.estimateFee(addressTo, amount);
        }


        amountText.setText(UIUtils.coinFiatSpannable(getActivity(), amount, exchangeRate, true, 0.75f));

        if (fee != null) {
            feeText.setText(UIUtils.coinFiatSpannable(getActivity(), fee, exchangeRate, true, 0.75f));
        } else {
            feeText.setText(R.string.unknown);
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            walletService = (WalletService.WalletServiceBinder) service;
            refreshAmountAndFee();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            walletService = null;
        }
    };
}
