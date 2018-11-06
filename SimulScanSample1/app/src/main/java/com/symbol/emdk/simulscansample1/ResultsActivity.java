/*
* Copyright (C) 2015-2017 Zebra Technologies Corp
* All rights reserved.
*/
package com.symbol.emdk.simulscansample1;

import java.util.ArrayList;
import java.util.List;

import com.symbol.emdk.simulscansample1.R;
import com.symbol.emdk.simulscan.SimulScanData;
import com.symbol.emdk.simulscan.SimulScanElement;
import com.symbol.emdk.simulscan.SimulScanGroup;
import com.symbol.emdk.simulscan.SimulScanRegion;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

public class ResultsActivity extends Activity {

    private final static String TAG = ResultsActivity.class.getCanonicalName();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "SSC onCreate");
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_results);
        if(DeviceControl.simulscanDataList.size() > 0){
            SimulScanData processedForm;
            synchronized (DeviceControl.simulscanDataList) {
                processedForm = DeviceControl.simulscanDataList.get(DeviceControl.simulscanDataList.size() - 1);
            }
            onFormProcessed(processedForm);
            synchronized (DeviceControl.simulscanDataList) {
                DeviceControl.simulscanDataList.clear();
            }
        }else{
            Log.w(TAG, "SimulScanData list is empty");
        }
    }

    public void onFormProcessed(SimulScanData processedForm) {

        TextView mtvTimestamp = (TextView)findViewById(R.id.tvTimestamp);
        String timestamp = processedForm.getTimestamp();
        Log.v(TAG, "onFormProcessed: timestamp-" + timestamp.toString());
        mtvTimestamp.setText(timestamp);

        //get list of elements, extract regions from groups
        List<SimulScanElement> processedElements = processedForm.getElements();
        List<SimulScanRegion> copyProcessedRegions = new ArrayList<SimulScanRegion>(); //expanding elements into regions
        for (SimulScanElement curElement : processedElements)
        {
            if (curElement instanceof SimulScanRegion)
            {
                copyProcessedRegions.add((SimulScanRegion) curElement);
            }
            else if (curElement instanceof SimulScanGroup)
            {
                List<SimulScanRegion> regionsInGroup = ((SimulScanGroup) curElement).getRegions();
                for (SimulScanRegion curRegion : regionsInGroup)
                {
                    copyProcessedRegions.add(curRegion);
                }
            }
        }

        RegionArrayAdapter adapter = new RegionArrayAdapter(this, R.layout.region_item, copyProcessedRegions);
        ListView mlvProcessedRegions = (ListView) this.findViewById(R.id.regionLV);
        mlvProcessedRegions.setAdapter(adapter);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        Log.v(TAG, "SSC onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        Log.v(TAG, "SSC onResume");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        Log.v(TAG, "SSC onDestroy");
        super.onDestroy();
    }
}
