package ch.papers.payments.communications.peers;

import android.content.Context;

import org.bitcoinj.uri.BitcoinURI;

import ch.papers.payments.WalletService;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class BluetoothLEPeer extends AbstractPeer {
    public BluetoothLEPeer(Context context, WalletService.WalletServiceBinder walletServiceBinder) {
        super(context, walletServiceBinder);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public void broadcastPaymentRequest(BitcoinURI paymentUri) {

    }
}
