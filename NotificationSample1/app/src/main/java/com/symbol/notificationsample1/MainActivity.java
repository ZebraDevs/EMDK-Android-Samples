/*
* Copyright (C) 2016-2017 Zebra Technologies Corp
* All rights reserved.
*/
package com.symbol.notificationsample1;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.notification.DeviceInfo;
import com.symbol.emdk.notification.DeviceType;
import com.symbol.emdk.notification.Notification;
import com.symbol.emdk.notification.Notification.Beep;
import com.symbol.emdk.notification.NotificationDevice;
import com.symbol.emdk.notification.NotificationException;
import com.symbol.emdk.notification.NotificationManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity implements EMDKListener{

    private Spinner spnLEDPattern = null;
    private Spinner spnBeepPattern = null;
    private Spinner spnVibratePattern = null;
    private Spinner spnNotificationDevice = null;
    private Spinner spnLEDColor = null;
    private EMDKManager emdkManager = null;
    private NotificationManager notificationManager = null;
    private NotificationDevice notificationDevice = null;
    private DeviceInfo deviceInfo = null;
    private TextView txtStatus = null;
    private Button btnReConnenct = null;
    private List<DeviceInfo> deviceInfoList = null;
    private int deviceIndex = 0;
    private int defaultIndex= 0;

    static final String PATTERN1 = "4 Short";
    static final String PATTERN2 = "2 Long";
    static final String PATTERN3 = "1 Short";

    static final String PATTERN_NONE = "None";

    public enum LedColor {

        Yellow (0xFFFF00),
        Orange(0xFF8C00),
        Red(0xFF0000),
        Blue(0x0000FF),
        Purple(0x800080),
        Black (0x000000),
        Grey(0xC0C0C0),
        Pink(0xFF00B6);
        private int value = 0x0000FF;
        LedColor(int val) {
            value = val;
        }
        public int getValue() {
            return value;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Setting default orientation of the app
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setDefaultOrientation();

        spnLEDPattern = (Spinner) findViewById(R.id.spnLEDPattern);
        spnBeepPattern = (Spinner) findViewById(R.id.spnBeepPattern);
        spnVibratePattern = (Spinner) findViewById(R.id.spnVibratePattern);
        spnNotificationDevice = (Spinner) findViewById(R.id.spnNotificationDevice);
        spnLEDColor = (Spinner) findViewById(R.id.spnLEDColor);
        txtStatus = (TextView)findViewById(R.id.txtStatus);
        btnReConnenct = (Button)findViewById(R.id.btnReConnect);


        addSpinnerDevicesListener();

        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            txtStatus.setText("Status: EMDKManager object request failed!");
        }

    }

    @Override
    public void onOpened(EMDKManager emdkManager) {

        txtStatus.setText("Status:  EMDK open success!");
        this.emdkManager = emdkManager;

        try
        {
            // Acquire the notification manager resources
            notificationManager = (NotificationManager) emdkManager.getInstance(FEATURE_TYPE.NOTIFICATION);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }

        if(notificationManager != null) {

            // Enumerate notification devices
            enumerateNotificationDevices();

            // Set default device
            spnNotificationDevice.setSelection(defaultIndex);

            // Initialize notification device
            initDevice();
            populateNotificationSpinners();

        }
    }

    private void setDefaultOrientation(){

        WindowManager windowManager =  (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = 0;
        int height = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                width = dm.widthPixels;
                height = dm.heightPixels;
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                width = dm.heightPixels;
                height = dm.widthPixels;
                break;
            default:
                break;
        }

        if(width > height){
            setContentView(R.layout.activity_main_landscape);
        } else {
            setContentView(R.layout.activity_main);
        }

    }

    void populateNotificationSpinners()
    {
        List<String> lstBeepPattern = new ArrayList<String>();
        lstBeepPattern.add(PATTERN1);
        lstBeepPattern.add(PATTERN2);
        lstBeepPattern.add(PATTERN3);
        lstBeepPattern.add(PATTERN_NONE);
        ArrayAdapter<String> beepAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, lstBeepPattern);
        beepAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnBeepPattern.setAdapter(beepAdapter);

        List<String> lstVibratePattern = new ArrayList<String>();
        if(deviceInfo.getDeviceType() == DeviceType.IMAGER)
        {
            lstVibratePattern.add(PATTERN1);
            lstVibratePattern.add(PATTERN2);
            btnReConnenct.setEnabled(true);
        }
        else
        {
            btnReConnenct.setEnabled(false);
        }

        lstVibratePattern.add(PATTERN3);
        lstVibratePattern.add(PATTERN_NONE);
        ArrayAdapter<String> vibrateAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, lstVibratePattern);
        vibrateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnVibratePattern.setAdapter(vibrateAdapter);

        List<String> lstColorPattern = new ArrayList<String>();
        lstColorPattern.add(PATTERN1);
        lstColorPattern.add(PATTERN2);
        lstColorPattern.add(PATTERN3);
        lstColorPattern.add(PATTERN_NONE);
        ArrayAdapter<String> colorPatternAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, lstColorPattern);
        colorPatternAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnLEDPattern.setAdapter(colorPatternAdapter);

        List<String> lstLedColor = new ArrayList<String>();
        for (LedColor capType : LedColor.values()) {
            lstLedColor.add(capType.name());
        }
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, lstLedColor);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnLEDColor.setAdapter(colorAdapter);

        //Check if LED is supported on the selected notification device
        if(deviceInfo.isLEDSupported())
        {
            spnLEDPattern.setEnabled(true);
            spnLEDColor.setEnabled(true);
        }
        else
        {
            spnLEDPattern.setEnabled(false);
            spnLEDColor.setEnabled(false);
        }


        //Check if Beeper is supported on the selected notification device
        if(deviceInfo.isBeepSupported())
        {
            spnBeepPattern.setEnabled(true);
        }
        else
        {
            spnBeepPattern.setEnabled(false);
        }

    }

    private void enumerateNotificationDevices() {

        if (notificationManager != null) {

            deviceInfoList = notificationManager.getSupportedDevicesInfo();

            List<String> friendlyNameList = new ArrayList<String>();
            int spinnerIndex = 0;

            if ((deviceInfoList != null) && (deviceInfoList.size() != 0)) {

                Iterator<DeviceInfo> it = deviceInfoList.iterator();
                while(it.hasNext()) {
                    DeviceInfo devInfo = it.next();
                    friendlyNameList.add(devInfo.getFriendlyName());
                    if(devInfo.isDefaultDevice()) {
                        defaultIndex = spinnerIndex;

                    }
                    ++spinnerIndex;
                }
            }

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, friendlyNameList);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spnNotificationDevice.setAdapter(spinnerAdapter);
        }
    }


    private void addSpinnerDevicesListener() {

        spnNotificationDevice.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View arg1,
                                       int position, long arg3) {

                if(deviceIndex != position) {
                    deviceIndex = position;
                    deInitDevice();
                    initDevice();
                    //Populate available patterns based on the selected device.
                    populateNotificationSpinners();
                }

            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });
    }


    @Override
    public void onClosed() {

        if (emdkManager != null) {

            // Release all the resources
            emdkManager.release();
            emdkManager = null;
        }

        txtStatus.setText("Status: EMDK closed unexpectedly! Please close and restart the application.");
    }

    public void onClickDeviceInfo(View arg0) {

        deviceInfo = notificationDevice.getDeviceInfo();
        String info = "FriendlyName = " + deviceInfo.getFriendlyName()
                + "\r\nModelNumber = " + deviceInfo.getModelNumber()
                + "\r\nDeviceType = " + deviceInfo.getDeviceType().toString()
                + "\r\nConnectionType = " + deviceInfo.getConnectionType().toString()
                + "\r\nisConnected = " + notificationDevice.isConnected()
                + "\r\nisDefaultDevice = " + deviceInfo.isDefaultDevice()
                + "\r\nisLEDSupported = " + deviceInfo.isLEDSupported()
                + "\r\nisBeepSupported = " + deviceInfo.isBeepSupported()
                + "\r\nisVibrateSupported = " + deviceInfo.isVibrateSupported();


        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Notification Device Info");
        alertDialog.setMessage(info);
        alertDialog.show();

    }

    public void onClickNotify(View arg0) {


        if(deviceInfo.getDeviceType() == DeviceType.IMAGER)
        {
            if((spnLEDPattern.getSelectedItem().toString().equals(PATTERN_NONE)) &&
                    (spnBeepPattern.getSelectedItem().toString().equals(PATTERN_NONE)) &&
                    (spnVibratePattern.getSelectedItem().toString().equals(PATTERN_NONE)))
            {
                txtStatus.setText("Status: Please select at least one pattern");
                return;
            }
        }
        else
        {
            if(spnVibratePattern.getSelectedItem().toString().equals(PATTERN_NONE))
            {
                txtStatus.setText("Status: Please select vibrate pattern");
                return;
            }
        }

        Notification notification = new Notification();
        notification = getLEDPattern(notification); //Getting LED related notification info
        notification = getBeepPattern(notification); //Getting Beep related notification info
        notification = getVibratePattern(notification); //Getting Vibrator related notification info

        if(notificationDevice != null) {

            if(notificationDevice.isConnected())
            {
                try {
                    notificationDevice.notify(notification);
                    txtStatus.setText("Status: Notification sent!");
                } catch (NotificationException e) {
                    txtStatus.setText("Status: " + e.getMessage());
                }
            }
            else
            {
                txtStatus.setText("Status: Notification device is not connected!");
            }
        }
        else
        {
            txtStatus.setText("Status: NotificationDevice is null!");
        }
    }

    Notification getBeepPattern(Notification notification )
    {
        String beepPattern = spnBeepPattern.getSelectedItem().toString();
        if(beepPattern.equals(PATTERN1))
        {
            Beep[] lstBeep = new Beep[8];

            lstBeep[0] = new Beep();	lstBeep[0].frequency =  2000;   lstBeep[0].time = 250; // On for 250ms
            lstBeep[1] = new Beep();	lstBeep[1].frequency =  0;      lstBeep[1].time = 250; // Off for 250ms
            lstBeep[2] = new Beep();	lstBeep[2].frequency =  2000;   lstBeep[2].time = 250;
            lstBeep[3] = new Beep();	lstBeep[3].frequency =  0;      lstBeep[3].time = 250;
            lstBeep[4] = new Beep();	lstBeep[4].frequency =  2000;   lstBeep[4].time = 250;
            lstBeep[5] = new Beep();	lstBeep[5].frequency =  0;      lstBeep[5].time = 250;
            lstBeep[6] = new Beep();	lstBeep[6].frequency =  2000;   lstBeep[6].time = 250;
            lstBeep[7] = new Beep();	lstBeep[7].frequency =  0;      lstBeep[7].time = 250;

            notification.beep.pattern = lstBeep;
        }
        else if(beepPattern.equals(PATTERN2))
        {
            Beep[] lstBeep = new Beep[4];

            lstBeep[0] = new Beep();	lstBeep[0].frequency =  3000;   lstBeep[0].time = 2500; // On for 2.5 seconds
            lstBeep[1] = new Beep();	lstBeep[1].frequency =  0;      lstBeep[1].time = 1000; // Off for 1 second
            lstBeep[2] = new Beep();	lstBeep[2].frequency =  3000;   lstBeep[2].time = 2500;
            lstBeep[3] = new Beep();	lstBeep[3].frequency =  0;      lstBeep[3].time = 1000;
            notification.beep.pattern = lstBeep;
        }
        else if(beepPattern.equals(PATTERN3))
        {
            notification.beep.pattern = new Beep[1];
            notification.beep.pattern[0] = new Beep();
            notification.beep.pattern[0].frequency =  3000;
            notification.beep.pattern[0].time = 250;
        }

        return notification;
    }
    Notification getLEDPattern(Notification notification )
    {
        String ledPattern = spnLEDPattern.getSelectedItem().toString();
        String ledColor = spnLEDColor.getSelectedItem().toString();
        LedColor color = LedColor.valueOf(ledColor);
        notification.led.color = color.getValue();

        if(ledPattern.equals(PATTERN1))
        {
            notification.led.onTime=  250;
            notification.led.offTime= 250;
            notification.led.repeatCount= 3;

        }
        else if(ledPattern.equals(PATTERN2))
        {
            notification.led.onTime=  2550;
            notification.led.offTime= 1000;
            notification.led.repeatCount= 1;
        }
        else if(ledPattern.equals(PATTERN3))
        {
            notification.led.onTime=  250;
            notification.led.offTime= 250;
        }

        return notification;
    }
    Notification getVibratePattern(Notification notification )
    {
        String vibratePattern = spnVibratePattern.getSelectedItem().toString();
        long[] lngArrVib;
        if(vibratePattern.equals(PATTERN1))
        {
            lngArrVib = new long[] {250,250,250,250,250,250,250,250};
            notification.vibrate.pattern = lngArrVib;

        }
        else if(vibratePattern.equals(PATTERN2))
        {
            lngArrVib = new long[] {1000,2550,1000,2550};
            notification.vibrate.pattern = lngArrVib;
        }
        else if(vibratePattern.equals(PATTERN3))
        {
            notification.vibrate.time = 250;
        }

        return notification;
    }

    public void onClickCancel(View arg0) {
        if(notificationDevice != null) {

            if(notificationDevice.isConnected())
            {
                try {
                    //Cancelling the existing notification
                    notificationDevice.cancelNotification();
                    txtStatus.setText("Status: Notification cancelled!");
                } catch (NotificationException e) {
                    txtStatus.setText("Status: " + e.getMessage());
                }
            }
            else
            {
                txtStatus.setText("Status: Notification device is not connected!");
            }
        }
        else
        {
            txtStatus.setText("Status: NotificationDevice is null!");
        }

    }

    public void onClickReconnect(View arg0) {

        //Re-connect RS6000. Release first and then initialize
        if(notificationDevice != null) {
            try {
                notificationDevice.release();
                notificationDevice = null;
                initDevice();
            } catch (NotificationException e) {
                txtStatus.setText("Status: " + e.getMessage());
            }
        }
        else
        {
            txtStatus.setText("Status: NotificationDevice is null!");
        }

    }


    private void initDevice() {

        if (notificationDevice == null) {

            if ((deviceInfoList != null) && (deviceInfoList.size() != 0)) {
                try {

                    deviceInfo = deviceInfoList.get(deviceIndex);

                    //Getting a notification device instance
                    notificationDevice = notificationManager.getDevice(deviceInfo);


                } catch (NotificationException e) {

                    txtStatus.setText("Status: " + e.getMessage());

                }
            }
            else {
                txtStatus.setText("Status: Failed to get the specified device! Please close and restart the application!");
                return;
            }
        }

        if (notificationDevice != null) {

            try {
                //Enabling notification device
                notificationDevice.enable();
                txtStatus.setText("Status: " + deviceInfo.getFriendlyName() + " is enabled!");

            } catch (NotificationException e) {

                txtStatus.setText("Status: " + e.getMessage());
            }
        }else{
            txtStatus.setText("Status: Failed to initialize the notification device!");
        }
    }

    private void deInitDevice() {

        try {

            if(notificationDevice != null)
            {
                // Release all the resources related to notification device
                notificationDevice.release();
                notificationDevice = null;
            }
            else
            {
                txtStatus.setText("Status: Already Released!");
            }

        } catch (Exception e) {

            txtStatus.setText("Status: Release failed!");
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The application is in background

        // De-initialize notification device
        deInitDevice();

        // Remove connection listener
        if (notificationManager != null) {
            notificationManager = null;
            deviceInfoList = null;
        }

        // Release the notification manager resources
        if (emdkManager != null) {
            emdkManager.release(FEATURE_TYPE.NOTIFICATION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The application is in foreground

        // Acquire the notification manager resources
        if (emdkManager != null) {
            notificationManager = (NotificationManager) emdkManager.getInstance(FEATURE_TYPE.NOTIFICATION);

            // Enumerate notification devices
            enumerateNotificationDevices();

            // Set selected notification device
            spnNotificationDevice.setSelection(deviceIndex);

            // Initialize notification device
            initDevice();

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // De-initialize notification device
        deInitDevice();

        // Release all the resources
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;

        }

    }

}
