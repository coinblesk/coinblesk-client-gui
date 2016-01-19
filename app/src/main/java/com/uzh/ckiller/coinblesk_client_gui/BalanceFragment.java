package com.uzh.ckiller.coinblesk_client_gui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by ckiller on 10/01/16.
 */

public class BalanceFragment extends Fragment {
    public static final String ARG_PAGE = "ARG_PAGE";

    private int mPage;

    public static BalanceFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        BalanceFragment fragment = new BalanceFragment();
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
        View view = inflater.inflate(R.layout.fragment_balance, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        insertNestedFragment();
    }

    // Embeds the child fragment dynamically
    private void insertNestedFragment() {
        Fragment balanceCurrent_childFragment = new BalanceCurrentFragment();
        Fragment history_childFragment = new HistoryFragment();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.current_balance_fragment, balanceCurrent_childFragment).replace(R.id.history_fragment, history_childFragment).commit();
    }
}