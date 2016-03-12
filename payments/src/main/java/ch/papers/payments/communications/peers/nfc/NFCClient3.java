package ch.papers.payments.communications.peers.nfc;

import ch.papers.payments.communications.peers.AbstractServer;
import ch.papers.payments.communications.peers.ServerPeerService;

/**
 * Created by draft on 12.03.16.
 */
public class NFCClient3 extends AbstractServer {
    public NFCClient3(ServerPeerService serverPeerService) {
        super(serverPeerService);
    }

    @Override
    public void onChangePaymentRequest() {

    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
