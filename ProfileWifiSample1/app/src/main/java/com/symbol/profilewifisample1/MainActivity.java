/*
* Copyright (C) 2015-2017 Zebra Technologies Corp
* All rights reserved.
*/
package com.symbol.profilewifisample1;

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
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity implements EMDKListener{

    //Assign the profile name used in EMDKConfig.xml
    private String profileName = "WifiProfile-1";

    //Declare a variable to store ProfileManager object
    private ProfileManager profileManager = null;

    //Declare a variable to store EMDKManager object
    private EMDKManager emdkManager = null;

    private TextView statusTextView = null;

    private String SSID = "";

    // Provides the error type for characteristic-error
    private String errorType = "";

    // Provides the parm name for parm-error
    private String parmName = "";

    // Provides error description
    private String errorDescription = "";

    // Provides error string with type/name + description
    private String errorString = "";

    private enum Action
    {
        DO_NOTHING,
        ADD,
        REMOVE,
        CONNECT,
        DISCONNECT,
        ENABLE,
        DISABLE
    };

    private enum RadioState
    {
        ENABLE,
        DISABLE
    };


    private Action action = Action.ADD;
    private RadioState radioState = RadioState.ENABLE;

    private String [] ActionStrings = {"Do Nothing", "Add", "Remove", "Connect", "Disconnect", "Enable", "Disable"};
    private String [] RadioStateStrings = {"enable", "disable"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = (TextView) findViewById(R.id.textViewStatus);

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

        //Get the ProfileManager object to process the profiles.
        profileManager = (ProfileManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.PROFILE);

    }

    private void addSetButtonListener() {

        Button setButton = (Button)findViewById(R.id.buttonSet);

        setButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (readValues())
                    modifyProfile_XMLString();
                else
                    statusTextView.setText("The SSID field cannot be empty.");
            }
        });

    }

    private boolean readValues() {

        Spinner spinner = (Spinner)findViewById(R.id.spinner1 );
        action = Action.values()[spinner.getSelectedItemPosition()];

        EditText SSIDEditText = (EditText)findViewById(R.id.editTextSSID);
        SSID = SSIDEditText.getText().toString().trim();

        if ((action != Action.DO_NOTHING) && ((SSID == null) || (SSID.length() == 0)))
        {
            return false;
        }

        RadioGroup radiogroup = (RadioGroup)findViewById(R.id.radioGroup1);

        if (R.id.radio0 == radiogroup.getCheckedRadioButtonId())
        {
            radioState = RadioState.ENABLE;
        }
        else
        {
            radioState = RadioState.DISABLE;
        }

        return true;
    }

    private void modifyProfile_XMLString() {

        statusTextView.setText("");
        errorType = "";
        parmName = "";
        errorDescription = "";
        errorString = "";

        //Prepare XML to modify the existing profile
        String[] modifyData = new String[1];
        modifyData[0]=
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                        "<characteristic type=\"Profile\">" +
                        "<parm name=\"ProfileName\" value=\"WifiProfile-1\"/>" +
                        "<characteristic type=\"Wi-Fi\" version=\"2.7\">" +
                        "<characteristic type=\"System\">" +
                        "<parm name=\"WiFiAction\" value=\"" + RadioStateStrings[radioState.ordinal()] + "\"/>" +
                        "</characteristic>";

        if (action != Action.DO_NOTHING)
        {

            modifyData[0] +=  "<parm name=\"NetworkAction\" value=\"" + ActionStrings[action.ordinal()] + "\"/>" +
                    "<characteristic type=\"network-profile\">" +
                    "<parm name=\"SSID\" value=\"" + SSID + "\"/>";

            switch (action)
            {
                case ADD:
                    modifyData[0] += "<parm name=\"SecurityMode\" value=\"0\"/>" +
                            "<parm name=\"UseDHCP\" value=\"1\"/>" +
                            "<parm name=\"UseProxy\" value=\"0\"/>";
					/*
					 * Intentional drop-through
					 */
                default:
                    modifyData[0] += "</characteristic>";

            }
        }

        modifyData[0] += "</characteristic></characteristic>";

        new ProcessProfileTask().execute(modifyData[0]);
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
