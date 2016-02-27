package com.uzh.ckiller.coinblesk_client_gui;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.uzh.ckiller.coinblesk_client_gui.ui.dialogs.ReceiveDialogFragment;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ReceivePaymentFragment extends KeyboardFragment {

    public static Fragment newInstance() {
        return new ReceivePaymentFragment();
    }

    @Override
    protected DialogFragment getDialogFragmemt() {
        return ReceiveDialogFragment.newInstance(this.getCoin());
    }
}
