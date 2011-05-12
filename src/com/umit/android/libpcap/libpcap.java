package com.umit.android.libpcap;

import android.app.Activity;
import com.umit.android.libpcap.R;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class libpcap extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Capture our button from layout
        Button button = (Button)findViewById(R.id.button1);
        // Register the onClick listener with the implementation above
        button.setOnClickListener(mCorkyListener);
    }
    
    private native void testLog(String logThis);
    
    static {
        System.loadLibrary("libpcap");
    }
    
 // Create an anonymous implementation of OnClickListener
    private OnClickListener mCorkyListener = new OnClickListener() {
        public void onClick(View v) {
            testLog("blah blah");
        }
    };

    
}