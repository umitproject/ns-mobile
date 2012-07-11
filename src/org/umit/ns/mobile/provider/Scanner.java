package org.umit.ns.mobile.provider;

import android.provider.BaseColumns;

import android.net.Uri;
import android.provider.BaseColumns;import java.lang.String;

public final class Scanner {
	private Scanner() {}

	public static final String AUTHORITY = "org.umit.ns.mobile.provider.Scanner";
	public static final String DEFAULT_SORT_ORDER = "_id DESC";

	public static final Uri SCANS_URI = Uri.parse("content://" + AUTHORITY+"/scans");
	public static final Uri HOSTS_URI = Uri.parse("content://" + AUTHORITY+"/hosts");
	public static final Uri DETAILS_URI = Uri.parse("content://" + AUTHORITY+"/details");

	public static final class Scans implements BaseColumns {
		private Scans() {
		}

		/**
		 * Client ID
		 * <P>Type: INTEGER</P>
		 */
		public static final String CLIENT_ID = "clientid";

		/**
		 * Action to fire up intent for activating the client
		 * <P>Type: TEXT</P>
		 */
		public static final String CLIENT_ACTION = "clientaction";

		/**
		 * Root Access, True=1; False=0;
		 * <P>Type: INTEGER</P>
		 */
		public static final String ROOT_ACCESS = "rootaccess";

		/**
		 * Scan ID
		 * <P>Type: INTEGER</P>
		 */
		public static final String SCAN_ID = "scanid";

		/**
		 * State of the Scan: Started=0; Finished=1;
		 * <P>Type: INTEGER</P>
		 */
		public static final String SCAN_STATE = "state";

		/**
		 * Scan Progress
		 * <P>Type: INTEGER</P>
		 */
		public static final String SCAN_PROGRESS = "progress";

		/**
		 * Parameters for the scan
		 * <P>Type: TEXT</P>
		 */
		public static final String SCAN_ARGUMENTS = "arguments";

		public static final String HOSTS_TABLE_NAME = "hoststablename";

		public static final int SCAN_STATE_STARTED = 0;
		public static final int SCAN_STATE_FINISHED = 1;

		public static final int ROOT_ACCESS_YES = 1;
		public static final int ROOT_ACCESS_NO = 0;

	}

	public static final class Hosts implements BaseColumns {
		private Hosts() {
		}


		/**
		 * Host IP address
		 * <P>Type: TEXT</P>
		 */
		public static final String IP = "ip";

		/**
		 * Hostname
		 * <P>Type: TEXT</P>
		 */
		public static final String NAME = "name";

		/**
		 * OS type: 0-9: Freebsd,IRIX,Linux,MacOSX,OpenBSD,RedHat,
		 * Ubuntu,Solaris,Windows,unknown
		 * <P>Type: INTEGER</P>
		 */
		public static final String OS = "os";

		/**
		 * Host state: Down=0; Up=1;
		 * <P>Type: INTEGER</P>
		 */
		public static final String STATE = "state";

		public static final String DETAILS_TABLE_NAME = "detailstablename";

		public static final int STATE_DOWN = 0;
		public static final int STATE_UP = 1;

		public static final int OS_FREEBSD = 0;
		public static final int OS_IRIX = 1;
		public static final int OS_LINUX = 2;
		public static final int OS_MACOSX = 3;
		public static final int OS_OPENBSD = 4;
		public static final int OS_REDHAT = 5;
		public static final int OS_UBUNTU = 6;
		public static final int OS_SOLARIS = 7;
		public static final int OS_WINDOWS = 8;
		public static final int OS_UNKNOWN = 9;
	}

	public static final class Details implements BaseColumns {
		private Details() {}

		/**
		 * Details Type: Port results / Script results / OS/Service fingerprinting
		 * <P>Type: TEXT</P>
		 */
		public static final String NAME = "name";

		/**
		 * The details themselves
		 * <P>Type: TEXT</P>
		 */
		public static final String DATA = "data";
	}

}