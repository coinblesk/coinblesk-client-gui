package com.uzh.ckiller.coinblesk_client_gui;


import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.uzh.ckiller.coinblesk_client_gui.ui.dialogs.QrDialogFragment;

import ch.papers.payments.WalletService;



/**
 * Created by ckiller
 */

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getName();
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;


//TODO Create Landscape views for all Fragments. E.g. Landscape View for send / receive with smaller representation of the Balance fragment.

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initToolbar();
        initNavigationView();
        initViewPager();
        setupWindowAnimations();

    }

    private void initViewPager() {
        // Get the ViewPager and set its PagerAdapter so that it can display items
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new MainPagerAdapter(getSupportFragmentManager()));

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void initToolbar() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public void setTitle(CharSequence title) {
        getActionBar().setTitle(title);
    }


    private void initNavigationView() {
        navigationView = (NavigationView) findViewById(R.id.navigation_view);

        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                if (menuItem.isChecked()) menuItem.setChecked(false);
                else menuItem.setChecked(true);

                //Closing drawer on item click
                drawerLayout.closeDrawers();

                switch (menuItem.getItemId()) {

                    case R.id.backup:
                        Toast.makeText(getApplicationContext(), "Backup Selected", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.settings:
                        Toast.makeText(getApplicationContext(), "Settings Selected", Toast.LENGTH_SHORT).show();
                        Intent newAct = new Intent(getApplicationContext(), SettingsActivity.class);
                        startActivity(newAct);
                        return (true);
                    case R.id.about_coinblesk:
                        Toast.makeText(getApplicationContext(), "About Coinblesk Selected", Toast.LENGTH_SHORT).show();
                        return true;
                    default:
                        Toast.makeText(getApplicationContext(), "Somethings Wrong", Toast.LENGTH_SHORT).show();
                        return true;

                }
            }
        });

        // Initializing Drawer Layout and ActionBarToggle
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, null, R.string.openDrawer, R.string.closeDrawer) {

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer opens, we don't need anything to happen yet, hence leave it blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer opens, we don't need anything to happen yet, hence leave it blank
                super.onDrawerOpened(drawerView);
            }
        };

        //Setting the actionbarToggle to the drawerLayout
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        //calling sync state is necessary because the menu icon wouldnt show up otheriwse
        actionBarDrawerToggle.syncState();

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         /*
         Handle action bar item clicks here. The action bar will
         automatically handle clicks on the Home/Up button, so long
         as you specify a parent activity in AndroidManifest.xml.
         */
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_settings:
                return true;
            case R.id.action_qr_code:
                showQrDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        MenuItem qrCodeItem = menu.add(0, R.id.action_qr_code, 0, R.string.action_qr_code);
        qrCodeItem.setIcon(R.drawable.ic_action_qr_code);
        MenuItemCompat.setShowAsAction(qrCodeItem, MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }

    public void showQrDialog() {
        final DialogFragment qrDialogFragment = new QrDialogFragment();
        qrDialogFragment.show(this.getSupportFragmentManager(), "qr_dialog_fragment");
    }

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"onStart");

        Intent intent = new Intent(this, WalletService.class);
        this.startService(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop");
        Intent intent = new Intent(this, WalletService.class);
        this.stopService(intent);
    }
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */


    private void setupWindowAnimations() {
        Slide slide = new Slide();
        slide.setDuration(1000);
        getWindow().setExitTransition(slide);
    }
}

