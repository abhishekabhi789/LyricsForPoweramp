/*
Copyright (C) 2011-2022 Maksim Petrov

Redistribution and use in source and binary forms, with or without
modification, are permitted for widgets, plugins, applications and other software
which communicate with Poweramp application on Android platform.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.maxmpz.poweramp.player;

import android.annotation.TargetApi;
import android.media.AudioDeviceInfo;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;


public interface RouterConsts {
	// Sync with plugininterface-output.h
    int DEVICE_HEADSET    = 0;
	int DEVICE_SPEAKER    = 1;
	int DEVICE_BT         = 2;
	int DEVICE_USB        = 3;
	int DEVICE_OTHER      = 4;
	int DEVICE_CHROMECAST = 5;
	// 6

	int DEVICE_UNKNOWN = 0xFF;
	
	int DEVICE_COUNT   = 6;
	int DEVICE_SAFE_DEFAULT = DEVICE_HEADSET;

	@NonNull String DEVICE_NAME_HEADSET = "headset";
	@NonNull String DEVICE_NAME_SPEAKER = "speaker";
	@NonNull String DEVICE_NAME_BT = "bt";
	@NonNull String DEVICE_NAME_USB = "usb";
	@NonNull String DEVICE_NAME_OTHER = "other";
	@NonNull String DEVICE_NAME_CHROMECAST = "chromecast";

	@TargetApi(23)
    static int toAndroidDeviceType(int device) {
		switch(device) {
			default:
			case DEVICE_HEADSET:
				return AudioDeviceInfo.TYPE_WIRED_HEADSET; // 3
			case DEVICE_SPEAKER:
				return AudioDeviceInfo.TYPE_BUILTIN_SPEAKER; // 2
			case DEVICE_BT:
				return AudioDeviceInfo.TYPE_BLUETOOTH_A2DP; // 8
			case DEVICE_USB:
				return AudioDeviceInfo.TYPE_USB_DEVICE; // 11
			case DEVICE_CHROMECAST:
				return AudioDeviceInfo.TYPE_IP; // 20
		}
	}

	/** @return true if the device is a valid known device (excluding {@link #DEVICE_UNKNOWN}) */
	static boolean isValidKnownDevice(int device) {
		return device >= 0 && device < DEVICE_COUNT;
	}

	static int getDeviceId(@Nullable String device) {
		if(device == null) {
			return -1;
		}
		switch(device) {
			case DEVICE_NAME_HEADSET:
				return DEVICE_HEADSET;
			case DEVICE_NAME_SPEAKER:
				return DEVICE_SPEAKER;
			case DEVICE_NAME_BT:
				return DEVICE_BT;
			case DEVICE_NAME_USB:
				return DEVICE_USB;
			case DEVICE_NAME_OTHER:
				return DEVICE_OTHER;
			case DEVICE_NAME_CHROMECAST:
				return DEVICE_CHROMECAST;
			default:
				return -1;
		}
	}

	// NOTE: used as pref part
	// REVISIT: refactor this and following statics into a helper?
	static @NonNull String getDeviceName(int device) {
		switch(device) {
			case DEVICE_HEADSET:
				return DEVICE_NAME_HEADSET;

			case DEVICE_SPEAKER:
				return DEVICE_NAME_SPEAKER;

			case DEVICE_BT:
				return DEVICE_NAME_BT;

			case DEVICE_USB:
				return DEVICE_NAME_USB;

			case DEVICE_OTHER:
				return DEVICE_NAME_OTHER;

			case DEVICE_CHROMECAST:
				return DEVICE_NAME_CHROMECAST;

			default:
				return "Unknown_" + device;
		}
	}
}
