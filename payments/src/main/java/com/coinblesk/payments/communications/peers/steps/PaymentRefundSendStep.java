package com.coinblesk.payments.communications.peers.steps;

import android.util.Log;

import com.coinblesk.json.SignTO;
import com.coinblesk.json.TxSig;
import com.coinblesk.payments.Constants;
import com.coinblesk.payments.PaymentProtocol;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.messages.DERInteger;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.messages.DERSequence;
import com.coinblesk.util.BitcoinUtils;
import com.coinblesk.util.CoinbleskException;
import com.coinblesk.util.InsuffientFunds;
import com.coinblesk.util.Pair;
import com.coinblesk.util.SerializeUtils;
import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.uri.BitcoinURI;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentRefundSendStep implements Step {
    private final static String TAG = PaymentRefundSendStep.class.getSimpleName();

    private final BitcoinURI bitcoinURI;
    private final WalletService.WalletServiceBinder walletServiceBinder;
    private final long timestamp;

    private final List<Pair<byte[],Long>> outpointPairs = new ArrayList<Pair<byte[], Long>>();
    private final List<TxSig> clientSignatures = new ArrayList<TxSig>();

    public List<TxSig> getServerSignatures() {
        return serverSignatures;
    }

    public List<Pair<byte[], Long>> getOutpointPairs() {
        return outpointPairs;
    }

    public List<TxSig> getClientSignatures() {
        return clientSignatures;
    }

    private final List<TxSig> serverSignatures = new ArrayList<TxSig>();
    private Transaction fullSignedTransaction;
    private Transaction halfSignedRefundTransaction;

    public PaymentRefundSendStep(WalletService.WalletServiceBinder walletServiceBinder, BitcoinURI bitcoinURI, long timestamp) {
        this.walletServiceBinder = walletServiceBinder;
        this.bitcoinURI = bitcoinURI;
        this.timestamp = timestamp;
    }

    @Override
    public DERObject process(DERObject input) {
        Log.d(TAG,"staring refund send");
        final List<TransactionSignature> serverTransactionSignatures = new ArrayList<TransactionSignature>();
        final DERSequence transactionSignaturesSequence = (DERSequence) input;
        for (DERObject transactionSignature : transactionSignaturesSequence.getChildren()) {
            final DERSequence signature = (DERSequence) transactionSignature;
            TransactionSignature serverSignature = new TransactionSignature(((DERInteger) signature.getChildren().get(0)).getBigInteger(), ((DERInteger) signature.getChildren().get(1)).getBigInteger());
            serverTransactionSignatures.add(serverSignature);
            Log.d(TAG,"adding server signature");
        }

        try {
            this.fullSignedTransaction = BitcoinUtils.createTx(Constants.PARAMS, walletServiceBinder.getUnspentInstantOutputs(), walletServiceBinder.getMultisigReceiveAddress(), this.bitcoinURI.getAddress(), this.bitcoinURI.getAmount().longValue());
        } catch (CoinbleskException e) {
            e.printStackTrace();
        } catch (InsuffientFunds insuffientFunds) {
            insuffientFunds.printStackTrace();
        }

        Log.d(TAG,"tx: "+fullSignedTransaction);
        final List<ECKey> keys = new ArrayList<>();
        keys.add(walletServiceBinder.getMultisigClientKey());
        keys.add(walletServiceBinder.getMultisigServerKey());
        Collections.sort(keys,ECKey.PUBKEY_COMPARATOR);

        Script redeemScript = ScriptBuilder.createRedeemScript(2,keys);
        final List<TransactionSignature> clientTransactionSignatures = BitcoinUtils.partiallySign(fullSignedTransaction, redeemScript, walletServiceBinder.getMultisigClientKey());

        for (int i = 0; i < clientTransactionSignatures.size(); i++) {
            Log.d(TAG,"starting verify");
            final TransactionSignature serverSignature = serverTransactionSignatures.get(i);
            final TransactionSignature clientSignature = clientTransactionSignatures.get(i);
            TxSig clientSignatureTxSig = new TxSig();
            clientSignatureTxSig.sigR(clientSignature.r.toString());
            clientSignatureTxSig.sigS(clientSignature.s.toString());
            TxSig serverSignatureTxSig = new TxSig();
            serverSignatureTxSig.sigR(serverSignature.r.toString());
            serverSignatureTxSig.sigS(serverSignature.s.toString());
            this.clientSignatures.add(clientSignatureTxSig);
            this.serverSignatures.add(serverSignatureTxSig);
            this.outpointPairs.add(new Pair<byte[], Long>(fullSignedTransaction.getInput(i).getOutpoint().unsafeBitcoinSerialize(),fullSignedTransaction.getInput(i).getValue().value));

            // yes, because order matters...
            List<TransactionSignature> signatures = keys.indexOf(walletServiceBinder.getMultisigClientKey())==0 ? ImmutableList.of(clientSignature,serverSignature) : ImmutableList.of(serverSignature,clientSignature);
            Script p2SHMultiSigInputScript = ScriptBuilder.createP2SHMultiSigInputScript(signatures, redeemScript);
            fullSignedTransaction.getInput(i).setScriptSig(p2SHMultiSigInputScript);
            fullSignedTransaction.getInput(i).verify();
            Log.d(TAG,"verify success");
        }


        // generate refund
        halfSignedRefundTransaction = PaymentProtocol.getInstance().generateRefundTransaction(fullSignedTransaction.getOutput(1), walletServiceBinder.getRefundAddress());

        SignTO halfSignedRefundTO = new SignTO()
                .clientPublicKey(walletServiceBinder.getMultisigClientKey().getPubKey())
                .transaction(halfSignedRefundTransaction.unsafeBitcoinSerialize())
                .messageSig(null)
                .currentDate(timestamp);
        SerializeUtils.signJSON(halfSignedRefundTO, walletServiceBinder.getMultisigClientKey());

        final List<DERObject> derObjectList = new ArrayList<DERObject>();
        derObjectList.add(new DERObject(halfSignedRefundTransaction.unsafeBitcoinSerialize()));
        derObjectList.add(new DERInteger(BigInteger.valueOf(timestamp)));
        derObjectList.add(new DERInteger(new BigInteger(halfSignedRefundTO.messageSig().sigR())));
        derObjectList.add(new DERInteger(new BigInteger(halfSignedRefundTO.messageSig().sigS())));

        Log.d(TAG,"all good with refund, sending back");

        DERObject payloadDerSequence =  new DERSequence(derObjectList);
        Log.d(TAG,"payload size:"+payloadDerSequence.serializeToDER().length);
        Log.d(TAG,"time:"+System.currentTimeMillis());
        return payloadDerSequence;
    }

    public Transaction getFullSignedTransaction() {
        return fullSignedTransaction;
    }

    public Transaction getHalfSignedRefundTransaction() {
        return halfSignedRefundTransaction;
    }
}
