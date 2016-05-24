package com.coinblesk.payments.communications.peers.steps;

import android.util.Log;

import com.coinblesk.json.VerifyTO;
import com.coinblesk.payments.WalletService;
import com.coinblesk.der.DERInteger;
import com.coinblesk.der.DERObject;
import com.coinblesk.der.DERSequence;
import com.coinblesk.util.BitcoinUtils;
import com.coinblesk.util.SerializeUtils;
import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentFinalSignatureSendStep implements Step {
    private final static String TAG = PaymentFinalSignatureSendStep.class.getSimpleName();

    private final Transaction fullSignedTransaction;
    private final WalletService.WalletServiceBinder walletServiceBinder;
    private final Transaction halfSignedRefundTransaction;

    public PaymentFinalSignatureSendStep(WalletService.WalletServiceBinder walletServiceBinder, Transaction fullSignedTransaction, Transaction halfSignedRefundTransaction) {
        this.walletServiceBinder = walletServiceBinder;
        this.fullSignedTransaction = fullSignedTransaction;
        this.halfSignedRefundTransaction = halfSignedRefundTransaction;
    }

    @Override
    public DERObject process(DERObject input) {
        final List<TransactionSignature> serverTransactionSignatures = new ArrayList<TransactionSignature>();
        final DERSequence transactionSignaturesSequence = (DERSequence) input;
        for (DERObject transactionSignature : transactionSignaturesSequence.getChildren()) {
            final DERSequence signature = (DERSequence) transactionSignature;
            TransactionSignature serverSignature = new TransactionSignature(((DERInteger) signature.getChildren().get(0)).getBigInteger(), ((DERInteger) signature.getChildren().get(1)).getBigInteger());
            serverTransactionSignatures.add(serverSignature);
            Log.d(TAG, "adding server signature");
        }

        final List<ECKey> keys = new ArrayList<>();
        keys.add(walletServiceBinder.getMultisigClientKey());
        keys.add(walletServiceBinder.getMultisigServerKey());
        Collections.sort(keys, ECKey.PUBKEY_COMPARATOR);

        Script redeemScript = ScriptBuilder.createRedeemScript(2, keys);
        final List<TransactionSignature> clientTransactionSignatures = BitcoinUtils.partiallySign(halfSignedRefundTransaction, redeemScript, walletServiceBinder.getMultisigClientKey());

        for (int i = 0; i < clientTransactionSignatures.size(); i++) {
            Log.d(TAG, "starting verify");
            final TransactionSignature serverSignature = serverTransactionSignatures.get(i);
            final TransactionSignature clientSignature = clientTransactionSignatures.get(i);

            // yes, because order matters...

            List<TransactionSignature> signatures = keys.indexOf(walletServiceBinder.getMultisigClientKey()) == 0 ? ImmutableList.of(clientSignature, serverSignature) : ImmutableList.of(serverSignature, clientSignature);
            Script p2SHMultiSigInputScript = ScriptBuilder.createP2SHMultiSigInputScript(signatures, redeemScript);
            halfSignedRefundTransaction.getInput(i).setScriptSig(p2SHMultiSigInputScript);
            halfSignedRefundTransaction.getInput(i).verify();

            Log.d(TAG, "verify success");
        }


        final BigInteger timestamp = BigInteger.valueOf(System.currentTimeMillis());
        VerifyTO completeSignTO = new VerifyTO()
                .publicKey(walletServiceBinder.getMultisigClientKey().getPubKey())
                .transaction(fullSignedTransaction.unsafeBitcoinSerialize())
                .currentDate(timestamp.longValue());

        if (completeSignTO.messageSig() == null) {
            SerializeUtils.signJSON(completeSignTO, walletServiceBinder.getMultisigClientKey());
        }


        final List<DERObject> derObjectList = new ArrayList<DERObject>();
        derObjectList.add(new DERObject(fullSignedTransaction.unsafeBitcoinSerialize()));
        derObjectList.add(new DERInteger(timestamp));
        derObjectList.add(new DERInteger(new BigInteger(completeSignTO.messageSig().sigR())));
        derObjectList.add(new DERInteger(new BigInteger(completeSignTO.messageSig().sigS())));

        final DERSequence payloadDerSequence = new DERSequence(derObjectList);
        Log.d(TAG,"payload size:"+payloadDerSequence.serializeToDER().length);
        Log.d(TAG,"time:"+System.currentTimeMillis());
        return payloadDerSequence;
    }

    public Transaction getFullSignedRefundTransation() {
        return halfSignedRefundTransaction;
    }
}
