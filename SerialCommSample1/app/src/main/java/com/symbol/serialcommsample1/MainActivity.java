/*
* Copyright (C) 2015-2017 Zebra Technologies Corp
* All rights reserved.
*/
package com.symbol.serialcommsample1;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.serialcomm.SerialComm;
import com.symbol.emdk.serialcomm.SerialCommException;
import com.symbol.emdk.serialcomm.SerialCommManager;
import com.symbol.emdk.serialcomm.SerialCommResults;
import com.symbol.emdk.serialcomm.SerialPortInfo;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.HashMap;
import java.util.List;



public class MainActivity extends Activity implements EMDKListener {

    private String TAG = MainActivity.class.getSimpleName();
    private EMDKManager emdkManager = null;
    private SerialComm serialCommPort = null;
    private SerialCommManager serialCommManager = null;
    private EditText txtDataToSend = null;
    private TextView txtStatus = null;
    private Button btnRead = null;
    private Button btnWrite = null;
    private Spinner spinnerPorts = null;
    public HashMap<String, SerialPortInfo> supportedPorts = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtDataToSend = (EditText) findViewById(R.id.txtDataToSend);
        txtDataToSend.setText("Serial Communication Write Data Testing.");

        spinnerPorts = (Spinner)findViewById(R.id.spinnerPorts);
        btnWrite = (Button) findViewById(R.id.btnWrite);
        btnRead = (Button) findViewById(R.id.btnRead);

        txtStatus = (TextView) findViewById(R.id.txtStatus);
        txtStatus.setText("");
        txtStatus.requestFocus();


        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            new AsyncStatusUpdate().execute("EMDKManager object request failed!");
        }

        new AsyncUiControlUpdate().execute(false);
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        this.emdkManager = emdkManager;

        Log.d(TAG, "EMDK opened");

        try{
            serialCommManager = (SerialCommManager) this.emdkManager.getInstance(FEATURE_TYPE.SERIALCOMM_EX);
            if(serialCommManager != null) {
                populatePorts();
            }
            else
            {
                new AsyncStatusUpdate().execute(FEATURE_TYPE.SERIALCOMM_EX.toString() +  " Feature not supported.");
            }
        }
        catch(Exception e)
        {
            Log.d(TAG, e.getMessage());
            new AsyncStatusUpdate().execute(e.getMessage());
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
    protected void onDestroy() {
        super.onDestroy();

        deinitSerialComm();

        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        deinitSerialComm();

        serialCommManager = null;
        supportedPorts = null;
        // Release the serialComm manager resources
        if (emdkManager != null) {
            emdkManager.release(FEATURE_TYPE.SERIALCOMM_EX);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Acquire the serialComm manager resources
        if (emdkManager != null) {
            serialCommManager = (SerialCommManager) emdkManager.getInstance(FEATURE_TYPE.SERIALCOMM_EX);

            if (serialCommManager != null) {
                populatePorts();
				if (supportedPorts != null)
					initSerialComm();
            }
        }


    }


    void populatePorts()
    {
        try {

            if(serialCommManager != null) {
                List<SerialPortInfo> serialPorts = serialCommManager.getSupportedPorts();
                if(serialPorts.size()>0) {
                    supportedPorts = new HashMap<String, SerialPortInfo> ();
                    String[] ports = new String[serialPorts.size()];
                    int count = 0;
                    for (SerialPortInfo info : serialPorts) {
                        supportedPorts.put(info.getFriendlyName(), info);
                        ports[count] = info.getFriendlyName();
                        count++;
                    }

                    spinnerPorts.setAdapter(new ArrayAdapter<String>(this,
                            android.R.layout.simple_spinner_dropdown_item, ports));

                    spinnerPorts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                            //Disabling previous serial port before getting the new one
                            deinitSerialComm();
                            initSerialComm();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });

                }
                else
                {
                    new AsyncStatusUpdate().execute("Failed to get available ports");
                }
            }
            else
            {
                new AsyncStatusUpdate().execute("SerialCommManager is null");
            }

        }
        catch (Exception ex)
        {
            Log.d(TAG, ex.getMessage());
            new AsyncStatusUpdate().execute(ex.getMessage());
        }
    }



    void initSerialComm() {
        new AsyncEnableSerialComm().execute(supportedPorts.get(spinnerPorts.getSelectedItem()));
    }

    @Override
    public void onClosed() {
        if(emdkManager != null) {
            emdkManager.release();
        }
        new AsyncStatusUpdate().execute("EMDK closed unexpectedly! Please close and restart the application.");
    }


    public void btnReadOnClick(View arg)
    {
        new AsyncReadData().execute();
    }

    public void btnWriteOnClick(View arg)
    {
        new AsyncUiControlUpdate().execute(false);
        try {
            String writeData = txtDataToSend.getText().toString();
            int bytesWritten = serialCommPort.write(writeData.getBytes(), writeData.getBytes().length);
            new AsyncStatusUpdate().execute("Bytes written: "+ bytesWritten);

        } catch (SerialCommException e) {
            new AsyncStatusUpdate().execute("write: "+ e.getResult().getDescription());
        }
        catch (Exception e) {
            new AsyncStatusUpdate().execute("write: "+ e.getMessage() + "\n");
        }
        new AsyncUiControlUpdate().execute(true);
    }


    void deinitSerialComm() {

        if (serialCommPort != null) {
            try {

                serialCommPort.disable();
                serialCommPort = null;

            } catch (Exception ex) {
                Log.d(TAG, "deinitSerialComm disable Exception: " + ex.getMessage());
            }
        }
    }

    private class AsyncStatusUpdate extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            return params[0];
        }

        @Override
        protected void onPostExecute(String result) {

            txtStatus.setText(result);

        }
    }

    private class AsyncUiControlUpdate extends AsyncTask<Boolean, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Boolean... arg0) {

            return arg0[0];
        }

        @Override
        protected void onPostExecute(Boolean bEnable) {

            btnRead.setEnabled(bEnable);
            btnWrite.setEnabled(bEnable);
            txtDataToSend.setEnabled(bEnable);
            spinnerPorts.setEnabled(bEnable);
        }
    }

    private class AsyncEnableSerialComm extends AsyncTask<SerialPortInfo, Void, SerialCommResults>
    {
        @Override
        protected SerialCommResults doInBackground(SerialPortInfo... params) {

            SerialCommResults returnvar = SerialCommResults.FAILURE;
            try {
                serialCommPort = serialCommManager.getPort(params[0]);

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (serialCommPort != null) {
                try {
                    serialCommPort.enable();
                    returnvar = SerialCommResults.SUCCESS;

                } catch (SerialCommException e) {
                    Log.d(TAG, e.getMessage());
                    e.printStackTrace();
                    returnvar = e.getResult();
                }
            }

            return returnvar;
        }

        @Override
        protected void onPostExecute(SerialCommResults result) {
            super.onPostExecute(result);
            if (result == SerialCommResults.SUCCESS) {
                new AsyncStatusUpdate().execute("Serial comm channel enabled: (" + spinnerPorts.getSelectedItem().toString() + ")");
                txtDataToSend.setText("Serial Communication Write Data Testing " + spinnerPorts.getSelectedItem().toString() + ".");
                new AsyncUiControlUpdate().execute(true);
            } else {
                new AsyncStatusUpdate().execute(result.getDescription());
                new AsyncUiControlUpdate().execute(false);
            }
        }
    }

    private class AsyncReadData extends AsyncTask<Void, Void, String>
    {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            new AsyncUiControlUpdate().execute(false);
            new AsyncStatusUpdate().execute("Reading..");
        }

        @Override
        protected String doInBackground(Void... params) {
            String statusText = "";
                try {

                    byte[] readBuffer = serialCommPort.read(10000); //Timeout after 10 seconds

                    if (readBuffer != null) {
                        String tempString = new String(readBuffer);
                        statusText = "Data Read:\n" + tempString;
                    } else {
                        statusText = "No Data Available";
                    }

                } catch (SerialCommException e) {
                    statusText = "read:" + e.getResult().getDescription();
                } catch (Exception e) {
                    statusText = "read:" + e.getMessage();
                }
            return statusText;
        }

        @Override
        protected void onPostExecute(String statusText) {
            super.onPostExecute(statusText);
            new AsyncUiControlUpdate().execute(true);
            new AsyncStatusUpdate().execute(statusText);
        }

    }
}
