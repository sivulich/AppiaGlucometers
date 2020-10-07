package com.appia.main;

import android.R.drawable;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.appia.Ble.BleProfileService;
import com.appia.Ble.BleProfileServiceReadyActivity;
import com.appia.onetouch.OnetouchInfo;
import com.appia.onetouch.OnetouchManager;
import com.appia.onetouch.OnetouchMeasurement;
import com.appia.onetouch.OnetouchService;
import com.appia.bioland.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

// TODO The GlucoseActivity should be rewritten to use the service approach, like other do.
public class OnetouchActivity extends BleProfileServiceReadyActivity<OnetouchService.OnetouchBinder> {
	@SuppressWarnings("unused")
	private static final String TAG = "GlucoseActivity";

	OnetouchService.OnetouchBinder mBinder;
	private int mBatteryCapacity;
	private byte[] mSerialNumber;

	private TextView batteryLevelView;
	private TextView statusView;
	private ProgressBar progressBar;
	private TextView unitView;
	private ListView mListView;
	private MeasurementsArrayAdapter mMeasArray;

	@Override
	protected void onCreateView(final Bundle savedInstanceState) {
		setContentView(R.layout.activity_main);
		setGUI();
	}

	@Override
	protected void onInitialize(final Bundle savedInstanceState) {
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, makeIntentFilter());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
	}

	private void setGUI() {

		// Measurements units
		unitView = findViewById(R.id.unit);

		// Device battery level
		batteryLevelView = findViewById(R.id.battery);

		// Device battery level
		statusView = findViewById(R.id.status);

		// Measurement progress bar
		progressBar = findViewById(R.id.progressBar);
		progressBar.setVisibility(View.INVISIBLE);

		// Measurements list view
		mListView = findViewById(R.id.list_view);

		// Measurement array adapter
		mMeasArray = new MeasurementsArrayAdapter(this,R.layout.measurement_item);
		mMeasArray.setNotifyOnChange(true);
		mListView.setAdapter(mMeasArray);
	}

	@Override
	protected void setDefaultUI() {
		batteryLevelView.setText(R.string.not_available);
	}

	@Override
	protected void onServiceBound(final OnetouchService.OnetouchBinder binder) {

		// Store binder
		mBinder = binder;

		// Update gui
		onMeasurementsReceived();
	}

	@Override
	protected void onServiceUnbound() {
		mBinder = null;
		// TODO: update gui??
	}

	@Override
	public void onDeviceDisconnected(@NonNull final BluetoothDevice device) {
		super.onDeviceDisconnected(device);
		runOnUiThread(() -> batteryLevelView.setText(R.string.not_available));
		statusView.setBackground(ContextCompat.getDrawable(this,drawable.button_onoff_indicator_off));
	}

	@Override
	public void onLinkLossOccurred(@NonNull final BluetoothDevice device) {
		runOnUiThread(() -> batteryLevelView.setText(""));
		statusView.setBackground(ContextCompat.getDrawable(this,drawable.button_onoff_indicator_off));
	}

	@Override
	public void onDeviceReady(@NonNull final BluetoothDevice device) {
		super.onDeviceConnected(device);
		progressBar.setProgress(0);
		statusView.setBackground(ContextCompat.getDrawable(this,drawable.button_onoff_indicator_on));
	}
	@Override
	public void onDeviceConnected(@NonNull final BluetoothDevice device) {
		super.onDeviceConnected(device);

	}


	@Override
	protected int getAboutTextId() {
		return R.string.gls_about_text;
	}

	@Override
	protected int getDefaultDeviceName() {
		return R.string.gls_default_name;
	}

	@Override
	protected UUID getFilterUUID() {
		return null;//OnetouchManager.ONETOUCH_SERVICE_UUID;
	}

	public void onMeasurementsReceived() {
		runOnUiThread(() -> {
			if(mBinder!=null) {
				progressBar.setVisibility(View.INVISIBLE);
				ArrayList<OnetouchMeasurement> newMeasurements = mBinder.getMeasurements();
				if (newMeasurements != null && newMeasurements.size()>0) {
					//Collections.reverse(newMeasurements);
					for(int i=0; i<newMeasurements.size(); i++){
						mMeasArray.insert(newMeasurements.get(i),0);
						Log.d(TAG,"Measurement: " + newMeasurements.get(i));
					}
				}
			}
		});
	}

	public void onInformationReceived() {
		// TODO: Show device information
		runOnUiThread(() -> {
					if(mBinder!=null) {
						OnetouchInfo info = mBinder.getDeviceInfo();
						Log.d(TAG,"Device information receivec: " + info.batteryCapacity + "% battery left");
						batteryLevelView.setText(info.batteryCapacity+"%");
					}
		});
	}

	public void onCountdownReceived(int count) {
		runOnUiThread(() -> {
			if(mBinder!=null) {
				progressBar.setVisibility(View.VISIBLE);
				ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", (5-count)*progressBar.getMax()/5);
				animator.setDuration(1200);
				animator.setInterpolator(new LinearInterpolator());
				if(count==0){
					animator.addListener(new Animator.AnimatorListener() {
						@Override
						public void onAnimationStart(Animator animation) {

						}

						@Override
						public void onAnimationEnd(Animator animation) {
							progressBar.setVisibility(View.INVISIBLE);
						}

						@Override
						public void onAnimationCancel(Animator animation) {

						}

						@Override
						public void onAnimationRepeat(Animator animation) {

						}
					});
				}
				animator.start();
			}
		});
	}
//	public class ProgressBarAnimation extends Animation{
//		private ProgressBar progressBar;
//		private float from;
//		private float  to;
//
//		public ProgressBarAnimation(ProgressBar progressBar, float from, float to) {
//			super();
//			this.progressBar = progressBar;
//			this.from = from;
//			this.to = to;
//		}
//
//		@Override
//		protected void applyTransformation(float interpolatedTime, Transformation t) {
//			super.applyTransformation(interpolatedTime, t);
//			float value = from + (to - from) * interpolatedTime;
//			progressBar.setProgress((int) value);
//		}
//
//	}
	/**
	 * Receive broadcast messages from the service
	 */
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();

			if (OnetouchService.BROADCAST_MEASUREMENT.equals(action)) {
				Log.d(TAG,"Broadcast measurement received! Binder is: " + mBinder);
				onMeasurementsReceived();
			}
			else if (OnetouchService.BROADCAST_COUNTDOWN.equals(action)) {
				int count = intent.getIntExtra(OnetouchService.EXTRA_COUNTDOWN,0);
				Log.d(TAG,"Countdown " + count);
				onCountdownReceived(count);
			}
			else if(OnetouchService.BROADCAST_INFORMATION.equals(action)) {
				Log.d(TAG,"Broadcast information received! Binder is: " + mBinder);
				mBatteryCapacity = intent.getIntExtra(OnetouchService.EXTRA_BATTERY_CAPACITY,0);
				mSerialNumber = intent.getByteArrayExtra(OnetouchService.EXTRA_SERIAL_NUMBER);
				onInformationReceived();
			}
			else if(OnetouchService.BROADCAST_COMM_FAILED.equals(action)) {
				Log.d(TAG,"Broadcast communication failed received! Binder is: " + mBinder);
				String msg = intent.getStringExtra(OnetouchService.EXTRA_ERROR_MESSAGE);
				showToast("Error: " + msg);
			}
		}
	};

	private static IntentFilter makeIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(OnetouchService.BROADCAST_COUNTDOWN);
		intentFilter.addAction(OnetouchService.BROADCAST_MEASUREMENT);
		intentFilter.addAction(OnetouchService.BROADCAST_INFORMATION);
		intentFilter.addAction(OnetouchService.BROADCAST_COMM_FAILED);
		return intentFilter;
	}

	@Override
	protected Class<? extends BleProfileService> getServiceClass() {
		return OnetouchService.class;
	}



	public class MeasurementsArrayAdapter extends ArrayAdapter<OnetouchMeasurement> {
		private static final String TAG = "MeasurementsArrayAdapter";
		private Context mContext;
		private LayoutInflater mInflater;

		public MeasurementsArrayAdapter(Context aContext, int aResource) {
			super(aContext,aResource);
			mContext = aContext;
			mInflater = LayoutInflater.from(aContext);
		}

		@SuppressLint({"SetTextI18n", "DefaultLocale"})
		@Override
		public View getView(int aPosition, View aConvertView, ViewGroup aParent) {

			View view = aConvertView;
			if (view == null) {
				view = mInflater.inflate(R.layout.measurement_item, aParent, false);
			}
			OnetouchMeasurement measurement = getItem(aPosition);
			if (measurement == null)
				return view; // this may happen during closing the activity
			// Lookup view for data population
			TextView time = view.findViewById(R.id.time);
			TextView value = view.findViewById(R.id.value);
			TextView id = view.findViewById(R.id.id);

			// Populate the data into the template view using the data object
			DateFormat format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT, Locale.getDefault());
			String dateString = format.format(measurement.mDate);
			time.setText(dateString);

			if(measurement.mErrorID==0) {
				value.setText(String.format(Locale.getDefault(), "%.2f", measurement.mGlucose));
				id.setText(String.format("ID: %s", measurement.mId));
			}
			else if(measurement.mErrorID==1280) {
				value.setText(String.format(Locale.getDefault(), "%.2f", measurement.mGlucose));
				id.setText(String.format("ID: %s HI", measurement.mId));
			}
			else{
				value.setText("-");
				id.setText(String.format("ID: %s ERROR CODE: %d", measurement.mId,measurement.mErrorID));
			}

			return view;
		}
	}

}
