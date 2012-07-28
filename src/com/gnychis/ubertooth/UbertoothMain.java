package com.gnychis.ubertooth;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;

public class UbertoothMain extends Activity {
	
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
	
    
    public String getAppUser() {
    	try {
    		List<String> res = RootTools.sendShell("ls -l /data/data | grep com.gnychis.coexisyst",0);
    		return res.get(0).split(" ")[1];
    	} catch(Exception e) {
    		return "FAIL";
    	}
    }
}
