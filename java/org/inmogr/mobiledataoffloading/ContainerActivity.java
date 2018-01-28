package org.inmogr.mobiledataoffloading;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ContainerActivity extends AppCompatActivity {

    private TextView tvMain, tvResponse;
    private String viewing;
    private AdapterViewCustom adapterViewCustom = new AdapterViewCustom();

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            viewing = item.getTitle().toString();
            findViewById(R.id.containerSpinnerNumerical).setVisibility(View.GONE);
            String tvMainText;
            switch (item.getItemId()) {
                case R.id.navigation_numerical:
                    tvMainText = getString(R.string.containerTVMain) +" "+ getString(R.string.containerTVMainNumerical);
                    tvMain.setText(tvMainText);
                    findViewById(R.id.containerSpinnerNumerical).setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_game:
                    tvMainText = getString(R.string.containerTVMain) +" "+ getString(R.string.containerTVMainGame);
                    tvMain.setText(tvMainText);
                    return true;
                case R.id.navigation_sorting:
                    tvMainText = getString(R.string.containerTVMain) +" "+ getString(R.string.containerTVMainSorting);
                    tvMain.setText(tvMainText);
                    return true;
                case R.id.navigation_searching:
                    tvMainText = getString(R.string.containerTVMain) +" "+ getString(R.string.containerTVMainSearch);
                    tvMain.setText(tvMainText);
                    return true;
                case R.id.navigation_files:
                    tvMainText = getString(R.string.containerTVMain) +" "+ getString(R.string.containerTVMainFiles);
                    tvMain.setText(tvMainText);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);
        setTitle("Select Algorithm");

        String tvMainText = getString(R.string.containerTVMain) +" "+ getString(R.string.containerTVMainNumerical);
        viewing = getString(R.string.title_numerical);
        tvMain = findViewById(R.id.containerTVMain);
        tvResponse = findViewById(R.id.containerTVResponse);
        tvResponse.setMovementMethod(new ScrollingMovementMethod());
        tvMain.setText(tvMainText);
        ((Spinner) findViewById(R.id.containerSpinnerNumerical)).setOnItemSelectedListener(adapterViewCustom);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        vConnection.init(getCacheDir());
    }

    public void perform(View view) {
        requestTask = "HasResources";
        tvResponse.setText("");
        JsonObject jsonObject = new JsonObject();
        if (isRequest) {
            Toast.makeText(this, "Please wait the task is being executed", Toast.LENGTH_SHORT).show();
            return;
        }
        requestTimer = new Date().getTime();
        isRequest = true;
        if (viewing.equals(getString(R.string.title_numerical))) {
            requestTaskWaiting = adapterViewCustom.getSelected();
            volley();
        }
        else if (viewing.equals(getString(R.string.title_game))) {
            requestTaskWaiting = "NQueenPuzzle";
            jsonObject.addProperty("n" , 8);
            requestData = jsonObject.toString();
            volley();
        }
        else if (viewing.equals(getString(R.string.title_sorting))) {
            requestTaskWaiting = "QuickSort";
            int[] array = {10,7,8,9,1,5};
            jsonObject.addProperty("array" , Arrays.toString(array));
            requestData = jsonObject.toString();
            volley();
        }
        else if (viewing.equals(getString(R.string.title_searching))) {
            requestTaskWaiting = "BinarySearch";
            int[] array1 = {2,3,4,10,40};
            jsonObject.addProperty("array" , Arrays.toString(array1));
            jsonObject.addProperty("key" , 40);
            requestData = jsonObject.toString();
            volley();
        }
        else if (viewing.equals(getString(R.string.title_files))) {
            requestTaskWaiting = "CreateWordDocument";
            volley();
        }
    }

    private void volley() {
        Map<String , String> DATA = new HashMap<>();
        DATA.put(ParaCloudLet , requestCloudLet);
        DATA.put(ParaTask , requestTask);
        DATA.put(ParaRequest , requestData);
        vConnection.request(this , WEB_APP, DATA);
    }

    private boolean isRequest = false;
    private long requestTimer;
    private String requestCloudLet = "CloudLet1";
    private String requestTask = "HasResources";
    private String requestTaskWaiting = "";
    private String requestData = "";
    private String responseLog = "";

    private VConnection vConnection = new VConnection() {
        int counter = 0;
        @Override
        void onResponse(String firedBy, String response) {
            counter++;
            Log.d("MyLog" , ""+counter);
            Log.d("MyLog" , response);
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.getString("Result").length() > 0)
                    response = jsonObject.getString("Result");
            } catch (JSONException ignored) {}
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.getString("Error").length() > 0)
                    response = "Error: " + jsonObject.getString("Error");
            } catch (JSONException ignored) {}
            response = response + "\n"
                    + requestCloudLet + "\n"
                    + requestTask + "\n"
                    + requestTaskWaiting + "\n"
                    + requestData;
            responseLog = response + "\n\n"
                        + responseLog;
            tvResponse.setText(response);
            isRequest = false;
            if (response.toLowerCase().contains("available")) {
                requestTask = requestTaskWaiting;
                volley();
            }
            else if (response.toLowerCase().contains("no sufficient resources")) {
                requestTask = "HasResources";
                switch (requestCloudLet) {
                    case "CloudLet1":
                        requestCloudLet = "CloudLet2";
                        volley();
                        break;
                    case "CloudLet2":
                        requestCloudLet = "CloudLet3";
                        volley();
                        break;
                    case "CloudLet3":
                        requestCloudLet = "Clouds";
                        requestTask = requestTaskWaiting;
                        volley();
                        break;
                }
            }
            else {
                double diff = new Date().getTime() - requestTimer;
                diff /= 1000;
                response = "Total Time Required: " + diff + " sec\n\n"
                        + tvResponse.getText();
                responseLog = "Total Time Required: " + diff + " sec\n\n"
                        + responseLog;
                tvResponse.setText(response);
                generateNote(responseLog);
            }
        }
    };

    private static final String WEB_APP = "https://inmogr.azurewebsites.net/DataOffloading";
    private static final String ParaCloudLet = "CloudLet";
    private static final String ParaTask = "Task";
    private static final String ParaRequest = "request";


    public void generateNote(String sBody) {
        try {
//            File root = new File(Environment.getDataDirectory(), "Mobile Data Offloading");
            File root = new File(Environment.getExternalStorageDirectory(), "");
            if (!root.exists()) {
                //noinspection ResultOfMethodCallIgnored
                root.mkdirs();
            }
            //noinspection SpellCheckingInspection
            File gpxfile = new File(root, "Logs.txt");
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(sBody);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
