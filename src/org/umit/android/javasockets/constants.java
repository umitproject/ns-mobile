package org.umit.android.javasockets;

interface constants 
{
	//now overridden by the UI element ttl (EditText)
	public static final int thread_sleep = 11;
	public static final int debug_lines = 15;
}

interface ttl {
	public static final int is_reachable = 2000;
	public static final int echo_ping = 5000;
	public static final int socket_ping = 1000;
}

interface ports{
	public static final int[] port = {
		139,
		445,
		22,
		80
	};
}