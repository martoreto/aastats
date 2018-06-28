package com.github.martoreto.aastats;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.anastr.speedviewlib.Speedometer;
import com.github.martoreto.aauto.vex.CarStatsClient;
import com.google.android.apps.auto.sdk.DayNightStyle;
import com.google.android.apps.auto.sdk.StatusBarController;
import com.google.common.collect.ImmutableMap;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends CarFragment {
    private final String TAG = "DashboardFragment";

    private CarStatsClient mStatsClient;
    private WheelStateMonitor mWheelStateMonitor;

    private static final float DISABLED_ALPHA = 0.3f;

    private Speedometer mOutputPower;
    private Speedometer mChargingPressure;
    private Speedometer mOutputTorque;
    private ProgressBar mBrakeAccel;
    private ImageView mSteeringWheelAngle;
    private ImmutableMap<String, View> mGearViews;
    private TextView mOilTemp;
    private TextView mGearboxTemp;
    private TextView mBatteryVoltage;
    private Float mLastSpeedKmh;
    private int mAnimationDuration;
    private WheelStateMonitor.WheelState mWheelState;

    public static final float FULL_BRAKE_PRESSURE = 100.0f;

    private Map<String, Object> mLastMeasurements = new HashMap<>();
    private Handler mHandler = new Handler();

    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    protected void setupStatusBar(StatusBarController sc) {
        sc.setDayNightStyle(DayNightStyle.AUTO);
        sc.showAppHeader();
        sc.showBatteryLevel();
        sc.showClock();
        sc.showConnectivityLevel();
        sc.showMicButton();
        sc.showTitle();
        sc.setTitle(getContext().getString(R.string.dashboard_title));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach");


        Intent serviceIntent = new Intent(getContext(), CarStatsService.class);
        getContext().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        mAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CarStatsService.CarStatsBinder carStatsBinder = (CarStatsService.CarStatsBinder)iBinder;
            mStatsClient = carStatsBinder.getStatsClient();
            mWheelStateMonitor = carStatsBinder.getWheelStateMonitor();
            mLastMeasurements = mStatsClient.getMergedMeasurements();
            mStatsClient.registerListener(mCarStatsListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mStatsClient.unregisterListener(mCarStatsListener);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);

        mOutputPower = rootView.findViewById(R.id.output_power_view);
        mChargingPressure = rootView.findViewById(R.id.charging_pressure_view);
        mOutputTorque = rootView.findViewById(R.id.output_torque_view);
        mBrakeAccel = rootView.findViewById(R.id.brake_accel_view);
        mSteeringWheelAngle = rootView.findViewById(R.id.wheel_angle_image);
        mOilTemp = rootView.findViewById(R.id.oil_temp_view);
        mGearboxTemp = rootView.findViewById(R.id.gearbox_temp_view);
        mBatteryVoltage = rootView.findViewById(R.id.battery_voltage_view);

        mGearViews = ImmutableMap.<String, View>builder()
                .put("Park", rootView.findViewById(R.id.gear_P))
                .put("Reverse", rootView.findViewById(R.id.gear_R))
                .put("NoGear", rootView.findViewById(R.id.gear_N))
                .put("Gear1", rootView.findViewById(R.id.gear_1))
                .put("Gear2", rootView.findViewById(R.id.gear_2))
                .put("Gear3", rootView.findViewById(R.id.gear_3))
                .put("Gear4", rootView.findViewById(R.id.gear_4))
                .put("Gear5", rootView.findViewById(R.id.gear_5))
                .put("Gear6", rootView.findViewById(R.id.gear_6))
                .put("Gear7", rootView.findViewById(R.id.gear_7))
                .build();

        doUpdate();

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onActivate");
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onDeactivate");
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        Log.i(TAG, "onDestroyView");

        mOutputPower = null;
        mChargingPressure = null;
        mOutputTorque = null;
        mBrakeAccel = null;
        mSteeringWheelAngle = null;
        mOilTemp = null;
        mGearboxTemp = null;
        mBatteryVoltage = null;
        mGearViews = null;

        super.onDestroyView();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDetach() {
        Log.i(TAG, "onDetach");

        mStatsClient.unregisterListener(mCarStatsListener);
        getContext().unbindService(mServiceConnection);

        super.onDetach();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    private final CarStatsClient.Listener mCarStatsListener = new CarStatsClient.Listener() {
        @Override
        public void onNewMeasurements(String provider, Date timestamp, Map<String, Object> values) {
            mLastMeasurements.putAll(values);
            postUpdate();
        }

        @Override
        public void onSchemaChanged() {
            // do nothing
        }
    };

    private void postUpdate() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                doUpdate();
            }
        });
    }

    private void doUpdate() {
        if (mOutputPower == null) {
            return;
        }

        Float maxOutputPower = (Float) mLastMeasurements.get("maxOutputPower");
        if (maxOutputPower != null && mOutputPower.getMaxSpeed() != (int) (float)maxOutputPower) {
            mOutputPower.setMaxSpeed((int) (float) maxOutputPower);
        }

        Float currentOutputPower = (Float) mLastMeasurements.get("currentOutputPower");
        mOutputPower.speedTo(currentOutputPower == null ? 0.0f : currentOutputPower);

        Float currentChargingPressure = (Float) mLastMeasurements.get("absChargingAirPressure");
        mChargingPressure.speedTo(currentChargingPressure == null ? 0.0f : currentChargingPressure);

        Float currentOutputTorque = (Float) mLastMeasurements.get("currentTorque");
        mOutputTorque.speedTo(currentOutputTorque == null ? 0.0f : currentOutputTorque);

        Float brakePressure = (Float) mLastMeasurements.get("brakePressure");
        Float accelPos = (Float) mLastMeasurements.get("acceleratorPosition");
        if (brakePressure != null && accelPos != null) {
            float normalizedBrakePressure = Math.min(Math.max(0.0f, brakePressure / FULL_BRAKE_PRESSURE), 1.0f);
            boolean isBraking = normalizedBrakePressure > 0;
            mBrakeAccel.setRotation(isBraking ? 180.0f : 0.0f);
            //noinspection deprecation
            mBrakeAccel.setProgressTintList(ColorStateList.valueOf(getContext().getResources()
                    .getColor(isBraking ? R.color.car_accent: R.color.car_primary)));
            mBrakeAccel.setProgress((int) ((isBraking ? normalizedBrakePressure : accelPos) * 10000));
        } else {
            mBrakeAccel.setProgress(0);
        }

        // Footer

        Boolean reverseGear = (Boolean) mLastMeasurements.get("reverseGear.engaged");
        Boolean parkingBrake = (Boolean) mLastMeasurements.get("parkingBrake.engaged");
        String currentGear = (String) mLastMeasurements.get("currentGear");
        if (parkingBrake != null && parkingBrake) {
            currentGear = "Park";
        } else if (reverseGear != null && reverseGear) {
            currentGear = "Reverse";
        }
        for (ImmutableMap.Entry<String, View> gear : mGearViews.entrySet()) {
            gear.getValue().setSelected(currentGear != null && currentGear.equals(gear.getKey()));
        }
        
        // Right panel

        Float oilTemp = (Float) mLastMeasurements.get("oilTemperature");
        if (oilTemp == null) {
            mOilTemp.setText("----");
        } else {
            mOilTemp.setText(String.format(Locale.US,
                    getContext().getText(R.string.temp_format).toString(), oilTemp));
        }

        Float gearboxTemp = (Float) mLastMeasurements.get("gearboxOilTemperature");
        if (gearboxTemp == null) {
            mGearboxTemp.setText("----");
        } else {
            mGearboxTemp.setText(String.format(Locale.US,
                    getContext().getText(R.string.temp_format).toString(), gearboxTemp));
        }

        Float batteryVoltage = (Float) mLastMeasurements.get("batteryVoltage");
        if (batteryVoltage == null) {
            mBatteryVoltage.setText("----");
        } else {
            mBatteryVoltage.setText(String.format(Locale.US,
                    getContext().getText(R.string.volt_format).toString(), batteryVoltage));
        }

        // Last speed

        Float lastSpeed = (Float) mLastMeasurements.get("vehicleSpeed");
        String speedUnit = (String) mLastMeasurements.get("vehicleSpeed.unit");
        if (lastSpeed != null && speedUnit != null) {
            switch (speedUnit) {
                case "mph":
                    lastSpeed *= 1.60934f;
                    break;
            }
        }
        mLastSpeedKmh = lastSpeed;

        // Steering wheel angle

        Float currentWheelAngle = (Float) mLastMeasurements.get("wheelAngle");
        mWheelState = mWheelStateMonitor == null ? WheelStateMonitor.WheelState.WHEEL_UNKNOWN
                : mWheelStateMonitor.getWheelState();
        mSteeringWheelAngle.setRotation(currentWheelAngle == null ? 0.0f :
                Math.min(Math.max(-WheelStateMonitor.WHEEL_CENTER_THRESHOLD_DEG, -currentWheelAngle),
                        WheelStateMonitor.WHEEL_CENTER_THRESHOLD_DEG));

        animateAlpha(mOutputPower, currentOutputPower == null ? DISABLED_ALPHA : 1.0f);
        animateAlpha(mOutputTorque, currentOutputTorque == null ? DISABLED_ALPHA : 1.0f);
        animateAlpha(mBrakeAccel, brakePressure == null || accelPos == null ? DISABLED_ALPHA : 1.0f);
        for (ImmutableMap.Entry<String, View> gear : mGearViews.entrySet()) {
            animateAlpha(gear.getValue(), currentGear == null ? DISABLED_ALPHA : 1.0f);
        }
        animateAlpha(mSteeringWheelAngle, mWheelState == WheelStateMonitor.WheelState.WHEEL_DRIVING
                || mWheelState == WheelStateMonitor.WheelState.WHEEL_UNKNOWN ? 0.0f : 1.0f);
        animateAlpha(mChargingPressure, mWheelState != WheelStateMonitor.WheelState.WHEEL_DRIVING
                && mWheelState != WheelStateMonitor.WheelState.WHEEL_UNKNOWN ? 0.0f :
                (currentChargingPressure == null ? DISABLED_ALPHA : 1.0f));
    }

    private void animateAlpha(View view, float alpha) {
        if (view.getAlpha() == alpha) {
            return;
        }
        if (isVisible()) {
            view.animate().alpha(alpha).setDuration(mAnimationDuration).setListener(null);
        } else {
            view.setAlpha(alpha);
        }
    }

}
