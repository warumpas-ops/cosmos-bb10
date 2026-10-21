package com.cosmos.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
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

    // Loud Alarm Sound Buttons
    private Button btnSoundDigital;
    private Button btnSoundSiren;
    private Button btnSoundReveille;
    private Button btnSoundBuzzer;
    private Button btnTestSound;
    private String selectedSound = "digital";
    private MediaPlayer testPlayer;

    private Button btnDelete;
    private Button btnSave;
    private Button btnCancel;

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
        btnCancel = (Button) findViewById(R.id.btnCancel);

        // Days
        dayButtons[0] = (Button) findViewById(R.id.btnDaySun);
        dayButtons[1] = (Button) findViewById(R.id.btnDayMon);
        dayButtons[2] = (Button) findViewById(R.id.btnDayTue);
        dayButtons[3] = (Button) findViewById(R.id.btnDayWed);
        dayButtons[4] = (Button) findViewById(R.id.btnDayThu);
        dayButtons[5] = (Button) findViewById(R.id.btnDayFri);
        dayButtons[6] = (Button) findViewById(R.id.btnDaySat);

        // Sounds
        btnSoundDigital = (Button) findViewById(R.id.btnSoundDigital);
        btnSoundSiren = (Button) findViewById(R.id.btnSoundSiren);
        btnSoundReveille = (Button) findViewById(R.id.btnSoundReveille);
        btnSoundBuzzer = (Button) findViewById(R.id.btnSoundBuzzer);
        btnTestSound = (Button) findViewById(R.id.btnTestSound);

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

        selectedSound = alarm.soundType != null ? alarm.soundType : "digital";
        updateSoundButtons();

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

        // Sound listeners
        btnSoundDigital.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSound = "digital";
                updateSoundButtons();
                restartTestSoundIfPlaying();
            }
        });
        btnSoundSiren.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSound = "siren";
                updateSoundButtons();
                restartTestSoundIfPlaying();
            }
        });
        btnSoundReveille.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSound = "reveille";
                updateSoundButtons();
                restartTestSoundIfPlaying();
            }
        });
        btnSoundBuzzer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSound = "buzzer";
                updateSoundButtons();
                restartTestSoundIfPlaying();
            }
        });

        btnTestSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTestSound();
            }
        });

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
                stopTestSound();
                storage.deleteAlarm(alarm.id);
                AlarmScheduler.rescheduleAll(AlarmEditorActivity.this);
                Toast.makeText(AlarmEditorActivity.this, "Alarm Deleted", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTestSound();
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTestSound();
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

    private void updateSoundButtons() {
        highlightSoundBtn(btnSoundDigital, "digital".equals(selectedSound));
        highlightSoundBtn(btnSoundSiren, "siren".equals(selectedSound));
        highlightSoundBtn(btnSoundReveille, "reveille".equals(selectedSound));
        highlightSoundBtn(btnSoundBuzzer, "buzzer".equals(selectedSound));
    }

    private void highlightSoundBtn(Button btn, boolean selected) {
        if (selected) {
            btn.setBackgroundColor(Color.parseColor("#F2F2F2"));
            btn.setTextColor(Color.parseColor("#0F0F10"));
        } else {
            btn.setBackgroundColor(Color.parseColor("#1C1C1F"));
            btn.setTextColor(Color.parseColor("#808086"));
        }
    }

    private void toggleTestSound() {
        if (testPlayer != null && testPlayer.isPlaying()) {
            stopTestSound();
        } else {
            startTestSound();
        }
    }

    private void startTestSound() {
        stopTestSound();
        int soundRes = R.raw.alarm_digital_screamer;
        if ("siren".equals(selectedSound)) {
            soundRes = R.raw.alarm_nuclear_siren;
        } else if ("reveille".equals(selectedSound)) {
            soundRes = R.raw.alarm_military_reveille;
        } else if ("buzzer".equals(selectedSound)) {
            soundRes = R.raw.alarm_emergency_buzzer;
        }

        try {
            // Unmute and ensure media volume is audible
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int current = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (current < max / 2) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, max * 3 / 4, 0);
                }
            }

            testPlayer = MediaPlayer.create(this, soundRes);
            if (testPlayer != null) {
                testPlayer.setLooping(true);
                testPlayer.start();
                btnTestSound.setText("[ ■ STOP ]");
                btnTestSound.setBackgroundColor(Color.parseColor("#FF4444"));
                btnTestSound.setTextColor(Color.parseColor("#FFFFFF"));
            } else {
                Toast.makeText(this, "Audio resource unavailable", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Audio error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void restartTestSoundIfPlaying() {
        if (testPlayer != null && testPlayer.isPlaying()) {
            startTestSound();
        }
    }

    private void stopTestSound() {
        if (testPlayer != null) {
            try {
                if (testPlayer.isPlaying()) testPlayer.stop();
                testPlayer.reset();
                testPlayer.release();
            } catch (Exception e) {}
            testPlayer = null;
        }
        if (btnTestSound != null) {
            btnTestSound.setText("[ ▶ TEST ]");
            btnTestSound.setBackgroundColor(Color.parseColor("#252528"));
            btnTestSound.setTextColor(Color.parseColor("#F2F2F2"));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTestSound();
    }

    private void updateMissionButtons() {
        highlightBtn(btnMissionMath, selectedMission == MissionType.MATH);
        highlightBtn(btnMissionShake, selectedMission == MissionType.SHAKE);
        highlightBtn(btnMissionTyping, selectedMission == MissionType.TYPING);
        highlightBtn(btnMissionNormal, selectedMission == MissionType.NORMAL);

        LinearLayout countContainer = (LinearLayout) findViewById(R.id.countContainer);
        TextView tvCountLabel = (TextView) findViewById(R.id.tvCountLabel);

        if (selectedMission == MissionType.MATH) {
            countContainer.setVisibility(View.VISIBLE);
            tvCountLabel.setText("MATH PROBLEMS COUNT:");
            btnCount1.setText("[ 1x ]");
            btnCount3.setText("[ 3x ]");
            btnCount5.setText("[ 5x ]");
        } else if (selectedMission == MissionType.SHAKE) {
            countContainer.setVisibility(View.VISIBLE);
            tvCountLabel.setText("SHAKE TARGET COUNT:");
            btnCount1.setText("[ 10x ]");
            btnCount3.setText("[ 30x ]");
            btnCount5.setText("[ 50x ]");
        } else {
            countContainer.setVisibility(View.GONE);
        }
    }

    private void updateCountButtons() {
        highlightBtn(btnCount1, selectedCount == 1);
        highlightBtn(btnCount3, selectedCount == 3);
        highlightBtn(btnCount5, selectedCount == 5);
    }

    private void highlightBtn(Button btn, boolean selected) {
        if (selected) {
            btn.setBackgroundColor(Color.parseColor("#F2F2F2"));
            btn.setTextColor(Color.parseColor("#0F0F10"));
        } else {
            btn.setBackgroundColor(Color.parseColor("#1C1C1F"));
            btn.setTextColor(Color.parseColor("#808086"));
        }
    }

    private void saveAlarm() {
        alarm.hour = timePicker.getCurrentHour();
        alarm.minute = timePicker.getCurrentMinute();
        String lbl = etLabel.getText().toString().trim();
        alarm.label = lbl.isEmpty() ? "Alarm" : lbl;
        System.arraycopy(selectedDays, 0, alarm.repeatDays, 0, 7);
        alarm.soundType = selectedSound;
        alarm.missionType = selectedMission;
        alarm.missionCount = selectedCount;
        alarm.enabled = true;

        if (isNew) {
            storage.saveAlarm(alarm);
        } else {
            storage.updateAlarm(alarm);
        }

        AlarmScheduler.rescheduleAll(this);
        Toast.makeText(this, "Alarm set for " + alarm.formattedTime(), Toast.LENGTH_SHORT).show();
        finish();
    }
}
