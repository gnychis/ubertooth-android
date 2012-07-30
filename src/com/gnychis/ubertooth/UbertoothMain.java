package com.gnychis.ubertooth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.gnychis.ubertooth.Core.USBMon;
import com.gnychis.ubertooth.DeviceHandlers.UbertoothOne;
import com.gnychis.ubertooth.Interfaces.GraphSpectrum;
import com.gnychis.ubertooth.Interfaces.IChart;
import com.stericson.RootTools.RootTools;

public class UbertoothMain extends Activity implements OnClickListener {
	
	public UbertoothOne ubertooth;
	protected USBMon usbmon;
	public Button buttonScanSpectrum;
	public IChart graphSpectrum;
	UbertoothMain _this;
	public ArrayList<Integer> _scan_result;
	
	public BlockingQueue<String> toastMessages;
	private ProgressDialog pd;
	
	// A few message types that are used to pass information between several threads.
	// This, for example, allows a thread handling the Ubertooth device to report a finished
	// or failed scan.
	public enum ThreadMessages {
		UBERTOOTH_CONNECTED,
		UBERTOOTH_DISCONNECTED,
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
        
        // Install a cross-compiled version of ubertooth-util which we run to
        // retrieve the firmware version on the Ubertooth.  This is nothing more
        // than to demonstrate the possibility.  This is build using ndk-build
        // and is in jni/ubertooth/Android.mk.  You can easily modify it to
        // cross-compile any of the other tools.
        // Note that this Android application does *not* cross-compile and run
        // ubertooth-specan.  Instead, it communicates directly with the Ubertooth
        // and uses native code to initiate the scan and get the results.  This
        // demonstrates direct access to the device without the need of relying on
        // external applications.
        RootTools.installBinary(this, R.raw.ubertooth_util, "ubertooth_util");
        
    	try {  // Load several libraries.  These are all built in jni/ with ndk-build
    		System.loadLibrary("usb");
    		System.loadLibrary("usb-compat");
    		System.loadLibrary("usbhelper");	// provides native calls to check for USB changes
    		System.loadLibrary("btbb");			// need to include the cross-compiled version of btbb
    		System.loadLibrary("ubertooth");	// this is a "helper" library to allow native Ubertooth access
    	} catch (Exception e) {
    		Log.e("UbertoothMain", "error trying to load a USB related library", e);
    	}
    	
    	_this = this;	// Save an instance to this class
    	
    	toastMessages = new ArrayBlockingQueue<String>(20);	// Used for toast messages
    	graphSpectrum = new GraphSpectrum(this);			// Used to graph the power in the spectrum
		
    	// Setup a button to click which initiates the spectrum scan.  Disable it until the Ubertooth is plugged in
    	buttonScanSpectrum = (Button) findViewById(R.id.buttonScan); buttonScanSpectrum.setOnClickListener(this);
    	buttonScanSpectrum.setEnabled(false);
    	
    	// Instantiate the Ubertooth and the USB monitor (which checks for plugs of the device)
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
			if(msg.what == ThreadMessages.UBERTOOTH_CONNECTED.ordinal()) {
				ubertoothSettling();
			}
			
			// The main activity has received a message that the ubertooth has been initialited.
			// We can initiate the scan button.
			if(msg.what == ThreadMessages.UBERTOOTH_INITIALIZED.ordinal()) {
				pd.dismiss();
				Toast.makeText(getApplicationContext(), "Successfully initialized Ubertooth One device (" + ubertooth._firmware_version + ")", Toast.LENGTH_LONG).show();	
				usbmon.startUSBMon();	
				buttonScanSpectrum.setEnabled(true);
			}
			
			// Failed to initialize the ubertooth device
			if(msg.what == ThreadMessages.UBERTOOTH_FAILED.ordinal()) {
				pd.dismiss();
				usbmon.startUSBMon();
				Toast.makeText(getApplicationContext(), "Failed to initialize Ubertooth One device", Toast.LENGTH_LONG).show();			
			}
			
			// The scan failed (never had this happen, but...)
			if(msg.what == ThreadMessages.UBERTOOTH_SCAN_FAILED.ordinal()) {
				pd.dismiss();
				usbmon.startUSBMon();
				Toast.makeText(getApplicationContext(), "Failed to initialize scan on the Ubertooth", Toast.LENGTH_LONG).show();
			}
			
			// A message that specifies the scan was complete.  We save the scan result and start
			// the activity to graph the result.
			if(msg.what == ThreadMessages.UBERTOOTH_SCAN_COMPLETE.ordinal()) {
				usbmon.startUSBMon();
				_scan_result = (ArrayList<Integer>)msg.obj;
				Intent i = graphSpectrum.execute(_this);
				startActivity(i);
				pd.dismiss();
			}
			
			// The Ubertooth device has been disconnected.  We call .disconnected() which also disables
			// the scan button.
			if(msg.what == ThreadMessages.UBERTOOTH_DISCONNECTED.ordinal()) {
				Toast.makeText(getApplicationContext(), "Ubertooth device has been disconnected", Toast.LENGTH_LONG).show();
				ubertooth.disconnected();
			}
			
			
			///////////////////////////////////////////////////////////////////////
			// A set of messages that that deal with hardware connections
			if(msg.what == ThreadMessages.SHOW_TOAST.ordinal()) {
				try {
					String m = toastMessages.remove();
					Toast.makeText(getApplicationContext(), m, Toast.LENGTH_LONG).show();	
				} catch(Exception e) { }
			}
		}
	};
	
	// When we get a click to scan the spectrum, we disable the USB monitor (so it doesn't do
	// a periodic poll while we're scanning), and starts the actual spectrum scan.
	public void onClick(View view) {
		if(view.getId() == R.id.buttonScan) {
			pd = new ProgressDialog(this);
			pd.setCancelable(false);
			pd.setMessage("Scanning spectrum with Ubertooth...");
			pd.show();
			usbmon.stopUSBMon();
			ubertooth.scanStart();
		}
	}
	
	public void ubertoothSettling() {
		pd = ProgressDialog.show(this, "", "Initializing Ubertooth One device...", true, false);
		usbmon.stopUSBMon();
		ubertooth.connected();
	}

	// Display some toast messages if needed...
	public void sendToastMessage(Handler h, String msg) {
		try {
			toastMessages.put(msg);
			Message m = new Message();
			m.what = ThreadMessages.SHOW_TOAST.ordinal();
			h.sendMessage(m);
		} catch (Exception e) {
			Log.e("UbertoothMain", "Exception trying to put toast msg in queue:", e);
		}
	}
    
	// This is used to get the Android application's username (e.g., app_115).  This is used
	// to chown the Ubertooth device in /dev/ so that the application can natively access the device.
	// Otherwise, it will get an access denied error (the device is default root).
    public String getAppUser() {
    	try {
    		List<String> res = RootTools.sendShell("ls -l /data/data | grep com.gnychis.ubertooth",0);
    		return res.get(0).split(" ")[1];
    	} catch(Exception e) {
    		return "FAIL";
    	}
    }
}
