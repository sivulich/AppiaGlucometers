package com.appia.common;

import java.util.Calendar;

public class GlucoseMeasurement {

    /** Record sequence number */
    int sequenceNumber;
    /** The base time of the measurement */
    Calendar time;
    /** The glucose concentration. 0 if not present */
    float glucoseConcentration;

}