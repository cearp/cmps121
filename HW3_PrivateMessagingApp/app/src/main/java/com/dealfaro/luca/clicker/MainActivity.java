package com.dealfaro.luca.clicker;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;


public class MainActivity extends ActionBarActivity {

    private double longitude;
    private double latitude;
    private double curr_lat;
    private double curr_lng;

    private static final String TAG = "MyActivity";

    Location lastLocation = new Location("");
    private double lastAccuracy = (double) 1e10;
    private long lastAccuracyTime = 0;

    private static final String LOG_TAG = "lmsngr";

    private static final float GOOD_ACCURACY_METERS = 100;

    // This is an id for my app, to keep the key space separate from other apps.
    private AppInfo appInfo;
    private static String MY_APP_ID = "cearp4456341635";


    private static final String SERVER_URL_PREFIX = "https://hw3n-dot-luca-teaching.appspot.com/store/default/";

    //To remember the favorite account
    public static final String PREF_ACCOUNT = "pref_account";

    // To remember the post we received.
    public static final String PREF_POSTS = "pref_posts";

    // Uploader.
    private ServerCall uploader;
    // Remember whether we have already successfully checked in.
    private boolean checkinSuccessful = false;

    private ArrayList<String> accountList;

    private class ListElement {
        ListElement() {};

        public String msgid;
        public String msg;
        public String ts;
    }

    private ArrayList<ListElement> aList;

    private class MyAdapter extends ArrayAdapter<ListElement> {

        int resource;
        Context context;

        public MyAdapter(Context _context, int _resource, List<ListElement> items) {
            super(_context, _resource, items);
            resource = _resource;
            context = _context;
            this.context = _context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout newView;

            ListElement w = getItem(position);

            // Inflate a new view if necessary.
            if (convertView == null) {
                newView = new LinearLayout(getContext());
                String inflater = Context.LAYOUT_INFLATER_SERVICE;
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
                vi.inflate(resource,  newView, true);
            } else {
                newView = (LinearLayout) convertView;
            }

            // Fills in the view.
            TextView tvMsg = (TextView) newView.findViewById(R.id.itemMsg);
            TextView tvTs = (TextView) newView.findViewById(R.id.itemTs);
            tvMsg.setText(w.msg);
            tvTs.setText(w.ts);
            return newView;
        }
    }

    private MyAdapter aa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        aList = new ArrayList<ListElement>();
        aa = new MyAdapter(this, R.layout.list_element, aList);
        ListView myListView = (ListView) findViewById(R.id.listView);
        myListView.setAdapter(aa);
        aa.notifyDataSetChanged();
        //creates the singleton object
        appInfo = AppInfo.getInstance(this);
        MY_APP_ID = appInfo.toString();
        spinOff();

        Log.v(TAG, "Initialized properly");

        PostMessageSpec myCallSpec = new PostMessageSpec();

        String lat = "" + latitude;
        String lng = "" + longitude;

        myCallSpec.url = SERVER_URL_PREFIX + "get_local";
        myCallSpec.context = MainActivity.this;

        HashMap<String, String> m = new HashMap<String, String>();
        m.put("lat", lat);
        m.put("lng", lng);
        myCallSpec.setParams(m);
        if(uploader != null){
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);
    }

    @Override
    protected void onStart() {
        super.onStart();
        lastLocation.setLatitude(curr_lat);
        lastLocation.setLongitude(curr_lng);
        initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // First super, then do stuff.
        // Let us display the previous posts, if any.
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String result = settings.getString(PREF_POSTS, null);
        if (result != null) {
            displayResult(result);
        }
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    @Override
    protected void onPause() {
        // Stops the upload if any.
        if (uploader != null) {
            uploader.cancel(true);
            uploader = null;
        }
        super.onPause();
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            lastLocation = location;
            Log.i(LOG_TAG, location.toString());
            curr_lng = lastLocation.getLongitude();
            curr_lat = lastLocation.getLatitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };

    public void clickButton(View v) {
        spinOn();
        // Get the text we want to send.
        EditText et = (EditText) findViewById(R.id.editText);
        String msg = et.getText().toString();

        et.setText(null);

        spinOn();

        String lat = "" + latitude;
        String lng = "" + longitude;
        String randID = "8as4da22da1";
        // Then, we start the call.
        PostMessageSpec myCallSpec = new PostMessageSpec();


        myCallSpec.url = SERVER_URL_PREFIX + "put_local";
        myCallSpec.context = MainActivity.this;
        // Let's add the parameters.
        HashMap<String,String> m = new HashMap<String,String>();
        m.put("msgid", reallyComputeHash(msg));
        m.put("msg", msg);
        m.put("lat", lat);
        m.put("lng", lng);
        m.put("app_id", randID);

        myCallSpec.setParams(m);
        // Actual server call.
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);
    }

    private String reallyComputeHash(String s) {
        // Computes the crypto hash of string s, in a web-safe format.
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(s.getBytes());
            digest.update("My secret key".getBytes());
            byte[] md = digest.digest();
            // Now we need to make it web safe.
            String safeDigest = Base64.encodeToString(md, Base64.URL_SAFE);
            return safeDigest;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * This class is used to do the HTTP call, and it specifies how to use the result.
     */
    class PostMessageSpec extends ServerCallSpec {
        @Override
        public void useResult(Context context, String result) {
            if (result == null) {
                Log.i(LOG_TAG, "The server call failed.");
            } else {
                Log.i(LOG_TAG, "Received string: " + result);
                displayResult(result);
                // Stores in the settings the last messages received.
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREF_POSTS, result);
                editor.commit();
                EditText et = (EditText) findViewById(R.id.editText);
                et.setText("");
            }
            spinOff();
        }
    }

    private void displayResult(String result) {
        Gson gson = new Gson();
        MessageList ml = gson.fromJson(result, MessageList.class);
        // Fills aList, so we can fill the listView.
        aList.clear();
        //SimpleDateFormat  date = new SimpleDateFormat("MM-cc");
        for (int i = 0; i < ml.messages.length; i++) {
            repJson m = ml.messages[i];
            String msg = m.getMsg();
            String msgid = m.getMsgid();
            String ts = m.getTS();
            String timeStamp = getTime(ts);
            String call = "msgid: " + msgid + " msg: " + msg + " ts: " + timeStamp;

            Log.d(LOG_TAG, call);
            ListElement ael = new ListElement();
            ael.msg = msg;
            ael.ts = timeStamp;
            aList.add(ael);

        }
        aa.notifyDataSetChanged();
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

    public void refreshButton(View v) {
        GetMessageSpec myCallSpec = new GetMessageSpec();
        spinOn();

        myCallSpec.url = SERVER_URL_PREFIX + "get_local";
        myCallSpec.context = MainActivity.this;
        // Let's add the parameters.
        HashMap<String,String> m = new HashMap<String,String>();

        Location thisLocation = lastLocation;

        if (thisLocation == null) {
            Log.i(LOG_TAG, "Location has not been initialized, quitting");
            return;
        }


        double lat = curr_lat;
        double lng = curr_lng;

        Log.i(LOG_TAG, "using this as lat " + lat);
        Log.i(LOG_TAG, "using this as long " + lng);

        m.put("lat", Double.toString(lat));
        m.put("lng", Double.toString(lng));

        myCallSpec.setParams(m);
        // Actual server call.
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);

    }

    //gets messages from last known location
    public void initialize() {
        GetMessageSpec myCallSpec = new GetMessageSpec();
        double lat = curr_lat;
        double lng = curr_lng;
        Location curr_loc = lastLocation;
        myCallSpec.url = SERVER_URL_PREFIX + "get_local";
        myCallSpec.context = MainActivity.this;

        HashMap<String,String> m = new HashMap<String,String>();
        m.put("lat", Double.toString(lat));
        m.put("lng", Double.toString(lng));

        if (curr_loc == null) {
            Log.i(LOG_TAG, "Location has not been initialized");
            return;
        }

        Log.i(LOG_TAG, "Latitude: " + lat);
        Log.i(LOG_TAG, "Longitude " + lng);

        myCallSpec.setParams(m);
        // Actual server call.
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);
    }
    //spinner from hw2 reviews + piazza
    private void spinOn() {
        ProgressBar spinner = (ProgressBar) findViewById(R.id.progressBar);
        spinner.setVisibility(View.VISIBLE);
    }
    private void spinOff() {
        ProgressBar spinner = (ProgressBar) findViewById(R.id.progressBar);
        spinner.setVisibility(View.INVISIBLE);
    }

    class GetMessageSpec extends ServerCallSpec {
        @Override
        public void useResult(Context context, String result) {
            if (result == null) {
                Log.i(LOG_TAG, "Failed to contact server");
            } else {
                // Translates the string result, decoding the Json.
                Log.i(LOG_TAG, "Received string: " + result);
                displayResult(result);
                // Stores in the settings the last messages received.
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREF_POSTS, result);
                editor.commit();
            }
            spinOff();
        }
    }

    private String getTime(String ts){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date timeStamp = new Date();
        String result = "";

        try {
            timeStamp = dateFormat.parse(ts);
            Date current = new Date();
            long time = current.getTime() - timeStamp.getTime();
            long minutes = time / (60 * 1000) % 60;
            long hours = time / (60 * 60 * 1000) % 24;
            long days = time / (24 * 60 * 60 * 1000);
            result = Long.toString(days) + "d " + Long.toString(hours) + "h " + Long.toString(minutes) + "m ago";
            Log.d(LOG_TAG, "Time = " + time);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return result;

    }

}
