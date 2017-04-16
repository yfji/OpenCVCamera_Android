LOCAL_PATH := $(call my-dir)
OPENCV_PATH:=I:/Develop/MyAndroid/OpenCV-2.4.10-android-sdk/sdk/native/jni

include $(CLEAR_VARS)
OPENCV_INSTALL_MODULES := on
OPENCV_CAMERA_MODULES := off

include $(OPENCV_PATH)/OpenCV.mk

LOCAL_C_INCLUDES:= $(LOCAL_PATH)\
					$(OPENCV_PATH)/include

LOCAL_LDLIBS    += -lm -llog

LOCAL_MODULE    := ImageProcess
LOCAL_SRC_FILES := ImageProcess.cpp

include $(BUILD_SHARED_LIBRARY)
