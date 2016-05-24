package com.coinblesk.payments.communications.peers.nfc;

import android.app.Activity;
import android.content.Intent;

import com.coinblesk.client.config.Constants;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.peers.AbstractClient;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 25/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class NFCClient extends AbstractClient {
    public NFCClient(Activity activity, WalletService.WalletServiceBinder walletServiceBinder) {
        super(activity, walletServiceBinder);
    }

    @Override
    protected void onStart() {
        Intent intent = new Intent(getContext(), NFCClientServiceCLTV.class);
        intent.putExtra(Constants.CLIENT_STARTED_KEY, true);
        getContext().startService(intent);
    }

    @Override
    protected void onStop() {
        Intent intent = new Intent(getContext(), NFCClientServiceCLTV.class);
        intent.putExtra(Constants.CLIENT_STARTED_KEY, false);
        getContext().startService(intent);
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
