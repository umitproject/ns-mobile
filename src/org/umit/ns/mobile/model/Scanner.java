package org.umit.ns.mobile.model;

import android.net.Uri;
import android.provider.BaseColumns;

public final class Scanner {
	private Scanner() {}

	public static final String AUTHORITY = "org.umit.ns.mobile.provider.Scanner";
	public static final String DEFAULT_SORT_ORDER = "_id DESC";
	public static final String SCANS_TYPE = "vnd.android.cursor.dir/vnd.umit.scan";
	public static final String HOSTS_TYPE = "vnd.android.cursor.dir/vnd.umit.host";
	public static final String DETAILS_TYPE = "vnd.android.cursor.dir/vnd.umit.detail";
	public static final String SCAN_TYPE = "vnd.android.cursor.item/vnd.umit.scan";
	public static final String HOST_TYPE = "vnd.android.cursor.item/vnd.umit.host";
	public static final String DETAIL_TYPE = "vnd.android.cursor.item/vnd.umit.detail";

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
		 * TASK Progress
		 * <P>Type: INTEGER</P>
		 */
		public static final String TASK_PROGRESS = "progress";

		/**
		 * Parameters for the scan
		 * <P>Type: TEXT</P>
		 */
		public static final String SCAN_ARGUMENTS = "arguments";

		/**
		 * Running task
		 * <P>Type: TEXT</P>
		 */
		public static final String TASK = "task";

		/**
		 * Error Message
		 * <P>Type: TEXT</P>
		 */
		public static final String ERRORMESSAGE = "errormessage";

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
		public static final int STATE_UNKNOWN = 2;
		public static final int STATE_SKIPPED = 3;
		public static final int STATE_NULL = -1;


		public static final int OS_FREEBSD = 0;
		public static final int OS_IRIX = 1;
		public static final int OS_LINUX = 2;
		public static final int OS_MACOSX = 3;
		public static final int OS_OPENBSD = 4;
		public static final int OS_NETBSD = 10;
		public static final int OS_REDHAT = 5;
		public static final int OS_UBUNTU = 6;
		public static final int OS_SOLARIS = 7;
		public static final int OS_WINDOWS = 8;
		public static final int OS_UNKNOWN = 9;
	}

	public static final class Details implements BaseColumns {
		private Details() {
		}

		/**
		 * Details Type: Port results / Script results / OS/Service fingerprinting
		 * <P>Type: TEXT</P>
		 */
		public static final String TYPE = "type";

		/**
		 * Details Name: Port number / Script name ...
		 * <P>Type: TEXT</P>
		 */
		public static final String NAME = "name";

		/**
		 * State of Details: 0=NOT_PORT etc...
		 * <P>Type: INTEGER</P>
		 */
		public static final String STATE = "state";

		public static final int STATE_NOT_PORT = 0;
		public static final int STATE_PORT_OPEN = 1;
		public static final int STATE_PORT_FILTERED = 2;
		public static final int STATE_PORT_UNFILTERED = 3;
		public static final int STATE_PORT_CLOSED = 4;
		public static final int STATE_PORT_OPENFILTERED = 5;
		public static final int STATE_PORT_CLOSEDFILTERED = 6;
		public static final int STATE_PORT_UNKNOWN = 7;

		/**
		 * The details themselves
		 * <P>Type: TEXT</P>
		 */
		public static final String DATA = "data";
	}

}
