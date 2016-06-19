package com.sam_chordas.android.stockhawk.rest;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by daniloplacer on 6/14/16.
 */
public class QuoteDataPoint implements Parcelable {
    private String date;
    private float bidValue;

    public QuoteDataPoint(String date, float bidValue) {
        this.date = date;
        this.bidValue = bidValue;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public float getBidValue() {
        return bidValue;
    }

    public void setBidValue(float bidValue) {
        this.bidValue = bidValue;
    }

    // Parcelable implementation

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.date);
        dest.writeFloat(this.bidValue);
    }

    protected QuoteDataPoint(Parcel in) {
        date = in.readString();
        bidValue = in.readFloat();
    }

    public static final Creator<QuoteDataPoint> CREATOR = new Creator<QuoteDataPoint>() {
        @Override
        public QuoteDataPoint createFromParcel(Parcel in) {
            return new QuoteDataPoint(in);
        }

        @Override
        public QuoteDataPoint[] newArray(int size) {
            return new QuoteDataPoint[size];
        }
    };

}