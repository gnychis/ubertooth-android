package com.gnychis.ubertooth.DeviceHandlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

import com.gnychis.ubertooth.UbertoothMain;
import com.gnychis.ubertooth.UbertoothMain.ThreadMessages;
import com.stericson.RootTools.RootTools;

public class UbertoothOne {
	private static final String TAG = "UbertoothOneDev";
	private static final boolean VERBOSE = true;

	public static final int UBERTOOTH_CONNECT = 400;
	public static final int UBERTOOTH_DISCONNECT = 401;
	public static final String UBERTOOTH_SCAN_RESULT = "com.gnychis.coexisyst.UBERTOOTH_SCAN_RESULT";
	public static final int POLLS_IN_MAX = 10;
	
	UbertoothMain _mainActivity;
	
	public boolean _device_connected;
	
	ArrayList<Integer> _scan_result;
	
	UbertoothState _state;
	private Semaphore _state_lock;
	public enum UbertoothState {
		IDLE,
		SCANNING,
	}
	
	public UbertoothOne(UbertoothMain c) {
		_state_lock = new Semaphore(1,true);
		_mainActivity = c;
		_state = UbertoothState.IDLE;
	}
	
	public boolean isConnected() {
		return _device_connected;
	}
	
	public void connected() {
		_device_connected=true;
		UbertoothOneInit wsi = new UbertoothOneInit();
		wsi.execute(_mainActivity);
	}
	
	public void disconnected() {
		_device_connected=false;
	}
	
	protected class UbertoothOneInit extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		UbertoothMain mainActivity;
		
		private void debugOut(String msg) {
			if(VERBOSE)
				Log.d("WiSpyInit", msg);
		}
		
		// Used to send messages to the main Activity (UI) thread
		protected void sendMainMessage(UbertoothMain.ThreadMessages t) {
			Message msg = new Message();
			msg.obj = t;
			mainActivity._handler.sendMessage(msg);
		}
		
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			mainActivity = (UbertoothMain) params[0];
			
			// To use the WiSpy device, we need to give the USB device the application's permissions
			runCommand("find /dev/bus -exec chown " + mainActivity.getAppUser() + " {} \\;");
			
			// Try to initialize the Ubertooth One
			if(startUbertooth()==1)
				sendMainMessage(ThreadMessages.UBERTOOTH_INITIALIZED);
			else
				sendMainMessage(ThreadMessages.UBERTOOTH_FAILED);

			return "OK";
		}
		
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
	
	public native int startUbertooth();
	public native int stopUbertooth();
}
