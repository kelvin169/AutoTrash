package e.allan.smarttrash;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Calendar;
import java.util.Random;

public class BackgroundService extends Service {

    final String pubTopic = "smarttrash/app";
    public MqttAndroidClient mqttAndroidClient;

    final String brokerUri = "tcp://broker.mqttdashboard.com:1883";

    String clientId;
    final String subscriptionTopic = "smarttrash/can";

    public BackgroundService(Context context){
        super();
    }

    public BackgroundService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent i = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, i,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = "smarttrash_id";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("SmartTrash")
                        .setContentText("Listening for new messages")
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "smarttrash",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(defaultSoundUri, null);
            notificationManager.createNotificationChannel(channel);
        }


        startForeground(1, notificationBuilder.build());

        Calendar calendar = Calendar.getInstance();
        clientId = "TriviaAppClient"+Integer.toString(new Random().nextInt(50))+Long.toString(calendar.getTimeInMillis());
        mqttAndroidClient = new MqttAndroidClient(this, brokerUri, clientId);
        Log.e("Mqtt", "Service start");
        connectMqtt();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private boolean internetIsConnected(){
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    private void connectMqtt(){
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(false);
        mqttConnectOptions.setCleanSession(false);
//        mqttConnectOptions.setUserName(username);
//        mqttConnectOptions.setPassword(password.toCharArray());

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    startMqtt();
                    subscribeToTopic();
                    Log.e("Mqtt", "connected");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("Mqtt", "Failed to connect to: " + brokerUri + exception.toString());
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    private void startMqtt(){
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Intent intent = new Intent("Connection");
                intent.putExtra("status", "Connected");
                LocalBroadcastManager.getInstance(BackgroundService.this).sendBroadcast(intent);
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Intent intent = new Intent("Connection");
                intent.putExtra("status", "Disconnected");
                LocalBroadcastManager.getInstance(BackgroundService.this).sendBroadcast(intent);
                Log.e("Mqtt", "diconnected");
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) {
                Log.e("MQTT", mqttMessage.toString());
                String message = mqttMessage.toString();
                handleMessage(message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

    private void handleMessage(String message){
        if(MainActivity.isRunning){
            Intent intent = new Intent("NewData");
            intent.putExtra("data", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }else{
            UserPrefs userPrefs = new UserPrefs(this);
            userPrefs.setNotifCache(message);
            displayNotification(message);
        }
    }

    private void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 1, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.e("Mqtt","Subscribed to topic!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("Mqtt", "Subscribed fail!");
                }
            });

        } catch (MqttException ex) {
            if(ex.getMessage() != null){
                Log.e("Exception subscribing", ex.getMessage());
            }else{
                ex.printStackTrace();
            }
        }
    }

    private void displayNotification(String message) {
        Intent intent = new Intent(this, MainActivity.class);
        //open specific activity based on click_action property of notification
//        Intent intent = new Intent(clickAction);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("Notif", message);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = "smarttrash_id";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("Alert")
                        .setContentText("New Data")
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "smarttrash",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(defaultSoundUri, null);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    private void disconnectMQTT(){
        try{
            mqttAndroidClient.disconnectForcibly(2, 2);
            Log.e("force disconn", "success");
        }catch(MqttException ex){
            if(ex.getMessage() != null){
                Log.e("Error disconnecting", ex.getMessage());
            }else{
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mqttAndroidClient == null){
            mqttAndroidClient = new MqttAndroidClient(this, brokerUri, clientId);
        }
        mqttAndroidClient.unregisterResources();
        mqttAndroidClient.close();

        Intent intent = new Intent("Connection");
        intent.putExtra("status", "Disconnected");
        LocalBroadcastManager.getInstance(BackgroundService.this).sendBroadcast(intent);

//        Intent broadcastIntent = new Intent(this, ServiceRestartReceiver.class);
//        sendBroadcast(broadcastIntent);
        Log.e("Mqtt", "Service destroyed");
    }
}
