package com.appia.main;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.UUID;
import java.util.ArrayList;

import com.appia.bioland.BiolandMeasurement;
import com.appia.bioland.R;
import com.appia.bioland.Ble.BleProfileService;
import com.appia.bioland.Ble.BleProfileServiceReadyActivity;
import com.appia.bioland.BiolandManager;
import com.appia.bioland.BiolandService;

// TODO The GlucoseActivity should be rewritten to use the service approach, like other do.
public class BiolandActivity extends BleProfileServiceReadyActivity<BiolandService.BiolandBinder> {
	@SuppressWarnings("unused")
	private static final String TAG = "GlucoseActivity";


	///private MeasurementsListAdapter measurementList;
	ArrayList<BiolandMeasurement> mMeasurements;
	BiolandService.BiolandBinder mBinder;
	private int mProtocolVersion;
	private int mBatteryCapacity;
	private byte[] mSerialNumber;

	private TextView batteryLevelView;
	private View controlPanelStd;
	private TextView unitView;

	@Override
	public void onConnectClicked(final View view) {
		super.onConnectClicked(view);
	}

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
		unitView = findViewById(R.id.unit);
		controlPanelStd = findViewById(R.id.gls_control_std);
		batteryLevelView = findViewById(R.id.battery);

		findViewById(R.id.action_info).setOnClickListener(
				v -> {
					if(mBinder!=null) {
						mBinder.requestDeviceInfo();
						setOperationInProgress(true);
					}});

		findViewById(R.id.action_read).setOnClickListener(
				v -> {
					if(mBinder!=null) {
						mBinder.requestMeasurements();
						setOperationInProgress(true);
					}});
		// todo TBD Action
		//findViewById(R.id.action_tbd).setOnClickListener(v -> );

		// Create measurements list
		//setListAdapter(adapter = new ExpandableRecordAdapter(this, glucoseManager));
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
		setOperationInProgress(false);
		runOnUiThread(() -> batteryLevelView.setText(R.string.not_available));
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
			// Todo: Deberia chequear si esta bindeado?
			if(mBinder!=null) {
				mMeasurements = mBinder.getMeasurements();
				if (mMeasurements != null &&mMeasurements.size() > 0) {
					//final int unit = mMeasurements.valueAt(0).unit;
					unitView.setVisibility(View.VISIBLE);
					unitView.setText(R.string.gls_unit_mgpdl);
				} else {
					unitView.setVisibility(View.GONE);
				}
				// Update list view
				// measurementList.notifyDataSetChanged();
			}
		});
	}

	public void onInformationReceived() {
		// TODO: Show device information
		runOnUiThread(() -> {

		});
	}

	/**
	 * Receive broadcast messages from the service
	 */
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();

			if (BiolandService.BROADCAST_MEASUREMENT.equals(action)) {
				Log.d(TAG,"Broadcast measurement received! Binder is: " + mBinder);
				setOperationInProgress(false);
			}
			else if(BiolandService.BROADCAST_INFORMATION.equals(action)) {
				Log.d(TAG,"Broadcast information received! Binder is: " + mBinder);
				mBatteryCapacity = intent.getIntExtra(BiolandService.EXTRA_BATTERY_CAPACITY,0);
				mSerialNumber = intent.getByteArrayExtra(BiolandService.EXTRA_SERIAL_NUMBER);

				setOperationInProgress(false);
			}
			else if(BiolandService.BROADCAST_COMM_FAILED.equals(action)) {
				Log.d(TAG,"Broadcast communication failed received! Binder is: " + mBinder);
				String msg = intent.getStringExtra(BiolandService.EXTRA_ERROR_MESSAGE);
				showToast("Error: " + msg);
				setOperationInProgress(false);
			}
		}
	};

	private static IntentFilter makeIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BiolandService.BROADCAST_MEASUREMENT);
		intentFilter.addAction(BiolandService.BROADCAST_INFORMATION);
		intentFilter.addAction(BiolandService.BROADCAST_COMM_FAILED);
		return intentFilter;
	}

	@Override
	protected Class<? extends BleProfileService> getServiceClass() {
		return BiolandService.class;
	}

	private void setOperationInProgress(final boolean progress) {
		runOnUiThread(() -> controlPanelStd.setVisibility(!progress ? View.VISIBLE : View.GONE));
	}
//
//	public class MeasurementsListAdapter extends BaseExpandableListAdapter {
//
//		private Context context;
//		private List<String> expandableListTitle;
//		private HashMap<String, List<String>> expandableListDetail;
//
//		public MeasurementsListAdapter(Context context, List<String> expandableListTitle,
//										   HashMap<String, List<String>> expandableListDetail) {
//			this.context = context;
//			this.expandableListTitle = expandableListTitle;
//			this.expandableListDetail = expandableListDetail;
//		}
//
//		@Override
//		public Object getChild(int listPosition, int expandedListPosition) {
//			return this.expandableListDetail.get(this.expandableListTitle.get(listPosition))
//					.get(expandedListPosition);
//		}
//
//		@Override
//		public long getChildId(int listPosition, int expandedListPosition) {
//			return expandedListPosition;
//		}
//
//		@Override
//		public View getChildView(int listPosition, final int expandedListPosition,
//								 boolean isLastChild, View convertView, ViewGroup parent) {
//			final String expandedListText = (String) getChild(listPosition, expandedListPosition);
//			if (convertView == null) {
//				LayoutInflater layoutInflater = (LayoutInflater) this.context
//						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//				convertView = layoutInflater.inflate(R.layout.list_item, null);
//			}
//			TextView expandedListTextView = (TextView) convertView
//					.findViewById(R.id.expandedListItem);
//			expandedListTextView.setText(expandedListText);
//			return convertView;
//		}
//
//		@Override
//		public int getChildrenCount(int listPosition) {
//			return this.expandableListDetail.get(this.expandableListTitle.get(listPosition))
//					.size();
//		}
//
//		@Override
//		public Object getGroup(int listPosition) {
//			return this.expandableListTitle.get(listPosition);
//		}
//
//		@Override
//		public int getGroupCount() {
//			return this.expandableListTitle.size();
//		}
//
//		@Override
//		public long getGroupId(int listPosition) {
//			return listPosition;
//		}
//
//		@Override
//		public View getGroupView(int listPosition, boolean isExpanded,
//								 View convertView, ViewGroup parent) {
//			String listTitle = (String) getGroup(listPosition);
//			if (convertView == null) {
//				LayoutInflater layoutInflater = (LayoutInflater) this.context.
//						getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//				convertView = layoutInflater.inflate(R.layout.list_group, null);
//			}
//			TextView listTitleTextView = (TextView) convertView
//					.findViewById(R.id.listTitle);
//			listTitleTextView.setTypeface(null, Typeface.BOLD);
//			listTitleTextView.setText(listTitle);
//			return convertView;
//		}
//
//		@Override
//		public boolean hasStableIds() {
//			return false;
//		}
//
//		@Override
//		public boolean isChildSelectable(int listPosition, int expandedListPosition) {
//			return true;
//		}
//	}

//	@Override
//	public boolean onMenuItemClick(final MenuItem item) {
//		switch (item.getItemId()) {
//		case R.id.action_refresh:
//			biolandManager.refreshRecords();
//			break;
//		case R.id.action_first:
//			biolandManager.getFirstRecord();
//			break;
//		case R.id.action_clear:
//			biolandManager.clear();
//			break;
//		case R.id.action_delete_all:
//			biolandManager.deleteAllRecords();
//			break;
//		}
//		return true;
//	}




}
