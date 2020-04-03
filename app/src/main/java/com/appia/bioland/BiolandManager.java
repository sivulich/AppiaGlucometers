/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.appia.bioland;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import androidx.annotation.NonNull;

import android.util.Log;



import java.util.UUID;
import java.lang.String;
import java.util.ArrayList;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.WriteRequest;


public class BiolandManager extends BleManager<BiolandCallbacks> implements ProtocolCallbacks{
	/** Bioland communication service UUID */
	public final static UUID BIOLAND_SERVICE_UUID = UUID.fromString("00001000-0000-1000-8000-00805f9b34fb");
	/** RX characteristic UUID */
	private final static UUID BIOLAND_RX_CHARACTERISTIC_UUID = UUID.fromString("00001001-0000-1000-8000-00805f9b34fb");
	/** TX characteristic UUID */
	private final static UUID BIOLAND_TX_CHARACTERISTIC_UUID = UUID.fromString("00001002-0000-1000-8000-00805f9b34fb");

	private final static String TAG = "BiolandManager";

	private BluetoothGattCharacteristic mRxCharacteristic, mTxCharacteristic;


	/**
	 * Bioland Manager constructor
	 * @param context
	 */
	BiolandManager(final Context context) {
		super(context);
	}

	/**
	 * Sends the request to obtain all the records stored in the device.
	 */
	public void requestMeasurements(){
		mProtocol.requestMeasurements();
	}

	/**
	 * Sends the request to obtain the device information.
	 */
	public void requestDeviceInfo(){
		mProtocol.requestDeviceInfo();
	}

	/**
	 * Returns all measurements as a array.
	 *
	 * @return the records list.
	 */
	ArrayList<BiolandMeasurement> getMeasurements() {
		return mMeasurements;
	}

	/**
	 * Clear the measurement list locally.
	 */
	public void clearMeasurements() {
		mMeasurements.clear();
	}

	/**
	 *
	 * @return
	 */
	public int getBatteryCapacity() {return mInfo.batteryCapacity;}

	/**
	 *
	 * @return
	 */
	public byte[] getSerialNumber() {return mInfo.serialNumber;}

	@NonNull
	@Override
	protected BleManagerGattCallback getGattCallback() {
		return new BiolandManagerGattCallback();
	}

	/**
	 * BluetoothGatt callbacks for connection/disconnection, service discovery,
	 * receiving indication, etc.
	 */
	private class BiolandManagerGattCallback extends BleManagerGattCallback {
		@Override
		protected void initialize() {
			if(isConnected()) {

				requestMtu(100)
						.with((device, mtu) -> Log.d(TAG, "MTU changed to " + mtu))
						.done(device -> {})
						.fail((device, status) -> log(Log.WARN, "MTU change not supported"))
						.enqueue();

				setNotificationCallback(mTxCharacteristic)
						.with((device, data) -> {
							Log.v(TAG,  data.toString() + " received");
							mProtocol.onDataReceived(data.getValue());
						});

				enableNotifications(mTxCharacteristic)
						.done(device -> log(Log.INFO, "Bioland notifications enabled"))
						.fail((device, status) -> log(Log.WARN, "Bioland TX characteristic not found"))
						.enqueue();
			}
		}

		@Override
		public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(BIOLAND_SERVICE_UUID);
			if (service != null) {
				mRxCharacteristic = service.getCharacteristic(BIOLAND_RX_CHARACTERISTIC_UUID);
				mTxCharacteristic = service.getCharacteristic(BIOLAND_TX_CHARACTERISTIC_UUID);
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

			return mRxCharacteristic != null && mTxCharacteristic != null && (writeRequest || writeCommand);
		}

		@Override
		protected void onDeviceDisconnected() {
			// Release all references.
			mRxCharacteristic = null;
			mTxCharacteristic = null;
		}
	}

	public void onMeasurementsReceived(ArrayList<BiolandMeasurement> aMeasurements) {

		/* Store received measurements. */
		mMeasurements = aMeasurements;

		/* Notify new measurements were received. */
		mCallbacks.onMeasurementsReceived();
	}
	public void onDeviceInfoReceived(BiolandInfo aInfo) {

		/* Store received info. */
		mInfo = aInfo;

		/* Notify info was received. */
		mCallbacks.onDeviceInfoReceived();
	}
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
			final WriteRequest request = writeCharacteristic(mRxCharacteristic, bytes)
					.with((device, data) -> Log.v(TAG,
							"\"" + data.toString() + "\" sent"));
			//if (!useLongWrite) {
			// This will automatically split the long data into MTU-3-byte long packets.
			request.split();
			//}
			request.enqueue();
		}
	}

	private BiolandProtocol mProtocol = new BiolandProtocol(this);
	BiolandInfo mInfo;
	private ArrayList<BiolandMeasurement> mMeasurements;
}
