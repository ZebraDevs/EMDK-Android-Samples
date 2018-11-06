/*
* Copyright (C) 2015-2017 Zebra Technologies Corp
* All rights reserved.
*/
package com.symbol.emdk.simulscansample1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.symbol.emdk.simulscansample1.R;
import com.symbol.emdk.simulscan.SimulScanRegion;
import com.symbol.emdk.simulscan.RegionType;

public class RegionArrayAdapter extends ArrayAdapter<SimulScanRegion> {

    private List<SimulScanRegion> mRegions = null;

    public RegionArrayAdapter(Context context, int textViewResourceId, List<SimulScanRegion> regions) {
        super(context, textViewResourceId, regions);
        this.mRegions = regions;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        final CharSequence[] omrStatus = {"Checked", "Unchecked"};
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.region_item, parent, false);
        }

        final SimulScanRegion region = mRegions.get(position);
        if (null != region) {
            ImageView imgImage = (ImageView) v.findViewById(R.id.regionImage);
            TextView txtName = (TextView) v.findViewById(R.id.regionName);
            TextView txtData = (TextView) v.findViewById(R.id.regionData);
            TextView txtProcessingMode = (TextView) v.findViewById(R.id.regionProcessingMode);
            TextView txtAbsConf = (TextView) v.findViewById(R.id.regionAbsConf);
            TextView txtRelConf = (TextView) v.findViewById(R.id.regionRelConf);

            if ((null != imgImage) && (null != region.getImage())) {
                ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
                region.getImage().compressToJpeg(new Rect(0, 0, region.getImage().getWidth(), region.getImage().getHeight()), 100, baoStream);
                Bitmap bm = BitmapFactory.decodeStream(new ByteArrayInputStream(baoStream.toByteArray()));
                imgImage.setImageBitmap(bm);
            }
            //if no image, clear ImageView so incorrect images don't show
            else if (region.getImage() == null)
            {
                imgImage.setImageResource(android.R.color.transparent);
            }

            if ((null != txtName) && (null != region.getName())) {
                txtName.setText(region.getName().toString());
            }

            if ((null != txtProcessingMode) && (null != region.getRegionType().name())) {
                txtProcessingMode.setText(region.getRegionType().name());
            }

            if (txtAbsConf != null)
            {
                String absConfText = "AC: ";
                if (region.getAbsoluteConfidence() == -1)
                {
                    absConfText = "";
                }
                else
                {
                    absConfText += region.getAbsoluteConfidence();
                }
                txtAbsConf.setText(absConfText);
            }

            if (txtRelConf != null)
            {
                String relConfText = "RC: ";
                if (region.getRelativeConfidence() == -1)
                {
                    relConfText = "";
                }
                else
                {
                    relConfText += region.getRelativeConfidence();
                }
                txtRelConf.setText(relConfText);
            }

            if (null != txtData) {
                String sText = "";
                if (region.getRegionType() == RegionType.OCR)
                {
                    if (region.getData() != null)
                    {
                        String[] OCRResults = (String[]) region.getData();
                        if (null != OCRResults) {
                            for (int nIndex = 0; nIndex < OCRResults.length; nIndex++)
                            {
                                if (nIndex != 0) //if not first index, prepend with newline
                                    sText = sText.concat("\n");
                                sText = sText.concat(OCRResults[nIndex]);
                            }
                        }
                    }
                    txtData.setText(sText);
                }
                else if (region.getRegionType() == RegionType.OMR)
                {
                    if (region.getData() != null)
                    {
                        int iChecked = (Integer)region.getData();

                        switch (iChecked) {
                            case 1 :
                                sText = sText.concat(omrStatus[0].toString());
                                break;
                            case -1 :
                                sText = sText.concat(omrStatus[1].toString());
                                break;
                            default :
                                break;
                        }
                    }
                    else
                    {
                        sText = sText.concat(omrStatus[1].toString()); //default to unchecked
                    }
                    txtData.setText(sText);
                }
                else if (region.getRegionType() == RegionType.BARCODE)
                {
                    if (region.getData() != null)
                    {
                        try
                        {
                            sText = sText.concat((String) region.getData());
                        }
                        catch (ClassCastException e) //will get here if post-processing is off
                        {
                            sText = "Post-processing is off";
                        }
                    }
                    txtData.setText(sText);
                }
                else if (region.getRegionType() == RegionType.PICTURE)
                {
                    if (region.getData() != null)
                    {
                        //byte[] jpegPicture = (byte[])region.getData();
                        txtData.setText("");
                    }
                }
                else {
                    txtData.setText(region.getData().toString());
                }
            } else {

            }

            if ((null != txtProcessingMode) && (null != region.getRegionType())) {
                txtProcessingMode.setText(region.getRegionType().name());
            }
        }

        return v;
    }

}
