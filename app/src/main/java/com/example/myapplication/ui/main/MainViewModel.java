package com.example.myapplication.ui.main;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class MainViewModel extends ViewModel {
    public MutableLiveData<GeoPoint> src,dest,defaultLocation;
    public MutableLiveData<MapView> map;
    public MutableLiveData<Integer> flag;

    public MainViewModel () {
        src = new MutableLiveData<>();
        dest = new MutableLiveData<>();
        defaultLocation = new MutableLiveData<>();
        map = new MutableLiveData<>();
        flag = new MutableLiveData<>();
        defaultLocation.setValue(new GeoPoint(35.715298, 51.404343));
        flag.setValue(0);

    }

    public MutableLiveData<GeoPoint> getSrc() {
        return src;
    }

    public MutableLiveData<GeoPoint> getDest() {
        return dest;
    }

    public MutableLiveData<GeoPoint> getDefaultLocation() {
        return defaultLocation;
    }

    public MutableLiveData<MapView> getMap() {
        return map;
    }

    public MutableLiveData<Integer> getFlag() {
        return flag;
    }
}