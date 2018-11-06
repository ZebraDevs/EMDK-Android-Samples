/*
* Copyright (C) 2015-2017 Zebra Technologies Corp
* All rights reserved.
*/
package com.symbol.emdk.simulscansample1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.simulscan.SimulScanConfig;
import com.symbol.emdk.simulscan.SimulScanData;
import com.symbol.emdk.simulscan.SimulScanException;
import com.symbol.emdk.simulscan.SimulScanManager;
import com.symbol.emdk.simulscan.SimulScanMultiTemplate;
import com.symbol.emdk.simulscan.SimulScanReader;
import com.symbol.emdk.simulscan.SimulScanReader.DataListerner;
import com.symbol.emdk.simulscan.SimulScanReader.StatusListerner;
import com.symbol.emdk.simulscan.SimulScanReaderInfo;
import com.symbol.emdk.simulscan.SimulScanStatusData;
import com.symbol.emdk.EMDKResults;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class DeviceControl extends Fragment implements OnItemSelectedListener,
        EMDKListener, DataListerner, StatusListerner, OnClickListener {

    private final static String TAG = DeviceControl.class.getCanonicalName();

    private TextView textViewStatus = null;
    private Spinner spinner2 = null;
    private Button readBtn = null;
    private Button stopReadBtn = null;
    private EMDKManager emdkManager = null;
    private SimulScanManager simulscanManager = null;
    List<SimulScanReaderInfo> readerInfoList = null;
    // DeviceIdentifier selectedDeviceIdentifier = DeviceIdentifier.DEFAULT;
    EMDKResults results;
    SimulScanReader selectedSimulScanReader = null;
    static List<SimulScanData> simulscanDataList = Collections
            .synchronizedList(new ArrayList<SimulScanData>());
    Exception lastException;
    // private List<SimulScanReaderInfo> deviceList = null;
    private int readerIndex = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "SSC onCreate");
        // getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        results = EMDKManager.getEMDKManager(getActivity()
                .getApplicationContext(), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "SSC onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_main, container,
                false);

        textViewStatus = (TextView) rootView.findViewById(R.id.textView1);
        textViewStatus.setText("Status: " + " Starting..");

        spinner2 = (Spinner) rootView.findViewById(R.id.spinner2);
        spinner2.setOnItemSelectedListener(this);

        readBtn = (Button) rootView.findViewById(R.id.button4);
        readBtn.setOnClickListener(this);

        stopReadBtn = (Button) rootView.findViewById(R.id.button5);
        stopReadBtn.setOnClickListener(this);

        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            Log.e(TAG, "EMDKManager object request failed!");
            textViewStatus.setText("Status: "
                    + "EMDKManager object request failed!");
        }

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "SSC onStart");
        if (selectedSimulScanReader != null)
            try {
                if (!selectedSimulScanReader.isEnabled())
                    selectedSimulScanReader.enable();
            } catch (SimulScanException e) {
                Log.e(TAG, "Error enabling reader: " + e.getMessage());
                e.printStackTrace();
                textViewStatus.setText("Status: " + "Error enabling reader");
            }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "SSC onResume");
        super.onResume();
        // The application is in foreground

        // Acquire the SimulScan manager resources
        if (emdkManager != null) {
            simulscanManager = (SimulScanManager) emdkManager
                    .getInstance(FEATURE_TYPE.SIMULSCAN);

            // Initialize scanner
            try {
                if (simulscanManager != null) {
                    prepareScanner(spinner2.getSelectedItemPosition());
                } else {
                    Log.d(TAG, "SSC onResume simulscanManager is null");
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "SSC onPause");
        super.onPause();
        // The application is in background

        // De-initialize scanner
        try {
            deinitCurrentScanner();
        } catch (SimulScanException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Remove connection listener
        if (simulscanManager != null) {
            simulscanManager = null;
        }

        // Release the SimulScan manager resources
        if (emdkManager != null) {
            emdkManager.release(FEATURE_TYPE.SIMULSCAN);
        }

    }

    @Override
    public void onStop() {
        Log.d(TAG, "SSC onStop");
        if (selectedSimulScanReader != null) {
            if (selectedSimulScanReader.isReadPending()) {
                try {
                    selectedSimulScanReader.cancelRead();
                } catch (SimulScanException e) {
                    Log.e(TAG, "Error stopping reader: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            try {
                if (selectedSimulScanReader.isEnabled()) {
                    selectedSimulScanReader.disable();
                }
            } catch (SimulScanException e) {
                Log.e(TAG, "Error disabling reader: " + e.getMessage());
                e.printStackTrace();
            }
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "SSC onDestroy");

        if (selectedSimulScanReader != null) {
            selectedSimulScanReader.removeDataListener(this);
            selectedSimulScanReader.removeStatusListener(this);
        }

        if (simulscanManager != null) {
            // simulscanManager.release();
            emdkManager.release(FEATURE_TYPE.SIMULSCAN);
            simulscanManager = null;
        }

        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

    private void addItemsOnSpinner(Spinner spinner, List<String> list) {

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.simple_spinner_item, list);
        dataAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
    }

    public void deinitCurrentScanner() throws SimulScanException {
        if (selectedSimulScanReader != null) {
            if (selectedSimulScanReader.isReadPending())
                selectedSimulScanReader.cancelRead();
            if (selectedSimulScanReader.isEnabled())
                selectedSimulScanReader.disable();
            selectedSimulScanReader.removeDataListener(this);
            selectedSimulScanReader.removeStatusListener(this);
            selectedSimulScanReader = null;
        }
    }

    public void initCurrentScanner() throws SimulScanException {
        selectedSimulScanReader.addStatusListener(this);
        selectedSimulScanReader.addDataListener(this);
        selectedSimulScanReader.enable();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos,
                               long arg3) {
        if (parent.equals(spinner2)) {
            prepareScanner(pos);
        }
    }

    public void prepareScanner(int pos) {
        if (simulscanManager != null) {
            SimulScanReaderInfo readerInfo = readerInfoList.get(pos);
            if (readerInfo != null) {
                Log.d(TAG, "onItemSelected:" + readerInfo.getFriendlyName());
                if (readerIndex != pos) {
                    readerIndex = pos;
                }
//				if (readerInfo.getDeviceIdentifier() != selectedDeviceIdentifier) {
//					selectedDeviceIdentifier = readerInfo.getDeviceIdentifier();
                try {
                    deinitCurrentScanner();
                    selectedSimulScanReader = simulscanManager.getDevice(readerInfoList.get(readerIndex));
                    initCurrentScanner();
                } catch (SimulScanException e) {
                    Log.e(TAG, "Error enabling reader: " + e.getMessage());
                    e.printStackTrace();
                    textViewStatus.setText("Status: "
                            + "Error enabling reader");
                }
            }

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        if (parent.equals(spinner2)) {
            Log.d(TAG, "onNothingSelected");
            try {
                deinitCurrentScanner();
            } catch (SimulScanException e) {
                Log.e(TAG, "Error disabling reader: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        Log.d(TAG, "onOpened");
        textViewStatus.setText("Status: " + "EMDK open success!");
        this.emdkManager = emdkManager;
        simulscanManager = (SimulScanManager) emdkManager
                .getInstance(FEATURE_TYPE.SIMULSCAN);
        if (null == simulscanManager) {
            Log.e(TAG, "Get SimulScanManager instance failed!");
            textViewStatus.setText("Status: "
                    + "Get SimulScanManager instance failed!");
            return;
        }

        readerInfoList = simulscanManager.getSupportedDevicesInfo();
        List<String> nameList = new ArrayList<String>();
        for (SimulScanReaderInfo rinfo : readerInfoList) {
            nameList.add(rinfo.getFriendlyName());
        }
        addItemsOnSpinner(spinner2, nameList);
        spinner2.setOnItemSelectedListener(this);
        readerIndex = 0;
        try {
            selectedSimulScanReader = simulscanManager.getDevice(readerInfoList
                    .get(readerIndex));
            initCurrentScanner();
        } catch (SimulScanException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void onClosed() {
        Log.d(TAG, "onClosed: EMDK closed unexpectedly");

        emdkManager.release();
        emdkManager = null;

        textViewStatus.setText("Status: " + "EMDK closed unexpectedly!");
    }

    private void setCurrentConfig() throws Exception {
        if (selectedSimulScanReader != null) {
            SimulScanConfig config = selectedSimulScanReader.getConfig();
            if (config != null) {

                // set template
                if (config.multiTemplate != null) {
                    Log.d(TAG,
                            "Conf template name: "
                                    + config.multiTemplate.getTemplateName());
                } else
                    Log.d(TAG, "Conf template is null");

                MainActivity parentActivity = (MainActivity) getActivity();
                if ((parentActivity.localSettings.fileList == null)
                        || (parentActivity.localSettings.fileList.size() == 0)) {
                    Log.e(TAG, "Invalid template Path");
                    throw new Exception();
                }

                Log.d(TAG, "Template index: "
                        + parentActivity.localSettings.selectedFileIndex);
                SimulScanMultiTemplate multiTemplate;
                multiTemplate = new SimulScanMultiTemplate(
                        simulscanManager,
                        Uri.fromFile(parentActivity.localSettings.fileList
                                .get(parentActivity.localSettings.selectedFileIndex)));

                if (multiTemplate != null)
                    config.multiTemplate = multiTemplate;

                config.autoCapture = parentActivity.localSettings.enableAutoCapture;
                config.audioFeedback = parentActivity.localSettings.enableFeedbackAudio;
                config.hapticFeedback = parentActivity.localSettings.enableHaptic;
                config.ledFeedback = parentActivity.localSettings.enableLED;
                config.userConfirmationOnScan = parentActivity.localSettings.enableResultConfirmation;
                config.identificationTimeout = parentActivity.localSettings.identificationTimeout;
                config.processingTimeout = parentActivity.localSettings.processingTimeout;

                selectedSimulScanReader.setConfig(config);
            }
        }
    }

    private void readCurrentScanner() throws Exception {
        setCurrentConfig();
        if (selectedSimulScanReader != null) {
            selectedSimulScanReader.read();
        }
    }

    private void stopReadCurrentScanner() throws SimulScanException {
        if (selectedSimulScanReader != null)
            selectedSimulScanReader.cancelRead();
    }

    @Override
    public void onClick(View arg0) {
        if (arg0.equals(readBtn)) {
            Log.d(TAG, "Read clicked");
            try {
                readCurrentScanner();
            } catch (Exception e) {
                lastException = e;
                textViewStatus.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewStatus.setText("Status: "
                                + lastException.getMessage());
                    }
                });
                Log.e(TAG, "Exception while starting read: " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (arg0.equals(stopReadBtn)) {
            Log.d(TAG, "Stop Read clicked");
            try {
                stopReadCurrentScanner();
            } catch (SimulScanException e) {
                lastException = e;
                textViewStatus.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewStatus.setText("Status: "
                                + lastException.getMessage());
                    }
                });
                Log.e(TAG,
                        "Exception while cancelling read : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onStatus(SimulScanStatusData statusData) {

        if (statusData != null) {

            String statusText = "";
            switch (statusData.getState()) {
                case DISABLED:
                    new AsyncStatusUpdate().execute("Status: "
                            + statusData.getFriendlyName()
                            + ": Closed reader successfully");
                    break;
                case ENABLED:
                    new AsyncStatusUpdate().execute("Status: "
                            + statusData.getFriendlyName()
                            + ": Opened reader successfully");

                    break;
                case SCANNING:
                    new AsyncStatusUpdate().execute("Status: "
                            + statusData.getFriendlyName()
                            + ": Started reader successfully");

                    break;
                case IDLE:
                    statusText = "Status: "
                            + statusData.getFriendlyName()
                            + ": Stopped reader successfully";

                    switch(statusData.getExtendedState())
                    {
                        case PROCESSING_TIMEOUT:
                            statusText += "\nReason: Processing timeout";
                            break;
                        case IDENTIFICATION_TIMEOUT:
                            statusText += "\nReason: Identification timeout";
                            break;
                        case CANCELLED:
                            statusText += "\nReason: Cancelled";
                            break;
                        case FORM_DECODED:
                            statusText += "\nReason: Form decoded successfully";
                            break;
                    }
                    new AsyncStatusUpdate().execute(statusText);
                    break;
                case ERROR:
                    statusText = "Status: "
                            + statusData.getFriendlyName() + " Error";

                    switch(statusData.getExtendedState())
                    {
                        case UNLICENSED_FEATURE:
                            statusText += "\nUnlicensed Feature detected: " + statusData.getStatusDescription();
                            break;
                        default:
                            statusText += "\n" + statusData.getStatusDescription();
                            break;
                    }
                    new AsyncStatusUpdate().execute(statusText);
                    break;
                case UNKNOWN:
                default:
                    break;
            }
        }
    }

    @Override
    public void onData(SimulScanData simulScanData) {
        // TODO Auto-generated method stub
        Log.v(TAG, "onData");
        Intent intent = new Intent(getActivity(), ResultsActivity.class);
        synchronized (simulscanDataList) {
            simulscanDataList.add(simulScanData);
        }
        startActivity(intent);
    }

    private class AsyncStatusUpdate extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            return params[0];
        }

        @Override
        protected void onPostExecute(String result) {

            textViewStatus.setText(result);
        }
    }
}