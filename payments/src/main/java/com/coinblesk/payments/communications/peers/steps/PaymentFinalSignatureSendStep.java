package com.coinblesk.payments.communications.peers.steps;

import android.util.Log;

import com.coinblesk.json.CompleteSignTO;
import com.coinblesk.util.BitcoinUtils;
import com.coinblesk.util.SerializeUtils;
import com.google.common.collect.ImmutableList;
import com.coinblesk.payments.communications.messages.DERInteger;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.messages.DERSequence;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.coinblesk.payments.WalletService;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentFinalSignatureSendStep implements Step{
    private final static String TAG = PaymentFinalSignatureSendStep.class.getSimpleName();

    private final Transaction transaction;
    private final WalletService.WalletServiceBinder walletServiceBinder;
    private final Transaction fullSignedRefundTransaction;

    public PaymentFinalSignatureSendStep(WalletService.WalletServiceBinder walletServiceBinder, Address recipientAddress, Transaction transaction, Transaction fullSignedRefundTransaction) {
        this.walletServiceBinder = walletServiceBinder;
        this.transaction = transaction;
        this.fullSignedRefundTransaction = fullSignedRefundTransaction;
    }

    @Override
    public DERObject process(DERObject input) {
        final List<TransactionSignature> serverTransactionSignatures = new ArrayList<TransactionSignature>();
        final DERSequence transactionSignaturesSequence = (DERSequence) input;
        for (DERObject transactionSignature : transactionSignaturesSequence.getChildren()) {
            final DERSequence signature = (DERSequence) transactionSignature;
            TransactionSignature serverSignature = new TransactionSignature(((DERInteger) signature.getChildren().get(0)).getBigInteger(), ((DERInteger) signature.getChildren().get(1)).getBigInteger());
            serverTransactionSignatures.add(serverSignature);
            Log.d(TAG,"adding server signature");
        }

        final List<ECKey> keys = new ArrayList<>();
        keys.add(walletServiceBinder.getMultisigClientKey());
        keys.add(walletServiceBinder.getMultisigServerKey());
        Collections.sort(keys,ECKey.PUBKEY_COMPARATOR);

        Script redeemScript = ScriptBuilder.createRedeemScript(2,keys);
        final List<TransactionSignature> clientTransactionSignatures = BitcoinUtils.partiallySign(fullSignedRefundTransaction, redeemScript, walletServiceBinder.getMultisigClientKey());

        for (int i = 0; i < clientTransactionSignatures.size(); i++) {
            Log.d(TAG,"starting verify");
            final TransactionSignature serverSignature = serverTransactionSignatures.get(i);
            final TransactionSignature clientSignature = clientTransactionSignatures.get(i);

            // yes, because order matters...
            List<TransactionSignature> signatures = keys.indexOf(walletServiceBinder.getMultisigClientKey())==0 ? ImmutableList.of(clientSignature,serverSignature) : ImmutableList.of(serverSignature,clientSignature);
            Script p2SHMultiSigInputScript = ScriptBuilder.createP2SHMultiSigInputScript(signatures, redeemScript);
            fullSignedRefundTransaction.getInput(i).setScriptSig(p2SHMultiSigInputScript);
            fullSignedRefundTransaction.getInput(i).verify();
            Log.d(TAG,"verify success");
        }


        final BigInteger timestamp = BigInteger.valueOf(System.currentTimeMillis());
        CompleteSignTO completeSignTO = new CompleteSignTO()
                .clientPublicKey(walletServiceBinder.getMultisigClientKey().getPubKey())
                .fullSignedTransaction(transaction.unsafeBitcoinSerialize())
                .currentDate(timestamp.longValue());

        if (completeSignTO.messageSig() == null) {
            SerializeUtils.sign(completeSignTO, walletServiceBinder.getMultisigClientKey());
        }


        final List<DERObject> derObjectList = new ArrayList<DERObject>();
        derObjectList.add(new DERObject(transaction.unsafeBitcoinSerialize()));
        derObjectList.add(new DERInteger(timestamp));
        derObjectList.add(new DERInteger(new BigInteger(completeSignTO.messageSig().sigR())));
        derObjectList.add(new DERInteger(new BigInteger(completeSignTO.messageSig().sigS())));

        final DERSequence payloadDerSequence = new DERSequence(derObjectList);
        Log.d(TAG, "responding with complete TX:" + payloadDerSequence.serializeToDER().length);
        return payloadDerSequence;
    }

    public Transaction getFullSignedRefundTransation() {
        return fullSignedRefundTransaction;
    }
}
