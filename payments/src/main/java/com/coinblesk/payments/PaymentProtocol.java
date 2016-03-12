package com.coinblesk.payments;

import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.util.List;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 21/01/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentProtocol {

    private static PaymentProtocol INSTANCE;

    private PaymentProtocol() {

    }

    public static PaymentProtocol getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PaymentProtocol();
        }
        return INSTANCE;
    }


    public Script generateMultisigScript(ECKey publicServerKey, ECKey publicClientKey) {
        final List<ECKey> keys = ImmutableList.of(publicClientKey, publicServerKey);
        final Script script = ScriptBuilder.createMultiSigOutputScript(2, keys);
        return script;
    }

    public Transaction generateToMultisigTransaction(ECKey publicServerKey, ECKey publicClientKey, Coin amount) {
        final Transaction toMultisigTransaction = new Transaction(Constants.PARAMS);
        final Script script = this.generateMultisigScript(publicServerKey, publicClientKey);
        toMultisigTransaction.addOutput(amount, script);
        return toMultisigTransaction;
    }

    public Transaction generateFromMultisigTransaction(TransactionOutput multisigOutput, ECKey publicServerKey, ECKey publicClientKey, Coin amount, Address address) {
        Coin remainingAmount = multisigOutput.getValue();
        remainingAmount = remainingAmount.subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE);
        remainingAmount = remainingAmount.subtract(amount);
        final List<ECKey> keys = ImmutableList.of(publicClientKey, publicServerKey);
        final Script script = ScriptBuilder.createP2SHOutputScript(2, keys);

        final Transaction externalTransaction = new Transaction(Constants.PARAMS);
        externalTransaction.addInput(multisigOutput);
        externalTransaction.addOutput(remainingAmount, script);
        externalTransaction.addOutput(amount, address);
        return externalTransaction;
    }

    public TransactionSignature signMultisigTransaction(TransactionOutput multisigOutput, Transaction toMultisigTransaction, ECKey serverKey) {
        final Script multisigScript = multisigOutput.getScriptPubKey();
        final Sha256Hash sighash = toMultisigTransaction.hashForSignature(0, multisigScript, Transaction.SigHash.ALL, false);
        return new TransactionSignature(serverKey.sign(sighash), Transaction.SigHash.ALL, false);
    }

    public TransactionSignature signMultisigTransaction(Script multisigScript, Transaction toMultisigTransaction, ECKey serverKey) {
        final Sha256Hash sighash = toMultisigTransaction.hashForSignature(0, multisigScript, Transaction.SigHash.ALL, false);
        return new TransactionSignature(serverKey.sign(sighash), Transaction.SigHash.ALL, false);
    }

    public Transaction generateRefundTransaction(TransactionOutput output, Address toAddress) {
        final Transaction refundTransaction = new Transaction(Constants.PARAMS);
        final long unixTime = System.currentTimeMillis()/ 1000L;
        final long lockTime = unixTime + (Constants.LOCK_TIME_MONTHS * Constants.UNIX_TIME_MONTH);
        refundTransaction.addInput(output);

        Coin remainingAmount = output.getValue();
        remainingAmount = remainingAmount.subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE);
        refundTransaction.addOutput(remainingAmount, toAddress);
        refundTransaction.setLockTime(lockTime);

        return refundTransaction;
    }

}
