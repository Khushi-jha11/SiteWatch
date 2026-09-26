package com.sitewatch.tests;

import com.sitewatch.utils.ResultRecorder;
import org.testng.ITestContext;
import org.testng.ITestListener;

/** Registered in testng.xml. Writes docs/data/history.csv once, after the whole suite finishes. */
public class ResultsListener implements ITestListener {

    @Override
    public void onFinish(ITestContext context) {
        ResultRecorder.flush();
    }
}
