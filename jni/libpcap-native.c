#include <jni.h>  
#include <string.h>  
#include <android/log.h>
#include <pcap.h>

#define DEBUG_TAG "Sample_LIBPCAP_DEBUGGING"

void Java_com_umit_android_libpcap_libpcap_testLog(JNIEnv * env, jobject this, jstring logThis)  
{  
    jboolean isCopy;  
    char *dev, errbuf[100];
	char * szLogThis;
	
	dev = pcap_lookupdev(errbuf);
		if (dev == NULL) {
			szLogThis = "Couldn't find default device";		
		}
		else szLogThis = dev;
		
    __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]", szLogThis);  
  
    (*env)->ReleaseStringUTFChars(env, logThis, szLogThis);  
}