package com.appia.common;

import java.util.Date;

import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

/**
 *  Callbacks to be implemented by the Activity.
 *  TODO: Agregar lo que sea necesario
 *  TODO: Completar y mejorar la clase GlucoseMeasurement
 */
public interface GlucometerCallbacks {

    /**
     *
     * @param aCallbackType
     * @param aResult
     */
    public void onScanResult(int aCallbackType, ScanResult aResult);

    /**
     *
     * @param aMeasurement
     */
    public void onMeasurementReady(GlucoseMeasurement aMeasurement);

}
