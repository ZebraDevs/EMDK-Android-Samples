/*
* Copyright (C) 2018 Zebra Technologies Corp
* All rights reserved.
*/
package com.symbol.multibarcodesample1;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.BarcodeManager.ConnectionState;
import com.symbol.emdk.barcode.BarcodeManager.ScannerConnectionListener;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.ScanDataCollection.ScanData;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.Scanner.TriggerType;
import com.symbol.emdk.barcode.StatusData.ScannerStates;
import com.symbol.emdk.barcode.StatusData;
import com.symbol.emdk.barcode.TokenizedData;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends Activity implements EMDKListener, DataListener, StatusListener, ScannerConnectionListener {

    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;

    private TextView textViewStatus = null;
    private Spinner spinnerScannerDevices = null;
    private Spinner spinnerTriggers = null;
    private EditText barcodeCount = null;

    private List<ScannerInfo> deviceList = new ArrayList<ScannerInfo>();
    private String statusString = "";

    private int scannerIndex = 0;
    private int defaultIndex = 0;
    private int triggerIndex = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

		EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
		if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
			textViewStatus.setText("Status: EMDKManager object request failed!");
		}

        textViewStatus = (TextView)findViewById(R.id.textViewStatus);
        spinnerScannerDevices = (Spinner)findViewById(R.id.spinnerScannerDevices);
        spinnerTriggers = (Spinner)findViewById(R.id.spinnerTriggers);
        barcodeCount = (EditText)findViewById(R.id.barcodeCount);

        addSpinnerScannerDevicesListener();
        addSpinnerTriggersListener();
        addBarcodeCountListener();
        addStartScanButtonListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // De-initialize scanner
        deInitScanner();

        // Remove connection listener
        if (barcodeManager != null) {
            barcodeManager.removeConnectionListener(this);
            barcodeManager = null;
        }

        // Release all the resources
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;

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

        textViewStatus.setText("Status: " + "EMDK open success!");

        this.emdkManager = emdkManager;

        // Acquire the barcode manager resources
        barcodeManager = (BarcodeManager) emdkManager.getInstance(FEATURE_TYPE.BARCODE);

        // Add connection listener
        if (barcodeManager != null) {
            barcodeManager.addConnectionListener(this);
        }

        // Enumerate scanner devices
        enumerateScannerDevices();

        // Set default scanner
        spinnerScannerDevices.setSelection(defaultIndex);
    }

    @Override
    public void onClosed() {

        if (emdkManager != null) {

            // Remove connection listener
            if (barcodeManager != null){
                barcodeManager.removeConnectionListener(this);
                barcodeManager = null;
            }

            // Release all the resources
            emdkManager.release();
            emdkManager = null;
        }
        textViewStatus.setText("Status: EMDK closed unexpectedly! Please close and restart the application.");
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {

        String outString = "\nMultiBarcode Data: \n";

        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {

            // Get data
            if (scanDataCollection.getScanData() != null) {

                ArrayList<TableRow> rows = new ArrayList<TableRow>();

                // Adding header row
                TableRow row= new TableRow(this);
                row.setBackgroundColor(Color.BLACK);
                row.setPadding(1, 1, 1, 1);

                TableRow.LayoutParams llp = new TableRow.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,TableLayout.LayoutParams.MATCH_PARENT);
                llp.setMargins(0, 0, 2, 0);

                TextView keyText = new TextView(this);
                keyText.setPadding(5, 5, 5, 5);
                keyText.setLayoutParams(llp);
                keyText.setBackgroundColor(Color.WHITE);
                keyText.setText("Type");
                row.addView(keyText);

                TextView valueText = new TextView(this);
                valueText.setPadding(5, 5, 5, 5);
                valueText.setBackgroundColor(Color.WHITE);
                valueText.setText("Value");
                row.addView(valueText);

                rows.add(row);

                for (ScanData data : scanDataCollection.getScanData()) {

                    row= new TableRow(this);
                    row.setBackgroundColor(Color.BLACK);
                    row.setPadding(1, 1, 1, 1);

                    String mKey = data.getLabelType().toString();
                    String mValue = data.getData();

                    keyText = new TextView(this);
                    keyText.setPadding(5, 5, 5, 5);
                    keyText.setLayoutParams(llp);
                    keyText.setBackgroundColor(Color.WHITE);
                    keyText.setText(mKey);
                    row.addView(keyText);

                    valueText = new TextView(this);
                    valueText.setPadding(5, 5, 5, 5);
                    valueText.setBackgroundColor(Color.WHITE);
                    valueText.setLayoutParams(llp);
                    valueText.setText(mValue);
                    row.addView(valueText);

                    rows.add(row);
                }

                // Update the tokens and the decode type to UI
                new AsyncMultiDataUpdate(rows).execute("MultiBarcode Scanning result");
            }
        }
    }

    @Override
    public void onStatus(StatusData statusData) {

        ScannerStates state = statusData.getState();

        new AsyncStatusUpdate().execute(state.toString());
    }

    private void addSpinnerScannerDevicesListener() {

        spinnerScannerDevices.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View arg1, int position, long arg3) {

                if ((scannerIndex != position) || (scanner==null)) {
                    scannerIndex = position;
                    deInitScanner();
                    initScanner();
                    setTrigger();
                    setConfig();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private void addSpinnerTriggersListener() {

        spinnerTriggers.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int position, long arg3) {

                triggerIndex = position;
                setTrigger();

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });
    }

    private void addStartScanButtonListener() {

        Button btnStartScan = (Button)findViewById(R.id.buttonStartScan);

        btnStartScan.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                startScan();
            }
        });
    }

    private void addBarcodeCountListener() {

        barcodeCount.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int aft )
            {
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                setConfig();
            }
        });
    }

    private void enumerateScannerDevices() {

        if (barcodeManager != null) {

            List<String> friendlyNameList = new ArrayList<String>();
            int spinnerIndex = 0;

            deviceList = barcodeManager.getSupportedDevicesInfo();

            if ((deviceList != null) && (deviceList.size() != 0)) {

                Iterator<ScannerInfo> it = deviceList.iterator();
                while(it.hasNext()) {
                    ScannerInfo scnInfo = it.next();
                    friendlyNameList.add(scnInfo.getFriendlyName());
                    if(scnInfo.isDefaultScanner()) {
                        defaultIndex = spinnerIndex;
                    }
                    ++spinnerIndex;
                }
            }
            else {
                textViewStatus.setText("Status: Failed to get the list of supported scanner devices! Please close and restart the application.");
            }

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, friendlyNameList);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spinnerScannerDevices.setAdapter(spinnerAdapter);
        }
    }

    private void setTrigger() {

        if (scanner == null) {
            initScanner();
        }

        if (scanner != null) {
            switch (triggerIndex) {
                case 0: // Selected "HARD"
                    scanner.triggerType = TriggerType.HARD;
                    break;
                case 1: // Selected "SOFT"
                    scanner.triggerType = TriggerType.SOFT_ALWAYS;
                    break;
            }
        }
    }

    private void setConfig() {

        if (scanner == null) {
            initScanner();
        }

        if ((scanner != null) && (scanner.isEnabled())) {
            try {

                ScannerConfig config = scanner.getConfig();

                // Scan Mode set to Multi Barcode
                config.readerParams.readerSpecific.imagerSpecific.scanMode = ScannerConfig.ScanMode.MULTI_BARCODE;

                // Setting the barcode count
                if(barcodeCount.getText().toString().length() > 0) {
                    config.multiBarcodeParams.barcodeCount = Integer.parseInt(barcodeCount.getText().toString());
                }

                scanner.setConfig(config);

                textViewStatus.setText("Status: Configuration changed!");

            } catch (ScannerException e) {

                textViewStatus.setText("Status: " + e.getMessage());
            }
        }
    }

    private void startScan() {

        if(scanner == null) {
            initScanner();
        }

        if (scanner != null) {
            try {

				if(scanner.isEnabled())
				{
					// Submit a new read.
					scanner.read();
				}
				else
				{
					textViewStatus.setText("Status: Scanner is not enabled");
				}

            } catch (ScannerException e) {

                textViewStatus.setText("Status: " + e.getMessage());
            }
        }

    }

    private void initScanner() {

        if (scanner == null) {

            if ((deviceList != null) && (deviceList.size() != 0)) {
                scanner = barcodeManager.getDevice(deviceList.get(scannerIndex));
            }
            else {
                textViewStatus.setText("Status: Failed to get the specified scanner device! Please close and restart the application.");
                return;
            }

            if (scanner != null) {

                scanner.addDataListener(this);
                scanner.addStatusListener(this);

                try {
                    scanner.enable();

                    checkMultiBarcodeSupport();
                } catch (ScannerException e) {

                    textViewStatus.setText("Status: " + e.getMessage());
                }
            }else{
                textViewStatus.setText("Status: Failed to initialize the scanner device.");
            }
        }
    }

    private void checkMultiBarcodeSupport() {
        if (scanner != null) {
            try {
                ScannerConfig config = scanner.getConfig();
                if (!config.isParamSupported("config.multiBarcodeParams.barcodeCount")){
                    new AsyncMultiDataUpdate(new ArrayList<TableRow>()).execute("NOTE: Multibarcode feature is unsupported in the selected scanner");
                } else {
                    new AsyncMultiDataUpdate(new ArrayList<TableRow>()).execute("");
                }
            } catch (Exception e) {
                textViewStatus.setText("Status: " + e.getMessage());
            }
        }
    }

    private void deInitScanner() {

        if (scanner != null) {

            try {

                scanner.cancelRead();
                scanner.disable();
			
			} catch (Exception e) {

                textViewStatus.setText("Status: " + e.getMessage());
            }
			
			try {
				scanner.removeDataListener(this);
                scanner.removeStatusListener(this);

            } catch (Exception e) {

                textViewStatus.setText("Status: " + e.getMessage());
            }

            try{
                scanner.release();
            } catch (Exception e) {

                textViewStatus.setText("Status: " + e.getMessage());
            }

            scanner = null;
        }
    }

    private class AsyncStatusUpdate extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            return params[0];
        }

        @Override
        protected void onPostExecute(String result) {

            textViewStatus.setText("Status: " + result);
        }
    }

    private class AsyncMultiDataUpdate extends AsyncTask<String, Void, String> {

        private ArrayList<TableRow> rows;
        AsyncMultiDataUpdate(ArrayList<TableRow> rows){
            this.rows = rows;
        }

        @Override
        protected String doInBackground(String... params) {

            return params[0];
        }

        @Override
        protected void onPostExecute(String decodeType) {

            TextView tv = (TextView) findViewById(R.id.textViewType);
            tv.setText(decodeType);

            TableLayout tl = (TableLayout) findViewById(R.id.tableView);

            tl.removeAllViews();
            for (TableRow row : rows) {
                tl.addView(row);
            }
        }
    }

    @Override
    public void onConnectionChange(ScannerInfo scannerInfo, ConnectionState connectionState) {

        String status;
        String scannerName = "";

        String statusExtScanner = connectionState.toString();
        String scannerNameExtScanner = scannerInfo.getFriendlyName();

        if (deviceList.size() != 0) {
            scannerName = deviceList.get(scannerIndex).getFriendlyName();
        }

        if (scannerName.equalsIgnoreCase(scannerNameExtScanner)) {

            switch(connectionState) {
                case CONNECTED:
                    deInitScanner();
                    initScanner();
                    setTrigger();
                    setConfig();
                    break;
                case DISCONNECTED:
                    deInitScanner();
                    break;
            }

            status = scannerNameExtScanner + ":" + statusExtScanner;
            new AsyncStatusUpdate().execute(status);
        }
        else {
            status =  statusString + " " + scannerNameExtScanner + ":" + statusExtScanner;
            new AsyncStatusUpdate().execute(status);
        }
    }
}

