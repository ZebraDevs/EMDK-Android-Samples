/*
* Copyright (C) 2015-2019 Zebra Technologies Corporation and/or its affiliates
* All rights reserved.
*/
package com.symbol.profilegprsmgrsample1;

import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.profilegprssample1.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.text.TextUtils;
import android.util.Xml;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

public class MainActivity extends Activity implements EMDKListener{

    //Assign the profile name used in EMDKConfig.xml
    private String profileName = "GPRSProfile-1";

    //Declare a variable to store ProfileManager object
    private ProfileManager profileManager = null;

    //Declare a variable to store EMDKManager object
    private EMDKManager emdkManager = null;

    private TextView statusTextView = null;

    private String APN = "";
    private String AccessPoint = "";
    private String UserName = "";
    private String Password = "";
    private int ReplaceExisting = 0;
    private int MakeDefault = 0;

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
        ADD_REPLACE(1),
        REMOVE(2);

        private int value;

        private Action(int v)
        {
            value = v;
        }

        public int getValue()
        {
            return value;
        }
    };


    private Action action = Action.ADD_REPLACE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = (TextView) findViewById(R.id.textViewStatus);

        addSetButtonListener();
        addsetOnCheckedChangeListener();

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

                if (readValues())
                    modifyProfile_XMLString();
                else
                    statusTextView.setText("The APN Name and Access Point (applicable to Add/Replace only) fields cannot be empty.");
            }
        });

    }

    private void addsetOnCheckedChangeListener() {

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                EditText ap = (EditText)findViewById(R.id.EditTextAccessPoint);
                EditText user = (EditText)findViewById(R.id.EditTextUserName);
                EditText pass = (EditText)findViewById(R.id.EditTextPassword);
                CheckBox replace = (CheckBox)findViewById(R.id.checkBoxReplace);
                CheckBox make_default = (CheckBox)findViewById(R.id.checkBoxMakeDefault);
                TextView apText = (TextView)findViewById(R.id.textViewAccessPoint);
                TextView userText = (TextView)findViewById(R.id.TextViewUserName);
                TextView passText = (TextView)findViewById(R.id.TextViewPassword);

                if (R.id.radio0 == checkedId)
                {
                    ap.setEnabled(true);
                    user.setEnabled(true);
                    pass.setEnabled(true);
                    replace.setEnabled(true);
                    make_default.setEnabled(true);
                    apText.setEnabled(true);
                    userText.setEnabled(true);
                    passText.setEnabled(true);
                }
                else
                {
                    ap.setEnabled(false);
                    user.setEnabled(false);
                    pass.setEnabled(false);
                    replace.setEnabled(false);
                    make_default.setEnabled(false);
                    apText.setEnabled(false);
                    userText.setEnabled(false);
                    passText.setEnabled(false);
                }
            }
        });
    }

    private boolean readValues() {


        EditText APNEditText = (EditText)findViewById(R.id.editTextAPN);
        APN = APNEditText.getText().toString().trim();

        if ((APN == null) || (APN.length() == 0))
        {
            return false;
        }

        RadioGroup radiogroup = (RadioGroup)findViewById(R.id.radioGroup1);
        if (R.id.radio0 == radiogroup.getCheckedRadioButtonId())
        {
            action = Action.ADD_REPLACE;

            EditText APEditText = (EditText)findViewById(R.id.EditTextAccessPoint);
            AccessPoint = APEditText.getText().toString().trim();

            if ((AccessPoint == null) || (AccessPoint.length() == 0))
            {
                return false;
            }

            EditText UserEditText = (EditText)findViewById(R.id.EditTextUserName);
            UserName = UserEditText.getText().toString().trim();

            EditText PassEditText = (EditText)findViewById(R.id.EditTextPassword);
            Password = PassEditText.getText().toString().trim();

            CheckBox cb = (CheckBox)findViewById(R.id.checkBoxReplace);
            if (cb.isChecked())
            {
                ReplaceExisting = 1;
            }
            else
            {
                ReplaceExisting = 0;
            }
            cb = (CheckBox)findViewById(R.id.checkBoxMakeDefault);
            if (cb.isChecked())
            {
                MakeDefault = 1;
            }
            else
            {
                MakeDefault = 0;
            }
        }
        else
        {
            action = Action.REMOVE;
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
                        "<parm name=\"ProfileName\" value=\"GPRSProfile-1\"/>" +
                        "<characteristic type=\"GprsMgr\" version=\"0.2\">" +
                        "<parm name=\"GprsAction\" value=\"" + action.getValue() + "\"/>";


        if (action == Action.ADD_REPLACE)
        {
            modifyData[0] +=    "<parm name=\"GprsCarrier\" value=\"0\"/>" +
                    "<characteristic type=\"gprs-details\">" +
                    "<parm name=\"ApnName\" value=\"" + APN + "\"/>" +
                    "<parm name=\"ReplaceIfExisting\" value=\"" + ReplaceExisting + "\"/>" +
                    "<parm name=\"MakeDefault\" value=\"" + MakeDefault + "\"/>" +
                    "</characteristic>" +
                    "<characteristic type=\"custom-details\">" +
                    "<parm name=\"CustomAccessPoint\" value=\"" + AccessPoint + "\"/>" +
                    "<parm name=\"CustomUserName\" value=\"" + UserName + "\"/>" +
                    "<parm name=\"CustomPassword\" value=\"" + Password + "\"/>" +
                    "</characteristic>";
        }
        else
        {
            modifyData[0] +=	"<characteristic type=\"gprs-details\">" +
                    "<parm name=\"ApnName\" value=\"" + APN + "\"/>" +
                    "</characteristic>";
        }

        modifyData[0] +=	"</characteristic>" +
                "</characteristic>";

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
