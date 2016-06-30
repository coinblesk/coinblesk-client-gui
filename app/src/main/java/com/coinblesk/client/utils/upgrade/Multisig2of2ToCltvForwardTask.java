/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.coinblesk.client.utils.upgrade;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.coinblesk.client.R;
import com.coinblesk.client.config.Constants;
import com.coinblesk.json.SignTO;
import com.coinblesk.json.VerifyTO;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.util.BitcoinUtils;
import com.coinblesk.util.CoinbleskException;
import com.coinblesk.util.InsufficientFunds;
import com.coinblesk.util.SerializeUtils;
import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Response;

/**
 * @author  Andreas Albrecht
 */
public class Multisig2of2ToCltvForwardTask extends AsyncTask<Void, Void, Transaction> {
    private static final String TAG = Multisig2of2ToCltvForwardTask.class.getName();

    private WalletService.WalletServiceBinder walletService;
    private ECKey clientKey, serverKey;

    private Exception thrownException;
    private final WeakReference<Activity> weakActivity;

    public Multisig2of2ToCltvForwardTask(Activity activity,
                                         WalletService.WalletServiceBinder walletServiceBinder,
                                         ECKey multisigClientKey, ECKey multisigServerKey) {
        this.weakActivity = new WeakReference<>(activity);
        this.walletService = walletServiceBinder;
        this.clientKey = multisigClientKey;
        this.serverKey = multisigServerKey;
        this.thrownException = null;
    }

    @Override
    protected Transaction doInBackground(Void... params) {
        try {
            return forwardFromMultisig2of2();
        } catch (Exception e) {
            Log.w(TAG, "Exception while forwarding funds from 2-of-2 multisig to cltv: ", e);
            thrownException = e;
        }
        return null;
    }

    @Override
    protected void onPostExecute (Transaction transaction) {
        // runs on UI thread
        final Activity activity = weakActivity.get();
        if (activity == null || activity.isDestroyed()) {
            // may happen if user goes back before task completed.
            return;
        }

        if (thrownException != null) {
            // case: error
            String errorMsg = thrownException.getMessage();
            if (errorMsg == null) errorMsg = "unknown";

            AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogAccent);
            builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setTitle(R.string.upgrade_multisig_2of2_to_cltv_forward_title)
                    .setMessage(activity.getString(R.string.upgrade_multisig_2of2_to_cltv_forward_error_message, errorMsg))
                    .create()
                    .show();
        } else if (transaction != null) {
            // case: OK
            AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogAccent);
            builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setTitle(R.string.upgrade_multisig_2of2_to_cltv_forward_title)
                    .setMessage(R.string.upgrade_multisig_2of2_to_cltv_forward_message)
                    .create()
                    .show();
        } else {
            // no exception but also no transaction (i.e. currently no outputs)
        }
    }

    private Transaction forwardFromMultisig2of2() throws InsufficientFunds, CoinbleskException, IOException {
        List<TransactionOutput> outputs = getMultisig2of2Outputs();
        if (outputs.isEmpty()) {
            Log.d(TAG, "No outputs found for 2-of-2 multisig address: "
                    + getMultisig2of2Address().toBase58());
            return null;
        }

        Address addressTo = walletService.getCurrentReceiveAddress();
        final Transaction transaction = BitcoinUtils.createSpendAllTx(
                Constants.PARAMS,
                outputs,
                addressTo,
                addressTo);

        // let server sign first
        SignTO transactionTO = new SignTO();
        transactionTO.currentDate(System.currentTimeMillis());
        transactionTO.publicKey(clientKey.getPubKey());
        transactionTO.transaction(transaction.unsafeBitcoinSerialize());
        SerializeUtils.signJSON(transactionTO, clientKey);

        Response<SignTO> signTOResponse = getCoinbleskService().sign(transactionTO).execute();
        SignTO signedTO = signTOResponse.body();

        //This is needed because otherwise we mix up signature order
        List<ECKey> keys = getSortedKeys();

        Script redeemScript = createRedeemScript();

        List<TransactionSignature> clientTxSignatures = BitcoinUtils.partiallySign(
                transaction,
                redeemScript,
                clientKey);
        List<TransactionSignature> serverTxSignatures = SerializeUtils.deserializeSignatures(signedTO.signatures());

        for (int i = 0; i < clientTxSignatures.size(); i++) {
            TransactionSignature serverSignature = serverTxSignatures.get(i);
            TransactionSignature clientSignature = clientTxSignatures.get(i);
            Script scriptSig = createScriptSig(clientSignature, serverSignature);
            transaction.getInput(i).setScriptSig(scriptSig);
            transaction.getInput(i).verify();
        }

        VerifyTO verifyRequestTO = new VerifyTO()
                .currentDate(System.currentTimeMillis())
                .publicKey(clientKey.getPubKey())
                .transaction(transaction.unsafeBitcoinSerialize());
        SerializeUtils.signJSON(verifyRequestTO, clientKey);

        walletService.broadcastTransaction(transaction);

        return transaction;
    }

    private Coin calculateBalance(List<TransactionOutput> outputs) {
        Coin balance = Coin.ZERO;
        for (TransactionOutput txOut : outputs) {
            balance = balance.add(txOut.getValue());
        }
        return balance;
    }

    private List<TransactionOutput> getMultisig2of2Outputs() {
        Address address2of2 = getMultisig2of2Address();
        List<TransactionOutput> txOutputs2of2 = new ArrayList<>();
        List<TransactionOutput> candidates = walletService.getWallet().calculateAllSpendCandidates(false, false);
        for (TransactionOutput txOut : candidates) {
            Address paidTo = txOut.getAddressFromP2SH(Constants.PARAMS);
            if (paidTo != null && paidTo.equals(address2of2)) {
                txOutputs2of2.add(txOut);
            }
        }
        return txOutputs2of2;
    }

    private Script createScriptSig(TransactionSignature clientSignature, TransactionSignature serverSignature) {
        Script redeemScript = createRedeemScript();
        List<TransactionSignature> signatures = isClientKeyFirst()
                ? ImmutableList.of(clientSignature, serverSignature)
                : ImmutableList.of(serverSignature, clientSignature);
        Script p2SHMultiSigInputScript = ScriptBuilder.createP2SHMultiSigInputScript(signatures, redeemScript);
        return p2SHMultiSigInputScript;
    }

    private boolean isClientKeyFirst() {
        return getSortedKeys().indexOf(clientKey) == 0;
    }

    private Script createRedeemScript() {
        List<ECKey> keys = getSortedKeys();
        return ScriptBuilder.createRedeemScript(2, keys);
    }

    private CoinbleskWebService getCoinbleskService() {
        return Constants.RETROFIT.create(CoinbleskWebService.class);
    }

    private Address getMultisig2of2Address() {
        Script script = createMultisig2of2Script(clientKey, serverKey);
        return script.getToAddress(Constants.PARAMS);
    }

    private Script createMultisig2of2Script(ECKey clientKey, ECKey serverKey) {
        return BitcoinUtils.createP2SHOutputScript(2, ImmutableList.of(clientKey, serverKey));
    }

    private List<ECKey> getSortedKeys() {
        List<ECKey> keys = new ArrayList<>();
        keys.add(clientKey);
        keys.add(serverKey);
        Collections.sort(keys, ECKey.PUBKEY_COMPARATOR);
        return keys;
    }

}