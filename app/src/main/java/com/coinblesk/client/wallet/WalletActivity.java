package com.coinblesk.client.wallet;


import android.content.*;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.*;

import com.coinblesk.client.R;
import com.coinblesk.client.ui.dialogs.SendDialogFragment;
import com.coinblesk.payments.WalletService;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;

public class WalletActivity extends AppCompatActivity
                            implements SendDialogFragment.SendDialogListener {

    private static final String TAG = WalletActivity.class.getName();

    private WalletService.WalletServiceBinder walletService;

    private WalletSectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);
        initViewPager();
        initToolbar();

        Intent walletServiceIntent = new Intent(this, WalletService.class);
        startService(walletServiceIntent);
        bindService(walletServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    private void initViewPager() {
        sectionsPagerAdapter = new WalletSectionsPagerAdapter(getSupportFragmentManager());
        viewPager = (ViewPager) findViewById(R.id.wallet_viewpager);
        if (viewPager != null) {
            viewPager.setAdapter(sectionsPagerAdapter);
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.wallet_tablayout);
        if (tabLayout != null) {
            tabLayout.setTabMode(TabLayout.MODE_FIXED);
            tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
            tabLayout.setupWithViewPager(viewPager);
        }
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    public void sendCoins(Address address, Coin amount) {
        // this is called by the send dialog if the user collects the refund
        // Note: in this case, the amount is just to inform the user - we spend all!
        //TODO: wait for confirmation/broadcast, inform about payment success?
        try {
            walletService.collectRefund(address);
        } catch (Exception e) {
            Log.e(TAG, "sendCoins - address=" + address + ", amount= " + amount, e);
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletService = (WalletService.WalletServiceBinder) binder;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletService = null;
        }
    };


    /**
     * TABS integration
     */
    private static class WalletSectionsPagerAdapter extends FragmentStatePagerAdapter {

        private final int ITEMS_COUNT = 2;
        private final String[] TAB_TITLES = {
                "Addresses",
                "Outputs"
        };

        private WalletSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int position) {
            switch(position) {
                case 0:
                    return WalletAddressList.newInstance();
                case 1:
                    return Outputs.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return ITEMS_COUNT;
        }

        @Override
        public CharSequence getPageTitle(final int position) {
            return (position >= 0 && position < TAB_TITLES.length)
                    ? TAB_TITLES[position]
                    : super.getPageTitle(position);
        }
    }
}
