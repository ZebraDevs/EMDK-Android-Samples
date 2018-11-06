/*
* Copyright (C) 2015-2018 Zebra Technologies Corp
* All rights reserved.
*/
package com.symbol.personalshoppersample1;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.personalshopper.CradleException;
import com.symbol.emdk.personalshopper.CradleInfo;
import com.symbol.emdk.personalshopper.CradleLedFlashInfo;
import com.symbol.emdk.personalshopper.CradleResults;
import com.symbol.emdk.personalshopper.DiagnosticConfig;
import com.symbol.emdk.personalshopper.DiagnosticData;
import com.symbol.emdk.personalshopper.DiagnosticException;
import com.symbol.emdk.personalshopper.DiagnosticParamId;
import com.symbol.emdk.personalshopper.*;


public class MainActivity extends Activity implements EMDKListener {
    private EMDKManager emdkManager = null;
    private PersonalShopper PsObject = null;
    boolean mLedsmooth = false;
    private TextView textViewStatus = null;

    private CheckBox mFCState = null;
    private CheckBox mFCSmooth = null;
    private Button btnCrdInfo = null;
    private Button btnSetCfgLeds = null;
    private Button btnUnlock = null;
    private Button btnFlashLeds = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewStatus = (TextView)findViewById(R.id.PStextView);
        mFCState = (CheckBox)this.findViewById(R.id.ChecBoxfastcharge);
        mFCSmooth = (CheckBox)findViewById(R.id.checkBox1);
        btnCrdInfo = (Button)this.findViewById(R.id.CradleInfoButton1);
        btnSetCfgLeds = (Button)this.findViewById(R.id.Diagnosticdata);
        btnUnlock = (Button)this.findViewById(R.id.UnlockButton1);
        btnFlashLeds = (Button)this.findViewById(R.id.FlashLEDButton1);

        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            textViewStatus.setText("Status: " + "Failed in getEMDKManager::"+results.statusCode);
        }
        else{
            textViewStatus.setText("Status: " + "getEMDKManager Success");
        }

        addCrdInfoButtonListener();
        addbtnUnlockButtonListener();
        addFlashLedsButtonListener();
        addFCCheckboxListener() ;
        addDiagnosticButtonListener();
    }

    private void addDiagnosticButtonListener() {
        btnSetCfgLeds.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                getDiagnosticData();
            }
        });
    }

    private void getDiagnosticData() {
        DiagnosticData diagnosticData = null;
        DiagnosticParamId diagnosticparamID = new DiagnosticParamId();
        @SuppressWarnings("static-access")
        int paramId = diagnosticparamID.ALL;

        DiagnosticConfig diagnosticconfig = new DiagnosticConfig(200,60);

        if(null!=PsObject.diagnostic)
        {
            try{
                diagnosticData =  PsObject.diagnostic.getDiagnosticData(paramId, diagnosticconfig);
            }catch(DiagnosticException e){
                e.printStackTrace();
                textViewStatus.setText("Status: " + e.getMessage());
            }
            if(diagnosticData!=null)
            {
                showMessage("\n Battery Capacity in Per : "+diagnosticData.batteryStateOfCharge
                                +"\n Battery Capacity in mins: "+diagnosticData.batteryTimeToEmpty
                                +"\n Battery SOH in Per: "+diagnosticData.batteryStateOfHealth
                                +"\n Battery Charging Time Required mins: "+diagnosticData.batteryChargingTime
                                +"\n Battery Replacement in days: "+diagnosticData.timeSinceBatteryReplaced
                                +"\n Time since Last reboot  mins: "+diagnosticData.timeSinceReboot
                                +"\n Battery Charging Time Elapsed mins: "+diagnosticData.batteryChargingTimeElapsed
                                +"\n Manufacturing Date: "+diagnosticData.batteryDateOfManufacture,Toast.LENGTH_LONG
                );
            }
        }
    }

    private void addSmoothCheckboxListener() throws CradleException {
        if(null!=PsObject.cradle){
            mLedsmooth = mFCSmooth.isChecked() ;
        }
    }

    private void addFCCheckboxListener() {
        mFCState.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if(null!=PsObject.cradle){
                    try {
                        if (mFCState.isChecked())
                        {
                            PsObject.cradle.config.setFastChargingState(true);
                            if(PsObject.cradle.config.getFastChargingState())
                                textViewStatus.setText("Status: " +"fast charge enabled");
                            else
                                textViewStatus.setText("Status: " +"fast charge enabling failed");
                        }
                        else
                        {
                            PsObject.cradle.config.setFastChargingState(false);
                            if(!(PsObject.cradle.config.getFastChargingState()))
                                textViewStatus.setText("Status: " +"fast charge disabled");
                            else
                                textViewStatus.setText("Status: " +"fast charge disabling failed");
                        }
                    } catch (CradleException e) {
                        e.printStackTrace();
                        textViewStatus.setText("Status: " + e.getMessage());
                    }
                }
            }
        });
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

    public void showMessage(String text)
    {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void showMessage(String text,int duration)
    {
        Toast toast = Toast.makeText(getApplicationContext(), text, duration);
        toast.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (PsObject != null) {
            disable();
            PsObject = null;
        }

        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }

    }
    @Override
    public void onClosed() {

        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }

    }

    void addFlashLedsButtonListener() {
        btnFlashLeds.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                flashLeds();
            }
        });
    }

    protected void flashLeds() {
        if(null!=PsObject.cradle){
            int onDuration = 2000;
            int offDuration = 1000;
            int flashCount = 5;

            try {
                addSmoothCheckboxListener();
                CradleLedFlashInfo ledFlashInfo = new CradleLedFlashInfo(onDuration, offDuration, mLedsmooth);
                CradleResults result = PsObject.cradle.flashLed(flashCount, ledFlashInfo);
                if(result == CradleResults.SUCCESS){
                    textViewStatus.setText("Status: " + "Flashed LEDs ");
                }
                else{
                    textViewStatus.setText("Status: " + "Failed error "+result.getDescription());
                }
            }catch (CradleException e) {

                e.printStackTrace();
                textViewStatus.setText("Status: " + e.getMessage());
            }
        }
    }

    private void addbtnUnlockButtonListener() {
        btnUnlock.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                unlock();
            }
        });
    }


    protected void unlock() {
        if(null!=PsObject.cradle){
            int onDuration = 500;
            int offDuration = 500;
            int unlockDuration = 15;

            try {
                addSmoothCheckboxListener();
                CradleLedFlashInfo ledFlashInfo = new CradleLedFlashInfo(onDuration, offDuration, mLedsmooth);
                CradleResults result = PsObject.cradle.unlock(unlockDuration, ledFlashInfo);
                if(result == CradleResults.SUCCESS){
                    textViewStatus.setText("Status: " + "Unlocked");
                }
                else{
                    textViewStatus.setText("Status: " + "Failed error "+result.getDescription());
                }
            }catch (CradleException e) {

                e.printStackTrace();
                textViewStatus.setText("Status: " + e.getMessage());
            }
        }
    }


    private void addCrdInfoButtonListener() {
        btnCrdInfo.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                getCradleInfo();
            }
        });
    }


    protected void getCradleInfo() {

        CradleInfo cradleInfo = null;
        if(null!=PsObject.cradle)
        {
            try{
                cradleInfo =  PsObject.cradle.getCradleInfo();
            }catch(CradleException e){
                e.printStackTrace();
                textViewStatus.setText("Status: " + e.getMessage());
            }
            if(cradleInfo!=null)
            {
                Log.d("PartNo","part No="+cradleInfo.getPartNumber());
                showMessage("FirmwareVersion: "+cradleInfo.getFirmwareVersion()
                                +"\nDateOfManufacturing: "+cradleInfo.getDateOfManufacture()
                                +"\nHardwareID: "+cradleInfo.getHardwareID()
                                +"\nPartnumber: "+cradleInfo.getPartNumber()
                                +"\nSerialNumber: "+cradleInfo.getSerialNumber(),Toast.LENGTH_LONG
                );
            }
        }
    }

    protected void disable() {
        try {
            if (null != PsObject.cradle) {
                PsObject.cradle.disable();
            }
        } catch (CradleException e) {

            e.printStackTrace();
            textViewStatus.setText("Status: " + e.getMessage());
        }
    }

    protected void enable() {
        try {
            if(null!=PsObject.cradle) {
                PsObject.cradle.enable();
            }
        } catch (CradleException e) {

            e.printStackTrace();
            textViewStatus.setText("Status: " + e.getMessage());
        }
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {

        this.emdkManager = emdkManager;
        try{
            PsObject = (PersonalShopper) this.emdkManager.getInstance(FEATURE_TYPE.PERSONALSHOPPER);
        }
        catch(Exception e)
        {
            textViewStatus.setText("Status: " + e.getMessage());
        }

        if (PsObject == null) {
            textViewStatus.setText("Status: " + "PersonalShopper Feature NOT supported");
            disableUI();
        }
        else
        {
            enable();
        }
    }

    private void disableUI() {
        mFCState.setEnabled(false);
        mFCSmooth.setEnabled(false);
        btnCrdInfo.setEnabled(false);
        btnSetCfgLeds.setEnabled(false);
        btnUnlock.setEnabled(false);
        btnFlashLeds.setEnabled(false);
    }

}
