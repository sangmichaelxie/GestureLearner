package com.michaelxie.gesturelearner;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link TrainingFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class TrainingFragment extends Fragment {
	private final String TAG = "TrainingFragment";
	private float[][] gestureDataContainer = new float[100][3]; //Capacity for a ~2 second gesture
	private int rowsFilled;

	//UI components
	private Button clearDataButton;
	private EditText trainingSetName, gestureName;
	private ToggleButton toggleLearnModeButton;
	private boolean isLearning;

	File dir, root; //File storage

	//Creates new instances of fragment
    public static TrainingFragment newInstance() {
        return new TrainingFragment();
    }

    public TrainingFragment() {
        // Required empty public constructor
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		root = android.os.Environment.getExternalStorageDirectory();
		dir = new File (root.getAbsolutePath() + saveDir);
		dir.mkdirs();
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.fragment_training, container, false);

		trainingSetName = (EditText) v.findViewById(R.id.training_set);
		gestureName = (EditText) v.findViewById(R.id.gesture_name);
		toggleLearnModeButton = (ToggleButton) v.findViewById(R.id.toggleButton);
		clearDataButton = (Button) v.findViewById(R.id.clear_data);
		isLearning = false;

		toggleLearnModeButton.setTextOff("Learn Mode Off");
		toggleLearnModeButton.setTextOn("Learning gesture");
		toggleLearnModeButton.setChecked(false);
		toggleLearnModeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String trainingSet = trainingSetName.getText().toString();
				String gesture = gestureName.getText().toString();

				if(trainingSet.trim().length() == 0 || gesture.trim().length() == 0) {
					MainActivity.toast("Please provide the training set name and gesture name.", getActivity().getApplicationContext());
					toggleLearnModeButton.setText(toggleLearnModeButton.getTextOff());
					return;
				}

				if(!isLearning) {
					try {
						isLearning = true;
						toggleLearnModeButton.setText(toggleLearnModeButton.getTextOn());
						MainActivity.currTrainingSetName = "";
						MainActivity.currTrainingSetName += trainingSet;
						MainActivity.toast("Learning mode start for " + trainingSet + " gesture " + "\"" + gesture + "\"", getActivity().getApplicationContext());

						rowsFilled = 0;
						//Accel data
						((MainActivity) getActivity()).startAccelerometer();
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		clearDataButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String trainingSet = trainingSetName.getText().toString();
				if(trainingSet.trim().length() == 0) {
					MainActivity.toast("Please provide the training set name to delete.", getActivity().getApplicationContext());
					return;
				}

				try {
					MainActivity.toast("Data for training set " + trainingSet + " cleared.", getActivity().getApplicationContext());
				} catch(Exception e) {
					e.printStackTrace();
				}


			}
		});

		return v;
    }
	
	@Override
	public void onResume() {
		super.onResume();
		toggleLearnModeButton.setChecked(false);
	}

	private final String saveDir = "/GestureLearner";

	public void collectData(float[] xyz) {
		if(rowsFilled < gestureDataContainer.length) {
			gestureDataContainer[rowsFilled][0] = xyz[0];
			gestureDataContainer[rowsFilled][1] = xyz[1];
			gestureDataContainer[rowsFilled][2] = xyz[2];
			rowsFilled++;

		} else {
			//File name based on training set
			final String saveFileName = MainActivity.currTrainingSetName + ".txt";

			((MainActivity) getActivity()).stopAccelerometer();
			Log.i(TAG, "gestureDataContainer filled. Writing data.");
			try {
				writeLineToFile(saveFileName, gestureName.getText().toString()); //Label
				for (int i = 0; i < gestureDataContainer.length; i++) { //Data
					writeLineToFile(saveFileName, gestureDataContainer[i][0] + "," + gestureDataContainer[i][1] + "," + gestureDataContainer[i][2]);
				}
				writeLineToFile(saveFileName, "\n");
			} catch(Exception e) {
				e.printStackTrace();
			}

			Log.i("RESULT", readFromFile(saveFileName));

			rowsFilled = 0;

			try {
				MainActivity.toast("Wrote training example. Learning mode off", getActivity().getApplicationContext());
			} catch(Exception e) {
				e.printStackTrace();
			}
			toggleLearnModeButton.setText(toggleLearnModeButton.getTextOff());
			isLearning = false;
		}
	}


	private void writeLineToFile(String filename, String data) {
		try {
			File file = new File(dir, filename);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
			bw.write(data);
			bw.newLine();
			bw.close();
		}
		catch (IOException e) {
			Log.e(TAG, "File write failed: " + e.toString());
		}
	}


	private String readFromFile(String filename) {
		String ret = null;

		try {
			File file = new File(dir, filename);
			BufferedReader buf = new BufferedReader(new FileReader(file));
			String receiveString = "";
			StringBuilder stringBuilder = new StringBuilder();

			while ( (receiveString = buf.readLine()) != null ) {
				stringBuilder.append(receiveString).append('\n');
			}
			ret = stringBuilder.toString();
			buf.close();

		}
		catch (FileNotFoundException e) {
			Log.e(TAG, "File not found: " + e.toString());
		}
		catch (IOException e) {
			Log.e(TAG, "Can not read file: " + e.toString());
		}

		return ret;
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
