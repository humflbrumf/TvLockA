package com.humflbrumf.tvlock;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

public class Service extends android.app.Service {

    final static String TAG = "Service";

    final static boolean mAllowRebind = true; // indicates whether onRebind should be used
    private final IBinder mBinder = new TvLockBinder();
    public class TvLockBinder extends Binder {
        Service getService() {
            // Return this instance of LocalService so clients can call public methods
            return Service.this;
        }
    }

    private boolean screenOn;

    private boolean isPIN;
    public void setIsPIN(boolean flag){
        isPIN = flag;
    }

    private ServiceHandler serviceHandler;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {

            Log.d(TAG,"handleMessage()");

            int counter = 0;
            while ( true ){
                counter++;
                Log.d(TAG,"handleMessage() - counter = " + counter);
                if ( !isPIN && screenOn ) {
                    if ( !MainActivity.active ) {
                        Log.d(TAG, "handleMessage() activate MainActivity - isPIN = " + isPIN + " - MainActivity.active = " + MainActivity.active + " - screenOn = " + screenOn);
                        Intent notificationIntent = new Intent(Service.this, MainActivity.class);
                        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        PendingIntent pendingIntent = PendingIntent.getActivity(Service.this, 0, notificationIntent, 0);
                        try {
                            pendingIntent.send();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        Thread.sleep(30000 );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            // stopSelf(msg.arg1);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {

        Log.d(TAG,"onCreate()");

        isPIN = false;
        screenOn = true;

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        // Get the HandlerThread's Looper and use it for our Handler
        Looper serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand()");
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        final BroadcastReceiver mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);
        Log.d(TAG,"onStartCommand() startId = " + startId);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"onBind()");
        // A client is binding to the service with bindService()
        Toast.makeText(this, "service bound", Toast.LENGTH_SHORT).show();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG,"onUnbind()");
        // All clients have unbound with unbindService()
        return mAllowRebind;
    }
    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG,"onRebind()");
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy()");
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        //Intent serviceIntent = new Intent(this, Service.class);
        //this.startService(serviceIntent);
    }

    public class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.d(TAG,"onReceive");
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                isPIN = false;
                screenOn = false;
                Log.i(TAG,"Screen Off intent received - screenOn = " + screenOn );
           } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                screenOn = true;
                Log.i(TAG,"Screen On intent received - screenOn = " + screenOn );
                Intent notificationIntent = new Intent(Service.this, MainActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(Service.this, 0, notificationIntent, 0);
                try
                {
                    pendingIntent.send();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

}
