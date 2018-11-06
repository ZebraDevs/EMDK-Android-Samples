/*
* Copyright (C) 2015-2017 Zebra Technologies Corp
* All rights reserved.
*/
package com.symbol.emdk.simulscansample1;

import com.symbol.emdk.simulscansample1.R;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends Activity{

    private BroadcastReceiver broadcastReceiver = null;

    Settings localSettings = new Settings();
    DeviceControl dc = new DeviceControl();
    final private int REQUEST_CODE_ASK_PERMISSIONS = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity","SSC onCreate");
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Initialize intent broadcast receiver
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        broadcastReceiver = new ScreenReceiver();
        registerReceiver(broadcastReceiver, filter);

        setContentView(R.layout.activity_main_2);
        if (Build.VERSION.SDK_INT > 22) {
            int hasWriteContactsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
            } else {
                continueToApplication();
            }
        }
        else
        {
            continueToApplication();
        }
    }

    private void continueToApplication()
    {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.device_controls, dc);
        transaction.add(R.id.settings, new SettingsFragment());
        transaction.commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "Storage permission was denied. Templates will not be loaded", Toast.LENGTH_LONG).show();
                }
                continueToApplication();
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        Log.d("MainActivity","SSC onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        Log.d("MainActivity","SSC onResume");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        Log.d("MainActivity","SSC onDestroy");
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    public class ScreenReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("ScreenReceiver","SSC onReceive");

            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                // Screen Off
                Log.d("ScreenReceiver" , "SSC Screen Off");
                new AsyncDeInitScanner().execute();
                //new AsyncUiControlUpdate().execute(true);
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                // Screen On
                Log.d("ScreenReceiver" , "SSC Screen On");
                new AsyncInitScanner().execute();
            }
        }
    }

    public class AsyncDeInitScanner extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... arg0) {


            return null;
        }
    }

    private class AsyncInitScanner extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... arg0) {


            return null;
        }
    }
}