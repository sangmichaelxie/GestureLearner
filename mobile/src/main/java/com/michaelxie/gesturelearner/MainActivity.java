package com.michaelxie.gesturelearner;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;


public class MainActivity extends Activity implements GestureDetector.OnGestureListener, SensorEventListener{
	public static String currTrainingSetName;
	public static MainActivity singleton;
	private static final String TAG = "MainActivity";
	private GestureDetectorCompat mDetector;
	private boolean isTrainingModeOn;
	private TrainingFragment trainingFragment;
	private TestFragment testFragment;

	private SensorManager mSensorManager;
	private Sensor accelerometer;

	public static File dir, root; //File storage
	public static final String saveDir = "/GestureLearner";

	public ArrayList<String> gestureNames;
	private int numGestures;

	public Model model;

	private ExecutorService threadPool;
	private Runnable loaderThread = new LoaderThread(false);


	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		threadPool = Executors.newFixedThreadPool(1);
		singleton = this;
		/*if(!checkExternalMedia()) {
			toast("No storage on your device. Gestures will not work.", getApplicationContext());
			//End activity
			this.finish();
		}*/

		root = android.os.Environment.getExternalStorageDirectory();
		dir = new File (root.getAbsolutePath() + saveDir);
		dir.mkdirs();

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

		//Load SVM model
		threadPool.execute(loaderThread);
	}

	public ArrayList<String> getGestureNames() {
		return gestureNames;
	}

	private void loadSavedModel() {
		// load model or use it directly
		File modelFile = new File(dir, "model.dat");
		try {
			Model.load(modelFile);
		}
		catch(Exception e) {
			Log.e(TAG, "Error saving SVM model");
		}
	}

	private void initModelFromFile() {
		gestureNames = new ArrayList<>();
		int numExamples = 0;
		int numFeatures = 0;
		File[] files = MainActivity.dir.listFiles();
		ArrayList<FeatureNode[]> trainingSet = new ArrayList<FeatureNode[]>();
		ArrayList<Integer> y = new ArrayList<Integer>();

		Integer classVal = 0;
		for (File file : files) {
			if (file.isFile() && file.getName().endsWith(".txt")){
				//Get name of gesture
				String[] gestureName = file.getName().split("\\.");
				gestureNames.add(gestureName[0]);


				double[][] data = readFromFile(file.getName());
				numExamples += data.length;
				for(int i = 0; i < data.length; i++) {
					//Truncate data in place
					testFragment.truncateData(data[i]);
					testFragment.smoothData(data[i]);
					double[] featureVec = testFragment.getFFTFeatureVec(data[i]);
					/*for(int j = 0; j < featureVec.length; j++) {
						System.out.print(featureVec[j] + " ");
					}
					System.out.println();*/


					if(numFeatures == 0) numFeatures = featureVec.length;
					trainingSet.add(testFragment.convertToLibLinear(featureVec));
					y.add(classVal);
				}
				classVal++;
			}
		}
		numGestures = gestureNames.size();

		System.out.println(gestureNames);

		//Get value list
		double[] targetValues = new double[y.size()];
		for(int i = 0; i < y.size(); i++) {
			targetValues[i] = y.get(i).doubleValue();
			//System.out.print(targetValues[i] + " ");
		}
		System.out.println();

		//Convert example list
		FeatureNode[][] liblinearExamples = new FeatureNode[trainingSet.size()][];
		for(int i = 0; i < trainingSet.size(); i++) {
			liblinearExamples[i] = trainingSet.get(i);
		}

		Problem problem = new Problem();
		problem.l = numExamples; // number of training examples
		problem.n = numFeatures; // number of features
		problem.x = liblinearExamples;// feature nodes
		problem.y = targetValues;// target values

		SolverType solver = SolverType.L2R_L2LOSS_SVC; // -s 1
		double C = 1.0;    // cost of constraints violation
		double eps = 0.1;
		Parameter parameter = new Parameter(solver, C, eps);
		model = Linear.train(problem, parameter);
		File modelFile = new File(dir, "model.dat");
		try {
			model.save(modelFile);
		}
		catch(Exception e) {
			Log.e(TAG, "Error saving SVM model");
		}

		/*for(int j = 0; j < model.getLabels().length; j++) {
			System.out.print(model.getLabels()[j] + " ");
		}
		System.out.println();

		for(int i = 0; i < trainingSet.size(); i++) {
			System.out.print(Linear.predict(model, liblinearExamples[i]) + " ");
		}
		System.out.println();*/
	}

	public class LoaderThread implements Runnable {
		private boolean initFromFile;
		public LoaderThread(boolean doFullInit){
			initFromFile = doFullInit;
		}

		@Override
		public void run() {
			if(model == null || initFromFile) {
				Log.i(TAG, "Initializing model from data files");
				initModelFromFile();
			} else {
				Log.i(TAG, "Initializing saved model");
				loadSavedModel();
			}
		}

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
			return null; // Or throw an exception
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
		double[] xyz = convertFloatsToDoubles(event.values);
		if(isTrainingModeOn) {
			trainingFragment.collectData(xyz);
		} else {
			testFragment.collectData(xyz);
		}
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

	public static void writeToFile(String filename, String data) {
		try {
			File file = new File(dir, filename);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
			bw.write(data);
			bw.close();
		}
		catch (IOException e) {
			Log.e(TAG, "File write failed: " + e.toString());
		}
	}

	public static void writeLineBreakToFile(String filename) {
		try {
			File file = new File(dir, filename);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
			bw.newLine();
			bw.close();
		}
		catch (IOException e) {
			Log.e(TAG, "File write failed: " + e.toString());
		}
	}


	/*returns an m by n array of doubles, m being the number of training examples, and n the length of each training example*/
	public static double[][] readFromFile(String filename) {
		ArrayList<double[]> container = new ArrayList<double[]>();
		try {
			File file = new File(dir, filename);
			BufferedReader buf = new BufferedReader(new FileReader(file));
			String receiveString = "";
			while ( (receiveString = buf.readLine()) != null ) {
				try {
					String[] trainingExampleStringArr = receiveString.split(" ");
					double[] trainingExample = new double[trainingExampleStringArr.length];

					for (int i = 0; i < trainingExampleStringArr.length; i++) {
						trainingExample[i] = Double.valueOf(trainingExampleStringArr[i]);
					}
					container.add(trainingExample);
				}
				catch(Exception e) {}
			}
			buf.close();
			double[][] ret = new double[container.size()][];
			return container.toArray(ret);
		}
		catch (FileNotFoundException e) {
			Log.e(TAG, "File not found: " + e.toString());
		}
		catch (IOException e) {
			Log.e(TAG, "Can not read file: " + e.toString());
		}
		catch (Exception e) {
			Log.e(TAG, "Something went wrong loading the data files. Try deleting the gestures.");
		}
		return null;
	}

	private boolean checkExternalMedia(){
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// Can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// Can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Can't read or write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		return mExternalStorageAvailable && mExternalStorageWriteable;
	}

}
