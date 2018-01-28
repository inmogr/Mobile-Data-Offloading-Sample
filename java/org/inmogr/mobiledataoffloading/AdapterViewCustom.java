package org.inmogr.mobiledataoffloading;

import android.view.View;
import android.widget.AdapterView;

/**
 * Created by INMOGR on 1/17/2018.
 * This class provides service to all Spinners
 */

public class AdapterViewCustom implements AdapterView.OnItemSelectedListener {
    private String selected = "Gaussian Elimination";
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selected = parent.getItemAtPosition(position).toString().replace(" " , "");
    }
    public void onNothingSelected(AdapterView<?> parent) {}
    String getSelected() {
        return selected;
    }
}
