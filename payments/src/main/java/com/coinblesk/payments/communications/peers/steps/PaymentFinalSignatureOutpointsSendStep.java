package com.coinblesk.payments.communications.peers.steps;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 16/04/16.
 * Papers.ch
 * a.decarli@papers.ch
 */


import android.util.Log;

import com.coinblesk.json.TxSig;
import com.coinblesk.json.VerifyTO;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.messages.DERInteger;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.messages.DERSequence;
import com.coinblesk.util.BitcoinUtils;
import com.coinblesk.util.Pair;
import com.coinblesk.util.SerializeUtils;
import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
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
public class PaymentFinalSignatureOutpointsSendStep implements Step {
    private final static String TAG = PaymentFinalSignatureSendStep.class.getSimpleName();

    private final Transaction fullSignedTransaction;
    private final WalletService.WalletServiceBinder walletServiceBinder;
    private final Transaction halfSignedRefundTransaction;
    private final Address recipientAddress;
    private final List<TxSig> clientSignatures;
    private final List<TxSig> serverSignatures;

    public PaymentFinalSignatureOutpointsSendStep(WalletService.WalletServiceBinder walletServiceBinder, Address recipientAddress, List<TxSig> clientSignatures, List<TxSig> serverSignatures, Transaction fullSignedTransaction, Transaction halfSignedRefundTransaction) {
        this.walletServiceBinder = walletServiceBinder;
        this.fullSignedTransaction = fullSignedTransaction;
        this.halfSignedRefundTransaction = halfSignedRefundTransaction;
        this.serverSignatures = serverSignatures;
        this.clientSignatures = clientSignatures;
        this.recipientAddress = recipientAddress;
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


        List<Pair<byte[], Long>> outpointCoinPairs = new ArrayList<Pair<byte[], Long>>();
        for (TransactionInput transactionInput : fullSignedTransaction.getInputs()) {
            outpointCoinPairs.add(new Pair<byte[], Long>(transactionInput.getOutpoint().unsafeBitcoinSerialize(), transactionInput.getValue().value));
        }

        VerifyTO completeSignTO = new VerifyTO()
                .clientPublicKey(walletServiceBinder.getMultisigClientKey().getPubKey())
                .p2shAddressTo(recipientAddress.toString())
                .amountToSpend(fullSignedTransaction.getOutput(0).getValue().value)
                .clientSignatures(clientSignatures)
                .serverSignatures(serverSignatures)
                .outpointsCoinPair(outpointCoinPairs)
                .currentDate(timestamp.longValue());

        if (completeSignTO.messageSig() == null) {
            SerializeUtils.sign(completeSignTO, walletServiceBinder.getMultisigClientKey());
        }

        final List<DERObject> serializedOutpoints = new ArrayList<DERObject>();
        for (Pair<byte[], Long> outpointPair : outpointCoinPairs) {
            serializedOutpoints.add(new DERSequence(ImmutableList.<DERObject>of(new DERObject(outpointPair.element0()), new DERInteger(BigInteger.valueOf(outpointPair.element1())))));
        }

        final List<DERObject> serializedClientSignatures = new ArrayList<DERObject>();
        for (TxSig clientSignature : clientSignatures) {
            serializedClientSignatures.add(new DERSequence(ImmutableList.<DERObject>of(new DERInteger(new BigInteger(clientSignature.sigR())), new DERInteger(new BigInteger(clientSignature.sigS())))));
        }

        final List<DERObject> derObjectList = new ArrayList<DERObject>();
        derObjectList.add(new DERSequence(serializedOutpoints));
        derObjectList.add(new DERSequence(serializedClientSignatures));
        derObjectList.add(new DERInteger(timestamp));
        derObjectList.add(new DERInteger(new BigInteger(completeSignTO.messageSig().sigR())));
        derObjectList.add(new DERInteger(new BigInteger(completeSignTO.messageSig().sigS())));

        final DERSequence payloadDerSequence = new DERSequence(derObjectList);
        Log.d(TAG, "responding with complete TX:" + payloadDerSequence.serializeToDER().length);
        return payloadDerSequence;
    }

    public Transaction getFullSignedRefundTransation() {
        return halfSignedRefundTransaction;
    }
}
