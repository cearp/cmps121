package com.dealfaro.luca.checkin;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;


public class MainActivity extends ActionBarActivity {

    Location lastLocation;
    private double lastAccuracy = (double) 1e10;
    private long lastAccuracyTime = 0;

    private static final String LOG_TAG = "lclicker";

    private static final float GOOD_ACCURACY_METERS = 100;

    private static final String SIGNING_KEY = "Pasta alla matriciana";
    private String signingKey;

    private static final String SERVER_URL_PREFIX = "https://luca-test.appspot.com/attendance/default/";

    // To remember the favorite account.
    public static final String PREF_ACCOUNT = "pref_account";

    // Uploader.
    private ServerCall uploader;

    // Remember whether we have already successfully checked in.
    private boolean checkinSuccessful = false;

    ArrayList<String> accountList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Creates the real signing key.
        signingKey = createSigningKey();
    }

    private String createSigningKey() {
        return SIGNING_KEY.replace("e", "q");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Sets the spinner options.
        Spinner sp = (Spinner) findViewById(R.id.spinner);
        accountList = getAccounts();
        // Builds an adapter between accountList and the spinner, using R.layout.spinner_layout
        // ArrayAdapter<String> myAdapter = ...
        // sp.setAdapter(myAdapter);
        // Reads the preferences, to set the last preferred account as the default one.
    }

    /**
     * Gets the list of accounts for the user.
     * @return the list of user accounts.
     */
    private ArrayList<String> getAccounts() {
        // From http://stackoverflow.com/questions/2112965/how-to-get-the-android-devices-primary-e-mail-address
        ArrayList<String> emails = new ArrayList<String>();
        Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
        Account[] accounts = AccountManager.get(this).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                String possibleEmail = account.name;
                emails.add(possibleEmail);
            }
        }
        return emails;
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Add your code here.
        // Set some default...
        // Then start to request location updates, directing them to locationListener.
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

    }

    @Override
    protected void onPause() {
        // Stops the location updates.
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(locationListener);
        // Disables the submit button.
        Button submitButton = (Button) findViewById(R.id.button);
        submitButton.setEnabled(false);
        // Stops the upload if any.
        if (uploader != null) {
            uploader.cancel(true);
            uploader = null;
        }
        super.onPause();
    }

    /**
     * Listenes to the location, and gets the most precise recent location.
     */
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            // Do something with the location you receive.
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };

    /**
     * Displays the accuracy to the user.
     * @param location
     */
    private void displayAccuracy(Location location) {
        // Displays the accuracy.
        TextView labelView = (TextView) findViewById(R.id.locationView);
        TextView accView = (TextView) findViewById(R.id.accuracyView);
        if (location == null) {
            labelView.setVisibility(View.INVISIBLE);
            accView.setVisibility(View.INVISIBLE);
        } else {
            String acc = String.format("%5.1f m", location.getAccuracy());
            labelView.setVisibility(View.VISIBLE);
            accView.setText(acc);
            accView.setVisibility(View.VISIBLE);
            // Colors the accuracy.
            if (location.getAccuracy() < GOOD_ACCURACY_METERS) {
                accView.setTextColor(Color.parseColor("#006400")); // Dark green
            } else {
                accView.setTextColor(Color.parseColor("#8b0000")); // Dark red
            }
        }
    }


    public void clickButton(View v) {
        if (lastLocation == null) {
            return;
        }

        // Disables the button.
        Button submitButton = (Button) findViewById(R.id.button);
        submitButton.setEnabled(false);

        // Prepares the payload.
        Payload p = new Payload();
        // Fill in the payload.
        // ...

        // Builds a single string, using Gson.
        Gson gson = new Gson();
        String payload = gson.toJson(p);
        // Computes the signature.
        String sig = computeHash(payload);

        // We send everything.
        // First, we make the progress circle visible.
        ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar);
        pb.setVisibility(View.VISIBLE);
        // Then, we start the call.
        ReportLocationSpec myCallSpec = new ReportLocationSpec();
        // myCallSpec.url = ...
        // myCallSpec.context = MainActivity.this;
        HashMap<String,String> m = new HashMap<String,String>();
        m.put("payload", payload);
        m.put("sig", sig);
        myCallSpec.setParams(m);

        // Performs the upload.
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);
    }


    /**
     * Generates the cryptographic signature for a string s.
     * @param s
     * @return A safe hash signature for the web.
     */
    private String computeHash(String s) {
        return signingKey;
    }


    /**
     * This class is used to do the HTTP call, and it specifies how to use the result.
     */
    class ReportLocationSpec extends ServerCallSpec {
        @Override
        public void useResult(Context context, String result) {
            if (result != null && result.startsWith("\"ok\"")) {
                // We succeeded.
            } else {
                // We failed.
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
