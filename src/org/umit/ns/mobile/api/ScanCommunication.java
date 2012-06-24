package org.umit.ns.mobile.api;

public interface ScanCommunication {
    //Requests are sent by the Activity, every request accompanied by ID of the running scan
    static final int RQST_SCAN_ID = 1; //msg.replyTo
    static final int RESP_SCAN_ID_OK = 2; //arg1=id, {arg2=1 if ROOT, arg2=0 if NO_ROOT}
    static final int RESP_SCAN_ID_ERR = 19; //arg1=id, {Info = Reason}

    static final int RQST_START_SCAN = 3; //msg.replyTo obj=Bundle: {string ScanArguments}
    static final int RESP_START_SCAN_OK = 4; //arg1=id
    static final int RESP_START_SCAN_ERR = 5; //arg1=id, Scan already started, or no ScanArguments present

    static final int RQST_STOP_SCAN = 6; //msg.replyTo arg1=id
    static final int RESP_STOP_SCAN_OK = 7; //arg1=id
    static final int RESP_STOP_SCAN_ERR = 8; //arg1=id, Scan not started

    static final int RQST_PROGRESS = 9; //msg.replyTo arg1=id
    static final int RESP_PROGRESS_OK = 10; //arg1=id, arg2=progress
    static final int RESP_PROGRESS_ERR = 11; //arg1=id, Scan not started

    static final int RQST_RESULTS = 12; //msg.replyTo arg1=id
    static final int RESP_RESULTS_OK = 13; //arg1=id, info= <scan results>
    static final int RESP_RESULTS_ERR = 14; //arg1=id, Scan not started, ??scan not finished??

    static final int NOTIFY_SCAN_FINISHED = 15; //msg.replyTo arg1=id, info=<scan results>
    static final int NOTIFY_SCAN_PROBLEM = 16;  //arg1=id, info= Thread caught a naughty exception

    static final int NOTIFY_ROOT_ACCESS=17;
    static final int NOTIFY_NATIVE_SETUP =18;
    //---response codes NOTIFY_NATIVE_SETUP
    static final int NATIVE_SETUP_SUCCESS=0;
    static final int PERMISSIONS_PROBLEM=1;
    static final int UNZIPPING_FAILED=2; //Info= reason;
    //--\response codes
}
