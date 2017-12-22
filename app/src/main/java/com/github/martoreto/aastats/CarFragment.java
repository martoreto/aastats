package com.github.martoreto.aastats;

import android.support.v4.app.Fragment;

import com.google.android.apps.auto.sdk.StatusBarController;

public abstract class CarFragment extends Fragment {
    private String mTitle;

    public CarFragment() {
        super();
    }

    abstract protected void setupStatusBar(StatusBarController sc);
}
