package com.appia.onetouch;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.appia.onetouch.protocol.Protocol;
import com.appia.onetouch.protocol.ProtocolCallbacks;
import com.appia.onetouch.OnetouchCallbacks;


import java.util.ArrayList;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;

public class OnetouchManager extends BleManager<OnetouchCallbacks> implements ProtocolCallbacks {
	/** Onetouch communication service UUID */
	public final static UUID ONETOUCH_SERVICE_UUID = UUID.fromString("af9df7a1-e595-11e3-96b4-0002a5d5c51b");
	/** RX characteristic UUID */
	private final static UUID ONETOUCH_RX_CHARACTERISTIC_UUID = UUID.fromString("af9df7a2-e595-11e3-96b4-0002a5d5c51b");
	/** TX characteristic UUID */
	private final static UUID ONETOUCH_TX_CHARACTERISTIC_UUID = UUID.fromString("af9df7a3-e595-11e3-96b4-0002a5d5c51b");

	private final static String TAG = "OnetouchManager";

	private BluetoothGattCharacteristic mRxCharacteristic;
	private BluetoothGattCharacteristic mTxCharacteristic;

	private Protocol mProtocol = new Protocol(this);
	/**
	 * Onetouch Manager constructor
	 * @param context
	 */
	OnetouchManager(final Context context) {
		super(context);
	}

	/**
	 * Sends the request to obtain all the records stored in the device.
	 */
	public void requestMeasurements(){
		//mProtocol.requestMeasurements(); TODO
	}

	/**
	 * Sends the request to obtain the device information.
	 */
	public void requestDeviceInfo(){/*mProtocol.requestDeviceInfo();*/} // TODO

	@Override
	public void log(final int priority, @NonNull final String message) {
		// Uncomment to see Bluetooth Logs
		Log.println(priority, TAG, message);
	}

	@NonNull
	@Override
	protected BleManagerGattCallback getGattCallback() {
		return new OnetouchManagerGattCallback();
	}

	/**
	 * BluetoothGatt callbacks for connection/disconnection, service discovery,
	 * receiving indication, etc.
	 */
	private class OnetouchManagerGattCallback extends BleManagerGattCallback {
		@Override
		protected void initialize() {
			if(isConnected()) {
				/* Register callback to get data from the device. */
				setNotificationCallback(mTxCharacteristic)
						.with((device, data) -> {
							Log.v(TAG,  "NOTIFICATION: " + data.toString() + " received");
							mProtocol.onDataReceived(data.getValue());
						});
				enableNotifications(mTxCharacteristic)
						.done(device -> Log.i(TAG, "Onetouch TX characteristic  notifications enabled"))
						.fail((device, status) -> {
							Log.w(TAG, "Onetouch TX characteristic  notifications not enabled");
						})
						.enqueue();
			}
		}

		@Override
		public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(ONETOUCH_SERVICE_UUID);

			if (service != null) {
				mRxCharacteristic = service.getCharacteristic(ONETOUCH_RX_CHARACTERISTIC_UUID);
				mTxCharacteristic = service.getCharacteristic(ONETOUCH_TX_CHARACTERISTIC_UUID);
			}

			boolean writeRequest = false;
			boolean writeCommand = false;
			if (mRxCharacteristic != null) {
				final int rxProperties = mRxCharacteristic.getProperties();
				writeRequest = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
				writeCommand = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0;

				// Set the WRITE REQUEST type when the characteristic supports it.
				// This will allow to send long write (also if the characteristic support it).
				// In case there is no WRITE REQUEST property, this manager will divide texts
				// longer then MTU-3 bytes into up to MTU-3 bytes chunks.
				//if (writeRequest)
					mRxCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

				//else
			}

			return mRxCharacteristic != null &&
					mTxCharacteristic != null &&
					(writeRequest || writeCommand);
		}

		@Override
		protected void onDeviceReady() {
			super.onDeviceReady();
			mProtocol.connect();
		}

		@Override
		protected void onDeviceDisconnected() {
			mProtocol.disconnect();
			// Release all references.
			mRxCharacteristic = null;
			mTxCharacteristic = null;
		}
	}

	public void onMeasurementsReceived(ArrayList<OnetouchMeasurement> aMeasurements) {

		/* Notify new measurements were received. */
		mCallbacks.onMeasurementsReceived(aMeasurements);
    }
//	public void onDeviceInfoReceived(OnetouchInfo aInfo) {
//		/* Notify info was received. */
//		mCallbacks.onDeviceInfoReceived(aInfo);
//	}

	public void onProtocolError(String aMessage) {
		mCallbacks.onProtocolError(aMessage);
	}
	/**
	 * Sends the given bytes to RX characteristic.
	 * @param bytes the text to be sent
	 */
	public void sendData(final byte[] bytes) {
		// Are we connected?
		if (mRxCharacteristic == null) {
			Log.e(TAG,"Tried to send data but mRxCharacteristic was null: " + bytes.toString());
			return;
		}

		if (bytes != null && bytes.length > 0) {
			writeCharacteristic(mRxCharacteristic, bytes)
					.with((device, data) -> Log.v(TAG,
							"\"" + data.toString() + "\" sent"))
					//.split()
					.enqueue();
		}
	}
}
