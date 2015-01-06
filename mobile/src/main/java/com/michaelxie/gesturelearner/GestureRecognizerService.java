package com.michaelxie.gesturelearner;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Michael Xie on 12/16/2014.
 */
public class GestureRecognizerService extends Service {
	/** interface for clients that bind */
	IBinder mBinder;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//TODO do something useful
		return Service.START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
}
