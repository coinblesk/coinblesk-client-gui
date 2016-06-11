package com.coinblesk.client.utils.upgrade;

import android.os.AsyncTask;
import android.util.Log;

import com.coinblesk.client.config.Constants;
import com.coinblesk.json.SignTO;
import com.coinblesk.json.VerifyTO;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.util.BitcoinUtils;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Response;

/**
 * @author  Andreas Albrecht
 */
public class Multisig2of2ToCltvForwardTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = Multisig2of2ToCltvForwardTask.class.getName();

    private WalletService.WalletServiceBinder walletService;
    private ECKey clientKey, serverKey;

    public Multisig2of2ToCltvForwardTask(WalletService.WalletServiceBinder walletServiceBinder,
                                         ECKey multisigClientKey, ECKey multisigServerKey) {
        this.walletService = walletServiceBinder;
        this.clientKey = multisigClientKey;
        this.serverKey = multisigServerKey;
    }

    @Override
    protected Void doInBackground(Void... params) {
        forwardFromMultisig2of2();
        return null;
    }

    private void forwardFromMultisig2of2() {
        try {
            List<TransactionOutput> outputs = getMultisig2of2Outputs();
            if (outputs.isEmpty()) {
                Log.d(TAG, "No outputs found for 2-of-2 multisig address: "
                        + getMultisig2of2Address().toBase58());
                return;
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
        } catch (Exception e) {
            Log.e(TAG, "Could not forward 2of2 multisig funds to cltv: " + e.getMessage(), e);
        }
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