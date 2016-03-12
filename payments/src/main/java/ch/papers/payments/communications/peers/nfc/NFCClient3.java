package ch.papers.payments.communications.peers.nfc;

import android.content.Context;

import ch.papers.payments.WalletService;
import ch.papers.payments.communications.peers.AbstractServer;

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
