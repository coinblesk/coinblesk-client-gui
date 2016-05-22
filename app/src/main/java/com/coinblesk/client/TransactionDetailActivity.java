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
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.coinblesk.client.helpers.SharedPrefUtils;
import com.coinblesk.client.helpers.UIUtils;
import com.coinblesk.payments.Constants;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.models.TransactionWrapper;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;

import java.util.List;


public class TransactionDetailActivity extends AppCompatActivity {
    private final static String TAG = TransactionDetailActivity.class.getName();
    private final static String BLOCKTRAIL_URL_MAINNET = "https://www.blocktrail.com/BTC/tx/";
    private final static String BLOCKTRAIL_URL_TESTNET = "https://www.blocktrail.com/tBTC/tx/";

    public static final String EXTRA_NAME = "transaction-hash";
    private String transactionHash;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_txdetail);

        this.findViewById(R.id.txdetail_status_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: this should be in a menu because otherwise, this functionality is completely hidden...
                if(walletServiceBinder != null && walletServiceBinder.isReady()) {
                    Transaction tx = walletServiceBinder.getTransaction(transactionHash).getTransaction();
                    walletServiceBinder.broadcastTransaction(tx);
                }
            }
        });

        this.findViewById(R.id.txdetail_copytx_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Transaction: " + transactionHash);
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Your TX", transactionHash);
                clipboard.setPrimaryClip(clip);
                Snackbar.make(v, UIUtils.toFriendlySnackbarString(getApplicationContext(),getResources()
                        .getString(R.string.snackbar_address_copied)), Snackbar.LENGTH_LONG)
                        .setActionTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                        .setAction("Action", null).show();

            }
        });

        this.findViewById(R.id.txdetail_opentx_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String blockchainExplorerUrl = null;
                if (SharedPrefUtils.isNetworkTestnet(TransactionDetailActivity.this)) {
                    blockchainExplorerUrl = BLOCKTRAIL_URL_TESTNET;
                } else if (SharedPrefUtils.isNetworkMainnet(TransactionDetailActivity.this)) {
                    blockchainExplorerUrl = BLOCKTRAIL_URL_MAINNET;
                }

                Uri txUri = Uri.parse(blockchainExplorerUrl + transactionHash);
                Intent intent = new Intent(Intent.ACTION_VIEW, txUri);
                startActivity(intent);
            }
        });

        Intent intent = getIntent();
        this.transactionHash = intent.getStringExtra(EXTRA_NAME);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.detail_transaction_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }


    private void setTransactionDetails() {
        final TransactionWrapper txWrapper = walletServiceBinder.getTransaction(transactionHash);
        final Transaction transaction = txWrapper.getTransaction();
        try {

            // No one liner because of color filter, sorry
            final ImageView statusIcon = (ImageView) this.findViewById(R.id.txdetail_status_icon);
            statusIcon.setImageResource(transaction.getConfidence().getDepthInBlocks() > 0 ? R.drawable.ic_checkbox_marked_circle_outline_white_18dp : R.drawable.ic_clock_white_18dp);
            statusIcon.setColorFilter(UIUtils.getStatusColorFilter(transaction.getConfidence().getDepthInBlocks(), false));

            ((TextView) this.findViewById(R.id.txdetail_amount_content)).setText(UIUtils.toFriendlyAmountString(getApplicationContext(), txWrapper));
            ((TextView) this.findViewById(R.id.txdetail_status_content)).setText(transaction.getConfidence().toString());
            ((TextView) this.findViewById(R.id.txdetail_date_content)).setText(transaction.getUpdateTime().toString());
            ((TextView) this.findViewById(R.id.txdetail_txhash_content)).setText(transactionHash);

            // for incoming tx, fee is null because inputs not known.
            Coin fee = transaction.getFee();
            if (fee != null) {
                ((TextView) this.findViewById(R.id.txdetail_fee_content)).setText(fee.toFriendlyString());
            } else {
                this.findViewById(R.id.txfee).setVisibility(View.GONE);
            }

            String addressSentTo = getSentToAddress(txWrapper);
            if (addressSentTo != null) {
                ((TextView) this.findViewById(R.id.txdetail_address_to_content)).setText(addressSentTo);
            } else {
                findViewById(R.id.txdetail_address_to).setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.w(TAG, "setTransactionDetails exception: ", e);
        }
    }

    private String getSentToAddress(TransactionWrapper txWrapper) {
        if (txWrapper.getAmount().isPositive()) {
            // incoming
            return null;
        }
        List<TransactionOutput> outputs = txWrapper.getTransaction().getOutputs();
        // due to space limitations we only display if we have exactly 1 out address (which is not ours, i.e. the change)
        String addressTo = null;
        if (outputs.size() <= 2) {
            for (TransactionOutput o : outputs) {
                if (o.isMineOrWatched(walletServiceBinder.getWallet())) {
                    continue;
                }

                Address to;
                if ((to = o.getAddressFromP2PKHScript(Constants.PARAMS)) != null) {
                    addressTo = to.toString();
                } else if ((to = o.getAddressFromP2SH(Constants.PARAMS)) != null) {
                    addressTo = to.toString();
                }
            }
        }
        return addressTo;
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(walletBalanceChangeBroadcastReceiver);
        this.unbindService(serviceConnection);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            IntentFilter filter = new IntentFilter(Constants.WALLET_TRANSACTIONS_CHANGED_ACTION);
            filter.addAction(Constants.WALLET_READY_ACTION);
            LocalBroadcastManager.getInstance(TransactionDetailActivity.this).registerReceiver(walletBalanceChangeBroadcastReceiver, filter);

            if(walletServiceBinder.isReady()){
                setTransactionDetails();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */
}
