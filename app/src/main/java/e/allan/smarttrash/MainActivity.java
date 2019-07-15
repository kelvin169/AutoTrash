package e.allan.smarttrash;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    ProgressDialog pd;
    Button btnConnect;
    TextView tvConnStat, tvMessage;

    public static boolean isRunning = false;
    private LocalBroadcastManager localBroadcastManager;
    private UserPrefs userPrefs;
    private List<Map<String, String>> dataList;
    private RecyclerView rv;
    private RVAdapter rvAdapter;
    private BackgroundService backgroundService;
    Intent bkgndServiceIntent;
    String connectionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pd = new ProgressDialog(this);
        pd.setTitle("Connecting");
        pd.setMessage("Please wait ...");
        btnConnect = findViewById(R.id.btn_conn); btnConnect.setOnClickListener(this);
        tvConnStat = findViewById(R.id.tv_connStat);
        rv = findViewById(R.id.rv_data);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(dataReceiver, new IntentFilter("NewData"));
        localBroadcastManager.registerReceiver(connectionReceiver, new IntentFilter("Connection"));
        userPrefs = new UserPrefs(this);

        dataList = new ArrayList<>();
        if(userPrefs.getSavedData() != null){
            dataList.addAll(userPrefs.getSavedData());
            userPrefs.clearSavedData();
        }

//        Intent notifIntent = getIntent();
//        String notifData = notifIntent.getStringExtra("Notif");
        //add any data received in background
        List<Map<String, String>> bkgList = userPrefs.getNotifCache();
        if(bkgList != null && bkgList.size() != 0){ //cached data present
            dataList.addAll(bkgList);
            userPrefs.clearNotifCache();
        }

        rvAdapter = new RVAdapter(this, dataList);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rv.setAdapter(rvAdapter);

        connectionStatus = userPrefs.getConnStat();
        if(connectionStatus != null){
            tvConnStat.setText("Status: "+connectionStatus);
            if(connectionStatus.equals("Connected")){
                btnConnect.setText("Disconnect");
            }
            if(connectionStatus.equals("Disconnected")){
                btnConnect.setText("Connect");
            }
            userPrefs.setConnectionStatus(null);
        }

//        backgroundService = new BackgroundService(this);
//        bkgndServiceIntent = new Intent(this, backgroundService.getClass());
//        if(!isServiceRunning(backgroundService.getClass())){
//            startService(bkgndServiceIntent);
//        }else{
//            tvConnStat.setText("Status: "+ "Connected");
//        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        isRunning = true;
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.e ("isServiceRunning?", true+"");
                return true;
            }
        }
        Log.e ("isServiceRunning?", false+"");
        return false;
    }

    private BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String data = intent.getStringExtra("data");
            Map<String, String> newMap = new HashMap<>();
            String[] parts = data.split(",");
            String location = parts[0].trim(); String time = parts[1].trim();
            newMap.put("location", location); newMap.put("time", time);
            rvAdapter.addData(newMap);
//            playAlert();
        }
    };

    private BroadcastReceiver connectionReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");
            tvConnStat.setText("Status: "+ status);
            connectionStatus = status;
            if(connectionStatus.equals("Connected")){
                btnConnect.setText("Disconnect");
            }
            if(connectionStatus.equals("Disconnected")){
                btnConnect.setText("Connect");
            }
            if(pd.isShowing()){
                pd.dismiss();
            }
        }
    };

    public void playAlert(){
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if(notification != null){
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        }

    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.btn_conn){
            Intent bkgndServiceIntent = new Intent(this, BackgroundService.class);
            if(btnConnect.getText().toString().equalsIgnoreCase("Connect")){
                startService(bkgndServiceIntent);
                pd.show();
            }
            if(btnConnect.getText().toString().equalsIgnoreCase("Disconnect")){
                stopService(bkgndServiceIntent);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isRunning = false;
        userPrefs.saveData(dataList);
        userPrefs.setConnectionStatus(connectionStatus);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
