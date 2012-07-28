package com.gnychis.ubertooth;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.gnychis.coexisyst.R;
import com.gnychis.ubertooth.Core.USBMon;
import com.gnychis.ubertooth.DeviceHandlers.UbertoothOne;
import com.stericson.RootTools.RootTools;

public class UbertoothMain extends Activity {
	
	public UbertoothOne ubertooth;
	protected USBMon usbmon;
	
	public BlockingQueue<String> toastMessages;
	private ProgressDialog pd;
	public enum ThreadMessages {
		UBERTOOTH_CONNECTED,
		UBERTOOTH_INITIALIZED,
		UBERTOOTH_FAILED,
		UBERTOOTH_SCAN_COMPLETE,
		UBERTOOTH_SCAN_FAILED,
		SHOW_TOAST,
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ubertooth_main);
        
        RootTools.installBinary(this, R.raw.ubertooth_util, "ubertooth_util");
        
    	try {  // Load the libusb related libraries
    		System.loadLibrary("usb");
    		System.loadLibrary("usb-compat");
    		System.loadLibrary("usbhelper");
    		System.loadLibrary("btbb");
    		System.loadLibrary("ubertooth");
    	} catch (Exception e) {
    		Log.e("UbertoothMain", "error trying to load a USB related library", e);
    	}
    	
    	toastMessages = new ArrayBlockingQueue<String>(20);
    	
        ubertooth = new UbertoothOne(this);		// Instantiate the UbertoothOne
        usbmon = new USBMon(this, _handler);	// Start the USB handler
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_ubertooth_main, menu);
        return true;
    }
    
	public Handler _handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			///////////////////////////////////////////////////////////////////////
			// A set of messages referring to scans goes to the scan class
			//if(msg.obj == ThreadMessages.NETWORK_SCANS_COMPLETE)
			//	networkScansComplete();
						
			///////////////////////////////////////////////////////////////////////
			// A set of messages that that deal with hardware connections
			if(msg.obj == ThreadMessages.UBERTOOTH_CONNECTED)
				ubertoothSettling();
			if(msg.obj == ThreadMessages.UBERTOOTH_INITIALIZED)
				ubertoothInitialized();
			if(msg.obj == ThreadMessages.UBERTOOTH_FAILED)
				ubertoothFailed();
			
			
			///////////////////////////////////////////////////////////////////////
			// A set of messages that that deal with hardware connections
			if(msg.obj == ThreadMessages.SHOW_TOAST) {
				try {
					String m = toastMessages.remove();
					Toast.makeText(getApplicationContext(), m, Toast.LENGTH_LONG).show();	
				} catch(Exception e) { }
			}
		}
	};
	
	
	public void ubertoothSettling() {
		pd = ProgressDialog.show(this, "", "Initializing Ubertooth One device...", true, false);
		usbmon.stopUSBMon();
		ubertooth.connected();
	}
	
	public void ubertoothInitialized() {
		pd.dismiss();
		Toast.makeText(getApplicationContext(), "Successfully initialized Ubertooth One device", Toast.LENGTH_LONG).show();	
		usbmon.startUSBMon();		
	}
	
	public void ubertoothFailed() {
		pd.dismiss();
		usbmon.startUSBMon();
		Toast.makeText(getApplicationContext(), "Failed to initialize Ubertooth One device", Toast.LENGTH_LONG).show();
	}
	
	public void sendToastMessage(Handler h, String msg) {
		try {
			toastMessages.put(msg);
			Message m = new Message();
			m.obj = ThreadMessages.SHOW_TOAST;
			h.sendMessage(m);
		} catch (Exception e) {
			Log.e("UbertoothMain", "Exception trying to put toast msg in queue:", e);
		}
	}
    
    public String getAppUser() {
    	try {
    		List<String> res = RootTools.sendShell("ls -l /data/data | grep com.gnychis.ubertooth",0);
    		return res.get(0).split(" ")[1];
    	} catch(Exception e) {
    		return "FAIL";
    	}
    }
}
