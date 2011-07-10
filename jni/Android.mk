LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := synscanner
LOCAL_SRC_FILES := main.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/libpcap
LOCAL_STATIC_LIBRARIES := libpcap

include $(BUILD_EXECUTABLE)

include $(LOCAL_PATH)/libpcap/Android.mk
