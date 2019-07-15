package e.allan.smarttrash;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserPrefs {

    private SharedPreferences sPrefs;
    private SharedPreferences.Editor editor;
    private Gson gson;
    private TypeToken listOfMapsType;

    public UserPrefs(Context context){
        sPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sPrefs.edit();
        gson = new Gson();
        listOfMapsType = new TypeToken<List<Map<String, String>>>(){};
    }

    public void setNotifCache(String data){
        String cache = sPrefs.getString("Notif", null);
        List<Map<String, String>> list = gson.fromJson(cache, listOfMapsType.getType());
        Map<String, String> newMap = new HashMap<>();
        String[] parts = data.split(",");
        String location = parts[0].trim(); String time = parts[1].trim();
        newMap.put("location", location); newMap.put("time", time);
        if(list == null){
            list = new ArrayList<>();
        }
        list.add(newMap);
        editor.putString("Notif", gson.toJson(list));
        editor.apply();
    }
    public void clearNotifCache(){
        editor.putString("Notif", null);
        editor.apply();
    }
    public List<Map<String, String>> getNotifCache(){
        String cache = sPrefs.getString("Notif", null);
        if(cache != null){
            return gson.fromJson(cache, listOfMapsType.getType());
        }
        return null;
    }

    public void saveData(List<Map<String, String>> dataList){
        editor.putString("Data", gson.toJson(dataList));
        editor.apply();
    }
    public void clearSavedData(){
        editor.putString("Data", null);
        editor.apply();
    }
    public List<Map<String, String>> getSavedData(){
        String cache = sPrefs.getString("Data", null);
        if(cache != null){
            return gson.fromJson(cache, listOfMapsType.getType());
        }
        return null;
    }

    public void setConnectionStatus(String status){
        editor.putString("connStat", status);
        editor.apply();
    }
    public String getConnStat(){
        return sPrefs.getString("connStat", null);
    }

}
