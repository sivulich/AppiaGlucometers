package com.appia.bioland;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public interface BiolandCallbacks {

    public void onScanResult(int callbackType, ScanResult result);

}
