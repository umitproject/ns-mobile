package org.umit.ns.mobile;

import org.umit.ns.mobile.model.FileManager;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class LogsViewer extends Activity {
    
    TextView logs;
    Button clear;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logs);
        
        logs = (TextView)findViewById(R.id.logs);
        
        String str = FileManager.read();
        logs.setText(str);
        
        clear = (Button)findViewById(R.id.clear);
        clear.setOnClickListener(clearLogs);
    }
    
    public OnClickListener clearLogs = new OnClickListener() {
        public void onClick(View v) {
            FileManager.clear();
            String str = FileManager.read();
            logs.setText(str);
        }
    };

    
}
