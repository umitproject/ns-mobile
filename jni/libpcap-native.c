#include <jni.h>  
#include <string.h>  
#include <android/log.h>
#include <pcap.h>

#define DEBUG_TAG "Sample_LIBPCAP_DEBUGGING"

void Java_org_umit_android_libpcaptest_libpcaptest_testLog(JNIEnv *env, jclass clazz, jstring message) 
{  
    char errbuf[1024];
    errbuf[0] = '\0';
	
	char *szLogThis;
	char *dev = pcap_lookupdev(errbuf);
	
	if (dev == NULL) {
		szLogThis = "Couldn't find default device";		
	}
	else szLogThis = dev;

    __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "Device : [%s]", szLogThis);  
  	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "Error buffer [%s]", errbuf);  
  
    (*env)->ReleaseStringUTFChars(env, message, szLogThis);  
}