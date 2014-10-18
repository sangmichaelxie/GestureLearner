package com.michaelxie.gesturelearner;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link TestFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class TestFragment extends Fragment {
	private TextView text;
	private ToggleButton toggleTestModeButton;
	private boolean isClassifying;
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
		text.setText(s);
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
					try {
						isClassifying = true;
						toggleTestModeButton.setText(toggleTestModeButton.getTextOn());
						MainActivity.toast("Listening for gestures not implemented yet", getActivity().getApplicationContext());
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else {
					try {
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

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
