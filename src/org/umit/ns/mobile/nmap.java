package org.umit.ns.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.umit.ns.mobile.api.ScanClientActivity;
import org.umit.ns.mobile.api.XmlParser;
import org.umit.ns.mobile.api.shellUtils;
import org.umit.ns.mobile.model.FileManager;

public class nmap extends ScanClientActivity {
    TextView cmd;
    static TextView results;
    static boolean started = false;
    static Button start;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.nmap);

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
            startScan("./"+cmd.getText().toString()+" -oX nmap.xml");
        }
    };

    public void onScanStart(){
        started=true;
    }

    public void onScanStop(){}
    public void onScanFinish(){
        started = false;
        getScanResults();
        shellUtils.killProcess("./nmap");
    }
    public void onScanResultsReceive(String scanResults){
        results.append("\n" + scanResults);
    }
    public void onScanProgressReceive(int progress){}
    public void onScanCrash(int RESP_CODE,String info){
        Log.e("UmitScanner","Scan has crashed. Info: "+info);
        Toast.makeText(getApplicationContext(),"Scanning problem: "+info,Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.cmdmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.clear:
                clearLogs();
                return true;
            case R.id.logs:
                loadLogs();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadLogs() {
        Intent n = new Intent(nmap.this, LogsViewer.class);
        startActivityForResult(n, 0);
    }

    public void clearLogs() {
        results.setText("");
    }
}