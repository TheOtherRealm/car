package android.car;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class Help extends Activity {
	private TextView Dagu;
	private TextView Addr;
	private TextView Version;
	
	
	private static final String TAG = "Help";
    private static final boolean D = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);  
        Dagu = (TextView) findViewById(R.id.dagu);
        Addr = (TextView) findViewById(R.id.hel);
        Version = (TextView) findViewById(R.id.Beta);
        Version.setText("Beta 1.5.0");
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
        }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
    }



    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    
}
