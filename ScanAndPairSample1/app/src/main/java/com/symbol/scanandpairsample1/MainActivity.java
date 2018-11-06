/*
* Copyright (C) 2015-2017 Zebra Technologies Corp
* All rights reserved.
*/
package com.symbol.scanandpairsample1;

import java.util.ArrayList;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.scanandpair.ScanAndPairConfig.ScanDataType;
import com.symbol.emdk.scanandpair.ScanAndPairConfig.TriggerType;
import com.symbol.emdk.scanandpair.StatusData;
import com.symbol.emdk.scanandpair.ScanAndPairConfig.NotificationType;
import com.symbol.emdk.scanandpair.ScanAndPairResults;
import com.symbol.emdk.scanandpair.ScanAndPairManager;
import com.symbol.scanandpairsample1.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class MainActivity extends Activity implements EMDKListener,
        com.symbol.emdk.scanandpair.ScanAndPairManager.StatusListener{

    private EditText btName = null;
    private EditText btAddress = null;
    private CheckBox checkboxHardTrigger = null;
    private CheckBox checkBoxAlwaysScan = null;
    private Button scanAndPairButton = null;
    private Button scanAndUnpairButton = null;
    private Spinner scandataType = null;
    private TextView statusView = null;
    private EMDKManager emdkManager = null;
    ScanAndPairManager scanAndPairMgr = null;

    com.symbol.emdk.scanandpair.ScanAndPairManager.StatusListener statusCallbackObj = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btName = (EditText) findViewById(R.id.name);
        btAddress = (EditText) findViewById(R.id.address);
        checkBoxAlwaysScan = (CheckBox) findViewById(R.id.alwaysscan);
        checkboxHardTrigger = (CheckBox) findViewById(R.id.triggerType);
        scanAndPairButton = (Button) findViewById(R.id.scanandpair);
        scanAndUnpairButton = (Button) findViewById(R.id.scanandunpair);
        statusView = (TextView) findViewById(R.id.logs);
        scandataType = (Spinner)findViewById(R.id.scanDataType);
        statusView.setText("\n");

        btName.setEnabled(false);
        btAddress.setEnabled(false);

        // The EMDKManager object creation and object will be returned in the callback.
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);

        // Check the return status of getEMDKManager ()
        if (results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
            statusView.setText("Please wait, initialization in progress...");
        } else {
            statusView.setText("Initialization failed!");
        }

        ArrayList<ScanDataType> scanDataTypes = new ArrayList<ScanDataType>();
        scanDataTypes.add(ScanDataType.MAC_ADDRESS);
        scanDataTypes.add(ScanDataType.DEVICE_NAME);
        scanDataTypes.add(ScanDataType.UNSPECIFIED);

        ArrayAdapter<ScanDataType> arrayAdapter = new ArrayAdapter<ScanDataType>(getApplicationContext(), R.layout.simple_spinner_item, scanDataTypes);
        scandataType.setAdapter(arrayAdapter);

        registerForButtonEvents ();

        addCheckBoxListener();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (emdkManager != null) {
            // Clean up the objects created by EMDK manager
            emdkManager.release();
            emdkManager=null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
    public void onOpened(EMDKManager emdkManager) {
        this.emdkManager = emdkManager;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusView.setText("Application Initialized.");
            }
        });
    }

    @Override
    public void onClosed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusView.setText("Error!! Restart the application!!");
            }
        });
    }

    private void addCheckBoxListener() {

        checkBoxAlwaysScan.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    btName.setEnabled(false);
                    btAddress.setEnabled(false);
                }
                else {
                    btName.setEnabled(true);
                    btAddress.setEnabled(true);
                }
            }
        });
    }

    private void registerForButtonEvents() {
        addScanAndPairButtonEvents();
        addScanAndUnpairButtonEvents();
    }

    private void addScanAndPairButtonEvents() {
        scanAndPairButton = (Button) findViewById(R.id.scanandpair);
        scanAndPairButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    statusView.setText("ScanAndPair started..."+ "\n");

                    if(scanAndPairMgr == null) {
                        scanAndPairMgr = (ScanAndPairManager) emdkManager.getInstance(FEATURE_TYPE.SCANANDPAIR);

                        if(scanAndPairMgr != null) {
                            scanAndPairMgr.addStatusListener(statusCallbackObj);
                        }
                    }

                    if(scanAndPairMgr != null) {
                        scanAndPairMgr.config.alwaysScan = checkBoxAlwaysScan.isChecked();
                        scanAndPairMgr.config.notificationType = NotificationType.BEEPER;
                        if(!checkBoxAlwaysScan.isChecked()) {
                            scanAndPairMgr.config.bluetoothInfo.macAddress = btAddress.getText().toString().trim();
                            scanAndPairMgr.config.bluetoothInfo.deviceName = btName.getText().toString().trim();
                        }
                        else {

                            scanAndPairMgr.config.scanInfo.scanTimeout = 5000;

                            if(checkboxHardTrigger.isChecked()) {
                                scanAndPairMgr.config.scanInfo.triggerType = TriggerType.HARD;
                            } else {
                                scanAndPairMgr.config.scanInfo.triggerType = TriggerType.SOFT;
                            }

                            //scanAndPairMgr.config.scanInfo.deviceIdentifier = DeviceIdentifier.INTERNAL_CAMERA1;

                            scanAndPairMgr.config.scanInfo.scanDataType = (ScanDataType)scandataType.getSelectedItem();
                        }

                        ScanAndPairResults resultCode = scanAndPairMgr.scanAndPair("0000");

                        if(!resultCode.equals(ScanAndPairResults.SUCCESS))
                            statusView.append(resultCode.toString()+ "\n\n");

                    } else {
                        statusView.append("ScanAndPairmanager intialization failed!");
                    }
                } catch (Exception e) {
                    statusView.setText("ScanAndUnpair Error:"+ e.getMessage() + "\n");
                }
            }
        });
    }

    private void addScanAndUnpairButtonEvents() {
        scanAndUnpairButton = (Button) findViewById(R.id.scanandunpair);

        scanAndUnpairButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    statusView.setText("ScanAndUnpair started..."+ "\n");
                    if(scanAndPairMgr == null) {
                        scanAndPairMgr = (ScanAndPairManager) emdkManager.getInstance(FEATURE_TYPE.SCANANDPAIR);

                        if(scanAndPairMgr != null) {
                            scanAndPairMgr.addStatusListener(statusCallbackObj);
                        }
                    }

                    if(scanAndPairMgr != null) {
                        scanAndPairMgr.config.alwaysScan = checkBoxAlwaysScan.isChecked();
                        scanAndPairMgr.config.notificationType = NotificationType.BEEPER;
                        if(!checkBoxAlwaysScan.isChecked()) {
                            scanAndPairMgr.config.bluetoothInfo.macAddress = btAddress.getText().toString().trim();
                            scanAndPairMgr.config.bluetoothInfo.deviceName = btName.getText().toString().trim();
                        }
                        else {
                            scanAndPairMgr.config.scanInfo.scanTimeout = 5000;

                            if(checkboxHardTrigger.isChecked()) {
                                scanAndPairMgr.config.scanInfo.triggerType = TriggerType.HARD;
                            } else {
                                scanAndPairMgr.config.scanInfo.triggerType = TriggerType.SOFT;
                            }
                            //scanAndPairMgr.config.scanInfo.deviceIdentifier = DeviceIdentifier.INTERNAL_CAMERA1;
                            scanAndPairMgr.config.scanInfo.scanDataType = (ScanDataType)scandataType.getSelectedItem();
                        }

                        ScanAndPairResults resultCode = scanAndPairMgr.scanAndUnpair();

                        if(!resultCode.equals(ScanAndPairResults.SUCCESS))
                            statusView.append(resultCode.toString()+ "\n\n");


                    } else {
                        statusView.append("ScanAndPairmanager intialization failed!");
                    }
                } catch (Exception e) {
                    statusView.setText("ScanAndUnpair Error:"+ e.getMessage() + "\n");
                }

            }
        });
    }

    @Override
    public void onStatus(StatusData statusData) {

        final StringBuilder text= new StringBuilder();

        boolean isUpdateAddress = false;

        switch (statusData.getState()) {
            case WAITING:
                text.append("Waiting for trigger press to scan the barcode");
                break;

            case SCANNING:
                text.append("Scanner Beam is on, aim at the barcode.");
                break;

            case DISCOVERING:
                text.append("Discovering for the Bluetooth device");
                isUpdateAddress = true;
                break;

            case PAIRED:
                text.append("Bluetooth device is paired successfully");
                break;

            case UNPAIRED:
                text.append("Bluetooth device is un-paired successfully");
                break;

            default:
            case ERROR:
                text.append("\n"+ statusData.getState().toString()+": " + statusData.getResult());
                break;
        }

        final boolean isUpdateUI = isUpdateAddress;
        runOnUiThread(new Runnable() {
            public void run() {
                statusView.setText(text + "\n");

                if(isUpdateUI) {
                    btName.setText(scanAndPairMgr.config.bluetoothInfo.deviceName);
                    btAddress.setText(scanAndPairMgr.config.bluetoothInfo.macAddress);
                }
            }
        });
    }
}
