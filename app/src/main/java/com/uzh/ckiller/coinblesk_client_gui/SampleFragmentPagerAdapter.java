package com.uzh.ckiller.coinblesk_client_gui;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;


/**
 * Created by ckiller
 */
public class SampleFragmentPagerAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 3;
    SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();
    private String tabTitles[] = new String[]{"BALANCE", "SEND", "RECEIVE"};
    private Context context;

    public SampleFragmentPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {

            //Replacing the main content with ContentFragment Which is our Inbox View;
            case 0:
                return BalanceFragment.newInstance(position + 1);

            case 1:
                return SendFragment.newInstance(position + 1);

            case 2:
                return SendFragment.newInstance(position + 1);

            default:
                return BalanceFragment.newInstance(position + 1);

        }
    }


    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        return tabTitles[position];
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public Fragment getRegisteredFragment(int position) {
        return registeredFragments.get(position);
    }

}