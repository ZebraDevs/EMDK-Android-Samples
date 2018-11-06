/*
* Copyright (C) 2015-2017 Zebra Technologies Corp
* All rights reserved.
*/
package com.symbol.profileappmgrsample1;

import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.text.TextUtils;
import android.util.Xml;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity implements EMDKListener, OnCheckedChangeListener {

    //Assign the profile name used in EMDKConfig.xml
    private String profileName = "AppMgrProfile-1";

    //Declare a variable to store ProfileManager object
    private ProfileManager profileManager = null;

    //Declare a variable to store EMDKManager object
    private EMDKManager emdkManager = null;

    private TextView statusTextView = null;
    private TextView helpTextView = null;
    private RadioGroup appRadioGroup = null;

    private String action = "";
    private String name = "";

    private boolean bInstall = false;

    // Provides the error type for characteristic-error
    private String errorType = "";

    // Provides the parm name for parm-error
    private String parmName = "";

    // Provides error description
    private String errorDescription = "";

    // Provides error string with type/name + description
    private String errorString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = (TextView) findViewById(R.id.textViewStatus);
        helpTextView = (TextView) findViewById(R.id.textViewHelp);
        appRadioGroup = (RadioGroup)findViewById(R.id.radioGroupApp);

        appRadioGroup.setOnCheckedChangeListener(this);

        addSetButtonListener();

        //The EMDKManager object will be created and returned in the callback.
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);

        //Check the return status of EMDKManager object creation.
        if(results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
            //EMDKManager object creation success
        }else {
            //EMDKManager object creation failed
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        //Clean up the objects created by EMDK manager
        if (profileManager != null)
            profileManager = null;

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
    public void onClosed() {

        //This callback will be issued when the EMDK closes unexpectedly.
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }

        statusTextView.setText("Status: " + "EMDK closed unexpectedly! Please close and restart the application.");
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {

        //This callback will be issued when the EMDK is ready to use.
        statusTextView.setText("EMDK open success.");

        this.emdkManager = emdkManager;

        //Get the ProfileManager object to process the profiles
        profileManager = (ProfileManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.PROFILE);
    }

    private void addSetButtonListener() {

        Button setButton = (Button)findViewById(R.id.buttonSet);

        setButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                int radioid = appRadioGroup.getCheckedRadioButtonId();

                switch (radioid) {
                    case R.id.radioInstall:
                        action = "Install";
                        bInstall = true;
                        break;
                    case R.id.radioUpgrade:
                        action = "Upgrade";
                        bInstall = true;
                        break;
                    case R.id.radioUninstall:
                        action = "Uninstall";
                        bInstall = false;
                        break;
                }

                if(readValues())
                    modifyProfile_XMLString();
                else
                    statusTextView.setText("The above field cannot be empty.");
            }
        });

    }

    private boolean readValues() {

        EditText nameEditText = (EditText)findViewById(R.id.editTextName);
        name = nameEditText.getText().toString().trim();

        if ((name != null) && (name.length() > 0)) {
            return true;
        }
        return false;
    }

    private void modifyProfile_XMLString() {

        statusTextView.setText("");
        errorType = "";
        parmName = "";
        errorDescription = "";
        errorString = "";

        //Prepare XML to modify the existing profile
        String[] modifyData = new String[1];

        if (bInstall) {
            modifyData[0]=
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                            "<characteristic type=\"Profile\">" +
                            "<parm name=\"ProfileName\" value=\"AppMgrProfile-1\"/>" +
                            "<characteristic type=\"AppMgr\" version=\"0.5\">" +
                            "<parm name=\"Action\" value=\"" + action + "\"/>" +
                            "<parm name=\"APK\" value=\"" + name + "\"/>" +
                            "</characteristic>" +
                            "</characteristic>";
        }
        else {
            modifyData[0]=
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                            "<characteristic type=\"Profile\">" +
                            "<parm name=\"ProfileName\" value=\"AppMgrProfile-1\"/>" +
                            "<characteristic type=\"AppMgr\" version=\"0.5\">" +
                            "<parm name=\"Action\" value=\"" + action + "\"/>" +
                            "<parm name=\"Package\" value=\"" + name + "\"/>" +
                            "</characteristic>" +
                            "</characteristic>";
        }

        new ProcessProfileTask().execute(modifyData[0]);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int id) {

        switch(id) {
            case R.id.radioInstall:
            case R.id.radioUpgrade:
                bInstall = true;
                helpTextView.setText("APK Name (Ex: /sdcard/TestApp.apk):");
                break;

            case R.id.radioUninstall:
                bInstall = false;
                helpTextView.setText("Package Name (Ex: com.example.TestApp):");
                break;
        }

    }

    // Method to parse the XML response using XML Pull Parser
    public void parseXML(XmlPullParser myParser) {
        int event;
        try {
            // Retrieve error details if parm-error/characteristic-error in the response XML
            event = myParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String name = myParser.getName();
                switch (event) {
                    case XmlPullParser.START_TAG:

                        if (name.equals("parm-error")) {
                            parmName = myParser.getAttributeValue(null, "name");
                            errorDescription = myParser.getAttributeValue(null, "desc");
                            errorString = " (Name: " + parmName + ", Error Description: " + errorDescription + ")";
                            return;
                        }

                        if (name.equals("characteristic-error")) {
                            errorType = myParser.getAttributeValue(null, "type");
                            errorDescription = myParser.getAttributeValue(null, "desc");
                            errorString = " (Type: " + errorType + ", Error Description: " + errorDescription + ")";
                            return;
                        }
                        break;
                    case XmlPullParser.END_TAG:

                        break;
                }
                event = myParser.next();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private class ProcessProfileTask extends AsyncTask<String, Void, EMDKResults> {

        @Override
        protected EMDKResults doInBackground(String... params) {

            //Call process profile to modify the profile of specified profile name
            EMDKResults results = profileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.SET, params);

            return results;
        }

        @Override
        protected void onPostExecute(EMDKResults results) {

            super.onPostExecute(results);

            String resultString = "";

            //Check the return status of processProfile
            if(results.statusCode == EMDKResults.STATUS_CODE.CHECK_XML) {

                // Get XML response as a String
                String statusXMLResponse = results.getStatusString();

                try {
                    // Create instance of XML Pull Parser to parse the response
                    XmlPullParser parser = Xml.newPullParser();
                    // Provide the string response to the String Reader that reads
                    // for the parser
                    parser.setInput(new StringReader(statusXMLResponse));
                    // Call method to parse the response
                    parseXML(parser);

                    if ( TextUtils.isEmpty(parmName) && TextUtils.isEmpty(errorType) && TextUtils.isEmpty(errorDescription) ) {

                        resultString = "Profile update success.";
                    }
                    else {

                        resultString = "Profile update failed." + errorString;
                    }

                } catch (XmlPullParserException e) {
                    resultString =  e.getMessage();
                }
            }

            statusTextView.setText(resultString);
        }
    }

}
