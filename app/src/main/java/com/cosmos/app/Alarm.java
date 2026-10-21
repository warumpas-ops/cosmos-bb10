package com.cosmos.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.Locale;

public class Alarm {
    public String id;
    public int hour;
    public int minute;
    public boolean enabled;
    public String label;
    public boolean[] repeatDays; // 0=Sun, 1=Mon, ..., 6=Sat
    public MissionType missionType;
    public int missionCount; // e.g. 3 problems, or 30 shakes

    public Alarm() {
        this.id = "alarm_" + System.currentTimeMillis();
        this.hour = 7;
        this.minute = 0;
        this.enabled = true;
        this.label = "Morning Alarm";
        this.repeatDays = new boolean[]{false, true, true, true, true, true, false}; // Mon-Fri by default
        this.missionType = MissionType.MATH;
        this.missionCount = 3;
    }

    public String formattedTime() {
        int h = hour % 12;
        if (h == 0) h = 12;
        String ampm = hour >= 12 ? "PM" : "AM";
        return String.format(Locale.US, "%02d:%02d %s", h, minute, ampm);
    }

    public String formatted24h() {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    public String repeatDescription() {
        int count = 0;
        for (boolean b : repeatDays) if (b) count++;
        if (count == 7) return "Everyday";
        if (count == 0) return "Once";
        if (count == 5 && !repeatDays[0] && !repeatDays[6]) return "Mon - Fri";
        if (count == 2 && repeatDays[0] && repeatDays[6]) return "Weekends";

        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (repeatDays[i]) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(dayNames[i]);
            }
        }
        return sb.toString();
    }

    public String missionDescription() {
        switch (missionType) {
            case MATH:
                return "math [" + missionCount + "x]";
            case SHAKE:
                return "shake [" + (missionCount * 10) + "x]";
            case TYPING:
                return "typing quote";
            default:
                return "standard";
        }
    }

    public long calculateNextTriggerMs() {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        boolean hasRepeat = false;
        for (boolean b : repeatDays) if (b) hasRepeat = true;

        if (!hasRepeat) {
            if (target.before(now) || target.equals(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1);
            }
            return target.getTimeInMillis();
        }

        // Find next active day
        for (int i = 0; i < 8; i++) {
            int dayOfWeek = target.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun
            if (repeatDays[dayOfWeek] && target.after(now)) {
                return target.getTimeInMillis();
            }
            target.add(Calendar.DAY_OF_YEAR, 1);
        }

        return target.getTimeInMillis();
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("hour", hour);
            obj.put("minute", minute);
            obj.put("enabled", enabled);
            obj.put("label", label);
            obj.put("missionType", missionType.name());
            obj.put("missionCount", missionCount);
            JSONArray arr = new JSONArray();
            for (boolean b : repeatDays) arr.put(b);
            obj.put("repeatDays", arr);
        } catch (Exception e) {}
        return obj;
    }

    public static Alarm fromJson(JSONObject obj) {
        Alarm a = new Alarm();
        a.id = obj.optString("id", a.id);
        a.hour = obj.optInt("hour", 7);
        a.minute = obj.optInt("minute", 0);
        a.enabled = obj.optBoolean("enabled", true);
        a.label = obj.optString("label", "Alarm");
        try {
            a.missionType = MissionType.valueOf(obj.optString("missionType", "MATH"));
        } catch (Exception e) {
            a.missionType = MissionType.MATH;
        }
        a.missionCount = obj.optInt("missionCount", 3);
        JSONArray arr = obj.optJSONArray("repeatDays");
        if (arr != null) {
            for (int i = 0; i < Math.min(7, arr.length()); i++) {
                a.repeatDays[i] = arr.optBoolean(i, false);
            }
        }
        return a;
    }
}
