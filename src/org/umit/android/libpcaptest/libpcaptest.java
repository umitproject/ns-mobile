package org.umit.android.libpcaptest;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class libpcaptest extends Activity {
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

	static {
		System.loadLibrary("pcaptest");
	}

	private native void testLog(String logThis);    
 // Create an anonymous implementation of OnClickListener
    private OnClickListener mCorkyListener = new OnClickListener() {
        public void onClick(View v) {
            testLog("Not a null object");
        }
    };
}