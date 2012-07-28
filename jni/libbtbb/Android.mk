LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= bluetooth_packet.c bluetooth_piconet.c
LOCAL_MODULE := libbtbb
LOCAL_C_INCLUDES += 
LOCAL_SHARED_LIBRARIES :=
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog 
include $(BUILD_SHARED_LIBRARY)
