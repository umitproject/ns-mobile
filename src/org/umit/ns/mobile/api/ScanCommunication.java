package org.umit.ns.mobile.api;

public interface ScanCommunication {

	static final int RQST_START_SCAN = 1;
	static final int RESP_START_SCAN_OK = 2;

	static final int RQST_STOP_SCAN = 3;
	static final int RESP_STOP_SCAN_OK = 4;

	static final int NOTIFY_SCAN_PROBLEM = 5;
	static final int NOTIFY_SCAN_FINISHED = 6;
	static final int NOTIFY_SCAN_PROGRESS = 7;
	static final int NOTIFY_ROOT_ACCESS = 8;
	static final int NOTIFY_NATIVE_SETUP = 9;

	static final int NATIVE_SETUP_SUCCESS = 1;
	static final int NATIVE_SETUP_FAIL = 0;

	static final int REGISTER_CLIENT = 10;
	static final int REGISTER_CLIENT_RESP = 11;

	static final int STOP_SCAN_SERVICE = 12;
}
