package com.cosmos.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AlarmStorage {

    private static final String PREFS = "cosmos_alarms";
    private static final String KEY_LIST = "alarms_json";
    private final SharedPreferences prefs;

    public AlarmStorage(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<Alarm> loadAlarms() {
        List<Alarm> list = new ArrayList<>();
        String raw = prefs.getString(KEY_LIST, null);
        if (raw == null || raw.trim().isEmpty()) {
            // Default first alarm
            Alarm def = new Alarm();
            def.hour = 7;
            def.minute = 0;
            def.label = "Cosmic Wake-up";
            def.missionType = MissionType.MATH;
            def.missionCount = 3;
            list.add(def);
            saveAlarms(list);
            return list;
        }

        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(Alarm.fromJson(obj));
            }
        } catch (Exception e) {}

        Collections.sort(list, new Comparator<Alarm>() {
            @Override
            public int compare(Alarm a, Alarm b) {
                if (a.hour != b.hour) return Integer.compare(a.hour, b.hour);
                return Integer.compare(a.minute, b.minute);
            }
        });

        return list;
    }

    public void saveAlarms(List<Alarm> list) {
        JSONArray arr = new JSONArray();
        for (Alarm a : list) {
            arr.put(a.toJson());
        }
        prefs.edit().putString(KEY_LIST, arr.toString()).commit();
    }

    public Alarm findById(String id) {
        for (Alarm a : loadAlarms()) {
            if (a.id.equals(id)) return a;
        }
        return null;
    }

    public void updateAlarm(Alarm updated) {
        List<Alarm> list = loadAlarms();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(updated.id)) {
                list.set(i, updated);
                saveAlarms(list);
                return;
            }
        }
        list.add(updated);
        saveAlarms(list);
    }

    public void deleteAlarm(String id) {
        List<Alarm> list = loadAlarms();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) {
                list.remove(i);
                saveAlarms(list);
                return;
            }
        }
    }
}
