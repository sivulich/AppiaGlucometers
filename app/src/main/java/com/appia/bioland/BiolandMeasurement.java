package com.appia.bioland;


import androidx.annotation.NonNull;

import java.util.GregorianCalendar;

public class BiolandMeasurement {

    /** The glucose concentration */
    float glucoseConcentration;

    /** The base time of the measurement */
    GregorianCalendar date;

    @NonNull
    @Override
    public String toString() {
        return ""; // TODO
    }
}