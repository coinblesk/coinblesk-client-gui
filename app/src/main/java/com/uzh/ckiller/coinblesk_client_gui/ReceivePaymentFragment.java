package com.uzh.ckiller.coinblesk_client_gui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.uzh.ckiller.coinblesk_client_gui.ui.dialogs.ReceiveDialogFragment;

import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.util.ArrayList;
import java.util.List;

import ch.papers.payments.WalletService;
import ch.papers.payments.communications.peers.Peer;
import ch.papers.payments.communications.peers.ServerPeerService;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ReceivePaymentFragment extends KeyboardFragment {

    public static Fragment newInstance() {
        return new ReceivePaymentFragment();
    }

    private final List<Peer> peers = new ArrayList<Peer>();



    @Override
    public void onSharedPrefsUpdated(String customKey) {
        super.initCustomButton(customKey);
    }

    @Override
    protected DialogFragment getDialogFragment() {
        try {
            return ReceiveDialogFragment.newInstance(new BitcoinURI(BitcoinURI.convertToBitcoinURI(walletServiceBinder.getCurrentReceiveAddress(),this.getCoin(),"","")));
        } catch (BitcoinURIParseException e) {
            return null;
        }
    }

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private WalletService.WalletServiceBinder walletServiceBinder;

    @Override
    public void onStart() {
        super.onStart();

        Intent walletServiceIntent = new Intent(this.getActivity(), WalletService.class);
        this.getActivity().bindService(walletServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        Intent serverServiceIntent = new Intent(this.getActivity(), ServerPeerService.class);
        this.getActivity().startService(serverServiceIntent);
    }

    @Override
    public void onStop() {
        super.onStop();

        for (Peer peer:this.peers) {
            peer.stop();
        }

        this.getActivity().unbindService(serviceConnection);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */
}
