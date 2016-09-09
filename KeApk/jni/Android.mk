LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := ZwcDecryptUtils
LOCAL_SRC_FILES := DecryptUtils.cpp
include $(BUILD_SHARED_LIBRARY)
