package org.umit.ns.mobile.model;

import android.net.Uri;
import android.provider.BaseColumns;

public final class ScanOverview {
    private ScanOverview(){};
    public static final String AUTHORITY = "org.umit.ns.mobile.provider.ScanOverview";
    public static final String DEFAULT_SORT_ORDER="_id DESC";
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.umit.scan";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.umit.scan";

    public static final class Scan implements BaseColumns {
        private Scan() {}
        public static final Uri SCANS_URI = Uri.parse("content://" + AUTHORITY+"/scanoverview");
        public static final Uri SCAN_RECORD_BASE_URI = Uri.parse("content://"+AUTHORITY);

        public static final String CLIENT_ID = "clientid";
        public static final String CLIENT_ACTION = "clientaction";
        public static final String ROOT_ACCESS = "rootaccess";
        public static final String SCAN_ID = "scanid";
        public static final String SCAN_ARGUMENTS = "arguments";
        public static final String SCAN_PROGRESS = "progress";
        public static final String SCAN_STATE = "state";
        public static final String SCAN_RESULTS = "results";

        public static final int SCAN_STATE_STARTED=0;
        public static final int SCAN_STATE_FINISHED=1;
    }

}
