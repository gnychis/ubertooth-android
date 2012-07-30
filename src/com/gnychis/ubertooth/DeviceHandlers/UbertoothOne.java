package com.gnychis.ubertooth.DeviceHandlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

import com.gnychis.ubertooth.UbertoothMain;
import com.gnychis.ubertooth.UbertoothMain.ThreadMessages;
import com.stericson.RootTools.RootTools;

// This class accesses native JNI methods that are wrappers around some functions
// in ubertooth.c, ultimately accessing the Ubertooth natively.  There is a helper
// JNI library called 'ubertooth' and built by jni/ubertooth/ubertooth_helper.  
// Others could build additional native JNI access to ubertooth functions by placing
// them in ubertooth_helper.  This is where startUbertooth(), stopUbertooth(), and 
// scanSpectrum() exist.
public class UbertoothOne {
	private static final String TAG = "UbertoothOneDev";
	private static final boolean VERBOSE = true;
	
	public static final int BT_LOW_FREQ=2402;
	public static final int BT_HIGH_FREQ=2480;

	public static final int UBERTOOTH_CONNECT = 400;
	public static final int UBERTOOTH_DISCONNECT = 401;
	public static final String UBERTOOTH_SCAN_RESULT = "com.gnychis.coexisyst.UBERTOOTH_SCAN_RESULT";
	public static final int SWEEPS_IN_MAX = 200;
	
	UbertoothMain _mainActivity;		// Keep the instance of the main activity
	public String _firmware_version;	// Just for asthetics, keep the firmware version
	
	UbertoothOneScan _scan_thread;		// We use a separate thread for scans so they don't block main activity
	public boolean _device_connected;	// Simple bool to keep track if the Ubertooth is currently connected
	ArrayList<Integer> _scan_result;	// Keep track of the last scan result
	
	public UbertoothOne(UbertoothMain c) { _mainActivity = c; }
	public boolean isConnected() { return _device_connected; }
	
	// If we are notified that the device is connected, we execute a thread which actually
	// initializes the Ubertooth USB device.  Again, so we don't block the main activity.  It sends
	// a notification when initialized, or when the initialization fails.
	public void connected() {
		_device_connected=true;
		UbertoothOneInit wsi = new UbertoothOneInit();
		wsi.execute(_mainActivity);
	}
	
	// When disconnected, we can disable the scan button.
	public void disconnected() {
		_mainActivity.buttonScanSpectrum.setEnabled(false);
		_device_connected=false;
	}
	
	// This is a thread that will initialize the Ubertooth device by calling a native
	// JNI library helper function called startUbertooth().  If the initialization fails,
	// we will be notified of it.  
	protected class UbertoothOneInit extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		UbertoothMain mainActivity;
		
		// Used to send messages to the main Activity (UI) thread
		protected void sendMainMessage(UbertoothMain.ThreadMessages t, Object obj) {
			Message msg = new Message();
			msg.what = t.ordinal();
			msg.obj = obj;
			mainActivity._handler.sendMessage(msg);
		}
		
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			mainActivity = (UbertoothMain) params[0];
			
			// To use the Ubertooth device, we need to give the USB device the application's permissions.
			// Otherwise, it is limited to root and the application cannot natively access the /dev handle.
			runCommand("find /dev/bus -exec chown " + mainActivity.getAppUser() + " {} \\;");
			
			// Get the firmware version for fun and demonstration
			_firmware_version = runCommand("/data/data/com.gnychis.ubertooth/files/ubertooth_util -v").get(0);
			_scan_result = new ArrayList<Integer>();
			
			// Try to initialize the Ubertooth One
			if(startUbertooth()==1)
				sendMainMessage(ThreadMessages.UBERTOOTH_INITIALIZED,null);
			else
				sendMainMessage(ThreadMessages.UBERTOOTH_FAILED,null);

			return "OK";
		}
		
		// This is a helper function I wrote to run a command as root and get the resulting
		// output from the shell.  Mainly provided by RootTools.
		public ArrayList<String> runCommand(String c) {
			ArrayList<String> res = new ArrayList<String>();
			try {
				// First, run the command push the result to an ArrayList
				List<String> res_list = RootTools.sendShell(c,0);
				Iterator<String> it=res_list.iterator();
				while(it.hasNext()) 
					res.add((String)it.next());
				
				res.remove(res.size()-1);
				
				// Trim the ArrayList of an extra blank lines at the end
				while(true) {
					int index = res.size()-1;
					if(index>=0 && res.get(index).length()==0)
						res.remove(index);
					else
						break;
				}
				return res;
				
			} catch(Exception e) {
				Log.e("WifiDev", "error writing to RootTools the command: " + c, e);
				return null;
			}
		}
	}
	
	// This starts the scan thread, passing the main activity and beginning the spectrum scan.
	public boolean scanStart() {
		_scan_result.clear();
		_scan_thread = new UbertoothOneScan();
		_scan_thread.execute(_mainActivity);
		return true;  // in scanning state, and channel hopping
	}
	
	// This is a thread to perform the actual scan (blocking and waiting for it), rather
	// than blocking the main activity.  When it is complete, it sends the results to the
	// main activity.
	protected class UbertoothOneScan extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		UbertoothMain mainActivity;
		
		// Used to send messages to the main Activity (UI) thread
		protected void sendMainMessage(UbertoothMain.ThreadMessages t, Object obj) {
			Message msg = new Message();
			msg.what = t.ordinal();
			msg.obj = obj;
			mainActivity._handler.sendMessage(msg);
		}
		
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			mainActivity = (UbertoothMain) params[0];
			
			// Perform the scan, specify the low and high freqs as well as
			// the number of sweeps to perform (this is a "max hold").
			int[] scan_res = scanSpectrum(BT_LOW_FREQ, BT_HIGH_FREQ, SWEEPS_IN_MAX);
			
			if(scan_res==null) {
				sendMainMessage(ThreadMessages.UBERTOOTH_SCAN_FAILED, null);
				return "NOPE";
			}
			
			_scan_result = new ArrayList<Integer>();
			for(int i=0; i<scan_res.length; i++)
				_scan_result.add(scan_res[i]);
				
			sendMainMessage(ThreadMessages.UBERTOOTH_SCAN_COMPLETE, _scan_result);
			
			return "PASS";
		}
		
	}
	
	public native int startUbertooth();
	public native int stopUbertooth();
	public native int[] scanSpectrum(int low_freq, int high_freq, int sweeps);
}
