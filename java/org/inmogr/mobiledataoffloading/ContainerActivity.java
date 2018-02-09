package org.inmogr.mobiledataoffloading;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.inmogr.mobiledataoffloading.algos.BinarySearch;
import org.inmogr.mobiledataoffloading.algos.GaussianElimination;
import org.inmogr.mobiledataoffloading.algos.NQueenPuzzle;
import org.inmogr.mobiledataoffloading.algos.QuickSort;
import org.inmogr.mobiledataoffloading.algos.RungeKutta;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ContainerActivity extends AppCompatActivity {

    // TextView that shown on the main screen (UI)
    private TextView tvMain, tvResponse;

    // holds the section sequence currently being viewed
    private String viewing;
    // holds the current data transmitting rate
    private double bandwidth = 0;

    private AdapterViewCustom adapterViewCustom = new AdapterViewCustom();
    private Broadcaster broadcastReceiver = new Broadcaster() {
        @Override
        public void showResponse(int level) {
            // store the current batter level in Logs.txt
            generateNote("Battery Level is" + level + "%");
            // check if the bandwidth is higher than 500KB and battery level at least 25%
            if (level >= 25 && bandwidth > 500) {
                // store a log in Logs.txt stating that the task will be offloaded
                generateNote("Executing task onClouds");
                // execute the task on clouds
                onClouds();
            }
            // by arriving to this line this means that the bandwidth or the battery level is not sufficient for data offloading
            // therefore, check if the device has at least 10% battery
            else if (level >= 10) {
                // store a log in Logs.txt stating that the task will be executed offline
                generateNote("Executing task onDevice");
                // execute the task on device
                onDevice();
            }
            // by arriving here this means the task failed to be offloaded due in sufficient resources
            else {
                // inform users that the task failed to be executed due in sufficient resources
                Toast.makeText(ContainerActivity.this, "Sorry! No sufficient Battery", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        /*
         * this is a UI operation area not important in terms of task execution
         */
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

        // check if the bandwidth data available
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                returnHome();
            } else {
                // get the bandwidth
                bandwidth = extras.getDouble("bandwidth");
            }
        } else {
            returnHome();
        }

        /*
         * UI area not important
         */
        String tvMainText = getString(R.string.containerTVMain) +" "+ getString(R.string.containerTVMainNumerical);
        viewing = getString(R.string.title_numerical);
        tvMain = findViewById(R.id.containerTVMain);
        tvResponse = findViewById(R.id.containerTVResponse);
        tvResponse.setMovementMethod(new ScrollingMovementMethod());
        tvMain.setText(tvMainText);
        ((Spinner) findViewById(R.id.containerSpinnerNumerical)).setOnItemSelectedListener(adapterViewCustom);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // establish connection and set cache folder
        vConnection.init(getCacheDir());
    }

    // since bandwidth data not available return home
    private void returnHome() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // "perform" button ==> on click ==> this function will be executed
    public void perform(View view) {
        // if the a task currently is executed the system will reject queuing another one to get accurate execution time
        if (isRequest) {
            // tell user to wait for the current task to be completed
            Toast.makeText(this, "Please wait the task is being executed", Toast.LENGTH_SHORT).show();
            return;
        }
        // init some variables
        responseLog = "";
        // init some variables
        requestTimer = new Date().getTime();
        // init some variables
        isRequest = true;
        // init some variables
        this.broadcastReceiver.setNewRequest();
        // check battery and decide how to execute the task
        this.registerReceiver(this.broadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void onClouds() {
        // check if the current cloud let has sufficient resources, requires informing clouds what is requestTaskWaiting
        requestTask = "HasResources";
        // init some variables
        tvResponse.setText("");
        // init some variables
        JsonObject jsonObject = new JsonObject();
        // decide what is requestTaskWaiting and then volley
        if (viewing.equals(getString(R.string.title_numerical))) {
            requestTaskWaiting = adapterViewCustom.getSelected();
            requestData = "";
            volley();
        }
        // decide what is requestTaskWaiting and back some data then volley
        else if (viewing.equals(getString(R.string.title_game))) {
            requestTaskWaiting = "NQueenPuzzle";
            jsonObject.addProperty("n" , 8);
            requestData = jsonObject.toString();
            volley();
        }
        // decide what is requestTaskWaiting and back some data then volley
        else if (viewing.equals(getString(R.string.title_sorting))) {
            requestTaskWaiting = "QuickSort";
            int[] array = {10,7,8,9,1,5};
            jsonObject.addProperty("array" , Arrays.toString(array));
            requestData = jsonObject.toString();
            volley();
        }
        // decide what is requestTaskWaiting and back some data then volley
        else if (viewing.equals(getString(R.string.title_searching))) {
            requestTaskWaiting = "BinarySearch";
            int[] array1 = {2,3,4,10,40};
            jsonObject.addProperty("array" , Arrays.toString(array1));
            jsonObject.addProperty("key" , 40);
            requestData = jsonObject.toString();
            volley();
        }
        // decide what is requestTaskWaiting and back some data then volley
        // this has been cancelled already
        else if (viewing.equals(getString(R.string.title_files))) {
            requestTaskWaiting = "CreateWordDocument";
            requestData = "";
            volley();
        }
    }

    private void onDevice() {
        // init some variables
        tvResponse.setText("");
        // init some variables
        String response = "";
        // inform the system to use the device for execution
        requestCloudLet = "Performed on device";
        // init some variables
        requestTaskWaiting = "";
        // decide what is requestTaskWaiting and back some data then execute offline
        if (viewing.equals(getString(R.string.title_numerical))) {
            requestTask = adapterViewCustom.getSelected();
            if (adapterViewCustom.getSelected().equals("Gaussian Elimination")) {
                response = new GaussianElimination().execute();
            }
            else {
                response = new RungeKutta().execute();
            }
        }
        // decide what is requestTaskWaiting and back some data then execute offline
        else if (viewing.equals(getString(R.string.title_game))) {
            requestTask = "NQueenPuzzle";
            int n = 8;
            response = new NQueenPuzzle().execute(n);
        }
        else if (viewing.equals(getString(R.string.title_sorting))) {
            requestTask = "QuickSort";
            int[] array = {10,7,8,9,1,5};
            response = new QuickSort().execute(array);
        }
        // decide what is requestTaskWaiting and back some data then execute offline
        else if (viewing.equals(getString(R.string.title_searching))) {
            requestTask = "BinarySearch";
            int[] array = {10,7,8,9,1,5};
            int key = 40;
            response = new BinarySearch().execute(array , key);
        }
        // decide what is requestTaskWaiting and back some data then execute offline
        // this has been cancelled already
        else if (viewing.equals(getString(R.string.title_files))) {
            requestTask = "CreateWordDocument";
//            File root = new File(Environment.getExternalStorageDirectory(), "");
//            try {
//                response = new CreateWordDocument().execute(root, "Test" , "This is some text to test");
//            } catch (Exception empty) { response = ""; }
            response = "Error caused by Apache";
        }
        // prepare the results for user
        JsonObject res = new JsonObject();
        res.addProperty("Result", response);
        response = res.toString();
        // update user with results
        updateTextView(response);
        // save the logs
        updateLogTxt();
    }

    /*
     * this functions packs the data and volley to clouds
     */
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
        @Override
        void onResponse(String firedBy, String response) {
            // update the user with the results
            updateTextView(response);
            // checks if the cloud let responded that it has available resources to execute the task
            if (response.toLowerCase().contains("available")) {
                // provide the task and data
                requestTask = requestTaskWaiting;
                // offload the data
                volley();
            }
            // if it does not have resources
            else if (response.toLowerCase().contains("no sufficient resources")) {
                // prepare the same question for another cloud let
                requestTask = "HasResources";
                /*
                 * choose the cloud let you want based on sequence till finally u reach to main clouds
                 */
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
                // once the task is finished then save that in a log file
                updateLogTxt();
            }
        }
    };

    private void updateLogTxt() {
        double diff = new Date().getTime() - requestTimer;
        diff /= 1000;
        String response = "Total Time Required: " + diff + " sec\n\n"
                + tvResponse.getText();
        responseLog = "Total Time Required: " + diff + " sec\n\n"
                + responseLog;
        tvResponse.setText(response);
        generateNote(responseLog);
    }

    private void updateTextView(String response) {
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
    }

    private static final String WEB_APP = "https://inmogr.azurewebsites.net/DataOffloading";
    private static final String ParaCloudLet = "CloudLet";
    private static final String ParaTask = "Task";
    private static final String ParaRequest = "request";


    public void generateNote(String sBody) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "");
            if (!root.exists()) {
                //noinspection ResultOfMethodCallIgnored
                root.mkdirs();
            }
            //noinspection SpellCheckingInspection
            File aFile = new File(root, "Logs.txt");
            String text = readTextFile(Uri.fromFile(aFile));
            FileWriter writer = new FileWriter(aFile);
            text = sBody + text;
//            writer.append(text);
            writer.write(text);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readTextFile(Uri uri){
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            //noinspection ConstantConditions
            reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.lineSeparator());
            }
        }
        catch (Exception ignored) {}
        finally {
            if (reader != null){
                try {
                    reader.close();
                }
                catch (Exception ignored) {}
            }
        }
        return builder.toString();
    }

}
