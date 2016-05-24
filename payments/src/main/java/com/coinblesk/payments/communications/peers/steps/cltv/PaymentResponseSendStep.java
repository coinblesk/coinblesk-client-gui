package com.coinblesk.payments.communications.peers.steps.cltv;

import android.content.Intent;
import android.util.Log;
import com.coinblesk.json.SignTO;
import com.coinblesk.client.config.Constants;
import com.coinblesk.payments.WalletService;
import com.coinblesk.der.DERObject;
import com.coinblesk.payments.communications.peers.steps.AbstractStep;
import com.coinblesk.client.utils.DERPayloadBuilder;
import com.coinblesk.util.CoinbleskException;
import com.coinblesk.util.InsufficientFunds;
import com.coinblesk.util.SerializeUtils;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.uri.BitcoinURI;

import java.util.List;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
public class PaymentResponseSendStep extends AbstractStep {
    private final static String TAG = PaymentResponseSendStep.class.getName();

    private final WalletService.WalletServiceBinder walletService;
    private Transaction transaction;
    private List<TransactionSignature> clientTransactionSignatures;

    public PaymentResponseSendStep(BitcoinURI bitcoinURI, WalletService.WalletServiceBinder walletServiceBinder) {
        super(bitcoinURI);
        this.walletService = walletServiceBinder;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public List<TransactionSignature> getClientTransactionSignatures() {
        return clientTransactionSignatures;
    }

    @Override
    public DERObject process(DERObject input) {
        // input is the bitcoinURI (constructor)
        if (!createTxAndSign(getBitcoinURI().getAddress(), getBitcoinURI().getAmount())) {
            // createTxAndSign sets error code
            return DERObject.NULLOBJECT;
        }
        DERObject response = createDERResponse();
        setSuccess();
        return response;
    }

    private boolean createTxAndSign(Address addressTo, Coin amount) {
        try {
            transaction = walletService.createTransaction(addressTo, amount);
            clientTransactionSignatures = walletService.signTransaction(transaction);
            return true;
        } catch (CoinbleskException e) {
            Log.w(TAG, "CoinbleskException: ", e);
            setResultCode(ResultCode.TRANSACTION_ERROR);
            broadcastInstantPaymentFailed(e.getMessage());
        } catch (InsufficientFunds e) {
            Log.w(TAG, "Insufficient funds: ", e);
            setResultCode(ResultCode.INSUFFICIENT_FUNDS);
            broadcastInsufficientBalance();
        }
        setError();
        return false;
    }


    private DERObject createDERResponse() {
        final ECKey clientKey = walletService.getMultisigClientKey();
        final SignTO signTO = createSignTO(transaction, clientTransactionSignatures, clientKey);
        DERPayloadBuilder builder = new DERPayloadBuilder()
                .add(getProtocolVersion());
        appendSignTO(builder, signTO);
        return builder.getAsDERSequence();
    }

    private SignTO createSignTO(Transaction transaction, List<TransactionSignature> txSignatures, ECKey clientKey) {
        SignTO signTO = new SignTO()
                .currentDate(System.currentTimeMillis())
                .publicKey(clientKey.getPubKey())
                .transaction(transaction.unsafeBitcoinSerialize())
                .signatures(SerializeUtils.serializeSignatures(txSignatures));
        SerializeUtils.signJSON(signTO, clientKey);
        return signTO;
    }

    private DERPayloadBuilder appendSignTO(DERPayloadBuilder builder, SignTO signTO) {
        builder.add(signTO.currentDate())
                .add(signTO.publicKey())
                .add(signTO.transaction())
                .add(signTO.signatures())
                .add(signTO.messageSig());
        return builder;
    }

    private void broadcastInsufficientBalance() {
        Intent walletInsufficientBalanceIntent = new Intent(Constants.WALLET_INSUFFICIENT_BALANCE_ACTION);
        walletService.getLocalBroadcastManager().sendBroadcast(walletInsufficientBalanceIntent);
    }

    private void broadcastInstantPaymentFailed(String msg) {
        Intent instantPaymentFailedIntent = new Intent(Constants.INSTANT_PAYMENT_FAILED_ACTION);
        instantPaymentFailedIntent.putExtra(Constants.ERROR_MESSAGE_KEY, msg != null ? msg : "");
        walletService.getLocalBroadcastManager().sendBroadcast(instantPaymentFailedIntent);
    }
}