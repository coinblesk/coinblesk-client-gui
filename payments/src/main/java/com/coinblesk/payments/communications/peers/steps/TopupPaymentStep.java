package com.coinblesk.payments.communications.peers.steps;

import android.content.Intent;

import com.coinblesk.json.SignTO;
import com.coinblesk.client.config.Constants;
import com.coinblesk.payments.PaymentProtocol;
import com.coinblesk.payments.WalletService;
import com.coinblesk.der.DERInteger;
import com.coinblesk.der.DERObject;
import com.coinblesk.der.DERSequence;
import com.coinblesk.client.models.RefundTransactionWrapper;
import com.coinblesk.util.BitcoinUtils;
import com.coinblesk.util.CoinbleskException;
import com.coinblesk.util.InsufficientFunds;
import com.coinblesk.util.SerializeUtils;

import org.bitcoinj.core.Transaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ch.papers.objectstorage.UuidObjectStorage;
import ch.papers.objectstorage.UuidObjectStorageException;
import org.bitcoinj.wallet.SendRequest;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 16/04/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class TopupPaymentStep implements Step {

    private final WalletService.WalletServiceBinder walletServiceBinder;

    public TopupPaymentStep(WalletService.WalletServiceBinder walletServiceBinder) {
        this.walletServiceBinder = walletServiceBinder;
    }

    @Override
    public DERObject process(DERObject input) {
        DERObject output = DERObject.NULLOBJECT;
        try {
            final Transaction transaction = BitcoinUtils.createSpendAllTx(Constants.PARAMS, walletServiceBinder.getWallet().calculateAllSpendCandidates(false, true), walletServiceBinder.getCurrentReceiveAddress(), walletServiceBinder.getMultisigReceiveAddress());
            final SendRequest sendRequest = SendRequest.forTx(transaction);
            this.walletServiceBinder.getWallet().signTransaction(sendRequest);

            long timestamp = System.currentTimeMillis();
            final Transaction refundTransaction = PaymentProtocol.getInstance().generateRefundTransaction(sendRequest.tx.getOutput(0), walletServiceBinder.getRefundAddress());

            SignTO halfSignedRefundTO = new SignTO()
                    .publicKey(walletServiceBinder.getMultisigClientKey().getPubKey())
                    .transaction(refundTransaction.unsafeBitcoinSerialize())
                    .messageSig(null)
                    .currentDate(timestamp);
            SerializeUtils.signJSON(halfSignedRefundTO, walletServiceBinder.getMultisigClientKey());

            final List<DERObject> derObjectList = new ArrayList<DERObject>();
            derObjectList.add(new DERObject(refundTransaction.unsafeBitcoinSerialize()));
            derObjectList.add(new DERInteger(BigInteger.valueOf(timestamp)));
            derObjectList.add(new DERInteger(new BigInteger(halfSignedRefundTO.messageSig().sigR())));
            derObjectList.add(new DERInteger(new BigInteger(halfSignedRefundTO.messageSig().sigS())));

            PaymentRefundReceiveStep paymentRefundReceiveStep = new PaymentRefundReceiveStep(walletServiceBinder.getMultisigClientKey());
            PaymentFinalSignatureSendStep paymentFinalSignatureSendStep = new PaymentFinalSignatureSendStep(walletServiceBinder, sendRequest.tx, refundTransaction);
            PaymentFinalSignatureReceiveStep paymentFinalSignatureReceiveStep = new PaymentFinalSignatureReceiveStep(walletServiceBinder.getMultisigClientKey());

            output = paymentRefundReceiveStep.process(new DERSequence(derObjectList));
            output = paymentFinalSignatureSendStep.process(output);
            output = paymentFinalSignatureReceiveStep.process(output);

            this.walletServiceBinder.commitAndBroadcastTransaction(paymentFinalSignatureReceiveStep.getFullSignedTransaction());
            UuidObjectStorage.getInstance().addEntry(new RefundTransactionWrapper(paymentFinalSignatureSendStep.getFullSignedRefundTransation()), RefundTransactionWrapper.class);
            UuidObjectStorage.getInstance().commit();

        } catch (CoinbleskException e) {
            e.printStackTrace();
        } catch (InsufficientFunds insuffientFunds) {
            Intent walletInsufficientBalanceIntent = new Intent(Constants.WALLET_INSUFFICIENT_BALANCE_ACTION);
            walletServiceBinder.getLocalBroadcastManager().sendBroadcast(walletInsufficientBalanceIntent);
        } catch (UuidObjectStorageException e) {
            e.printStackTrace();
        }
        return output;
    }
}
