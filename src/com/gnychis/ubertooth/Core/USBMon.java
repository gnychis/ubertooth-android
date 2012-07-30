package com.gnychis.ubertooth.Core;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gnychis.ubertooth.UbertoothMain;
import com.gnychis.ubertooth.UbertoothMain.ThreadMessages;
import com.gnychis.ubertooth.DeviceHandlers.UbertoothOne;
import com.stericson.RootTools.RootTools;

// A class to handle USB worker like things
public class USBMon
{
	private static boolean VERBOSE = false;
	
	UbertoothMain _mainActivity;
	private Handler _handler;
	private static int USB_POLL_TIME=7000;  // in milliseconds, poll time
	
	private Timer _scan_timer;
	
	public USBMon(UbertoothMain c, Handler h) {
		_mainActivity = c;
		_handler = h;
		_scan_timer=null;
		_mainActivity.sendToastMessage(_handler, initUSB());
		startUSBMon();
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d("USBMon", msg);
	}
	
	public boolean startUSBMon() {
		if(_scan_timer!=null)
			return false;
		_scan_timer=new Timer();
		_scan_timer.schedule(new TimerTask() {
			@Override
			public void run() {
				usbPoll();
			}

		}, 0, USB_POLL_TIME);
		return true;
	}
	
	public boolean stopUSBMon() {
		if(_scan_timer==null)
			return false;
		
		_scan_timer.cancel();
		_scan_timer=null;
		return true;
	}

	public void usbPoll( )
	{
		int ubertooth_in_devlist = USBcheckForDevice(0xffff, 0x0004);
				
		// Ubertooth check
		if(ubertooth_in_devlist==1 && _mainActivity.ubertooth._device_connected==false)
			updateState(UbertoothOne.UBERTOOTH_CONNECT);
		else if(ubertooth_in_devlist==0 && _mainActivity.ubertooth._device_connected==true)
			updateState(UbertoothOne.UBERTOOTH_DISCONNECT);
	}
	
	protected void updateState(int event)
	{		
		if(event == UbertoothOne.UBERTOOTH_CONNECT) {
			Message msg = new Message();
			msg.what = ThreadMessages.UBERTOOTH_CONNECTED.ordinal();
			_mainActivity._handler.sendMessage(msg);
			debugOut("got update that Ubertooth device was connected");
		}
		else if(event == UbertoothOne.UBERTOOTH_DISCONNECT) {
			debugOut("Ubertooth device now disconnected");
			_mainActivity.sendToastMessage(_handler, "Ubertooth device disconnected");
			_mainActivity.ubertooth.disconnected();
		}
	}
	public native void USBList();
	public native int USBcheckForDevice(int vid, int pid);
	public native String initUSB();
}
