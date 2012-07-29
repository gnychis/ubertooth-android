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
Java_com_gnychis_ubertooth_DeviceHandlers_UbertoothOne_startUbertooth(JNIEnv* env, jobject thiz)
{
	devh = ubertooth_start();
  if(devh == NULL)
    return -1;
  else
    return 1;
}


// Returns the "max" of the specified number of sweeps from low_freq to high_freq
jintArray
Java_com_gnychis_ubertooth_DeviceHandlers_UbertoothOne_scanSpectrum( JNIEnv* env, jobject thiz, int low_freq, int high_freq, int sweeps)
{
  int ns, xfer_size=512, num_blocks=0xFFFF, curr_sweep=0,z;
  int nbins = high_freq-low_freq;  // number of 1MHz bins
  jintArray result = (jintArray)(*env)->NewIntArray(env, nbins);
	jint *fill = (int *)malloc(sizeof(int) * nbins);

//  sweeps+=10;

  for(z=0; z<nbins; z++)
    fill[z]=-255;

  bool done=false;
        
  //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "call to scanSpectrum(%d,%d,%d)", low_freq, high_freq, sweeps);
  
  u8 buffer[BUFFER_SIZE];
  int r;
  int i, j;
  int xfer_blocks;
  int num_xfers;
  int transferred;
  int frequency;
  u32 time; /* in 100 nanosecond units */

  if (xfer_size > BUFFER_SIZE)
    xfer_size = BUFFER_SIZE;
  xfer_blocks = xfer_size / PKT_LEN;
  xfer_size = xfer_blocks * PKT_LEN;
  num_xfers = num_blocks / xfer_blocks;
  num_blocks = num_xfers * xfer_blocks;
  
  cmd_specan(devh, low_freq, high_freq);

  while (!done) {
    r = libusb_bulk_transfer(devh, DATA_IN, buffer, xfer_size,
        &transferred, TIMEOUT);
    if (r < 0) {
      __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "bulk read returned: %d, failed to read", r);
      return NULL;
    }
    if (transferred != xfer_size) {
      __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "bad data read size (%d)", transferred);
      return NULL;
    }

    /* process each received block */
    for (i = 0; i < xfer_blocks; i++) {
      time = buffer[4 + PKT_LEN * i]
          | (buffer[5 + PKT_LEN * i] << 8)
          | (buffer[6 + PKT_LEN * i] << 16)
          | (buffer[7 + PKT_LEN * i] << 24);

      for (j = PKT_LEN * i + SYM_OFFSET; j < PKT_LEN * i + 62; j += 3) {
        frequency = (buffer[j] << 8) | buffer[j + 1];
        int val = buffer[j+2];
        int bin = frequency - low_freq;
        //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "... freq: %d - val: %d - bin: %d", frequency, val, bin);
        if(val>fill[bin]) { // Do a max across the sweeps
          fill[bin]=val;
        //  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "... freq: %d - val: %d - bin: %d", frequency, val, bin);
        }

        if(frequency==high_freq) {
          curr_sweep++;
          //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Current sweep complete, now at: %d", curr_sweep);
        }

        if(curr_sweep==sweeps)
          done=true;
      }
    }
  }

  if(result==NULL) {
		result = (jintArray)(*env)->NewIntArray(env, 1);
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "not sure why, but the ubertooth scan failed");
  } else {
    (*env)->SetIntArrayRegion(env, (jintArray)result, (jsize)0, (jsize)nbins, fill);
    //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "ubertooth scan complete");
  }
  
	return result;
}

jint
Java_com_gnychis_ubertooth_DeviceHandlers_UbertoothOne_stopUbertooth(JNIEnv* env, jobject thiz)
{
	ubertooth_stop(devh);
  return 1;
}
