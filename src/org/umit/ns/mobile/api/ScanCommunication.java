package org.umit.ns.mobile.api;

public interface ScanCommunication {

        int RQST_REG_CLIENT=1;      //msg.replyTo
        int RQST_NEW_SCAN=2;        //clientID
        int RQST_START_SCAN=3;      //clientID, scanID
        int RQST_STOP_SCAN =4;      //clientID, scanID
        int RQST_REBIND_CLIENT =5;   //clientID, msg.replyTo

        int RESP_REG_CLIENT_OK =6;  //clientID rootAccess
        int RESP_REG_CLIENT_ERR =7; //msg.obj={"info"=info}

        int RESP_NEW_SCAN_OK=8;     //scanID
        int RESP_NEW_SCAN_ERR =9;   //msg.obj={"info"=info}

        int RESP_START_SCAN_OK=10;   //scanID
        int RESP_START_SCAN_ERR=11;  //msg.obj={"info"=info}

        int RESP_STOP_SCAN_OK=12;    //scanID
        int RESP_STOP_SCAN_ERR=13;   //msg.obj={"info"=info}

        int RESP_REBIND_CLIENT_OK=14;//
        int RESP_REBIND_CLIENT_ERR=15;//msg.obj={"info"=info}

        int NOTIFY_SCAN_PROGRESS=16;   //scanID + msg.arg2=progress(percent)
        int NOTIFY_SCAN_PROBLEM=17;    //scanID + info
        int NOTIFY_SCAN_FINISHED=18;   //scanID

        int NOTIFY_ROOT_ACCESS=19;     // internal to threads and service
        int NOTIFY_NATIVE_SETUP=20;     // internal to threads and service

    static final int NATIVE_SETUP_SUCCESS=1;
    static final int NATIVE_SETUP_FAIL=0;

    //Requests are sent by the Activity every request accompanied by ID of the running scan
//    static final int RQST_SCAN_ID = 1; //msg.replyTo
//    static final int RESP_SCAN_ID_OK = 2; //arg1=id, {arg2=1 if ROOT, arg2=0 if NO_ROOT}
//    static final int RESP_SCAN_ID_ERR = 19; //arg1=id, {Info = Reason}
//
//    static final int RQST_START_SCAN = 3; //msg.replyTo obj=Bundle: {string ScanArguments}
//    static final int RESP_START_SCAN_OK = 4; //arg1=id
//    static final int RESP_START_SCAN_ERR = 5; //arg1=id, Scan already started, or no ScanArguments present
//
//    static final int RQST_STOP_SCAN = 6; //msg.replyTo arg1=id
//    static final int RESP_STOP_SCAN_OK = 7; //arg1=id
//    static final int RESP_STOP_SCAN_ERR = 8; //arg1=id, Scan not started
//
//    static final int NOTIFY_SCAN_FINISHED = 15; //msg.replyTo arg1=id, info=<scan results>
//    static final int NOTIFY_SCAN_PROBLEM = 16;  //arg1=id, info= Thread caught a naughty exception
//
//    static final int NOTIFY_ROOT_ACCESS=17;
//    static final int NOTIFY_NATIVE_SETUP =18;
//    //---response codes NOTIFY_NATIVE_SETUP
    //--\response codes
}
