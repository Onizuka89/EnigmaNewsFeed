package com.stiandrobak.enigma;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.*;
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
    private final static ArrayList<Entry> entries_array = new ArrayList<Entry>();
    private static Context context;
    private static String json = "";
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
                TextView textView = (TextView) view.findViewById(R.id.elemtitle);
                Entry entry = entries_array.get(position);
                if(null == entry) {
                    Log.d(TAG,"Entry is Null at index " + position);
                }else if (null == textView) {
                    Log.d(TAG,"No textview!!!!!");
                }
                try {
                    textView.setText(entry.title);
                    textView = (TextView) view.findViewById(R.id.elemlink);
                    textView.setText(entry.link);
                    Linkify.addLinks(textView, Linkify.ALL);

                    textView = (TextView) view.findViewById(R.id.elemsnippet);
                    textView.setText(entry.snippet);
                    return view;
                }catch (NullPointerException e){
                    Log.e(TAG,"Nullpoint error exception in getView!");
                    return null;
                }
            }
        });
        context = this;
        doStuff();
        MainActivity.context = this;

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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class RetrieveJSONFeed extends AsyncTask<String, Void, String>{
        private final static String HOST = "ajax.googleapis.com";
        private final static String PATH = "ajax/services/feed/load?v=2.0&q=http%3A%2F%2Fgoo.gl%2FdfVCjV&num=20";
        public String doInBackground(String... params) {
            try {
                URL url = new URL("http", HOST, 80, PATH);
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
                return builder.toString();
            }catch (Exception e) {
                return "";
            }
        }

        public void onPostExecute(String json){
            MainActivity.update_listview();
            BaseAdapter adapter = (BaseAdapter)listView.getAdapter();
            adapter.notifyDataSetChanged();
            //MainActivity.update_listview(json);
        }
    }

    public static void update_listview(){
        JsonParser parser = new JsonParser();
        Log.d("TAG",json);
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
}
