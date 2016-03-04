package ch.papers.payments.communications.peers;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.provider.Settings;

import org.bitcoinj.uri.BitcoinURI;

import ch.papers.payments.WalletService;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 26/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class NFCPeer extends AbstractPeer{
    private final static String MIMETYPE = "application/vnd.com.example.android.beam";

    private final Activity activity;
    private final NfcAdapter nfcAdapter;

    public NFCPeer(Activity activity, WalletService.WalletServiceBinder walletServiceBinder) {
        super(activity, walletServiceBinder);
        this.activity = activity;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this.activity);
    }

    @Override
    public void start() {
        if (!nfcAdapter.isEnabled())
        {
            this.activity.startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
            return;
        }


    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isSupported() {
        return this.nfcAdapter != null;
    }

    @Override
    public void broadcastPaymentRequest(BitcoinURI paymentUri) {

    }
}
