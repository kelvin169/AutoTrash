package e.allan.smarttrash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceRestartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("Service", "Restarted");
        context.startService(new Intent(context, BackgroundService.class));
    }
}
