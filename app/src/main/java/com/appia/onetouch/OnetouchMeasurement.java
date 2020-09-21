package com.appia.onetouch;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;


public class OnetouchMeasurement {
    public OnetouchMeasurement(){

    }
    public OnetouchMeasurement(float aGlucose, Date aDate, String aId) {
        mGlucose = aGlucose;
        mDate = aDate;
        mId = aId;
    }

    public OnetouchMeasurement(float aGlucose, int aYear,
                               int aMonth, int aDay, int aHour, int aMin, String aId) {
        mGlucose = aGlucose;
        mYear = aYear;
        mMonth = aMonth;
        mDay = aDay;
        mHour = aHour;
        mMin = aMin;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, aYear);
        cal.set(Calendar.MONTH, aMonth);
        cal.set(Calendar.DAY_OF_MONTH, aDay);
        cal.set(Calendar.HOUR_OF_DAY, aHour);
        cal.set(Calendar.MINUTE, aMin);
        mDate = cal.getTime();
        mId = aId;
    }
    /** The glucose concentration */
    public float mGlucose;

    /** The base time of the measurement */
    public int mYear;
    public int mMonth;
    public int mDay;
    public int mHour;
    public int mMin;
    public Date mDate;
    public String mId;


    @NonNull
    @Override
    public String toString() {
        Calendar c = Calendar.getInstance(); //automatically set to current time
        c.set(mYear,mMonth,mDay,mHour,mMin);
        return mGlucose + "mmol/L @ " + DateFormat.getDateTimeInstance().format(c.getTime());
    }
}