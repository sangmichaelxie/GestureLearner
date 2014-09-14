package de.dfki.ccaal.gestures;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class Distribution implements Parcelable {
	private final Map<String, Double> distribution;
	String best;
	double minDistance = Double.MAX_VALUE;

	public Distribution() {
		distribution = new HashMap<String, Double>();
	}

	private Distribution(Parcel in) {

		distribution = new HashMap<String, Double>();
		Bundle bundle = in.readBundle();
		for (String key : bundle.keySet()) {
			distribution.put(key, bundle.getDouble(key));
		}
		best = in.readString();
		minDistance = in.readDouble();
	}

	public void addEntry(String tag, double distance) {
		if (!distribution.containsKey(tag) || distance < distribution.get(tag)) {
			distribution.put(tag, distance);
			if (distance < minDistance) {
				minDistance = distance;
				best = tag;
			}
		}
	}

	public String getBestMatch() {
		return best;
	}

	public double getBestDistance() {
		return minDistance;
	}

	public int size() {
		return distribution.size();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		Bundle bundle = new Bundle();
		for (String key : distribution.keySet()) {
			bundle.putDouble(key, distribution.get(key));
		}
		out.writeBundle(bundle);
		out.writeString(best);
		out.writeDouble(minDistance);
	}

	public static final Creator<Distribution> CREATOR = new Creator<Distribution>() {

		@Override
		public Distribution[] newArray(int size) {
			return new Distribution[size];
		}

		@Override
		public Distribution createFromParcel(Parcel in) {
			return new Distribution(in);
		}
	};
}
