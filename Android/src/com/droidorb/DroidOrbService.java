package com.droidorb;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.droidorb.observer.MissedCallsContentObserver;
import com.droidorb.observer.OnMissedCallListener;
import com.droidorb.server.Client;
import com.droidorb.server.Server;
import com.droidorb.server.ServerListener;

/**
 * Main background service to monitor accessory
 * 
 * @author toby
 */
public class DroidOrbService extends Service {
   public static final int SERVER_PORT = 4567;
   public static final int NOTIF_MAIN = 1;

   // Unique Identification Number for the Notification.
   // We use it on Notification start, and to cancel it.
   private int NOTIFICATION = 10; 
   
   Server server = null;
   MissedCallsContentObserver mcco;
   private NotificationManager mNM;
   
   // This is the object that receives interactions from clients. See
   // RemoteService for a more complete example.
   private final IBinder mBinder = new LocalBinder();

   @Override
   public IBinder onBind(Intent arg0) {
      return mBinder;
   }

   /**
    * Class for clients to access. Because we know this service always runs in
    * the same process as its clients, we don't need to deal with IPC.
    */
   public class LocalBinder extends Binder {
      DroidOrbService getService() {
         return DroidOrbService.this;
      }
   }

   @Override
   public void onCreate() {
      super.onCreate();

      mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

      // Display a notification about us starting.  We put an icon in the status bar.
      showNotification("Waiting for accessory...");
      
      mcco = new MissedCallsContentObserver(this, new OnMissedCallListener() {
         @Override
         public void onMissedCall(int missedCalls) {

         }
      });

      // Create TCP server
      server = new Server(SERVER_PORT);
      server.addListener(new ServerListener() {

         @Override
         public void onServerStopped(Server server) {
            Log.d(Main.LOG_TAG, "server stopped");
         }

         @Override
         public void onServerStarted(Server server) {
            Log.d(Main.LOG_TAG, "server started");
         }

         @Override
         public void onReceive(Client client, final byte[] data) {
            Log.d(Main.LOG_TAG, "data received " + data[0] + data[1]);
         }

         @Override
         public void onClientDisconnect(Server server, Client client) {
            Log.d(Main.LOG_TAG, "accessory disconnected");
         }

         @Override
         public void onClientConnect(Server server, Client client) {
            showNotification("Accessory connected");
            Log.d(Main.LOG_TAG, "accessory connected");
         }
      });

      // start missed calls listener
      mcco.start();

      try {
         server.start();
      } catch (IOException e) {
         Log.e(Main.LOG_TAG, "Error starting TCP/IP server", e);
      }
   }
   
   @Override
   public void onDestroy() {
       // Cancel the persistent notification.
       mNM.cancel(NOTIFICATION);

       // Tell the user we stopped.
       Toast.makeText(this, "DroidOrb stopped", Toast.LENGTH_SHORT).show();
   }   

   /**
    * Show a notification while this service is running.
    */
   private void showNotification(String text) {
      // In this sample, we'll use the same text for the ticker and the expanded
      // notification
      //CharSequence text = "DroidOrb waiting for accessory"; // getText(R.string.local_service_started);

      // Set the icon, scrolling text and timestamp
      Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());

      // The PendingIntent to launch our activity if the user selects this
      // notification
      PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, Main.class), 0);

      // Set the info for the views that show in the notification panel.
      notification.setLatestEventInfo(this, "DroidOrb", text, contentIntent);

      // Send the notification.
      mNM.notify(NOTIFICATION, notification);
   }
}
