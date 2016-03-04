package ch.papers.payments.communications.peers.steps;

import android.util.Log;

import com.coinblesk.util.BitcoinUtils;
import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.uri.BitcoinURI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.papers.payments.Constants;
import ch.papers.payments.PaymentProtocol;
import ch.papers.payments.WalletService;
import ch.papers.payments.communications.messages.DERInteger;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.messages.DERSequence;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentRefundSendStep implements Step {
    private final static String TAG = PaymentRefundSendStep.class.getSimpleName();

    final List<TransactionOutput> unspentTransactionOutputs;
    final Address refundAddress;
    final BitcoinURI bitcoinURI;
    final ECKey multisigClientKey;
    final ECKey multisigServerKey;
    final Script multisigAddressScript;

    public PaymentRefundSendStep(WalletService.WalletServiceBinder walletServiceBinder, BitcoinURI bitcoinURI) {
        this.unspentTransactionOutputs = walletServiceBinder.getUnspentInstantOutputs();
        this.refundAddress = walletServiceBinder.getRefundAddress();
        this.bitcoinURI = bitcoinURI;
        this.multisigClientKey = walletServiceBinder.getMultisigClientKey();
        this.multisigServerKey = walletServiceBinder.getMultisigServerKey();
        this.multisigAddressScript = walletServiceBinder.getMultisigAddressScript();
    }

    @Override
    public DERObject process(DERObject input) {
        Log.d(TAG,"staring refund send");
        final DERSequence transactionSignaturesSequence = (DERSequence) input;

        final List<TransactionSignature> serverTransactionSignatures = new ArrayList<TransactionSignature>();

        for (DERObject transactionSignature : transactionSignaturesSequence.getChildren()) {
            final DERSequence signature = (DERSequence) transactionSignature;
            TransactionSignature serverSignature = new TransactionSignature(((DERInteger) signature.getChildren().get(0)).getBigInteger(), ((DERInteger) signature.getChildren().get(1)).getBigInteger());
            serverTransactionSignatures.add(serverSignature);
            Log.d(TAG,"adding server signature");
        }

        final Transaction transaction = BitcoinUtils.createTx(Constants.PARAMS, unspentTransactionOutputs, multisigAddressScript.getToAddress(Constants.PARAMS), bitcoinURI.getAddress(), bitcoinURI.getAmount().longValue());

        final List<ECKey> keys = new ArrayList<>();
        keys.add(this.multisigClientKey);
        keys.add(this.multisigServerKey);

        Collections.sort(keys,ECKey.PUBKEY_COMPARATOR);

        Script redeemScript = ScriptBuilder.createRedeemScript(2,keys);
        final List<TransactionSignature> clientTransactionSignatures = BitcoinUtils.partiallySign(transaction, redeemScript, multisigClientKey);

        for (int i = 0; i < clientTransactionSignatures.size(); i++) {
            final TransactionSignature serverSignature = serverTransactionSignatures.get(i);
            final TransactionSignature clientSignature = clientTransactionSignatures.get(i);

            // yes, because order matters...
            List<TransactionSignature> signatures = keys.indexOf(multisigClientKey)==0 ? ImmutableList.of(clientSignature,serverSignature) : ImmutableList.of(serverSignature,clientSignature);
            Script p2SHMultiSigInputScript = ScriptBuilder.createP2SHMultiSigInputScript(signatures, redeemScript);
            transaction.getInput(i).setScriptSig(p2SHMultiSigInputScript);
            transaction.getInput(i).verify();
            Log.d(TAG,"verify success");
        }

        // generate refund
        final Transaction halfSignedRefundTransaction = PaymentProtocol.getInstance().generateRefundTransaction(transaction.getOutput(1), refundAddress);
        final List<TransactionSignature> refundTransactionSignatures = BitcoinUtils.partiallySign(transaction, redeemScript, multisigClientKey);

        List<DERObject> derObjectList = new ArrayList<>();

        for (TransactionSignature clientSignature:refundTransactionSignatures) {
            derObjectList.add(new DERSequence(ImmutableList.<DERObject>of(new DERInteger(clientSignature.r),new DERInteger(clientSignature.s))));
        }
        derObjectList.add(new DERObject(halfSignedRefundTransaction.bitcoinSerialize()));

        Log.d(TAG,"all good with refund, sending back");
        return new DERSequence(derObjectList);
    }
}
