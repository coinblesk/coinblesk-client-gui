package com.coinblesk.client;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;


/**
 * Created by ckiller
 */

public class MainPagerAdapter extends FragmentPagerAdapter {
    final static int PAGE_COUNT = 3;
    final static private String tabTitles[] = new String[]{"BALANCE", "SEND", "RECEIVE"};

    public MainPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return BalanceFragment.newInstance();
            case 1:
                return SendPaymentFragment.newInstance();
            case 2:
                return ReceivePaymentFragment.newInstance();
            default:
                return BalanceFragment.newInstance();
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }

}