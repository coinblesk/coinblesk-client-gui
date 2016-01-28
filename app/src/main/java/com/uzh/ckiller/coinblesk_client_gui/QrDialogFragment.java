package com.uzh.ckiller.coinblesk_client_gui;

//import android.support.v7.app.AlertDialog;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * Created by ckiller
 */

public class QrDialogFragment extends DialogFragment implements
        DialogInterface.OnClickListener {
    private View pView = null;
    public String pAddress = "1C22znxnqM9gsrj51DcL98LBWY94YkKDxp";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // TODO Add "generate QR Code" to the View (replace the sample qr code image with the real QR code)
        pView = getActivity().getLayoutInflater().inflate(R.layout.fragment_qr_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        return (builder.setView(pView).setPositiveButton(R.string.qr_code_close, this).create());

    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // TODO Implement Copy to Clipboard of the QR Code
        Toast.makeText(getActivity(), R.string.qr_code_copied, Toast.LENGTH_LONG).show();
    }


    @Override
    public void onDismiss(DialogInterface unused) {
        super.onDismiss(unused);
        Log.d(getClass().getSimpleName(), "Goodbye!");
    }

    @Override
    public void onCancel(DialogInterface unused) {
        super.onCancel(unused);
        Toast.makeText(getActivity(), R.string.app_name, Toast.LENGTH_LONG).show();
    }
}
