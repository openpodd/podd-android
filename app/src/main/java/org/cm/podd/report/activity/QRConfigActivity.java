package org.cm.podd.report.activity;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import org.cm.podd.report.R;

import java.util.ArrayList;
import java.util.List;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QRConfigActivity extends ActionBarActivity implements ZXingScannerView.ResultHandler {

    private static final String TAG = "QRConfigActivity";
    private ZXingScannerView mScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mScannerView = new ZXingScannerView(this);
        List supported = new ArrayList<BarcodeFormat>();
        supported.add(BarcodeFormat.QR_CODE);
        mScannerView.setFormats(supported);
        setContentView(mScannerView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public void handleResult(Result rawResult) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {}

        Log.v(TAG, rawResult.getText()); // Prints scan results
        Intent ret = new Intent();
        ret.putExtra("result", rawResult.getText());
        setResult(0, ret);
        finish();

    }


}
