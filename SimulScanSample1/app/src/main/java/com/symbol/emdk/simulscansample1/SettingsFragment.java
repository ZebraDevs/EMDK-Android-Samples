/*
* Copyright (C) 2015-2017 Zebra Technologies Corp
* All rights reserved.
*/
package com.symbol.emdk.simulscansample1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener{

    private final static String TAG = SettingsFragment.class.getCanonicalName();
    private static final String NO_TEMPLATE_FOUND = "(No templates found)";
    private static final String SETTINGS_LAST_TEMPLATE_POS = "lastTemplatePos";

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        MainActivity parentActivity = (MainActivity)getActivity();
        if (key.compareTo(getResources().getString(R.string.pref_key))== 0) {
            ListPreference connectionPref = (ListPreference) findPreference(key);
            Log.d(TAG, "Template PreferenceChanged: " + connectionPref.getValue());
            parentActivity.localSettings.selectedFileIndex = Integer.valueOf(connectionPref.getValue());
            connectionPref.setSummary(connectionPref.getEntry().toString());
            //save last template used
            Editor editor = prefs.edit();
            editor.putInt(SETTINGS_LAST_TEMPLATE_POS, parentActivity.localSettings.selectedFileIndex);
            editor.apply();
        }else if(key.compareTo("timeout_identification")==0){
            EditTextPreference connectionPref = (EditTextPreference) findPreference(key);
            Log.d(TAG, "Identification PreferenceChanged: " + connectionPref.getText());

            int idto = parentActivity.localSettings.identificationTimeout;

            try {
                idto = Integer.parseInt(connectionPref.getText());
            }
            catch(NumberFormatException ex)
            {
                Log.e(TAG, "Invalid identification timeout exception: " + ex.getMessage());
                Toast.makeText(parentActivity.getApplicationContext(), "Invalid identification timeout value", Toast.LENGTH_LONG).show();
            }

            if(idto<5000)
                idto = 5000;

            connectionPref.setText(String.valueOf(idto));
            parentActivity.localSettings.identificationTimeout = idto;

        }else if(key.compareTo("timeout_processing")==0){
            EditTextPreference connectionPref = (EditTextPreference) findPreference(key);
            Log.d(TAG, "Processing PreferenceChanged: " + connectionPref.getText());

            int processingTimeout = parentActivity.localSettings.processingTimeout;;

            try {
                processingTimeout = Integer.parseInt(connectionPref.getText());
            }
            catch(NumberFormatException ex)
            {
                Log.e(TAG, "Invalid processing timeout exception: " + ex.getMessage());
                Toast.makeText(parentActivity.getApplicationContext(), "Invalid processing timeout value", Toast.LENGTH_LONG).show();
            }

            connectionPref.setText(String.valueOf(processingTimeout));
            parentActivity.localSettings.processingTimeout = processingTimeout;

        }else if(key.compareTo("ui_result_confirmation")==0){
            CheckBoxPreference connectionPref = (CheckBoxPreference) findPreference(key);
            Log.d(TAG, "result confrmation PreferenceChanged: " + connectionPref.isChecked());
            parentActivity.localSettings.enableResultConfirmation = connectionPref.isChecked();
        }else if(key.compareTo("auto_capture")==0){
            CheckBoxPreference connectionPref = (CheckBoxPreference) findPreference(key);
            Log.d(TAG, "Auto capture PreferenceChanged: " + connectionPref.isChecked());
            parentActivity.localSettings.enableAutoCapture = connectionPref.isChecked();
        }else if(key.compareTo("feedback_audio")==0){
            CheckBoxPreference connectionPref = (CheckBoxPreference) findPreference(key);
            Log.d(TAG, "Audio PreferenceChanged: " + connectionPref.isChecked());
            parentActivity.localSettings.enableFeedbackAudio = connectionPref.isChecked();
        }else if(key.compareTo("feedback_haptic")==0){
            CheckBoxPreference connectionPref = (CheckBoxPreference) findPreference(key);
            Log.d(TAG, "Haptic PreferenceChanged: " + connectionPref.isChecked());
            parentActivity.localSettings.enableHaptic = connectionPref.isChecked();
        }else if(key.compareTo("feedback_led")==0){
            CheckBoxPreference connectionPref = (CheckBoxPreference) findPreference(key);
            Log.d(TAG, "LED PreferenceChanged: " + connectionPref.isChecked());
            parentActivity.localSettings.enableLED = connectionPref.isChecked();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Get the Preference Category which we want to add the ListPreference
        ListPreference customListPref = (ListPreference) findPreference("pref_template");

        MainActivity parentActivity = (MainActivity)getActivity();

        // Retrieve SimulScan default templates from the system
        RetrieveTemplates();

        if (customListPref != null) {

            List<String> entries = new ArrayList<String>();
            List<String> entryValues = new ArrayList<String>();

            String path = Environment.getExternalStorageDirectory().toString() + "/simulscan/templates";
            Log.d(TAG, "Path: " + path);
            File f = new File(path);

            File file[] = f.listFiles();
            if (file != null) {
                for (int i = 0; i < file.length; i++) {
                    Log.d(TAG, "FileName:" + file[i].getName());
                    //Log.d("Files", "value:" + file[i].getAbsolutePath());
                    entries.add(file[i].getName());
                    entryValues.add(Integer.valueOf(i).toString());
                }
            } else {
                Log.d(TAG, "Cant find folder");
            }

            if (entries.isEmpty()){
                entries.add(NO_TEMPLATE_FOUND);
                entryValues.add("");
            }

            customListPref.setEntries(entries.toArray(new CharSequence[entries.size()]));
            customListPref.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));

            //customListPref.setPersistent(true);

            SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
            int lastTemplatePos = prefs.getInt(SETTINGS_LAST_TEMPLATE_POS, 0);
            if(lastTemplatePos >= entries.size())
                lastTemplatePos = 0;

            customListPref.setValueIndex(lastTemplatePos);
            customListPref.setSummary(entries.get(lastTemplatePos));

            if(entryValues.get(lastTemplatePos).compareTo("") != 0){
                parentActivity.localSettings.fileList = new ArrayList<File>(Arrays.asList(file));
                parentActivity.localSettings.selectedFileIndex = lastTemplatePos;
            }
        }

        EditTextPreference pref1 = (EditTextPreference) findPreference("timeout_identification");
        parentActivity.localSettings.identificationTimeout = Integer.parseInt(pref1.getText());
        EditTextPreference pref2 = (EditTextPreference) findPreference("timeout_processing");
        parentActivity.localSettings.processingTimeout = Integer.parseInt(pref2.getText());
        CheckBoxPreference pref3 = (CheckBoxPreference) findPreference("ui_result_confirmation");
        parentActivity.localSettings.enableResultConfirmation = pref3.isChecked();
        CheckBoxPreference pref4 = (CheckBoxPreference) findPreference("auto_capture");
        parentActivity.localSettings.enableAutoCapture = pref4.isChecked();
        CheckBoxPreference pref6 = (CheckBoxPreference) findPreference("feedback_audio");
        parentActivity.localSettings.enableFeedbackAudio = pref6.isChecked();
        CheckBoxPreference pref7 = (CheckBoxPreference) findPreference("feedback_haptic");
        parentActivity.localSettings.enableHaptic = pref7.isChecked();
        CheckBoxPreference pref8 = (CheckBoxPreference) findPreference("feedback_led");
        parentActivity.localSettings.enableLED = pref8.isChecked();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    public void RetrieveTemplates() {

        File source = new File("/enterprise/device/settings/datawedge/templates");
        File dest = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/simulscan/templates");

        try {
            copyTemplateDirectory(source, dest);
        } catch (IOException e) {
            Log.e(TAG, "Exception while retrieving templates : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void copyTemplateDirectory(File sourceLocation, File targetLocation)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (int i = 0; i < sourceLocation.listFiles().length; i++) {

                // Skip templates.properties file and copy only the template (XML) files
                if (!children[i].contains("templates.properties")) {
                    copyTemplateDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
                }
            }
        } else {

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from input stream to output stream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            in.close();
            out.close();
        }
    }
}
