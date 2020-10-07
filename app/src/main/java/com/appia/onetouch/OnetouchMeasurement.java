package com.appia.onetouch;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;


public class OnetouchMeasurement {
    public OnetouchMeasurement(){

    }
    public OnetouchMeasurement(float aGlucose, Date aDate, String aId, int aErrorID) {
        mGlucose = aGlucose;
        mDate = aDate;
        mId = aId;
        mErrorID = aErrorID;
    }
    public OnetouchMeasurement(float aGlucose, Date aDate, String aId) {
        mGlucose = aGlucose;
        mDate = aDate;
        mId = aId;
        mErrorID = 0;
    }


    /** The glucose concentration */
    public float mGlucose;

    /** The base time of the measurement */
    public Date mDate;
    public String mId;
    public int mErrorID;

    @NonNull
    @Override
    public String toString() {
        return mGlucose + " mmol/L @ " + mDate.toString() + " Error: " + mErrorID;
    }
}