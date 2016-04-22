package com.coinblesk.payments.communications.peers.steps;

import android.content.Intent;
import android.util.Log;

import com.coinblesk.json.SignTO;
import com.coinblesk.payments.Constants;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.messages.DERInteger;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.messages.DERSequence;
import com.coinblesk.util.BitcoinUtils;
import com.coinblesk.util.CoinbleskException;
import com.coinblesk.util.InsuffientFunds;
import com.coinblesk.util.SerializeUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentRequestReceiveStep implements Step {
    private final static String TAG = PaymentRequestReceiveStep.class.getSimpleName();

    private BitcoinURI bitcoinURI;
    private final long timestamp = System.currentTimeMillis();
    private final WalletService.WalletServiceBinder walletServiceBinder;

    public PaymentRequestReceiveStep(WalletService.WalletServiceBinder walletServiceBinder) {
        this.walletServiceBinder = walletServiceBinder;
    }

    public BitcoinURI getBitcoinURI() {
        return bitcoinURI;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public DERObject process(DERObject input) {
        final DERSequence derSequence = (DERSequence) input;
        final Coin amount = Coin.valueOf(((DERInteger) derSequence.getChildren().get(0)).getBigInteger().longValue());
        Log.d(TAG, "received amount:" + amount);

        Address address;
        if (((DERInteger) derSequence.getChildren().get(1)).getBigInteger().longValue() == 1) {
            address = Address.fromP2SHHash(Constants.PARAMS, derSequence.getChildren().get(2).getPayload());
        } else {
            address = new Address(Constants.PARAMS, derSequence.getChildren().get(2).getPayload());
        }
        Log.d(TAG, "received address:" + address);


        try {
            this.bitcoinURI = new BitcoinURI(BitcoinURI.convertToBitcoinURI(address, amount, "", ""));
            Log.d(TAG, "bitcoin uri complete:" + bitcoinURI);
        } catch (BitcoinURIParseException e) {
            e.printStackTrace();
        }

        final BigInteger timestamp = BigInteger.valueOf(System.currentTimeMillis());
        Log.d(TAG, "sign timestamp:" + timestamp.longValue());

        Transaction fullSignedTransaction = null;
        try {
            fullSignedTransaction = BitcoinUtils.createTx(Constants.PARAMS, walletServiceBinder.getUnspentInstantOutputs(), walletServiceBinder.getMultisigReceiveAddress(), this.bitcoinURI.getAddress(), this.bitcoinURI.getAmount().longValue());
        } catch (CoinbleskException e) {
            Log.d(TAG,"CoinbleskException:" + e.getMessage());
            Intent instantPaymentFailedIntent = new Intent(Constants.INSTANT_PAYMENT_FAILED_ACTION);
            instantPaymentFailedIntent.putExtra(Constants.ERROR_MESSAGE_KEY, e.getMessage());
            walletServiceBinder.getLocalBroadcastManager().sendBroadcast(instantPaymentFailedIntent);
        } catch (InsuffientFunds insuffientFunds) {
            Log.d(TAG,"insufficient funds:" + insuffientFunds.getMessage());
            Intent walletInsufficientBalanceIntent = new Intent(Constants.WALLET_INSUFFICIENT_BALANCE_ACTION);
            walletServiceBinder.getLocalBroadcastManager().sendBroadcast(walletInsufficientBalanceIntent);
        }

        if (fullSignedTransaction == null) {
            return null;
        }

        SignTO refundTO = new SignTO()
                .clientPublicKey(walletServiceBinder.getMultisigClientKey().getPubKey())
                .transaction(fullSignedTransaction.unsafeBitcoinSerialize())
                .messageSig(null)
                .currentDate(timestamp.longValue());
        SerializeUtils.signJSON(refundTO, walletServiceBinder.getMultisigClientKey());

        final List<DERObject> derObjectList = new ArrayList<DERObject>();
        derObjectList.add(new DERObject(walletServiceBinder.getMultisigClientKey().getPubKey()));
        derObjectList.add(new DERObject(fullSignedTransaction.unsafeBitcoinSerialize()));
        derObjectList.add(new DERInteger(timestamp));
        derObjectList.add(new DERInteger(new BigInteger(refundTO.messageSig().sigR())));
        derObjectList.add(new DERInteger(new BigInteger(refundTO.messageSig().sigS())));

        final DERSequence payloadDerSequence = new DERSequence(derObjectList);
        Log.d(TAG,"payload size:"+payloadDerSequence.serializeToDER().length);
        Log.d(TAG,"time:"+System.currentTimeMillis());
        return payloadDerSequence;
    }
}
