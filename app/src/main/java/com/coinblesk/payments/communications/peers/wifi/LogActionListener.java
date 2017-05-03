package com.coinblesk.payments.communications.peers.wifi;

import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
class LogActionListener implements WifiP2pManager.ActionListener {
    private final String tag;
    public LogActionListener(String tag){
        this.tag = tag;
    }

    @Override
    public void onSuccess() {
        Log.d(tag, "onSuccess");
    }

    @Override
    public void onFailure(int reason) {
        String errorMessage = "";
        switch (reason){
            case WifiP2pManager.BUSY:
                errorMessage="busy";
                break;
            case WifiP2pManager.ERROR:
                errorMessage="error";
                break;
            case WifiP2pManager.P2P_UNSUPPORTED:
                errorMessage="p2p unsupported";
                break;
        }

        Log.d(tag,"onError: " + errorMessage);
    }
}
