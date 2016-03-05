package ch.papers.payments.communications.peers.nfc;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import ch.papers.payments.Constants;
import ch.papers.payments.Utils;
import ch.papers.payments.communications.peers.AbstractServer;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 04/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class NFCServer extends AbstractServer {
    public NFCServer(Context context) {
        super(context);
    }

    @Override
    public void onChangePaymentRequest() {
        if (this.hasPaymentRequestUri()) {
            Intent intent = new Intent(getContext(), NFCService.class);
            intent.putExtra(Constants.BITCOIN_URI_KEY, Utils.bitcoinUriToString(this.getPaymentRequestUri()));
            getContext().startService(intent);
        } else {
            Intent intent = new Intent(getContext(), NFCService.class);
            intent.putExtra(Constants.BITCOIN_URI_KEY, "");
            getContext().startService(intent);
        }
    }

    @Override
    public boolean isSupported() {
        return this.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
    }
}