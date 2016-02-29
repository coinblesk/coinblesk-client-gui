package com.uzh.ckiller.coinblesk_client_gui;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.uzh.ckiller.coinblesk_client_gui.ui.dialogs.SendDialogFragment;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class SendPaymentFragment extends KeyboardFragment {

    public static Fragment newInstance() {
        return new SendPaymentFragment();
    }

    @Override
    protected DialogFragment getDialogFragment() {
        return SendDialogFragment.newInstance(this.getCoin());
    }

    @Override
    public void onSharedPrefsUpdated(String customKey) {
        super.initCustomButton(customKey);
    }
}
