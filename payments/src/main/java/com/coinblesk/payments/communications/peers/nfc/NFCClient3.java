package com.coinblesk.payments.communications.peers.nfc;

import android.content.Context;

import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.peers.AbstractServer;

/**
 * Created by draft on 12.03.16.
 */
public class NFCClient3 extends AbstractServer {
    public NFCClient3(Context contex, WalletService.WalletServiceBinder walletServiceBinder) {
        super(contex, walletServiceBinder);
    }

    @Override
    public void onChangePaymentRequest() {

    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
