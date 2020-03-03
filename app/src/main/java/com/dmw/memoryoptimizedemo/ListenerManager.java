package com.dmw.memoryoptimizedemo;

import java.util.ArrayList;
import java.util.List;

public class ListenerManager {

    private static ListenerManager sInstance;

    private List<SampleListener> listeners = new ArrayList<>();

    private ListenerManager() {
    }

    public static synchronized ListenerManager getInstance() {
        if (sInstance == null) {
            sInstance = new ListenerManager();
        }

        return sInstance;
    }

    public void addListener(SampleListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SampleListener listener) {
        listeners.remove(listener);
    }
}

