/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uzh.ckiller.coinblesk_client_gui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.transition.Fade;
import android.transition.Slide;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.uzh.ckiller.coinblesk_client_gui.helpers.SpannableStringFormatter;

import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import ch.papers.payments.Constants;
import ch.papers.payments.WalletService;
import ch.papers.payments.models.TransactionWrapper;


public class TransactionDetailActivity extends AppCompatActivity {
    private final static String TAG = TransactionDetailActivity.class.getName();

    public static final String EXTRA_NAME = "transaction-hash";
    private SpannableStringFormatter spannableStringFormatter;

    private String transactionHash;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_txdetail);
        spannableStringFormatter = new SpannableStringFormatter(this.getApplicationContext());

        final Button copyTxButton = (Button) this.findViewById(R.id.txdetail_copytx_button);

        copyTxButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Snackbar.make(v, spannableStringFormatter.toFriendlySnackbarString(getResources()
                        .getString(R.string.snackbar_address_copied)), Snackbar.LENGTH_LONG)
                .setActionTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                        .setAction("Action", null).show();

            }
        });

        setupWindowAnimations();

        Intent intent = getIntent();
        this.transactionHash = intent.getStringExtra(EXTRA_NAME);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.detail_transaction_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void setTransactionDetails() {
        TransactionWrapper transaction = walletServiceBinder.getTransaction(transactionHash);

        try {

            ((TextView) this.findViewById(R.id.txdetail_amount_content)).setText(spannableStringFormatter.toFriendlyAmountString(transaction));

            ((TextView) this.findViewById(R.id.txdetail_confidence_content)).setText(transaction.getTransaction().getConfidence().toString());

            //TODO Get Exchange Rate from transaction
            ((TextView) this.findViewById(R.id.txdetail_exchangerate_content)).setText("433.43 CHF");

            // TODO Format the Date properly (make it shorter, without MEZ indication)
            ((TextView) this.findViewById(R.id.txdetail_date_content)).setText(transaction.getTransaction().getUpdateTime() + "");

            ((TextView) this.findViewById(R.id.txdetail_fee_content)).setText(transaction.getTransaction().getFee().toFriendlyString());
            ((TextView) this.findViewById(R.id.txdetail_txhash_content)).setText(transaction.getTransaction().getHashAsString());

        }catch(Exception e){
            e.printStackTrace();
        }
    }


    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private final BroadcastReceiver walletBalanceChangeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setTransactionDetails();
        }
    };

    private WalletService.WalletServiceBinder walletServiceBinder;

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, WalletService.class);
        this.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.unbindService(serviceConnection);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(walletBalanceChangeBroadcastReceiver);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            walletServiceBinder.setExchangeRate(new ExchangeRate(Fiat.parseFiat("CHF", "430")));
            IntentFilter filter = new IntentFilter(Constants.WALLET_TRANSACTIONS_CHANGED_ACTION);
            LocalBroadcastManager.getInstance(TransactionDetailActivity.this).registerReceiver(walletBalanceChangeBroadcastReceiver, filter);
            setTransactionDetails();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */

    private void setupWindowAnimations() {
        Fade fade = new Fade();
        fade.setDuration(1000);
        getWindow().setEnterTransition(fade);

        Slide slide = new Slide();
        slide.setDuration(1000);
        getWindow().setReturnTransition(slide);
    }
}
