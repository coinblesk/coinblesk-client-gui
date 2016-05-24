package com.coinblesk.payments.communications.steps.cltv;

import com.coinblesk.payments.WalletService;
import com.coinblesk.der.DERObject;
import com.coinblesk.payments.communications.steps.AbstractStep;
import com.coinblesk.client.models.TimeLockedAddressWrapper;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.uri.BitcoinURI;

import java.util.List;

/**
 * @author Andreas Albrecht
 */
public class PaymentFinalizeStep extends AbstractStep {

    private final Transaction transaction;
    private final List<TransactionSignature> clientTxSignatures;
    private final List<TransactionSignature> serverTxSignatures;
    private WalletService.WalletServiceBinder walletService;

    public PaymentFinalizeStep(BitcoinURI bitcoinURI, Transaction transaction,
                                                        List<TransactionSignature> clientTxSignatures,
                                                        List<TransactionSignature> serverTxSignatures,
                                                        WalletService.WalletServiceBinder walletService) {
        super(bitcoinURI);
        this.transaction = transaction;
        this.clientTxSignatures = clientTxSignatures;
        this.serverTxSignatures = serverTxSignatures;
        this.walletService = walletService;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    public DERObject process(DERObject input) {

        try {
            if (!assembleTransaction()) {
                return DERObject.NULLOBJECT;
            }
            setSuccess();
        } catch (Exception e) {
            setError();
        }
        return DERObject.NULLOBJECT;
    }

    private boolean assembleTransaction() {
        int numClientSigs = clientTxSignatures.size();
        int numServerSigs = serverTxSignatures.size();
        if (numClientSigs != numServerSigs) {
            setResultCode(ResultCode.TRANSACTION_ERROR);
            return false;
        }
        for (int i = 0; i < numClientSigs; ++i) {
            TransactionInput txIn = transaction.getInput(i);
            byte[] hash = txIn.getConnectedOutput().getScriptPubKey().getPubKeyHash();
            TimeLockedAddressWrapper redeemData = walletService.findTimeLockedAddressByHash(hash);
            Script scriptSig = redeemData
                    .getTimeLockedAddress()
                    .createScriptSigBeforeLockTime(clientTxSignatures.get(i), serverTxSignatures.get(i));
            txIn.setScriptSig(scriptSig);
            txIn.verify();
        }
        transaction.verify();
        return true;
    }
}
