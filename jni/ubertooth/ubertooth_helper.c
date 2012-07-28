/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <jni.h>
#include <errno.h>
#include <stdbool.h>
#include <android/log.h>
#include "ubertooth.h"
#include <stdio.h>
#include <getopt.h>
#include <unistd.h>

#define LOG_TAG "UbertoothDriver" // text for log tag 

extern char Quiet;
extern char Ubertooth_Device;
	
struct libusb_device_handle *devh = NULL;

int main(int argc, char *argv[]) {
  return 1;
}

jint
Java_com_gnychis_coexisyst_DeviceHandlers_UbertoothOne_startUbertooth(JNIEnv* env, jobject thiz)
{
	devh = ubertooth_start();
  if(devh == NULL)
    return -1;
  else
    return 1;
}

jint
Java_com_gnychis_coexisyst_DeviceHandlers_UbertoothOne_stopUbertooth(JNIEnv* env, jobject thiz)
{
	ubertooth_stop(devh);
  return 1;
}
