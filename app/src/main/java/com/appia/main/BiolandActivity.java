package com.appia.main;

import android.animation.Animator;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.R.drawable;

import android.view.animation.LinearInterpolator;
import android.widget.BaseExpandableListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.util.Log;
import android.animation.ObjectAnimator;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.UUID;
import java.util.Locale;
import java.util.ArrayList;
import java.text.DateFormat;

import com.appia.bioland.BiolandInfo;
import com.appia.bioland.BiolandMeasurement;
import com.appia.bioland.R;
import com.appia.bioland.Ble.BleProfileService;
import com.appia.bioland.Ble.BleProfileServiceReadyActivity;
import com.appia.bioland.BiolandManager;
import com.appia.bioland.BiolandService;

import org.w3c.dom.Text;

// TODO The GlucoseActivity should be rewritten to use the service approach, like other do.
public class BiolandActivity extends BleProfileServiceReadyActivity<BiolandService.BiolandBinder> {
	@SuppressWarnings("unused")
	private static final String TAG = "GlucoseActivity";

	BiolandService.BiolandBinder mBinder;
	private int mProtocolVersion;
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
	protected void onServiceBound(final BiolandService.BiolandBinder binder) {

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
		return BiolandManager.BIOLAND_SERVICE_UUID;
	}

	public void onMeasurementsReceived() {
		runOnUiThread(() -> {
			if(mBinder!=null) {
				progressBar.setVisibility(View.INVISIBLE);
				ArrayList<BiolandMeasurement> newMeasurements = mBinder.getMeasurements();
				if (newMeasurements != null && newMeasurements.size()>0) {
					mMeasArray.addAll(newMeasurements);
					for(int i=0; i<newMeasurements.size(); i++){
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
						BiolandInfo info = mBinder.getDeviceInfo();
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

			if (BiolandService.BROADCAST_MEASUREMENT.equals(action)) {
				Log.d(TAG,"Broadcast measurement received! Binder is: " + mBinder);
				onMeasurementsReceived();
			}
			else if (BiolandService.BROADCAST_COUNTDOWN.equals(action)) {
				int count = intent.getIntExtra(BiolandService.EXTRA_COUNTDOWN,0);
				Log.d(TAG,"Countdown " + count);
				onCountdownReceived(count);
			}
			else if(BiolandService.BROADCAST_INFORMATION.equals(action)) {
				Log.d(TAG,"Broadcast information received! Binder is: " + mBinder);
				mBatteryCapacity = intent.getIntExtra(BiolandService.EXTRA_BATTERY_CAPACITY,0);
				mSerialNumber = intent.getByteArrayExtra(BiolandService.EXTRA_SERIAL_NUMBER);
				onInformationReceived();
			}
			else if(BiolandService.BROADCAST_COMM_FAILED.equals(action)) {
				Log.d(TAG,"Broadcast communication failed received! Binder is: " + mBinder);
				String msg = intent.getStringExtra(BiolandService.EXTRA_ERROR_MESSAGE);
				showToast("Error: " + msg);
			}
		}
	};

	private static IntentFilter makeIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BiolandService.BROADCAST_COUNTDOWN);
		intentFilter.addAction(BiolandService.BROADCAST_MEASUREMENT);
		intentFilter.addAction(BiolandService.BROADCAST_INFORMATION);
		intentFilter.addAction(BiolandService.BROADCAST_COMM_FAILED);
		return intentFilter;
	}

	@Override
	protected Class<? extends BleProfileService> getServiceClass() {
		return BiolandService.class;
	}



	public class MeasurementsArrayAdapter extends ArrayAdapter<BiolandMeasurement> {
		private static final String TAG = "MeasurementsArrayAdapter";
		private Context mContext;
		private LayoutInflater mInflater;

		public MeasurementsArrayAdapter(Context aContext, int aResource) {
			super(aContext,aResource);
			mContext = aContext;
			mInflater = LayoutInflater.from(aContext);
		}

		@Override
		public View getView(int aPosition, View aConvertView, ViewGroup aParent) {

			View view = aConvertView;
			if (view == null) {
				view = mInflater.inflate(R.layout.measurement_item, aParent, false);
			}
			BiolandMeasurement measurement = getItem(aPosition);
			if (measurement == null)
				return view; // this may happen during closing the activity
			// Lookup view for data population
			TextView time = view.findViewById(R.id.time);
			TextView value = view.findViewById(R.id.value);
			// Populate the data into the template view using the data object

			DateFormat format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT, Locale.getDefault());
			String dateString = format.format(measurement.mDate);
			time.setText(dateString);
			value.setText(String.format(Locale.getDefault(),"%.2f",measurement.mGlucose));

			return view;

		}
	}

}
