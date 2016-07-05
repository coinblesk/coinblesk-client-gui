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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.client.utils.UIUtils;
import com.coinblesk.client.config.Constants;
import com.coinblesk.payments.WalletService;
import com.coinblesk.client.models.TransactionWrapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;

import java.util.List;

import javax.annotation.Nullable;


/**
 * @author ckiller
 * @author Andreas Albrecht
 * @author Alessandro De Carli
 */
public class TransactionDetailActivity extends AppCompatActivity {
    private final static String TAG = TransactionDetailActivity.class.getName();
    public static final String ARGS_TRANSACTION_HASH = "transaction_hash";

    private WalletService.WalletServiceBinder walletServiceBinder;
    private String transactionHash;

    public static void openTransaction(Context context, String transactionHash) {
        Intent intent = new Intent(context, TransactionDetailActivity.class);
        intent.putExtra(TransactionDetailActivity.ARGS_TRANSACTION_HASH, transactionHash);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_txdetail);

        Intent intent = getIntent();
        this.transactionHash = intent.getStringExtra(ARGS_TRANSACTION_HASH);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.detail_transaction_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, WalletService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter(Constants.WALLET_TRANSACTION_CONFIDENCE_CHANGED_ACTION);
        filter.addAction(Constants.WALLET_INIT_DONE_ACTION);
        filter.addAction(Constants.WALLET_DOWNLOAD_DONE_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(walletBalanceChangeBroadcastReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService(serviceConnection);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(walletBalanceChangeBroadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tx_detail_menu, menu);
        MenuItem openItem = menu.findItem(R.id.action_tx_open_web);
        openItem.setIcon(UIUtils.tintIconWhite(openItem.getIcon(), this));
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_tx_copy:
                copyTx();
                return true;
            case R.id.action_tx_open_web:
                openTx();
                return true;
            case R.id.action_tx_broadcast:
                broadcastTx();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void copyTx() {
        Log.d(TAG, "Copy Transaction Hash: " + transactionHash);
        Log.d(TAG, "Transaction Details:\n" + walletServiceBinder.getTransaction(transactionHash).toString());

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Your TX", transactionHash);
        clipboard.setPrimaryClip(clip);

        View root = findViewById(android.R.id.content);
        Snackbar.make(root,
                    UIUtils.toFriendlySnackbarString(this, getString(R.string.snackbar_address_copied)),
                    Snackbar.LENGTH_LONG)
                .setActionTextColor(ContextCompat.getColor(this, R.color.colorAccent))
                .setAction("Action", null)
                .show();
    }

    private void openTx() {
        NetworkParameters params = walletServiceBinder.networkParameters();
        String blockchainExplorerUrl;
        if (ClientUtils.isMainNet(params)) {
            blockchainExplorerUrl = getString(R.string.url_blocktrail_explorer_mainnet, transactionHash);
        } else if (ClientUtils.isTestNet(params)) {
            blockchainExplorerUrl = getString(R.string.url_blocktrail_explorer_testnet, transactionHash);;
        } else {
            throw new RuntimeException("Network not supported: " + params.getId());
        }

        Uri txUri = Uri.parse(blockchainExplorerUrl);
        Intent intent = new Intent(Intent.ACTION_VIEW, txUri);
        startActivity(intent);
    }

    private void broadcastTx() {
        if (walletServiceBinder != null && walletServiceBinder.isReady()) {
            Transaction tx = walletServiceBinder.getTransaction(transactionHash).getTransaction();
            ListenableFuture<Transaction> future = walletServiceBinder.broadcastTransaction(tx);
            Futures.addCallback(future, new FutureCallback<Transaction>() {
                @Override
                public void onSuccess(@Nullable Transaction result) {
                    Log.d(TAG, "Broadcast Tx Success: " + (result != null ? result.getHashAsString() : "null"));
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.d(TAG, "Broadcast Tx Failed: " + t.getMessage());
                }
            });
        }
    }

    private void updateTransactionDetails() {
        final TransactionWrapper txWrapper = walletServiceBinder.getTransaction(transactionHash);
        final Transaction transaction = txWrapper.getTransaction();
        try {

            // No one liner because of color filter, sorry
            final ImageView statusIcon = (ImageView) findViewById(R.id.txdetail_status_icon);
            statusIcon.setImageResource(
                    (transaction.getConfidence().getDepthInBlocks() > 0)
                            ? R.drawable.ic_checkbox_marked_circle_outline_white_18dp
                            : R.drawable.ic_clock_white_18dp);
            statusIcon.setColorFilter(UIUtils.getStatusColorFilter(transaction.getConfidence().getDepthInBlocks(), false));

            ((TextView) findViewById(R.id.txdetail_amount_content)).setText(UIUtils.toFriendlyAmountString(getApplicationContext(), txWrapper));
            ((TextView) findViewById(R.id.txdetail_status_content)).setText(transaction.getConfidence().toString());
            ((TextView) findViewById(R.id.txdetail_date_content)).setText(transaction.getUpdateTime().toString());
            ((TextView) findViewById(R.id.txdetail_txhash_content)).setText(transactionHash);

            // for incoming tx, fee is null because inputs not known.
            Coin fee = transaction.getFee();
            if (fee != null) {
                ((TextView) findViewById(R.id.txdetail_fee_content)).setText(fee.toFriendlyString());
            } else {
                findViewById(R.id.txfee).setVisibility(View.GONE);
            }

            String addressSentTo = sentToAddressOfTx(txWrapper);
            // TODO: also show if sent to one of my addresses
            if (addressSentTo != null) {
                ((TextView) findViewById(R.id.txdetail_address_to_content)).setText(addressSentTo);
            } else {
                findViewById(R.id.txdetail_address_to).setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.w(TAG, "updateTransactionDetails exception: ", e);
        }
    }

    private String sentToAddressOfTx(TransactionWrapper txWrapper) {
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

                Address addr = o.getScriptPubKey().getToAddress(walletServiceBinder.networkParameters());
                addressTo = addr.toBase58();
            }
        }
        return addressTo;
    }


    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private final BroadcastReceiver walletBalanceChangeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTransactionDetails();
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;

            if(walletServiceBinder.isReady()){
                updateTransactionDetails();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */
}
