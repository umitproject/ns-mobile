package org.umit.ns.mobile;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.umit.ns.mobile.api.ScanClientActivity;

public class ScanActivity extends ScanClientActivity {
    TextView cmd;
    static TextView results;
    static boolean started = false;
    static Button start;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.scan_activity);

        start = (Button)findViewById(R.id.startNmap);
        start.setOnClickListener(nmapLoad);

        cmd = (TextView)findViewById(R.id.nmapcmd);

        results = (TextView)findViewById(R.id.nmapOutput);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    public View.OnClickListener nmapLoad = new View.OnClickListener() {
        public void onClick(View v) {
            if(!started){
                rqstStartScan("./"+cmd.getText().toString());
                started=true;
                start.setEnabled(false);
            }
            else
                Toast.makeText(getApplicationContext(),"Scan not ready",Toast.LENGTH_SHORT).show();
        }
    };

    public void onScanStart(){
        started=true;
        start.setEnabled(true);
        start.setText("Stop");
    }

    public void onScanStop(){
        started=false;
        start.setEnabled(true);
        start.setText("Start");
    }

    public void onNotifyProgress(int progress){
        //TODO Test
    }

    protected void onNotifyProblem(String info){
        Log.e("UmitScanner","Scan has crashed. Info: "+info);
        //TODO Report to developer ;-)
        Toast.makeText(getApplicationContext(),"Scanning problem: "+info,Toast.LENGTH_LONG).show();
    }

    public void onNotifyFinished(String scanResults){
        started = false;
        start.setEnabled(true);
        start.setText("Start");
        results.append("\n" + scanResults);
    }
}