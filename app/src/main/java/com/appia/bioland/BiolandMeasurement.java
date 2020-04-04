package com.appia.bioland;

import androidx.annotation.NonNull;

import java.util.Calendar;

public class BiolandMeasurement {

    public BiolandMeasurement(float aGlucose,int aYear,
            int aMonth, int aDay, int aHour, int aMin) {
        mGlucose = aGlucose;
        mYear = aYear;
        mMonth = aMonth;
        mDay = aDay;
        mHour = aHour;
        mMin = aMin;

    }
    /** The glucose concentration */
    float mGlucose;

    /** The base time of the measurement */
    int mYear;
    int mMonth;
    int mDay;
    int mHour;
    int mMin;

    @NonNull
    @Override
    public String toString() {
        return ""; // TODO
    }
}