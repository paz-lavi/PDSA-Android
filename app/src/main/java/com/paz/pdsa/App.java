package com.paz.pdsa;

import android.app.Application;

import com.paz.logger.EZLog;
import com.paz.prefy_lib.Prefy;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

public class App extends Application {
    private final String devKey = "FTGDmOi0PnUFuspIRu6vKdZQu812";
    @Override
    public void onCreate() {
        super.onCreate();
        Prefy.init(getApplicationContext(),true);
        EZLog.init(devKey, this);
        EZLog.getInstance()
                .setDebug(true)
                .setPrintToLogcat(true)
                .start();
        PDFBoxResourceLoader.init(getApplicationContext());

    }
}
