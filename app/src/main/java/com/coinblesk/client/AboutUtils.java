package com.coinblesk.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.coinblesk.client.coinblesk_client_gui.R;

/**
 * Created by ckiller on 10/03/16.
 */
public class AboutUtils {

    public static void showTos(Activity activity) {
        FragmentManager fm = activity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("dialog_tos");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        new EulaDialog().show(ft, "dialog_tos");
    }

    public static class EulaDialog extends DialogFragment {

        public EulaDialog() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int padding = getResources().getDimensionPixelSize(R.dimen.padding_dialog);

            TextView eulaTextView = new TextView(getActivity());
            eulaTextView.setText(Html.fromHtml(getString(R.string.social_about_tos_content)));
            eulaTextView.setMovementMethod(LinkMovementMethod.getInstance());
            eulaTextView.setPadding(padding, padding, padding, padding);

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.social_about_tos)
                    .setView(eulaTextView)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();
        }
    }
}
