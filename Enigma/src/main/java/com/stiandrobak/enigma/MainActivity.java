package com.stiandrobak.enigma;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import android.widget.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MainActivity extends ActionBarActivity {
    private final static String TAG = "__ENIGMA_JSON_FEED__";
    private static ListView listView = null;
    protected final static ArrayList<Entry> entries_array = new ArrayList<Entry>();
    private static Context context;
    private static String json = "";
    private AlarmManager alarmManager = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button aButton = (Button)findViewById(R.id.button1);
        listView = (ListView) findViewById(R.id.listview);
        aButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doStuff();
            }
        });
        listView.setAdapter(new BaseAdapter() {
            public int getCount() {
                return entries_array.size();
            }

            public Object getItem(int position) {
                return entries_array.get(position);
            }

            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean isEnabled(int position){
                return false;
            }

            public View getView(int position, View convertView, ViewGroup parent) {
                LayoutInflater inflater= LayoutInflater.from(context);
                RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.listrow, listView, false);
                try {
                    assert view != null;
                    TextView textView = (TextView) view.findViewById(R.id.elemtitle);
                    Entry entry = entries_array.get(position);
                    if (null == entry) {
                        throw new Exception("Entry is Null at index " + position);
                    }
                    if (null == textView) {
                        throw new Exception("Could not find the textView in listrow layout.");
                    }
                    textView.setText(entry.title);
                    textView = (TextView) view.findViewById(R.id.elemlink);
                    textView.setText(entry.link);
                    Linkify.addLinks(textView, Linkify.ALL);
                    textView = (TextView) view.findViewById(R.id.elemsnippet);
                    textView.setText(entry.snippet);
                    return view;
                }catch(Exception e){
                    Log.e(TAG, e.getMessage());
                }
                return null;
            }
        });
        context = this;
        doStuff();
        MainActivity.context = this;
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent feedService = new Intent(this, FeedService.class);
        PendingIntent pendingFeedServiceIntent = PendingIntent.getService(this, 2222, feedService, PendingIntent.FLAG_CANCEL_CURRENT);
        try {
            //alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, (AlarmManager.INTERVAL_FIFTEEN_MINUTES / 15), pendingFeedServiceIntent);
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingFeedServiceIntent);
        }catch(Exception e){
            Log.d(TAG, "Feed don't work:\n"+ e.getMessage() + "\n" +e.getStackTrace());
        }
        //startService(new Intent(this, FeedService.class));
    }

    public static void doStuff(){
        try {
            new RetrieveJSONFeed().execute();
        }catch(Exception e){
            Log.d(TAG, String.format("An exception happened while attempting to get JSON with message: %s", e.getMessage()));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    public static class RetrieveJSONFeed extends AsyncTask<String, Void, String>{
        private final static String HOST = "ajax.googleapis.com";
        private final static String PATH = "ajax/services/feed/load?v=2.0&q=http%3A%2F%2Fgoo.gl%2FdfVCjV&num=20";
        private final static int port = 80;

        /*  for testing
        private final static String HOST = "";
        private final static String PATH = "json.json";
        private final static int port = 81;
        */

        private boolean isService = false;
        public String doInBackground(String... params) {
            try {
                // If argument is passed, onExecute doesn't update MainActivity's listview.
                if(params.length == 1){
                    isService = true;
                }
                URL url = new URL("http", HOST, port, PATH);
                URLConnection conn = url.openConnection();
                conn.connect();
                StringBuilder builder = new StringBuilder();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );
                String line;
                while ((line = in.readLine()) != null) {
                    builder.append(line);
                }
                in.close();
                MainActivity.json = builder.toString();
                Log.d("JSON",MainActivity.json);
                return builder.toString();
            }catch (Exception e) {
                Log.d("Network issue", e.getMessage());
                return "";
            }
        }

        public void onPostExecute(String json){
            MainActivity.update_listview();
            if(!isService){
                BaseAdapter adapter = (BaseAdapter)listView.getAdapter();
                adapter.notifyDataSetChanged();
            }else {
                Log.d(TAG, "I am bloody done!");
            }
            //MainActivity.update_listview(json);
        }
    }

    public static void update_listview(){
        JsonParser parser = new JsonParser();
        Log.d(TAG,json);
        JsonElement element = parser.parse(json);
        while(!entries_array.isEmpty()){
            entries_array.remove(0);
        }
        try {
            JsonArray entries = element.getAsJsonObject()
                    .get("responseData").getAsJsonObject()
                    .get("feed").getAsJsonObject()
                    .get("entries").getAsJsonArray();
            for(JsonElement entryElement:entries){
                Entry entry = new Entry();

                JsonObject entryData = entryElement.getAsJsonObject();
                String title = entryData.get("title").getAsString();
                String link = entryData.get("link").getAsString();
                String snippet = entryData.get("contentSnippet").getAsString();

                Log.d(TAG, String.format("Title: %s", title));
                Log.d(TAG, String.format("Link: %s", link));
                Log.d(TAG, String.format("Snippet: %s", snippet));

                entry.title = title;
                entry.snippet = snippet;
                entry.link = link;
                entries_array.add(entry);
            }

        }catch(Exception e) {
            Log.d(TAG, String.format("%s", e.getMessage()));
            Log.d(TAG, String.format("%s", e.getStackTrace()));
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();

        //Kills FeedService with the activity
        Intent stopIntent = new Intent(this, FeedService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 2222, stopIntent, 0);
        alarmManager.cancel(pendingIntent);
        try {
            pendingIntent.cancel();
        }catch (Exception e){
            Log.d(TAG,"Maybe I shouldn't have tried to cancel it...");
        }
    }
}
