package com.michaelxie.gesturelearner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;
import org.jtransforms.fft.DoubleFFT_1D;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link TestFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class TestFragment extends Fragment {
	private final String TAG = "TestFragment";
	private TextView text;
	private ToggleButton toggleTestModeButton;
	private boolean isClassifying;
	int windowSize = 100;
	int classificationRate = 2; //1 is fastest, slowest is windowSize
	private double[] gestureDataContainer = new double[3*windowSize]; //Capacity for a ~2 second gesture
	private int numFilled;

	/* Here we store the current values of acceleration, one for each axis */
	private double xAccel;
	private double yAccel;
	private double zAccel;

	/* And here the previous ones */
	private double xPreviousAccel;
	private double yPreviousAccel;
	private double zPreviousAccel;

	/* Used to suppress the first shaking */
	private boolean firstUpdate = true;

	/*What acceleration difference would we assume as a rapid movement? */
	private final double startThreshold = 0.5;

	//Concurrency
	private volatile boolean isRunning;
	private ExecutorService threadPool;
	//Preprocess and classify on different thread
	Runnable workerThread = new RecognizerThread();

	/**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     */
    public static TestFragment newInstance() {
        TestFragment fragment = new TestFragment();
        return fragment;
    }
    public TestFragment() {
        // Required empty public constructor
    }

	public void displayResult(String s) {
		final String displayText = s;
		MainActivity.singleton.runOnUiThread(new Runnable() {
			public void run() {
				text.setText(displayText);
			}
		});
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		threadPool = Executors.newFixedThreadPool(1);
		isRunning = false;
	}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.fragment_test, container, false);
		text = (TextView) v.findViewById(R.id.result_text);
		toggleTestModeButton = (ToggleButton) v.findViewById(R.id.toggleTestModeButton);
		isClassifying = false;
		toggleTestModeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(!isClassifying) {
					//Start classifying from a data stream
					try {
						isClassifying = true;
						toggleTestModeButton.setText(toggleTestModeButton.getTextOn());
						numFilled = 0;
						firstUpdate = true;
						((MainActivity) getActivity()).startAccelerometer();

					} catch(Exception e) {
						e.printStackTrace();
					}
				} else {
					try {
						((MainActivity) getActivity()).stopAccelerometer();
						MainActivity.toast("Stopped listening for gestures", getActivity().getApplicationContext());
					} catch(Exception e) {
						e.printStackTrace();
					}
					toggleTestModeButton.setText(toggleTestModeButton.getTextOff());
					isClassifying = false;
				}
			}
		});
        return v;


    }

	/* Store the acceleration values given by the sensor */
	private void updateAccelParameters(double xNewAccel, double yNewAccel,
									   double zNewAccel) {
                /* we have to suppress the first change of acceleration, it results from first values being initialized with 0 */
		if (firstUpdate) {
			xPreviousAccel = xNewAccel;
			yPreviousAccel = yNewAccel;
			zPreviousAccel = zNewAccel;
			firstUpdate = false;
		} else {
			xPreviousAccel = xAccel;
			yPreviousAccel = yAccel;
			zPreviousAccel = zAccel;
		}
		xAccel = xNewAccel;
		yAccel = yNewAccel;
		zAccel = zNewAccel;
	}

	/* If the values of acceleration have changed on at least two axises, we are probably in a shake motion */
	private boolean isAccelerationChanged() {
		double deltaX = Math.abs(xPreviousAccel - xAccel);
		double deltaY = Math.abs(yPreviousAccel - yAccel);
		double deltaZ = Math.abs(zPreviousAccel - zAccel);
		return (deltaX > startThreshold && deltaY > startThreshold)
				|| (deltaX > startThreshold && deltaZ > startThreshold)
				|| (deltaY > startThreshold && deltaZ > startThreshold);
	}

	int startIndex = 0;
	int gestureCounter = 0;
	boolean gestureInitiated = false;

	double[] unwrappedContainer = new double[3 * windowSize];
	public void collectData(double[] xyz) {
		//Wrap-around windows for each axis
		gestureDataContainer[startIndex] = xyz[0];
		gestureDataContainer[startIndex + windowSize] = xyz[1];
		gestureDataContainer[startIndex + 2 * windowSize] = xyz[2];

		//Log.i(TAG, "Incoming data: ("+xyz[0]+", " + xyz[1] + ", " + xyz[2] + ")");
		//wrap-around index
		if(++startIndex >= windowSize) startIndex = 0;

		//Keep tabs on current accelerations
		updateAccelParameters(xyz[0], xyz[1], xyz[2]);

		//for filling the first time
		if(++numFilled < windowSize) return;

		//Classify every classificationRate updates
		if(startIndex % classificationRate != 0) return;

		//If changed above a threshold, continue
		if (!isAccelerationChanged() && !gestureInitiated) {
			return;
		} else if(gestureInitiated) { //record until end of windowSize when gesture has been initiated
			gestureCounter++;
			Log.i(TAG, "Recording gesture t=" + gestureCounter);
			if(gestureCounter < windowSize) return;
		}
		else {
			Log.i(TAG, "gesture initiated");
			gestureCounter = 60; //Assume that it starts recording late
			gestureInitiated = true;
			return;
		}

		gestureInitiated = false;
		threadPool.execute(workerThread);
	}

	public class RecognizerThread implements Runnable {
		public RecognizerThread(){}

		@Override
		public void run() {
			unwrapData();
			boolean isGesture = truncateData(unwrappedContainer);
			if (!isGesture) {
				return;
			}

			//Smooth data
			smoothData(unwrappedContainer);

			//N-point fft
			double[] featureVec = getFFTFeatureVec(unwrappedContainer);

			FeatureNode[] liblinearFeatureVec = convertToLibLinear(featureVec);

			double prediction = Linear.predict(MainActivity.singleton.model, liblinearFeatureVec);
			System.out.println(prediction);

			String result = MainActivity.singleton.getGestureNames().get((int) prediction);
			displayResult(result);
		}

	}

	public void printArr(double[] data) {
		for(int i = 0; i < data.length; i++) {
			System.out.print(data[i] + " ");
		}
		System.out.println();
	}

	public FeatureNode[] convertToLibLinear(double[] featureVec) {
		FeatureNode[] liblinearFeatureVec = new FeatureNode[featureVec.length];
		for(int j = 0; j < featureVec.length; j++) {
			liblinearFeatureVec[j] = new FeatureNode(j + 1, featureVec[j]);
		}
		return liblinearFeatureVec;
	}

	public double[] getFFTFeatureVec(double[] data) {
		Log.i(TAG, "fft");

		int N = 21;

		DoubleFFT_1D fft = new DoubleFFT_1D(N);
		double[] x = new double[N];
		double[] y = new double[N];
		double[] z = new double[N];

		System.arraycopy(data, 0, x, 0, N);
		System.arraycopy(data, windowSize, y, 0, N);
		System.arraycopy(data, 2 * windowSize, z, 0, N);

		fft.realForward(x);
		fft.realForward(y);
		fft.realForward(z);

		double xLast =  Math.sqrt(Math.pow(x[(N-1)], 2) + Math.pow(x[1], 2));
		for(int k = 1; k < (N-1) / 2; k++) {
			x[k] = Math.sqrt(Math.pow(x[2*k], 2) + Math.pow(x[2*k + 1], 2));
		}
		x[(N-1)/2] = xLast;

		//Take only the first (N+1)/2 elements of x

		double[] featureVector = new double[data.length + 3 * ((N + 1) / 2)];
		System.arraycopy(data, 0, featureVector, 0, data.length);
		System.arraycopy(x, 0, featureVector, data.length, (N + 1) / 2);
		System.arraycopy(y, 0, featureVector, data.length + (N + 1) / 2, (N + 1) / 2);
		System.arraycopy(z, 0, featureVector, data.length + (N + 1), (N + 1) / 2);
		return featureVector;
	}

	public void smoothData(double[] data) {
		Log.i(TAG, "smooth");

		double alpha = 0.3;
		ExponentialMovingAverage filterX = new ExponentialMovingAverage(alpha);
		ExponentialMovingAverage filterY = new ExponentialMovingAverage(alpha);
		ExponentialMovingAverage filterZ = new ExponentialMovingAverage(alpha);

		for(int i = 0; i < windowSize; i++) {
			data[i] = filterX.average(data[i]);
			data[i + windowSize] = filterY.average(data[i + windowSize]);
			data[i + 2*windowSize] = filterZ.average(data[i + 2*windowSize]);
		}
	}

	public double[] concat(double[] a, double[] b) {
		int aLen = a.length;
		int bLen = b.length;
		double[] c= new double[aLen + bLen];
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);
		return c;
	}

	//computes std of unwrappedContainer
	private double std(double[] data) {
		double total = 0;

		for(int i = 0; i < data.length; i++){
			total += data[i]; // this is the calculation for summing up all the values
		}

		double mean = total / data.length;

		double variance = 0;
		for(int i = 0; i < data.length; i++){
			variance += Math.pow(data[i] - mean, 2);
		}
		variance /= data.length;
		return Math.sqrt(variance);

	}


	public boolean truncateData(double[] data) {

		printArr(data);
		Log.i(TAG, "truncate");

		int noiseDelta = 4;
		int truncatedStartIndex = noiseDelta;
		int truncatedEndIndex = windowSize - 1;
		double noiseNormThreshold = Math.sqrt(std(data)) / 4.57;

		//Truncate from the left
		for(int k = noiseDelta; k < windowSize; k++) {
			double diffNorm = Math.sqrt(Math.pow(data[k] - data[noiseDelta], 2)
					+ Math.pow(data[k + windowSize] - data[noiseDelta + windowSize], 2)
					+ Math.pow(data[k + 2 * windowSize] - data[noiseDelta + 2 * windowSize], 2));

			if (diffNorm < noiseNormThreshold) {
				truncatedStartIndex++;
			} else {
				break;
			}
		}

		//Truncate from the right
		for(int k = windowSize - 1; k >= 0; k--) {
			double diffNorm = Math.sqrt(Math.pow(data[k] - data[windowSize - 1], 2)
					+ Math.pow(data[k + windowSize] - data[2 * windowSize - 1], 2)
					+ Math.pow(data[k + 2 * windowSize] - data[3 * windowSize - 1], 2));

			if (diffNorm < noiseNormThreshold) {
				truncatedEndIndex--;
			}
			else{
				break;
			}
		}

		Log.i(TAG, noiseNormThreshold + ", " + truncatedStartIndex + ", " + truncatedEndIndex);

		//If the meat of the gesture spans more than some percentage of time points, then continue to predict it
		if(truncatedEndIndex - truncatedStartIndex < windowSize / 3) {
			return false;
		}

		Log.i(TAG, "Passed truncate");

		System.arraycopy(interpolate(truncatedStartIndex, truncatedEndIndex, data), 0, data, 0, 3 * windowSize);
		return true;
	}

	private double mapToRaw(double mappedIndex, int start, int end, double[] data, int offset) {
		int prevPoint = (int)mappedIndex;
		int nextPoint = prevPoint + 1;

		if(nextPoint > end + offset) {
			return data[end + offset];
		} else if(prevPoint < start + offset) {
			return data[start + offset];
		}
		else {
			double slope = (data[nextPoint] - data[prevPoint]) / (nextPoint - prevPoint);
			double intercept = slope * (prevPoint) + data[prevPoint];
			return slope * mappedIndex + intercept;
		}
	}

	//Assumes 0 to windowSize-1 array
	private double[] interpolate(int start, int end, double[] data) {
		double[] interpolatedData = new double[data.length];
		double scale = (double)(end - start) / (windowSize - 1);

		for(int i = 0; i < windowSize; i++) {
			//map to truncated range
			double mappedIndexX = i * scale + start;
			double mappedIndexY = mappedIndexX + windowSize;
			double mappedIndexZ = mappedIndexX + 2 * windowSize;

			interpolatedData[i] = mapToRaw(mappedIndexX, start, end, data, 0);
			interpolatedData[i + windowSize] = mapToRaw(mappedIndexY, start, end, data, windowSize);
			interpolatedData[i + 2 * windowSize] = mapToRaw(mappedIndexZ, start, end, data, 2 * windowSize);
		}
		return interpolatedData;
	}

	private void unwrapData() {
		//Unwrap data
		for(int i = startIndex; i < windowSize; i++) {
			unwrappedContainer[i - startIndex] = gestureDataContainer[i];
			unwrappedContainer[i - startIndex + windowSize] = gestureDataContainer[i + windowSize];
			unwrappedContainer[i - startIndex + 2 * windowSize] = gestureDataContainer[i + 2 * windowSize];
		}
		for(int i = 0; i < startIndex; i++) {
			unwrappedContainer[windowSize - startIndex + i] = gestureDataContainer[i];
			unwrappedContainer[2 * windowSize - startIndex + i] = gestureDataContainer[i + windowSize];
			unwrappedContainer[3 * windowSize - startIndex + i] = gestureDataContainer[i + 2 * windowSize];
		}
	}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

	// Method to start the service
	public void startRecognition(View view) {
		getActivity().getApplicationContext().startService(new Intent(getActivity().getBaseContext(), GestureRecognizerService.class));
	}

	// Method to stop the service
	public void stopRecognition(View view) {
		getActivity().getApplicationContext().stopService(new Intent(getActivity().getBaseContext(), GestureRecognizerService.class));
	}

}

class ExponentialMovingAverage {
	private double alpha;
	private Double oldValue;
	public ExponentialMovingAverage(double alpha) {
		this.alpha = alpha;
	}

	public double average(double value) {
		if (oldValue == null) {
			oldValue = value;
			return value;
		}
		double newValue = oldValue + alpha * (value - oldValue);
		oldValue = newValue;
		return newValue;
	}
}