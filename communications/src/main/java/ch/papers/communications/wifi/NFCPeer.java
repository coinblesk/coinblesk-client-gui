package ch.papers.communications.wifi;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.provider.Settings;

import ch.papers.communications.AbstractPeer;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 26/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class NFCPeer extends AbstractPeer{
    private final static String MIMETYPE = "application/vnd.com.example.android.beam";

    private final Activity activity;
    private final NfcAdapter nfcAdapter;

    public NFCPeer(Activity activity) {
        super(activity);
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
}
