package ch.papers.payments.communications.peers.steps;

import com.coinblesk.json.RefundTO;
import com.coinblesk.util.BitcoinUtils;
import com.coinblesk.util.SerializeUtils;
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
import java.util.List;

import ch.papers.payments.Constants;
import ch.papers.payments.PaymentProtocol;
import ch.papers.payments.Utils;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentRefundSendStep implements Step {
    final List<TransactionOutput> unspentTransactionOutputs;
    final Address refundAddress;
    final BitcoinURI bitcoinURI;
    final ECKey multisigClientKey;
    final Script multisigAddressScript;

    public PaymentRefundSendStep(List<TransactionOutput> unspentTransactionOutputs, Address refundAddress, BitcoinURI bitcoinURI, ECKey multisigClientKey, Script multisigAddressScript) {
        this.unspentTransactionOutputs = unspentTransactionOutputs;
        this.refundAddress = refundAddress;
        this.bitcoinURI = bitcoinURI;
        this.multisigClientKey = multisigClientKey;
        this.multisigAddressScript = multisigAddressScript;
    }

    @Override
    public int expectedInputLength() {
        return 80;
    }

    @Override
    public byte[] process(byte[] input) {
        TransactionSignature transactionSignature = new TransactionSignature(TransactionSignature.decodeFromDER(input), Transaction.SigHash.ALL, false);

        final List<TransactionSignature> serverTransactionSignatures = new ArrayList<TransactionSignature>();
        serverTransactionSignatures.add(transactionSignature);

        final Transaction transaction = BitcoinUtils.createTx(Constants.PARAMS, unspentTransactionOutputs, refundAddress, bitcoinURI.getAddress(), bitcoinURI.getAmount().longValue());
        final List<TransactionSignature> clientTransactionSignatures = BitcoinUtils.partiallySign(transaction, multisigAddressScript, multisigClientKey);

        for (int i = 0; i < clientTransactionSignatures.size(); i++) {
            final TransactionSignature serverSignature = serverTransactionSignatures.get(i);
            final TransactionSignature clientSignature = clientTransactionSignatures.get(i);

            Script p2SHMultiSigInputScript = ScriptBuilder.createP2SHMultiSigInputScript(ImmutableList.of(clientSignature, serverSignature), multisigAddressScript);
            transaction.getInput(i).setScriptSig(p2SHMultiSigInputScript);
            transaction.getInput(i).verify(unspentTransactionOutputs.get(i));
        }

        // generate refund
        final Transaction halfSignedRefundTransaction = PaymentProtocol.getInstance().generateRefundTransaction(transaction.getOutput(1), refundAddress);
        final List<TransactionSignature> refundTransactionSignatures = BitcoinUtils.partiallySign(transaction, multisigAddressScript, multisigClientKey);


        final RefundTO clientRefundTO = new RefundTO();
        clientRefundTO.clientPublicKey(multisigClientKey.getPubKey());
        clientRefundTO.clientSignatures(SerializeUtils.serializeSignatures(refundTransactionSignatures));
        clientRefundTO.refundTransaction(halfSignedRefundTransaction.bitcoinSerialize());
        byte[] refundTransaction = halfSignedRefundTransaction.bitcoinSerialize();
        byte[] clientSignature = refundTransactionSignatures.get(0).encodeToDER();
        byte refundTransactionSize = (byte) refundTransaction.length;
        byte clientSignatureSize = (byte) clientSignature.length;
        return Utils.concatBytes(new byte[]{refundTransactionSize}, refundTransaction, new byte[]{clientSignatureSize}, clientSignature);
    }
}
