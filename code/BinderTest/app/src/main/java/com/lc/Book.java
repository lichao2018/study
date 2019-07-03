package com.lc;

import android.os.Parcel;
import android.os.Parcelable;

public class Book implements Parcelable {
    public String name;
    public int id;

    public Book(int id, String name){
        this.id = id;
        this.name = name;
    }

    public Book(Parcel in){
        readFromParcel(in);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public static final Creator<Book> CREATOR = new Creator<Book>() {
        @Override
        public Book createFromParcel(Parcel source) {
            return new Book(source);
        }

        @Override
        public Book[] newArray(int size) {
            return new Book[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
    }

    public void readFromParcel(Parcel in){
        id = in.readInt();
        name = in.readString();
    }
}
