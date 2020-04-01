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

import com.appia.bioland.BiolandCallbacks;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Calendar;

import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.lang.String;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.WriteRequest;
import no.nordicsemi.android.ble.data.Data;


public class BiolandManager extends BleManager<BiolandCallbacks> {
	/** Bioland communication service UUID */
	public final static UUID BIOLAND_SERVICE_UUID = UUID.fromString("00001000-0000-1000-8000-00805f9b34fb");
	/** RX characteristic UUID */
	private final static UUID BIOLAND_RX_CHARACTERISTIC_UUID = UUID.fromString("00001001-0000-1000-8000-00805f9b34fb");
	/** TX characteristic UUID */
	private final static UUID BIOLAND_TX_CHARACTERISTIC_UUID = UUID.fromString("00001002-0000-1000-8000-00805f9b34fb");

	private final static String TAG = "BiolandManager";

	private BluetoothGattCharacteristic mRxCharacteristic, mTxCharacteristic;

	private ArrayList<BiolandMeasurement> mMeasurements = new ArrayList<BiolandMeasurement>();

	private int mProtocolVersion;
	private int mBatteryCapacity;
	private Calendar mProductionDate;
	private byte[] mSerialNumber;

	Timer mTimer;

	TimerTask mSendPacketTask;

	byte[] mMessage;

	/**
	 * A flag indicating whether Long Write can be used. It's set to false if the UART RX
	 * characteristic has only PROPERTY_WRITE_NO_RESPONSE property and no PROPERTY_WRITE.
	 * If you set it to false here, it will never use Long Write.
	 *
	 * TODO change this flag if you don't want to use Long Write even with Write Request.
	 */
	private boolean useLongWrite = true;

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
	public void readMeasurements() {

		mCallbacks.onCommunicationStarted();
		// Protocol initiation
		// TODO: Santi
		mMessage = hexStringToByteArray("5A0A031005020F213BEB");

		if(mTimer==null) {
			mTimer = new Timer();
		}
		mSendPacketTask = new TimerTask(){
			@Override
			public void run() {
				send(mMessage);
			}
		};
		mTimer.scheduleAtFixedRate(mSendPacketTask, 1000, 300);
	}

	/**
	 * Sends the request to obtain the device information.
	 */
	public void readDeviceInfo() {
		// Protocol initiate
		// TODO: Santi

		// INFO READ PACKER
		mMessage = hexStringToByteArray("5A0A001005020F213BE8");

		if(mTimer==null) {
			mTimer = new Timer();
		}
		mSendPacketTask = new TimerTask(){
			@Override
			public void run() {
				send(mMessage);
			}
		};
		mTimer.scheduleAtFixedRate(mSendPacketTask, 1000, 300);
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
	public int getBatteryCapacity() {return mBatteryCapacity;}

	/**
	 *
	 * @return
	 */
	public byte[] getSerialNumber() {return mSerialNumber;}

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
							onDataReceived(device,data);
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
					useLongWrite = false;
			}

			return mRxCharacteristic != null && mTxCharacteristic != null && (writeRequest || writeCommand);
		}

		@Override
		protected void onDeviceDisconnected() {
			// Release all references.
			mRxCharacteristic = null;
			mTxCharacteristic = null;
			useLongWrite = true;
		}
	}

	private void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data){

		Log.d(TAG, "\"" + data.toString() + "\" received!!!");

		byte[] bytes = data.getValue();

//		// Build packet from bytes received
//		ProtocolPacket packet = new ProtocolPacket(bytes);
//
//		// Check checksum and packet length
//		if(packet.isValid()==false){
//			// Should retry a predefined number of times and if it keeps failing communicate
//			// upper layer of communication error.
//			mRetries++;
//			if(mRetries==5) {
//				mSendPacketTask.cancel();
//				mCallbacks.onCommunicationFailed();
//			}
//			return;
//		}
//		// Packet is valid, reset retries counter.
//		mRetries = 0;

		//if(packet.type() ==Protocol::INFORMATION_PACKET)
		if(bytes[2]==0){

			mProtocolVersion = bytes[3];
			mBatteryCapacity = bytes[5];
			mSerialNumber = Arrays.copyOfRange(bytes,8,16);
			//mProductionDate.set(year + 1900, month, 1); Only in protocol V1

			/*Stop communication*/
			mSendPacketTask.cancel();

			Log.d(TAG, "INFORMATION PACKET RECEIVED: Bat="+ mBatteryCapacity + "% ProtocolVersion = " + mProtocolVersion + " SerialN: " + mSerialNumber.toString() );
			// Notify device information has been read
			mCallbacks.onInformationRead();
		}
		//else if(packet.type() ==Protocol::MEASUREMENT_PACKET)
		else if(bytes[2]==3) {
			BiolandMeasurement measurement = new BiolandMeasurement();
			measurement.glucoseConcentration = (float)(((bytes[10]&0x000000FF)<<8)+ (bytes[9]&0x000000FF));
			measurement.date = new GregorianCalendar();
			measurement.date.set(bytes[3],bytes[4],bytes[5],bytes[6],bytes[7]);

			mMeasurements.add(measurement);
			Log.d(TAG, "MEASUREMENT PACKET RECEIVED: " + measurement.glucoseConcentration + "mg/dl" + measurement.date);
		}
		//else if(packet.type() ==Protocol::END_PACKET)
		else if(bytes[2]==5) {
			Log.d(TAG, "END PACKET RECEIVED!!");

			/*Stop communication*/
			mSendPacketTask.cancel();

			// Notify new measurements were read.
			mCallbacks.onMeasurementsRead();
		}
		//else if(packet.type() ==Protocol::HANDSHAKE_PACKET)
		else if(bytes[2]==9) {
			Log.d(TAG, "HANDSHAKE PACKET RECEIVED!!");

		}
		// Si hay algun momento hay un error en el protocolo llamar a:
		//mCallbacks.onCommunicationFailed();
	}
	/**
	 * Sends the given text to RX characteristic.
	 * @param text the text to be sent
	 */
	private void send(final String text) {
		// Are we connected?
		if (mRxCharacteristic == null) {
			Log.d(TAG,"Tried to send data but mRxCharacteristic was null: " + text);
			return;
		}

		if (!TextUtils.isEmpty(text)) {
			final WriteRequest request = writeCharacteristic(mRxCharacteristic, text.getBytes())
					.with((device, data) -> Log.d(TAG,
							"\"" + data.getStringValue(0) + "\" sent"));
			//if (!useLongWrite) {
				// This will automatically split the long data into MTU-3-byte long packets.
				request.split();
			//}
			request.enqueue();
		}
	}

	/**
	 * Sends the given bytes to RX characteristic.
	 * @param bytes the text to be sent
	 */
	private void send(final byte[] bytes) {
		// Are we connected?
		if (mRxCharacteristic == null) {
			Log.d(TAG,"Tried to send data but mRxCharacteristic was null: " + bytes.toString());
			return;
		}

		if (bytes != null && bytes.length > 0) {
			final WriteRequest request = writeCharacteristic(mRxCharacteristic, bytes)
					.with((device, data) -> Log.d(TAG,
							"\"" + data.toString() + "\" sent"));
			//if (!useLongWrite) {
			// This will automatically split the long data into MTU-3-byte long packets.
			request.split();
			//}
			request.enqueue();
		}
	}

	private static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}
}
