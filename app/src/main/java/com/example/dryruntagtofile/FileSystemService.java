package com.example.dryruntagtofile;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class FileSystemService extends Service {

    private String channelId = "FILE_SYSTEM_WATCHER_SERVICE";
    WatchService ws = null;
    Thread watcherThread;

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "service created", Toast.LENGTH_LONG).show();
        Context ctx = this;
        watcherThread = new Thread(new Runnable() {
            @Override
            public void run() {
                FileMovementLog flm = FileMovementLog.getInstance(ctx);

                try {
                    ws = FileSystems.getDefault().newWatchService();
                    // check for permission
                    File root = FileUtilities.getFileRoot(ctx);
                    Queue<File> children = new LinkedList<>();
                    children.add(root);
                    while(!children.isEmpty()){
                        // folder currently we are in
                        File f = children.remove();
                        Log.d("Initialising", f.getAbsolutePath());
                        // attach watch service
                        f.toPath().register(ws, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                        // find child directories to be added to watch service
                        File[] childFiles = f.listFiles();
                        if(childFiles != null && f.isDirectory()){
                            for(int i = 0; i < childFiles.length; i++){
                                if(childFiles[i].isDirectory()) {
                                    // Log.d("Initialising cf", childFiles[i].getAbsolutePath().contains("Android/obb"));
                                    if(!childFiles[i].getAbsolutePath().contains("Android/obb") && !childFiles[i].getAbsolutePath().contains("Android/data")) children.add(childFiles[i]);
                                }
                            }
                        }
                    }


                    boolean keepPolling = true;
                    while(keepPolling && ws != null){
                        try {
                            WatchKey wkey = ws.poll(1, TimeUnit.DAYS);
                            if(wkey != null){
                                List<WatchEvent<?>> events = wkey.pollEvents();
                                for(int i = 0; i < events.size(); i++){
                                    if (i < events.size() - 1) {
                                        flm.recordMovement(events.get(i), ((Path) wkey.watchable()).toAbsolutePath().toString(), events.get(i + 1));
                                    } else {
                                        flm.recordMovement(events.get(i), ((Path) wkey.watchable()).toAbsolutePath().toString(), null);
                                    }
                                }
                                wkey.reset();
                            } else {
                                keepPolling = false;
                            }
                        } catch (Exception e){
                            Log.e("Initialising Watcher", e.toString());
                        }
                    }


                } catch (Exception e){
                    Log.e("Initialising", e.toString());
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Started Service", Toast.LENGTH_LONG).show();

        try{
            NotificationChannel channel = new NotificationChannel(channelId, "name", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);

            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,0, notificationIntent, PendingIntent.FLAG_MUTABLE);
            Notification notification = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle("Service is Running")
                    .setContentText("Listening for Screen Off/On events")
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_baseline_folder_24)
                    .setContentIntent(pendingIntent)
                    .build();

            startForeground(1, notification);

            watcherThread.start();

        } catch(Exception e){
            Log.e("Initialising Error Service", e.toString());
        }

        Toast.makeText(this, "RETURNED Service", Toast.LENGTH_LONG).show();
        //
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
