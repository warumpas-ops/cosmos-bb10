package com.cosmos.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class AlarmEditorActivity extends Activity {

    private AlarmStorage storage;
    private Alarm alarm;
    private boolean isNew = false;

    private TimePicker timePicker;
    private EditText etLabel;
    private final Button[] dayButtons = new Button[7];
    private final boolean[] selectedDays = new boolean[7];

    private Button btnMissionMath;
    private Button btnMissionShake;
    private Button btnMissionTyping;
    private Button btnMissionNormal;
    private MissionType selectedMission = MissionType.MATH;

    private Button btnCount1;
    private Button btnCount3;
    private Button btnCount5;
    private int selectedCount = 3;

    private Button btnDelete;
    private Button btnSave;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_editor);

        storage = new AlarmStorage(this);

        String alarmId = getIntent().getStringExtra("alarm_id");
        if (alarmId != null && !alarmId.isEmpty()) {
            alarm = storage.findById(alarmId);
        }

        if (alarm == null) {
            alarm = new Alarm();
            isNew = true;
        }

        timePicker = (TimePicker) findViewById(R.id.timePicker);
        etLabel = (EditText) findViewById(R.id.etLabel);
        btnDelete = (Button) findViewById(R.id.btnDelete);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnBack = (Button) findViewById(R.id.btnBack);

        // Days
        dayButtons[0] = (Button) findViewById(R.id.btnDaySun);
        dayButtons[1] = (Button) findViewById(R.id.btnDayMon);
        dayButtons[2] = (Button) findViewById(R.id.btnDayTue);
        dayButtons[3] = (Button) findViewById(R.id.btnDayWed);
        dayButtons[4] = (Button) findViewById(R.id.btnDayThu);
        dayButtons[5] = (Button) findViewById(R.id.btnDayFri);
        dayButtons[6] = (Button) findViewById(R.id.btnDaySat);

        // Missions
        btnMissionMath = (Button) findViewById(R.id.btnMissionMath);
        btnMissionShake = (Button) findViewById(R.id.btnMissionShake);
        btnMissionTyping = (Button) findViewById(R.id.btnMissionTyping);
        btnMissionNormal = (Button) findViewById(R.id.btnMissionNormal);

        // Counts
        btnCount1 = (Button) findViewById(R.id.btnCount1);
        btnCount3 = (Button) findViewById(R.id.btnCount3);
        btnCount5 = (Button) findViewById(R.id.btnCount5);

        // Load alarm data into UI
        timePicker.setIs24HourView(false);
        timePicker.setCurrentHour(alarm.hour);
        timePicker.setCurrentMinute(alarm.minute);
        etLabel.setText(alarm.label);

        System.arraycopy(alarm.repeatDays, 0, selectedDays, 0, 7);
        updateDayButtons();

        selectedMission = alarm.missionType;
        selectedCount = alarm.missionCount;
        updateMissionButtons();
        updateCountButtons();

        // Day click listeners
        for (int i = 0; i < 7; i++) {
            final int dayIndex = i;
            dayButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedDays[dayIndex] = !selectedDays[dayIndex];
                    updateDayButtons();
                }
            });
        }

        // Mission listeners
        btnMissionMath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedMission = MissionType.MATH;
                updateMissionButtons();
            }
        });
        btnMissionShake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedMission = MissionType.SHAKE;
                updateMissionButtons();
            }
        });
        btnMissionTyping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedMission = MissionType.TYPING;
                updateMissionButtons();
            }
        });
        btnMissionNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedMission = MissionType.NORMAL;
                updateMissionButtons();
            }
        });

        // Count listeners
        btnCount1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedCount = 1;
                updateCountButtons();
            }
        });
        btnCount3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedCount = 3;
                updateCountButtons();
            }
        });
        btnCount5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedCount = 5;
                updateCountButtons();
            }
        });

        btnDelete.setVisibility(isNew ? View.GONE : View.VISIBLE);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storage.deleteAlarm(alarm.id);
                AlarmScheduler.rescheduleAll(AlarmEditorActivity.this);
                Toast.makeText(AlarmEditorActivity.this, "Alarm Deleted", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAlarm();
            }
        });

        FontHelper.apply(findViewById(R.id.editorRoot), this);
    }

    private void updateDayButtons() {
        for (int i = 0; i < 7; i++) {
            if (selectedDays[i]) {
                dayButtons[i].setBackgroundColor(Color.parseColor("#F2F2F2"));
                dayButtons[i].setTextColor(Color.parseColor("#0F0F10"));
            } else {
                dayButtons[i].setBackgroundColor(Color.parseColor("#1C1C1F"));
                dayButtons[i].setTextColor(Color.parseColor("#808086"));
            }
        }
    }

    private void updateMissionButtons() {
        highlightButton(btnMissionMath, selectedMission == MissionType.MATH);
        highlightButton(btnMissionShake, selectedMission == MissionType.SHAKE);
        highlightButton(btnMissionTyping, selectedMission == MissionType.TYPING);
        highlightButton(btnMissionNormal, selectedMission == MissionType.NORMAL);

        LinearLayout countContainer = (LinearLayout) findViewById(R.id.countContainer);
        if (selectedMission == MissionType.MATH || selectedMission == MissionType.SHAKE) {
            countContainer.setVisibility(View.VISIBLE);
            TextView tvCountLabel = (TextView) findViewById(R.id.tvCountLabel);
            if (selectedMission == MissionType.MATH) {
                tvCountLabel.setText("PROBLEMS COUNT:");
                btnCount1.setText("[ 1x ]");
                btnCount3.setText("[ 3x ]");
                btnCount5.setText("[ 5x ]");
            } else {
                tvCountLabel.setText("SHAKE COUNT:");
                btnCount1.setText("[ 10x ]");
                btnCount3.setText("[ 30x ]");
                btnCount5.setText("[ 50x ]");
            }
        } else {
            countContainer.setVisibility(View.GONE);
        }
    }

    private void updateCountButtons() {
        highlightButton(btnCount1, selectedCount == 1);
        highlightButton(btnCount3, selectedCount == 3);
        highlightButton(btnCount5, selectedCount == 5);
    }

    private void highlightButton(Button b, boolean active) {
        if (active) {
            b.setBackgroundColor(Color.parseColor("#F2F2F2"));
            b.setTextColor(Color.parseColor("#0F0F10"));
        } else {
            b.setBackgroundColor(Color.parseColor("#1C1C1F"));
            b.setTextColor(Color.parseColor("#808086"));
        }
    }

    private void saveAlarm() {
        alarm.hour = timePicker.getCurrentHour();
        alarm.minute = timePicker.getCurrentMinute();
        alarm.label = etLabel.getText().toString().trim();
        if (alarm.label.isEmpty()) alarm.label = "Cosmic Alarm";

        System.arraycopy(selectedDays, 0, alarm.repeatDays, 0, 7);
        alarm.missionType = selectedMission;
        alarm.missionCount = selectedCount;
        alarm.enabled = true;

        storage.updateAlarm(alarm);
        AlarmScheduler.rescheduleAll(this);

        Toast.makeText(this, "Alarm Saved for " + alarm.formattedTime(), Toast.LENGTH_SHORT).show();
        finish();
    }
}
