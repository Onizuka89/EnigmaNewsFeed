package com.stiandrobak.enigma;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.concurrent.TimeUnit;


/**
 * Created by stiansd on 28.03.14.
 */
public class FeedService extends Service{
    AlarmManager alarmManager;
    protected static String latestHeader = null;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID){
        MainActivity.RetrieveJSONFeed jsonFeed = new MainActivity.RetrieveJSONFeed();
        jsonFeed.execute("Service");
        try {
            jsonFeed.get(15, TimeUnit.SECONDS);
            Log.d("DEBUG", "FeedService started.");
            if (null == alarmManager)
                alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if(MainActivity.entries_array.size() > 0) {
                Entry entry = MainActivity.entries_array.get(0);
                if(null == latestHeader){
                    latestHeader = entry.title;
                }else if(!latestHeader.equals(entry.title)) {
                    latestHeader = entry.title;
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                            .setContentTitle(entry.title)
                            .setContentText(entry.snippet)
                            .setSmallIcon(R.drawable.abc_ic_clear_normal);
                    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.notify(1, builder.build());
                }
            }
        } catch (Exception e){
            Log.e("ERROR","Issue occured while waiting for JSON feed to be retrieved:\n"+e.getMessage());
        }
        return START_NOT_STICKY;
    }
}
