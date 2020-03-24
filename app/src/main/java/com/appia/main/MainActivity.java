package com.appia.main;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.os.Bundle;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;

import com.appia.bioland.BiolandGlucometer;

import com.appia.bioland.R;

import com.appia.common.GlucometerCallbacks;
import com.appia.common.GlucoseMeasurement;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

/**
 * Main activity implements GlucometerCallbacks
 */
public class MainActivity extends AppCompatActivity implements GlucometerCallbacks{

    BiolandGlucometer mBiolandGlucometer;
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

    public void onScanResult(int aCallbackType, ScanResult aResult) {

    }

    public void onMeasurementReady(GlucoseMeasurement aMeasurement) {

    }

////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                }
            });
            builder.show();
        }

        mBiolandGlucometer = new BiolandGlucometer(this,this);
    }

}
