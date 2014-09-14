package com.michaelxie.gesturelearner;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.util.List;


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
	private float[][] gestureDataContainer;
	private int rowsFilled;

	//Creates new instances of fragment
    public static TrainingFragment newInstance() {
        return new TrainingFragment();
    }

    public TrainingFragment() {
        // Required empty public constructor
    }
	private Button clearDataButton;
	private EditText trainingSetName, gestureName;
	private ToggleButton toggleLearnModeButton;
	private boolean isLearning;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
						MainActivity.recognitionService.startLearnMode(trainingSet, gesture);
						MainActivity.currTrainingSetName = "";
						MainActivity.currTrainingSetName += trainingSet;
						MainActivity.toast("Learning mode start for " + trainingSet + " gesture " + "\"" + gesture + "\"", getActivity().getApplicationContext());

						gestureDataContainer = new float[100][3]; //Capacity for a ~2 second gesture
						rowsFilled = 0;
						//Accel data
						((MainActivity) getActivity()).startAccelerometer();
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else {
					try {
						MainActivity.recognitionService.stopLearnMode();
						MainActivity.toast("Learning mode off", getActivity().getApplicationContext());
					} catch(Exception e) {
						e.printStackTrace();
					}
					toggleLearnModeButton.setText(toggleLearnModeButton.getTextOff());
					isLearning = false;
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
					MainActivity.recognitionService.deleteTrainingSet(trainingSet);
					MainActivity.toast("Data for training set " + trainingSet + " cleared.", getActivity().getApplicationContext());
				} catch(Exception e) {
					e.printStackTrace();
				}


			}
		});

		return v;
    }

	public void collectData(float[] xyz) {
		if(rowsFilled < gestureDataContainer.length) {
			gestureDataContainer[rowsFilled++] = xyz;
		} else {
			((MainActivity) getActivity()).stopAccelerometer();
			Log.i(TAG, "gestureDataContainer filled. Writing data.");
		}
	}



}
