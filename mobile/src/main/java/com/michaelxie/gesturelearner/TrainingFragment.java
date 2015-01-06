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
import java.util.ArrayList;


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
	private double[][] gestureDataContainer = new double[3][100]; //Capacity for a ~2 second gesture
	private int numFilled;

	//UI components
	private Button clearDataButton;
	private EditText gestureNameUI;
	private ToggleButton toggleLearnModeButton;
	private boolean isLearning;

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
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.fragment_training, container, false);


		gestureNameUI = (EditText) v.findViewById(R.id.gesture_name);
		toggleLearnModeButton = (ToggleButton) v.findViewById(R.id.toggleButton);
		clearDataButton = (Button) v.findViewById(R.id.clear_data);
		isLearning = false;

		toggleLearnModeButton.setTextOff("Learn Mode Off");
		toggleLearnModeButton.setTextOn("Learning gesture");
		toggleLearnModeButton.setChecked(false);
		toggleLearnModeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String gesture = gestureNameUI.getText().toString();

				if(gesture.trim().length() == 0) {
					MainActivity.toast("Please provide the gesture name.", getActivity().getApplicationContext());
					toggleLearnModeButton.setText(toggleLearnModeButton.getTextOff());
					return;
				}

				if(!isLearning) {
					try {
						isLearning = true;
						toggleLearnModeButton.setText(toggleLearnModeButton.getTextOn());
						MainActivity.toast("Learning mode start for gesture " + "\"" + gesture + "\"", getActivity().getApplicationContext());

						numFilled = 0;
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
				try {
					String gestureName = gestureNameUI.getText().toString();
					String saveFileName = gestureName + ".txt";
					File file = new File(saveFileName);
					boolean success = file.delete();
					if(success) {
						MainActivity.toast("Data for gesture " + gestureNameUI + " cleared.", getActivity().getApplicationContext());
					} else {
						MainActivity.toast("Error clearing data for gesture " + gestureNameUI + ".", getActivity().getApplicationContext());

					}
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

	public void collectData(double[] xyz) {
		if(numFilled < gestureDataContainer[0].length) {
			gestureDataContainer[0][numFilled] = xyz[0];
			gestureDataContainer[1][numFilled] = xyz[1];
			gestureDataContainer[2][numFilled] = xyz[2];
			numFilled++;

		} else {
			//File name based on gesture name
			String gestureName = gestureNameUI.getText().toString();
			String saveFileName = gestureName + ".txt";

			((MainActivity) getActivity()).stopAccelerometer();
			Log.i(TAG, "gestureDataContainer filled. Writing data.");
			try {
				/*writes a training example in format [x1.....xn y1....yn z1.....zn]*/
				
				for (int i = 0; i < gestureDataContainer[0].length; i++) { //Data
					MainActivity.writeToFile(saveFileName, gestureDataContainer[0][i] + " ");
				}
				for (int i = 0; i < gestureDataContainer[1].length; i++) { //Data
					MainActivity.writeToFile(saveFileName, gestureDataContainer[1][i] + " ");
				}
				for (int i = 0; i < gestureDataContainer[2].length; i++) { //Data
					MainActivity.writeToFile(saveFileName, gestureDataContainer[2][i] + " ");
				}
				MainActivity.writeLineBreakToFile(saveFileName);
			} catch(Exception e) {
				e.printStackTrace();
			}

			numFilled = 0;

			try {
				MainActivity.toast("Wrote training example. Learning mode off", getActivity().getApplicationContext());
			} catch(Exception e) {
				e.printStackTrace();
			}
			toggleLearnModeButton.setText(toggleLearnModeButton.getTextOff());
			isLearning = false;
		}
	}


}
