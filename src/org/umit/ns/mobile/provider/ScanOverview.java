package org.umit.ns.mobile.provider;

import android.content.IntentFilter;
import android.net.Uri;
import android.provider.BaseColumns;

public class ScanOverview {
    public static final class Scan implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri SCANS_URI =
                Uri.parse("content://org.umit.ns.mobile.provider.ScanOverview/scanoverview");

        public static final Uri SCAN_RECORD_BASE_URI = Uri.parse("content://org.umit.ns.mobile.provider.ScanOverview");

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_id DESC";

        /**
         * Client ID
         * <P>Type: INTEGER</P>
         */
        public static final String CLIENT_ID = "clientid";

        /**
         * Client Action that would be put in an Intent which would
         * fire up the activity
         * <P>Type: TEXT</P>
         */
        public static final String CLIENT_ACTION = "clientaction";

        /**
         * Whether the service has root access
         * <P>Type: INTEGER</P>
         */
        public static final String ROOT_ACCESS = "rootaccess";

        /**
         * Scan ID
         * <P>Type: INTEGER</P>
         */
        public static final String SCAN_ID = "scanid";

        /**
         * Scan arguments
         * <P>Type: TEXT</P>
         */
        public static final String SCAN_ARGUMENTS = "arguments";

        /**
         * Progress of the scan
         * <P>Type: INTEGER</P>
         */
        public static final String SCAN_PROGRESS = "progress";

        /**
         * State of the scan: Scanning, Finished
         * <P>Type: INTEGER</P>
         */
        public static final String SCAN_STATE = "state";

        /**
         * Scan results
         * <P>Type: TEXT</P>
         */
        public static final String SCAN_RESULTS = "results";

        public static final int SCAN_STATE_STARTED=0;
        public static final int SCAN_STATE_FINISHED=1;

    }


}
