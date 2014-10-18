package com.michaelxie.gesturelearner;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Toast;

import de.dfki.ccaal.gestures.*;


public class MainActivity extends Activity implements GestureDetector.OnGestureListener, SensorEventListener{
	public static String currTrainingSetName;
	private GestureDetectorCompat mDetector;
	private boolean isTrainingModeOn;
	private TrainingFragment trainingFragment;
	private TestFragment testFragment;

	private SensorManager mSensorManager;
	private Sensor accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		//Init sensors
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Log.i(TAG, mSensorManager.getSensorList(Sensor.TYPE_ALL).toString());
		accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (accelerometer == null){
			toast("No accelerometer on your device. Gestures will not work.", getApplicationContext());
			//End activity
			this.finish();
		}

		if (savedInstanceState == null) {
			testFragment = TestFragment.newInstance();
			trainingFragment = TrainingFragment.newInstance();
			getFragmentManager().beginTransaction().add(R.id.main_rel_layout, trainingFragment).commit();
			isTrainingModeOn = true;
		}

		// Instantiate the gesture detector with the
		// application context and an implementation of
		// GestureDetector.OnGestureListener
		mDetector = new GestureDetectorCompat(this,this);
	}

	private boolean isRegistered = false;
	public void startAccelerometer() {
		if(!isRegistered) mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME); //20 ms delay
		isRegistered = true;
	}

	public void stopAccelerometer() {
		if(isRegistered) mSensorManager.unregisterListener(this);
		isRegistered = false;
	}

	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
	}

	public static double[] convertFloatsToDoubles(float[] input)
	{
		if (input == null)
		{
			return null; // Or throw an exception - your choice
		}
		double[] output = new double[input.length];
		for (int i = 0; i < input.length; i++)
		{
			output[i] = input[i];
		}
		return output;
	}

	@Override
	public final void onSensorChanged(SensorEvent event) {
		// The light sensor returns a single value.
		// Many sensors return 3 values, one for each axis.
		double[] xyz = convertFloatsToDoubles(event.values);
		if(isTrainingModeOn) {
			trainingFragment.collectData(xyz);
		} else {

		}
		// Do something with this sensor value.
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopAccelerometer();
	}

	//For fling to switch fragments

	@Override
	public boolean onTouchEvent(MotionEvent event){
		this.mDetector.onTouchEvent(event);
		// Be sure to call the superclass implementation
		return super.onTouchEvent(event);
	}

	private final String TAG = "MainActivity";
	@Override
	public boolean onDown(MotionEvent event) {
		Log.i(TAG, "Down");
		return true;
	}

	@Override
	public boolean onFling(MotionEvent event1, MotionEvent event2,
						   float velocityX, float velocityY) {
		Log.i(TAG, "Fling");
		if(isTrainingModeOn) {
			getFragmentManager().beginTransaction().replace(R.id.main_rel_layout, testFragment).commit();
			isTrainingModeOn = false;
		} else {
			getFragmentManager().beginTransaction().replace(R.id.main_rel_layout, trainingFragment).commit();
			isTrainingModeOn = true;
		}

		return true;
	}

	@Override
	public void onLongPress(MotionEvent event) {
		Log.i(TAG, "LongPress");

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
							float distanceY) {
		Log.i(TAG, "Scroll");

		return true;
	}

	@Override
	public void onShowPress(MotionEvent event) {
		Log.i(TAG, "ShowPress");

	}
	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		Log.i(TAG, "SingleTapUp");
		return true;
	}

	public static void toast(String message, Context context) {
		try {
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
