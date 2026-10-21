package com.cosmos.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Random;

public class AlarmRingingActivity extends Activity implements SensorEventListener {

    private Alarm alarm;
    private MediaPlayer mediaPlayer;
    private Ringtone fallbackRingtone;
    private Vibrator vibrator;
    private final Handler handler = new Handler();

    private PixelMoonView moonView;
    private TextView tvAlarmTime;
    private TextView tvAlarmLabel;
    private TextView tvMissionStatus;
    private TextView tvProblem;
    private EditText etInput;
    private Button btnAction;
    private Button btnEmergency;
    private LinearLayout missionContainer;

    // Math Mission State
    private int mathTotal = 3;
    private int mathSolved = 0;
    private int expectedAnswer = 0;

    // Shake Mission State
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private int shakeTotal = 30;
    private int shakeCount = 0;
    private long lastShakeTime = 0;
    private float lastX, lastY, lastZ;

    // Typing Mission State
    private static final String[] QUOTES = {
        "RISE AND SHINE",
        "NEW DAY NEW BEGINNING",
        "WAKE UP AND CONQUER",
        "TIME TO FOCUS",
        "THE UNIVERSE AWAITS",
        "BREATHE DEEP AND STEP FORWARD",
        "EVERY SUNRISE IS A FRESH START"
    };
    private String targetQuote = "RISE AND SHINE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Turn on screen over lockscreen
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_alarm_ringing);

        String alarmId = getIntent().getStringExtra("alarm_id");
        AlarmStorage storage = new AlarmStorage(this);
        alarm = alarmId != null ? storage.findById(alarmId) : null;
        if (alarm == null) {
            alarm = new Alarm();
        }

        moonView = (PixelMoonView) findViewById(R.id.moonView);
        tvAlarmTime = (TextView) findViewById(R.id.tvAlarmTime);
        tvAlarmLabel = (TextView) findViewById(R.id.tvAlarmLabel);
        tvMissionStatus = (TextView) findViewById(R.id.tvMissionStatus);
        tvProblem = (TextView) findViewById(R.id.tvProblem);
        etInput = (EditText) findViewById(R.id.etInput);
        btnAction = (Button) findViewById(R.id.btnAction);
        btnEmergency = (Button) findViewById(R.id.btnEmergency);
        missionContainer = (LinearLayout) findViewById(R.id.missionContainer);

        tvAlarmTime.setText(alarm.formattedTime());
        tvAlarmLabel.setText("[ " + alarm.label + " · " + alarm.soundName() + " ]");

        startAlarmSoundAndVibration();
        setupMission();

        btnEmergency.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(AlarmRingingActivity.this, "Emergency Dismiss Triggered", Toast.LENGTH_SHORT).show();
                dismissAlarm();
                return true;
            }
        });

        FontHelper.apply(findViewById(R.id.ringRoot), this);
    }

    private void startAlarmSoundAndVibration() {
        // Force Alarm volume to MAXIMUM (Alarmy loud mode)
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0);
            }
        } catch (Exception e) {}

        // Select the chosen loud alarm audio
        int soundRes = R.raw.alarm_digital_screamer;
        if ("siren".equals(alarm.soundType)) {
            soundRes = R.raw.alarm_nuclear_siren;
        } else if ("reveille".equals(alarm.soundType)) {
            soundRes = R.raw.alarm_military_reveille;
        } else if ("buzzer".equals(alarm.soundType)) {
            soundRes = R.raw.alarm_emergency_buzzer;
        }

        try {
            mediaPlayer = MediaPlayer.create(this, soundRes);
            if (mediaPlayer != null) {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            } else {
                startFallbackRingtone();
            }
        } catch (Exception e) {
            startFallbackRingtone();
        }

        // Relentless looping vibration
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            long[] pattern = {0, 600, 300, 600, 300, 800};
            vibrator.vibrate(pattern, 0); // 0 = loop forever
        }
    }

    private void startFallbackRingtone() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            fallbackRingtone = RingtoneManager.getRingtone(this, alarmUri);
            if (fallbackRingtone != null) {
                fallbackRingtone.setStreamType(AudioManager.STREAM_ALARM);
                fallbackRingtone.play();
            }
        } catch (Exception e) {}
    }

    private void stopAlarmSoundAndVibration() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {}
            mediaPlayer = null;
        }
        if (fallbackRingtone != null && fallbackRingtone.isPlaying()) {
            try { fallbackRingtone.stop(); } catch (Exception e) {}
            fallbackRingtone = null;
        }
        if (vibrator != null) {
            try { vibrator.cancel(); } catch (Exception e) {}
        }
        AlarmReceiver.releaseWakeLock();
    }

    private void setupMission() {
        switch (alarm.missionType) {
            case MATH:
                mathTotal = Math.max(1, alarm.missionCount);
                mathSolved = 0;
                setupMathProblem();
                break;

            case SHAKE:
                shakeTotal = Math.max(10, alarm.missionCount * 10);
                shakeCount = 0;
                setupShakeMission();
                break;

            case TYPING:
                setupTypingMission();
                break;

            case NORMAL:
            default:
                tvMissionStatus.setText("[ STANDARD ALARM ]");
                tvProblem.setVisibility(View.GONE);
                etInput.setVisibility(View.GONE);
                btnAction.setText("[ DISMISS ALARM ]");
                btnAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismissAlarm();
                    }
                });
                break;
        }
    }

    // --- MATH MISSION ---
    private void setupMathProblem() {
        tvMissionStatus.setText(String.format("MATH MISSION [%d / %d]", mathSolved, mathTotal));
        etInput.setVisibility(View.VISIBLE);
        etInput.setText("");
        etInput.setHint("answer & press enter");
        etInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);

        Random r = new Random();
        int a = r.nextInt(40) + 12;
        int b = r.nextInt(35) + 8;
        int op = r.nextInt(3); // 0: +, 1: -, 2: *
        String opStr;

        if (op == 0) {
            expectedAnswer = a + b;
            opStr = "+";
        } else if (op == 1) {
            expectedAnswer = a - b;
            opStr = "-";
        } else {
            a = r.nextInt(12) + 4;
            b = r.nextInt(9) + 3;
            expectedAnswer = a * b;
            opStr = "×";
        }

        tvProblem.setVisibility(View.VISIBLE);
        tvProblem.setText(a + " " + opStr + " " + b + " = ?");
        btnAction.setText("[ SUBMIT ANSWER ]");

        etInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                checkMathAnswer();
                return true;
            }
        });

        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkMathAnswer();
            }
        });
    }

    private void checkMathAnswer() {
        String text = etInput.getText().toString().trim();
        try {
            int ans = Integer.parseInt(text);
            if (ans == expectedAnswer) {
                mathSolved++;
                if (mathSolved >= mathTotal) {
                    Toast.makeText(this, "Mission Complete! Good morning!", Toast.LENGTH_SHORT).show();
                    dismissAlarm();
                } else {
                    Toast.makeText(this, "Correct! Next problem...", Toast.LENGTH_SHORT).show();
                    setupMathProblem();
                }
            } else {
                Toast.makeText(this, "Incorrect! Try again", Toast.LENGTH_SHORT).show();
                etInput.setText("");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Enter a valid number", Toast.LENGTH_SHORT).show();
        }
    }

    // --- SHAKE MISSION ---
    private void setupShakeMission() {
        tvMissionStatus.setText(String.format("SHAKE MISSION [%d / %d]", shakeCount, shakeTotal));
        tvProblem.setVisibility(View.VISIBLE);
        tvProblem.setText("SHAKE YOUR PHONE VIGOROUSLY");
        etInput.setVisibility(View.GONE);
        btnAction.setText(String.format("[ %d / %d SHAKES ]", shakeCount, shakeTotal));
        btnAction.setEnabled(false);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        long now = System.currentTimeMillis();
        if ((now - lastShakeTime) > 180) {
            float speed = Math.abs(x + y + z - lastX - lastY - lastZ);
            if (speed > 14.0f) {
                lastShakeTime = now;
                shakeCount++;
                btnAction.setText(String.format("[ %d / %d SHAKES ]", shakeCount, shakeTotal));
                tvMissionStatus.setText(String.format("SHAKE MISSION [%d / %d]", shakeCount, shakeTotal));

                if (shakeCount >= shakeTotal) {
                    if (sensorManager != null) sensorManager.unregisterListener(this);
                    Toast.makeText(this, "Wake Up Complete!", Toast.LENGTH_SHORT).show();
                    dismissAlarm();
                }
            }
            lastX = x;
            lastY = y;
            lastZ = z;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // --- TYPING MISSION ---
    private void setupTypingMission() {
        Random r = new Random();
        targetQuote = QUOTES[r.nextInt(QUOTES.length)];

        tvMissionStatus.setText("[ TYPING MISSION ]");
        tvProblem.setVisibility(View.VISIBLE);
        tvProblem.setText("\"" + targetQuote + "\"");
        etInput.setVisibility(View.VISIBLE);
        etInput.setText("");
        etInput.setHint("type exact quote above");
        etInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        btnAction.setText("[ SUBMIT ]");

        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkTypingQuote();
            }
        });

        etInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                checkTypingQuote();
                return true;
            }
        });
    }

    private void checkTypingQuote() {
        String entered = etInput.getText().toString().trim();
        if (entered.equalsIgnoreCase(targetQuote)) {
            Toast.makeText(this, "Mission Complete! Have a great day!", Toast.LENGTH_SHORT).show();
            dismissAlarm();
        } else {
            Toast.makeText(this, "Quote doesn't match, keep typing!", Toast.LENGTH_SHORT).show();
        }
    }

    private void dismissAlarm() {
        stopAlarmSoundAndVibration();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        // Reschedule recurring alarms or disable one-time alarms
        boolean hasRepeat = false;
        for (boolean b : alarm.repeatDays) if (b) hasRepeat = true;
        if (!hasRepeat) {
            alarm.enabled = false;
            AlarmStorage s = new AlarmStorage(this);
            s.updateAlarm(alarm);
        }
        AlarmScheduler.rescheduleAll(this);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarmSoundAndVibration();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
}
