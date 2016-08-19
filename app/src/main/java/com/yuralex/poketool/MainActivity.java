package com.yuralex.poketool;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.ads.MobileAds;
import com.omkarmoghe.pokemap.controllers.app_preferences.PokemapSharedPreferences;
import com.omkarmoghe.pokemap.controllers.net.NianticManager;
import com.omkarmoghe.pokemap.views.LoginActivity;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.yuralex.poketool.updater.AppUpdate;
import com.yuralex.poketool.updater.AppUpdateDialog;
import com.yuralex.poketool.updater.AppUpdateEvent;
import com.yuralex.poketool.updater.AppUpdateLoader;

public class MainActivity extends AppCompatActivity implements AppUpdateLoader.OnAppUpdateEventListener {
    private static final int STORAGE_PERMISSION_REQUESTED = 10;
    private PokemapSharedPreferences mPref;

    CollectionPagerAdapter mCollectionPagerAdapter;
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(getApplicationContext(), "");
        mPref = new PokemapSharedPreferences(this);

        mCollectionPagerAdapter = new CollectionPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        if (mViewPager != null) {
            mViewPager.setAdapter(mCollectionPagerAdapter);
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        if (tabLayout != null) {
            tabLayout.addTab(tabLayout.newTab().setText("Tab 1"));
            tabLayout.addTab(tabLayout.newTab().setText("Tab 2"));

            tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    mViewPager.setCurrentItem(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
            tabLayout.setupWithViewPager(mViewPager);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
//                startActivity(new Intent(this, LoginActivity.class));
                try {
                    NianticManager nianticManager = NianticManager.getInstance();
                    PokemonGo go = nianticManager.getPokemonGo();
                    go.getInventories().updateInventories(true);
                } catch (LoginFailedException | RemoteServerException e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < mCollectionPagerAdapter.getCount(); i++) {
                    Updatable currentFragment = (Updatable) mCollectionPagerAdapter.getRegisteredFragment(i); //mViewPager.getCurrentItem());
                    currentFragment.update();
                }
                return true;
            case R.id.action_logout:
                logout();
                return true;
            case R.id.action_check_update:
                new AppUpdateLoader(this, this).execute();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        mPref.clearLoginCredentials();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    public class CollectionPagerAdapter extends FragmentStatePagerAdapter {
        SparseArray<Fragment> mRegisteredFragments = new SparseArray<>();
        public CollectionPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return PokemonFragment.newInstance();
                case 1:
                    return PokedexFragment.newInstance();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Pokemons";
                case 1:
                    return "PokeDex";
                default:
                    return "";
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            mRegisteredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mRegisteredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getRegisteredFragment(int position) {
            return mRegisteredFragments.get(position);
        }
    }

    public interface Updatable {
        void update();
    }


    private void showAppUpdateDialog(final Context context, final AppUpdate update) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog dialog = builder.create();
        if(!dialog.isShowing()) {
            builder = new AlertDialog.Builder(context)
                    .setTitle(R.string.update_available_title)
                    .setMessage(context.getString(R.string.app_name) + " " + update.version + " " + context.getString(R.string.update_available_long) + "\n\n" + context.getString(R.string.changes) + "\n\n" + update.changelog)
                    .setIcon(R.mipmap.ic_launcher)
                    .setPositiveButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            AppUpdateDialog.downloadAndInstallAppUpdate(context, update);
                        }
                    })
                    .setNegativeButton(context.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
//                            goToLoginScreen();
                        }
                    })
                    .setCancelable(false);
            dialog = builder.create();
            dialog.show();
        }
    }

    public static boolean doWeHaveReadWritePermission(Context context) {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public void onAppUpdateEvent(AppUpdateEvent event) {
        switch (event.status) {
            case AppUpdateEvent.OK:
                if (doWeHaveReadWritePermission(this)) {
                    showAppUpdateDialog(this, event.appUpdate);
//                    checkingForUpdate = false;
                    System.out.println("Updating");
                } else{
                    getReadWritePermission();
                }
                break;
            case AppUpdateEvent.FAILED:
                Toast.makeText(this, R.string.update_check_failed, Toast.LENGTH_LONG).show();
//                checkingForUpdate = false;
                break;
            case AppUpdateEvent.UPTODATE:
                Toast.makeText(this, R.string.update_check_uptodate, Toast.LENGTH_LONG).show();
//                checkingForUpdate = false;
                break;
        }
    }

    public void getReadWritePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setCancelable(false)
                    .setMessage(R.string.Permission_Required_Auto_Updater)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    android.Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUESTED);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUESTED);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case STORAGE_PERMISSION_REQUESTED:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(R.string.PERMISSION_OK);
                    new AppUpdateLoader(this, this).execute();
                } else {
                    Toast.makeText(this, R.string.update_canceled, Toast.LENGTH_SHORT).show();
//                    checkingForUpdate = false;
//                    goToLoginScreen();
                }
                break;
        }
    }

}
