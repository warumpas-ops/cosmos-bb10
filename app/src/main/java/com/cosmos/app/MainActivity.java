package com.cosmos.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {

    private AlarmStorage storage;
    private List<Alarm> alarms = new ArrayList<>();
    private AlarmAdapter adapter;

    private TextView tvNextCountdown;
    private TextView tvNextTime;
    private TextView tvNextMission;
    private ListView listAlarms;
    private TextView tvEmpty;

    private final Handler timeHandler = new Handler();
    private final Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            updateNextAlarmHero();
            timeHandler.postDelayed(this, 15000); // refresh every 15s
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage = new AlarmStorage(this);

        tvNextCountdown = (TextView) findViewById(R.id.tvNextCountdown);
        tvNextTime = (TextView) findViewById(R.id.tvNextTime);
        tvNextMission = (TextView) findViewById(R.id.tvNextMission);
        listAlarms = (ListView) findViewById(R.id.listAlarms);
        tvEmpty = (TextView) findViewById(R.id.tvEmpty);
        Button btnNewAlarm = (Button) findViewById(R.id.btnNewAlarm);

        listAlarms.setDivider(new ColorDrawable(Color.parseColor("#252528")));
        listAlarms.setDividerHeight(1);

        adapter = new AlarmAdapter();
        listAlarms.setAdapter(adapter);

        btnNewAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AlarmEditorActivity.class);
                startActivity(intent);
            }
        });

        listAlarms.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Alarm a = alarms.get(position);
                Intent intent = new Intent(MainActivity.this, AlarmEditorActivity.class);
                intent.putExtra("alarm_id", a.id);
                startActivity(intent);
            }
        });

        listAlarms.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Alarm a = alarms.get(position);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Alarm")
                        .setMessage("Delete alarm for " + a.formattedTime() + "?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                storage.deleteAlarm(a.id);
                                AlarmScheduler.rescheduleAll(MainActivity.this);
                                loadData();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            }
        });

        FontHelper.apply(findViewById(R.id.mainRoot), this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
        timeHandler.post(timeRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        timeHandler.removeCallbacks(timeRunnable);
    }

    private void loadData() {
        alarms = storage.loadAlarms();
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(alarms.isEmpty() ? View.VISIBLE : View.GONE);
        updateNextAlarmHero();
    }

    private void updateNextAlarmHero() {
        Alarm soonest = null;
        long soonestMs = Long.MAX_VALUE;

        for (Alarm a : alarms) {
            if (a.enabled) {
                long trigger = a.calculateNextTriggerMs();
                if (trigger < soonestMs) {
                    soonestMs = trigger;
                    soonest = a;
                }
            }
        }

        if (soonest != null && soonestMs < Long.MAX_VALUE) {
            long diffMs = soonestMs - System.currentTimeMillis();
            if (diffMs <= 0) diffMs = 0;

            long hours = diffMs / (1000 * 60 * 60);
            long minutes = (diffMs / (1000 * 60)) % 60;

            String countdownStr;
            if (hours > 0) {
                countdownStr = String.format("NEXT ALARM IN %dH %02dM", hours, minutes);
            } else {
                countdownStr = String.format("NEXT ALARM IN %d MINS", Math.max(1, minutes));
            }

            tvNextCountdown.setText(countdownStr);
            tvNextTime.setText(soonest.formattedTime());
            tvNextMission.setText("[ " + soonest.missionDescription() + " · " + soonest.repeatDescription() + " ]");
            tvNextMission.setVisibility(View.VISIBLE);
        } else {
            tvNextCountdown.setText("NO ACTIVE ALARMS");
            tvNextTime.setText("--:--");
            tvNextMission.setVisibility(View.GONE);
        }
    }

    private class AlarmAdapter extends BaseAdapter {
        @Override
        public int getCount() { return alarms.size(); }
        @Override
        public Object getItem(int position) { return alarms.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_alarm, parent, false);
            }

            final Alarm alarm = alarms.get(position);

            TextView tvTime = (TextView) convertView.findViewById(R.id.tvItemTime);
            TextView tvLabel = (TextView) convertView.findViewById(R.id.tvItemLabel);
            TextView tvSub = (TextView) convertView.findViewById(R.id.tvItemSub);
            final Button btnToggle = (Button) convertView.findViewById(R.id.btnToggle);

            tvTime.setText(alarm.formattedTime());
            tvLabel.setText(alarm.label);
            tvSub.setText(alarm.repeatDescription() + " · " + alarm.missionDescription() + " · " + alarm.soundName());

            if (alarm.enabled) {
                tvTime.setTextColor(Color.parseColor("#F2F2F2"));
                tvLabel.setTextColor(Color.parseColor("#F2F2F2"));
                tvSub.setTextColor(Color.parseColor("#808086"));
                btnToggle.setBackgroundColor(Color.parseColor("#F2F2F2"));
                btnToggle.setTextColor(Color.parseColor("#0F0F10"));
                btnToggle.setText("ON");
            } else {
                tvTime.setTextColor(Color.parseColor("#55555C"));
                tvLabel.setTextColor(Color.parseColor("#55555C"));
                tvSub.setTextColor(Color.parseColor("#404045"));
                btnToggle.setBackgroundColor(Color.parseColor("#1C1C1F"));
                btnToggle.setTextColor(Color.parseColor("#606066"));
                btnToggle.setText("OFF");
            }

            btnToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alarm.enabled = !alarm.enabled;
                    storage.updateAlarm(alarm);
                    AlarmScheduler.rescheduleAll(MainActivity.this);
                    loadData();
                }
            });

            FontHelper.apply(convertView, MainActivity.this);
            return convertView;
        }
    }
}
