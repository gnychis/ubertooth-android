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
    	
    	_this = this;
    	
    	toastMessages = new ArrayBlockingQueue<String>(20);
    	graphSpectrum = new GraphSpectrum(this);
		
    	buttonScanSpectrum = (Button) findViewById(R.id.buttonScan); buttonScanSpectrum.setOnClickListener(this);
    	buttonScanSpectrum.setEnabled(false);
    	
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
			
			if(msg.what == ThreadMessages.UBERTOOTH_INITIALIZED.ordinal()) {
				pd.dismiss();
				Toast.makeText(getApplicationContext(), "Successfully initialized Ubertooth One device (" + ubertooth._firmware_version + ")", Toast.LENGTH_LONG).show();	
				usbmon.startUSBMon();		
			}
			
			if(msg.what == ThreadMessages.UBERTOOTH_FAILED.ordinal()) {
				pd.dismiss();
				usbmon.startUSBMon();
				Toast.makeText(getApplicationContext(), "Failed to initialize Ubertooth One device", Toast.LENGTH_LONG).show();			
			}
			
			if(msg.what == ThreadMessages.UBERTOOTH_SCAN_FAILED.ordinal()) {
				pd.dismiss();
				usbmon.startUSBMon();
				Toast.makeText(getApplicationContext(), "Failed to initialize scan on the Ubertooth", Toast.LENGTH_LONG).show();
			}
			
			if(msg.what == ThreadMessages.UBERTOOTH_SCAN_COMPLETE.ordinal()) {
				usbmon.startUSBMon();
				_scan_result = (ArrayList<Integer>)msg.obj;
				pd.dismiss();
				
				Intent i = graphSpectrum.execute(_this);
				startActivity(i);
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
    
    public String getAppUser() {
    	try {
    		List<String> res = RootTools.sendShell("ls -l /data/data | grep com.gnychis.ubertooth",0);
    		return res.get(0).split(" ")[1];
    	} catch(Exception e) {
    		return "FAIL";
    	}
    }
}
