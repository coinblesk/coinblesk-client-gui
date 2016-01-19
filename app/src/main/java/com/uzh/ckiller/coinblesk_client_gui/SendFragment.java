package com.uzh.ckiller.coinblesk_client_gui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by ckiller on 16/01/16.
 */
public class SendFragment extends Fragment {
    public static final String ARG_PAGE = "ARG_PAGE";

    private int mPage;

    public static SendFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        SendFragment fragment = new SendFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPage = getArguments().getInt(ARG_PAGE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_send, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        insertNestedFragment();
    }

    // Embeds the child fragment dynamically
    private void insertNestedFragment() {
        Fragment childFragment = new SendAmountFragment();
        Fragment childFragment2 = new NumericKeypadFragment();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.send_amount, childFragment,"FRAGMENT_SEND_AMOUNT").replace(R.id.send_numeric_keypad, childFragment2).commit();
    }



}
