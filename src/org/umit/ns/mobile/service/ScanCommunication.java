package org.umit.ns.mobile.service;

public interface ScanCommunication {
    //Requests are sent by the Activity, every request accompanied by ID of the running scan
    static final int RQST_SCAN_ID = 1;
    static final int RESP_SCAN_ID = 2;

    static final int RQST_START_SCAN = 3; //expecting obj with a bundle with "ScanArguments", as well as replyTo
    static final int RESP_START_SCAN_OK = 4;
    static final int RESP_START_SCAN_ERR = 5;

    static final int RQST_STOP_SCAN = 6;
    static final int RESP_STOP_SCAN_OK = 7;
    static final int RESP_STOP_SCAN_ERR = 8;

    static final int RQST_PROGRESS = 9;
    static final int RESP_PROGRESS_OK = 10;
    static final int RESP_PROGRESS_ERR = 11;

    static final int RQST_RESULTS = 12;
    static final int RESP_RESULTS_OK = 13;
    static final int RESP_RESULTS_ERR = 14;

    static final int NOTIFY_SCAN_FINISHED = 15;
    static final int NOTIFY_SCAN_PROBLEM = 16;
}
