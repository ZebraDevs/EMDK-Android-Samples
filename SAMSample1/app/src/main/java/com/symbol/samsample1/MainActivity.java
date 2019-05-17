/*
 * Copyright (C) 2019 Zebra Technologies Corporation and/or its affiliates
 * All rights reserved.
 */

package com.symbol.samsample1;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.sam.SAM;
import com.symbol.emdk.sam.SAMException;
import com.symbol.emdk.sam.SAMManager;
import com.symbol.emdk.sam.SAMResults;
import com.symbol.emdk.sam.SAMType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements EMDKManager.EMDKListener {

    private static final String TAG = MainActivity.class.getCanonicalName();
    //Declare a variable to store EMDKManager object
    private EMDKManager emdkManager = null;
    private SAMManager samManager = null;
    boolean tagOperationInProgress = false;
    private Button btnGetSAMInfo = null;
    private HashMap<Integer, SAM> presetSAMList = new HashMap<>();
    private PendingIntent nfcIntent = null;
    private NfcAdapter nfcAdapter = null;
    private TextView txtStatus = null;
    private HashMap<SAMType,  byte[]> getVersionAPDUs = new HashMap<>();
    private String detectedTag = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setContentView(R.layout.activity_main);

        txtStatus = (TextView) findViewById(R.id.txtStatus);
        btnGetSAMInfo = (Button) findViewById(R.id.btnGetSAMInfo);

        getVersionAPDUs.put(SAMType.MIFARE, new byte[]{(byte) 0x80, (byte) 0x60, (byte) 0x00, (byte) 0x00,/*(byte)0x00,*/(byte) 0x00});
        getVersionAPDUs.put(SAMType.FELICA, new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xE6, (byte) 0x00, (byte) 0x00});

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Toast.makeText(this, getString(R.string.message_nfc_not_supported), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        nfcIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, this.getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        //The EMDKManager object will be created and returned in the callback.
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);

        //Check the return status of EMDKManager object creation.
        if (results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
            //EMDKManager object creation success
        } else {
            //EMDKManager object creation failed
        }
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        this.emdkManager = emdkManager;
        initSAMManager();
        enumerateSAMsAndGetInfo();
    }

    void enableDisableUIComponents(final boolean enabled)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnGetSAMInfo.setEnabled(enabled);
            }
        });
    }

    private void enumerateSAMsAndGetInfo() {
        if (samManager != null) {
            txtStatus.setText("");
            List<SAM> samList = null;
            presetSAMList.clear();
            try {
                samList = samManager.enumerateSAMs();
            } catch (SAMException ex) {
                updateStatus(getString(R.string.message_sam_exception_enumerate)+ " " + SAMResults.getErrorDescription(ex.getResult()));
                return;
            }

            if ((samList != null) && (samList.size() != 0)) {
                Iterator<SAM> it = samList.iterator();
                int i = 0;
                while (it.hasNext()) {
                    SAM sam = it.next();
                    presetSAMList.put(sam.getSamIndex(),sam);
                    getSAMInfo(sam);
                    i++;
                }
            } else {
                enableDisableUIComponents(false);
                updateStatus(getString(R.string.message_failed_to_get_sams));
            }
        }
    }

    public void onClickGetSAMInfo(View view)
    {
        enumerateSAMsAndGetInfo();
    }

    void updateStatus(String s)
    {
        String text = txtStatus.getText().toString();
        text = s + "\n\n" + text;
        if(text.length()>1000)
            text = s;
        txtStatus.setText(text);
    }

    private static String getHexString (byte[] buf)
    {
        StringBuffer sb = new StringBuffer();
        for (byte b:buf)
        {
            sb.append(String.format("0x%02x", b));
        }
        return sb.toString();
    }

    private void getSAMInfo(SAM sam) {

        String text = "";
        if (sam != null) {
            long tick = System.currentTimeMillis();

            /** Connect [Start] */
            try {
                if (!sam.isConnected()) {
                    sam.connect();
                    text += getString(R.string.message_sam_connected_successfully) + " " + sam.getSamType() + "(Slot "+ sam.getSamIndex() +")\n";
                }
            } catch (SAMException ex) {
                updateStatus(getString(R.string.message_connect_error) + " " + SAMResults.getErrorDescription(ex.getResult()));
                return;
            }
            /** Connect [End] */

            /** Transceive [Start] */
            byte[] getVersionAPDU = getVersionAPDUs.get(sam.getSamType());
            byte[] response = null;
            try {
                text += getString(R.string.message_transceive) + "\n";
                response = sam.transceive(getVersionAPDU, (short) 0, false);
                if (response != null) {
                    text += getString(R.string.version) + " " +  getHexString(response) +"\n";
                }
                else
                {
                    text += getString(R.string.version_error) + "\n";
                }
            } catch (SAMException ex) {
                text += getString(R.string.message_transceive_failed) + " " + SAMResults.getErrorDescription(ex.getResult()) + "\n";
            }
            /**Transceive [End] */

            /** Disconnect [Start] */
            if (sam.isConnected()) {
                sam.disconnect();
                text += getString(R.string.message_disconnecting) + " " + sam.getSamType() + "("+ sam.getSamIndex() +")\n";
            }
            /** Disconnect [End] */

            long timetook = (System.currentTimeMillis() - tick);
            text+="Time taken to get version: " + timetook + "ms";
            updateStatus(text);
        }
    }

    void initSAMManager()
    {
        if(emdkManager != null) {
            //Get the SAMManager object to process the profiles
            samManager = (SAMManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.SAM);
        }
    }

    private void deinitSAMManager()
    {
        //Clean up the objects created by EMDK samManager
        if (samManager != null) {
            emdkManager.release(EMDKManager.FEATURE_TYPE.SAM);
            samManager = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deinitSAMManager();

        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    public void onClosed() {
        //This callback will be issued when the EMDK closes unexpectedly.
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initSAMManager();

        if(tagOperationInProgress)
            tagOperationInProgress = false;

        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, nfcIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        deinitSAMManager();
        if(nfcAdapter!=null){
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        tagDetection(intent);
    }

    private void tagDetection(Intent intent) {

        tagOperationInProgress = true;

        if(samManager == null) {
            initSAMManager();
        }

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())
                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {

            Tag lTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            SAM sam = null;
            SAMType samTypeForTag = null;

            samTypeForTag = findCompatibleSAM(lTag);

            String text = detectedTag + " " + getString(R.string.message_tag_detected) + "\n";

            String compatibeSAMText = "";
            for (Map.Entry<Integer, SAM> entry : presetSAMList.entrySet())
            {
                if(entry.getValue().getSamType() == samTypeForTag)
                {
                    sam = entry.getValue();
                    compatibeSAMText += "\n\t" + sam.getSamType() + "(Slot " + sam.getSamIndex() + ")";
                }
            }


            if(!presetSAMList.isEmpty()) {
                if (!compatibeSAMText.isEmpty()) {

                    text += getString(R.string.message_tag_compatible) + " " + compatibeSAMText;

                    /**
                     * Connect to the appropriate SAM based on the Tag detected.
                     *
                    /** Connect [Start] 
                    try {
                        if (!sam.isConnected()) {
                            sam.connect();
                        }
                    } catch (SAMException ex) {
                      SAMResults.getErrorDescription(ex.getResult());
                    }
                    /** Connect [End] */

                    /** Transceive [Start]
                    byte[] response = null;
                    try {
                        response = sam.transceive(transceive_apdu_1, (short) 0, false);
                        response = sam.transceive(transceive_apdu_2, (short) 0, false);
                        response = sam.transceive(transceive_apdu_3, (short) 0, false);
                        response = sam.transceive(transceive_apdu_4, (short) 0, false);
                    } catch (SAMException ex) {
                       SAMResults.getErrorDescription(ex.getResult());
                    }
                    /**Transceive [End] */

                    /** Disconnect [Start]
                    if (sam.isConnected()) {
                        sam.disconnect();
                    }
                    /** Disconnect [End] */

                } else {
                    text += getString(R.string.message_tag_not_compatible);
                }
            }
            updateStatus(text);
        }
    }

    private SAMType findCompatibleSAM(Tag aTag) {
        SAMType lType = SAMType.UNKNOWN;
        detectedTag = "UNKNOWN";

        if (isNDEFTag(aTag)) {
            //NDEF Tag
            detectedTag = "NDEF";
        }else if(isFelicaTag(aTag)){
            lType = SAMType.FELICA;
            detectedTag = "FELICA";
        }else if (isMIFAREClassicTag(aTag)) {
            lType = SAMType.MIFARE;//MIFARE_CLASSIC;
            detectedTag = "MIFARE_CLASSIC";
        }else if (isMIFARETag(aTag)) {
            lType = SAMType.MIFARE;
        }
        return lType;
    }

    private boolean isNDEFTag(Tag tag)
    {
        boolean returnVar = false;
        Ndef lNdefTag = Ndef.get(tag);
        if(lNdefTag != null)
            returnVar = true;
        return returnVar;
    }

    private boolean isFelicaTag(Tag tag)
    {
        boolean returnVar = false;
        NfcF mNfcF = NfcF.get(tag);
        if(mNfcF != null)
        {
            byte [] mPMm;
            mPMm = mNfcF.getManufacturer();
            if(mPMm[0] == 0x01 && mPMm[1]== 0x20)
            {
                returnVar = true;

            }else if(mPMm[0] == 0x03 && mPMm[1] == 0x32)
            {
                returnVar = true;
            }
        }
        return returnVar;
    }

    private boolean isMIFAREClassicTag(Tag tag)
    {
        boolean returnVar = false;
        MifareClassic mifareClassic= MifareClassic.get(tag); //Get MIFARE CLASSIC tag
        if(mifareClassic != null)
            returnVar = true;
        return returnVar;
    }

    private boolean isMIFARETag(Tag tag)
    {
        boolean returnVar = false;
        if (Arrays.asList(tag.getTechList()).contains("android.nfc.tech.NfcA")) {
            NfcA nfc_A = NfcA.get(tag);
            byte[] atqa = nfc_A.getAtqa();
            Integer Sak = Integer.valueOf(""+nfc_A.getSak());
            switch (atqa[1]) {
                case 0x00:
                    if (atqa[0] == 0x44 || atqa[0] == 0x42
                            || atqa[0] == 0x02 || atqa[0] == 0x04) {
                        if(Sak == 0x20) {
                            returnVar = true;
                            detectedTag = "MIFARE_PLUS_SL3";
                        }
                        else if(Sak == 0x10) {
                            returnVar = true;
                            detectedTag = "MIFARE_PLUS_SL2";
                        }
                    }
                    break;
                case 0x03:
                    if (atqa[0] == 0x44 || atqa[0] == 0x4) {
                        returnVar = true;
                        detectedTag = "MIFARE_DESFIRE";
                    }
                default:
                    break;
            }
        }
        return returnVar;
    }
}