package com.cosmos.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
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
    private Ringtone ringtone;
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
        "THE UNIVERSE AWAITS"
    };
    private String targetQuote = "RISE AND SHINE";

    // Emergency Hold State
    private boolean isHoldingEmergency = false;
    private int holdProgress = 0;

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
        tvAlarmLabel.setText("[ " + alarm.label + " ]");

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
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            ringtone = RingtoneManager.getRingtone(this, alarmUri);
            if (ringtone != null) {
                ringtone.setStreamType(AudioManager.STREAM_ALARM);
                ringtone.play();
            }
        } catch (Exception e) {}

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            long[] pattern = {0, 600, 400, 600, 400, 800};
            vibrator.vibrate(pattern, 0); // 0 = loop forever
        }
    }

    private void stopAlarmSoundAndVibration() {
        if (ringtone != null && ringtone.isPlaying()) {
            try { ringtone.stop(); } catch (Exception e) {}
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
        tvMissionStatus.setText(String.format("MISSION: MATH (%d / %d)", mathSolved + 1, mathTotal));
        Random rnd = new Random();
        int a = rnd.nextInt(40) + 12;
        int b = rnd.nextInt(40) + 12;
        expectedAnswer = a + b;

        tvProblem.setText(String.format("%d + %d = [ ? ]", a, b));
        tvProblem.setVisibility(View.VISIBLE);
        etInput.setVisibility(View.VISIBLE);
        etInput.setText("");
        etInput.setHint("type answer & press Enter");
        etInput.requestFocus();

        btnAction.setText("[ SUBMIT ]");
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkMathAnswer();
            }
        });

        etInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    checkMathAnswer();
                    return true;
                }
                return false;
            }
        });
    }

    private void checkMathAnswer() {
        String ansStr = etInput.getText().toString().trim();
        try {
            int ans = Integer.parseInt(ansStr);
            if (ans == expectedAnswer) {
                mathSolved++;
                if (mathSolved >= mathTotal) {
                    Toast.makeText(this, "Wake-up Mission Complete!", Toast.LENGTH_SHORT).show();
                    dismissAlarm();
                } else {
                    Toast.makeText(this, "Correct! Next problem...", Toast.LENGTH_SHORT).show();
                    setupMathProblem();
                }
            } else {
                Toast.makeText(this, "Wrong! Try again", Toast.LENGTH_SHORT).show();
                etInput.setText("");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Enter a valid number", Toast.LENGTH_SHORT).show();
        }
    }

    // --- SHAKE MISSION ---
    private void setupShakeMission() {
        tvMissionStatus.setText(String.format("MISSION: SHAKE DEVICE (0 / %d)", shakeTotal));
        tvProblem.setText("Shake your BlackBerry to wake up!");
        tvProblem.setVisibility(View.VISIBLE);
        etInput.setVisibility(View.GONE);
        btnAction.setVisibility(View.GONE);

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
        if ((now - lastShakeTime) > 120) {
            float delta = Math.abs(x + y + z - lastX - lastY - lastZ);
            if (delta > 14) { // Shake detected
                lastShakeTime = now;
                shakeCount++;
                tvMissionStatus.setText(String.format("MISSION: SHAKE (%d / %d)", shakeCount, shakeTotal));

                if (shakeCount >= shakeTotal) {
                    if (sensorManager != null) sensorManager.unregisterListener(this);
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
        Random rnd = new Random();
        targetQuote = QUOTES[rnd.nextInt(QUOTES.length)];

        tvMissionStatus.setText("MISSION: TYPE QUOTE");
        tvProblem.setText("\"" + targetQuote + "\"");
        tvProblem.setVisibility(View.VISIBLE);
        etInput.setVisibility(View.VISIBLE);
        etInput.setText("");
        etInput.setHint("type quote exactly");
        etInput.requestFocus();

        btnAction.setText("[ SUBMIT ]");
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkTypingAnswer();
            }
        });

        etInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    checkTypingAnswer();
                    return true;
                }
                return false;
            }
        });
    }

    private void checkTypingAnswer() {
        String entered = etInput.getText().toString().trim();
        if (entered.equalsIgnoreCase(targetQuote)) {
            Toast.makeText(this, "Mission Complete! Have a great day!", Toast.LENGTH_SHORT).show();
            dismissAlarm();
        } else {
            Toast.makeText(this, "Quote does not match! Try again", Toast.LENGTH_SHORT).show();
        }
    }

    private void dismissAlarm() {
        stopAlarmSoundAndVibration();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        // Prevent accidental dismiss via back key
        Toast.makeText(this, "Complete mission to dismiss alarm!", Toast.LENGTH_SHORT).show();
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
