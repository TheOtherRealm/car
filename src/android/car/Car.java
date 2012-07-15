package android.car;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class Car extends Activity {

	private TextView Direction;
	private TextView Speed;

	private TextView device;
	private ImageButton mButtonF;
	private ImageButton mButtonB;
	private ImageButton mButtonL;
	private ImageButton mButtonR;
	private ImageButton mButtonS;
	private ImageButton mButtonFR;
	private ImageButton mButtonBR;
	private ImageButton mButtonFL;
	private ImageButton mButtonBL;
	private ImageButton mButton_A;
	private ImageButton mButton_B;
	private ImageButton mButton_C;
	private ImageButton mButton_D;

	private ImageButton Gsensor;
	private ImageButton Balence;

	private ImageButton Bluetooth;
	private ImageButton Connect;
	private ImageButton Disconnect;
	public Button remoteConnect;
	//public Button remoteDisonnect;

	private String mDeviceName;

	public static final String DeviceName = null;
	public static final String TOAST = null;

	private float XSpan;
	private float YSpan;
	private float Xinit;
	private float Yinit;

	private BluetoothAdapter mBluetoothAdapter = null;
	public BluetoothService mBluetoothService = null;

	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private float Value_X;
	private float Value_Y;
	// private float Value_Z;
	private float X_Init;
	private float Y_Init;

	byte y;
	byte cd;
	boolean flag, flag1;

	public static final int Message = 1;
	public static final int UImsg = 2;
	public static final int State_Change = 3;
	public static final int Device_Name = 4;

	private static final int Connect_Device = 1;

	private static final int Enable_Bluetooth = 2;

	public String theUrlToGet = "http://www.otherrealm.org/direction.html";
	public Byte results;
	private Timer myTimer;

	WakeLock wakeLock;

	// Main method where everything is initiated
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// these two lines make it so that the phones screen stays on
		PowerManager manager = ((PowerManager) getSystemService(POWER_SERVICE));
		wakeLock = manager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				"My Tag");
		// these two lines get the accelerometer and assign it a var to be used
		// later
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		// finds the Bluetooth adapter is available and if is not, sends a
		// message
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.bt_not_available, Toast.LENGTH_LONG)
					.show();
			return;
		}
		// if Bluetooth is turned on, set the image to one thing, else set it to
		// something else
		Bluetooth = (ImageButton) findViewById(R.id.IM_Bluetooth);
		if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
			Bluetooth.setImageResource(R.drawable.bt_off);
		} else {
			Bluetooth.setImageResource(R.drawable.bt_on);
		}
		// if the Bluetooth icon is clicked and Bluetooth is not on, set a new
		// intent and turn it on,
		// else, if it is already on, disable it
		Bluetooth.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
					if (!mBluetoothAdapter.isEnabled()) {
						Intent btintent = new Intent(
								BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(btintent, Enable_Bluetooth);
						if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
							Bluetooth.setImageResource(R.drawable.bt_off);
						} else {
							Bluetooth.setImageResource(R.drawable.bt_on);
						}
					}
				} else {
					mBluetoothAdapter.disable();
					Bluetooth.setImageResource(R.drawable.bt_off);
				}

			}
		});
		// update the remote bytecode every .1 seconds
		myTimer = new Timer();
		myTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				TimerMethod();
			}

		}, 0, 100);
	}

	private void TimerMethod() {
		// This method is called directly by the timer and runs in the same
		// thread as the timer.
		// Call the method that will work with the UI through the runOnUiThread
		// method.
		this.runOnUiThread(Timer_Tick);
	}

	private Runnable Timer_Tick = new Runnable() {
		public void run() {
			// This method runs in the same thread as the UI.
			// It calls the DownloadWebPageTask class and gets the returned
			// content of
			// http://www.otherrealm.org/direction.html (byte that is passed to
			// the Bluetooth)
			if (remote == 0) {
				// if remote is turned off, do nothing
			} else {
				DownloadWebPageTask task = new DownloadWebPageTask();
				task.execute(new String[] { theUrlToGet });
			}
		}
	};

	// This method is the first thing gets called when the app starts. It
	// determines if
	// the Bluetooth adapter is enabled and if not, initiates an intent to do
	// such
	@Override
	public void onStart() {
		super.onStart();
		if (!mBluetoothAdapter.isEnabled()) {
			Intent btintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(btintent, Enable_Bluetooth);
		} else if (mBluetoothService == null) {
			control();
		}
	}

	// if the app has lost focus and then regains it, this method reinitiates
	// the inability
	// for sleeping and makes sure the Bluetooth is still turned on
	@Override
	public synchronized void onResume() {
		super.onResume();
		wakeLock.acquire();
		if (mBluetoothService != null) {
			if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
				mBluetoothService.start();
			}
		}
	}

	// if the app loses focus; this method turns of the inability to sleep and
	// makes sure
	// the Car is no longer moving
	@Override
	protected void onPause() {
		super.onPause();
		wakeLock.release();
		unregist();
		if (mBluetoothAdapter.isEnabled()) {
			sendMessage((byte) 0x00);
		}
	}

	// the app has lost the appeal of the user, turn it of… @Override
	public void onStop() {
		super.onStop();
	}

	// Perform any final cleanup before an activity is destroyed. This can
	// happen either
	// because the activity is finishing (someone called finish() on it, or
	// because the
	// system is temporarily destroying this instance of the activity to save
	// space.
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mBluetoothService != null)
			mBluetoothService.stop();
	}

	// This is the main method used for getting user input regarding the
	// direction the car should move. Consists mainly of onTouch calls.
	private void control() {
		Bluetooth.setImageResource(R.drawable.bt_on);
		// This method happens before any buttons get pressed so as to set up
		// the text values that
		// show on the screen
		init();
		mButtonF = (ImageButton) findViewById(R.id.forward);
		mButtonF.setOnTouchListener(new Button.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					sendMessage((byte) 0x1f);
					mButtonF.setImageResource(R.drawable.forward1);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					sendMessage((byte) 0x00);
					mButtonF.setImageResource(R.drawable.forward);
				}
				return false;
			}
		});
		mButtonB = (ImageButton) findViewById(R.id.back);
		mButtonB.setOnTouchListener(new Button.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					sendMessage((byte) 0x2f);
					mButtonB.setImageResource(R.drawable.back2);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					sendMessage((byte) 0x00);
					mButtonB.setImageResource(R.drawable.back1);
				}
				return false;
			}
		});
		mButtonL = (ImageButton) findViewById(R.id.left);
		mButtonL.setOnTouchListener(new Button.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					sendMessage((byte) 0x3f);
					mButtonL.setImageResource(R.drawable.left1);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					sendMessage((byte) 0x00);
					mButtonL.setImageResource(R.drawable.left);
				}
				return false;
			}
		});
		mButtonR = (ImageButton) findViewById(R.id.right);
		mButtonR.setOnTouchListener(new Button.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					sendMessage((byte) 0x4f);
					mButtonR.setImageResource(R.drawable.right1);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					sendMessage((byte) 0x00);
					mButtonR.setImageResource(R.drawable.right);
				}
				return false;
			}
		});
		mButtonFR = (ImageButton) findViewById(R.id.right_F);
		mButtonFR.setOnTouchListener(new Button.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					sendMessage((byte) 0x6f);
					mButtonFR.setImageResource(R.drawable.lf);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					sendMessage((byte) 0x00);
					mButtonFR.setImageResource(R.drawable.rf);
				}
				return false;
			}
		});
		mButtonFL = (ImageButton) findViewById(R.id.left_F);
		mButtonFL.setOnTouchListener(new Button.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					sendMessage((byte) 0x5f);
					mButtonFL.setImageResource(R.drawable.lf);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					sendMessage((byte) 0x00);
					mButtonFL.setImageResource(R.drawable.rf);
				}
				return false;
			}
		});
		mButtonBR = (ImageButton) findViewById(R.id.right_B);
		mButtonBR.setOnTouchListener(new Button.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					sendMessage((byte) 0x8f);
					mButtonBR.setImageResource(R.drawable.lf);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					sendMessage((byte) 0x00);
					mButtonBR.setImageResource(R.drawable.rf);
				}
				return false;
			}
		});
		mButtonBL = (ImageButton) findViewById(R.id.left_B);
		mButtonBL.setOnTouchListener(new Button.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					sendMessage((byte) 0x7f);
					mButtonBL.setImageResource(R.drawable.lf);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					sendMessage((byte) 0x00);
					mButtonBL.setImageResource(R.drawable.rf);
				}
				return false;
			}
		});
		mButtonS = (ImageButton) findViewById(R.id.middle);
		mButtonS.setOnTouchListener(new Button.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {

				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					Xinit = event.getX();
					Yinit = event.getY();
					// flag = false;
					mButtonS.setImageResource(R.drawable.middle1);
				}
				XSpan = event.getX() - Xinit;
				YSpan = event.getY() - Yinit;
				if (event.getAction() == MotionEvent.ACTION_UP) {
					XSpan = 0;
					YSpan = 0;
					// flag = true;
					mButtonS.setImageResource(R.drawable.middle);
				}
				Caculate(XSpan, YSpan);
				return false;
			}
		});
		//Connect or disconnect from the remote control
		remoteConnect = (Button) findViewById(R.id.Remote_Connect);
		remoteConnect.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String curText = remoteConnect.getText()+"";     
			    if(curText.equals("Remote is Off")){
			    	remoteConnect.setText("Remote is On");
			    }if(curText.equals("Remote is On")){
			    	remoteConnect.setText("Remote is Off");
			    }if (remote == 0) {
					remote = 1;
			    }else{
					remote = 0;
			    }
			}
		});
		//initiates the connection to the Bluetooth of the Car.
		Connect = (ImageButton) findViewById(R.id.IM_Connect);
		Connect.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!mBluetoothAdapter.isEnabled()) {
					Intent btintent = new Intent(
							BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(btintent, Enable_Bluetooth);
				}
				Intent serverIntent = new Intent(Car.this, DeviceList.class);
				startActivityForResult(serverIntent, Connect_Device);
			}
		});
		// stops the connection to the Bluetooth of the Car.
		Disconnect = (ImageButton) findViewById(R.id.IM_Disconnect);
		Disconnect.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mBluetoothAdapter.isEnabled()) {
					mBluetoothService.stop();
				}
			}
		});
		/*
		 * Balence = (ImageButton) findViewById(R.id.imageBalence);
		 * Balence.setOnClickListener(new OnClickListener() { public void
		 * onClick(View v) { Set_Neutral(); } });
		 */
		flag1 = true;
		Gsensor = (ImageButton) findViewById(R.id.imageGsensor);
		Gsensor.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
					return;
				}
				if (flag1) {
					Gsensor.setImageResource(R.drawable.gravity2);
					regist();
					flag = true;
					flag1 = false;
				} else {
					unregist();
					sendMessage((byte) 0x00);
					Gsensor.setImageResource(R.drawable.gravity1);
					Direction.setText("x");
					Speed.setText("0");
					flag1 = true;
				}

			}
		});
		// initiate the BluetoothService class
		mBluetoothService = new BluetoothService(this, mHandler);
	}

	// send the command from either a button or the server to the
	// BluetoothService class
	public void sendMessage(byte command) {
		String logState = Byte.toString(command) + " | " + mBluetoothService.getState();
		Log.v("aResult", logState);
		if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
			return;
		}
		mBluetoothService.write(command);
	}

	// This class calls whatever is on
	// http://www.otherrealm.org/direction.html , converts it from a
	// String to a byte and then sends it to sendMessage which passes it
	// to the BluetoothService class that handles the transfer of the car
	private class DownloadWebPageTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			// String that becomes whatever the contents of the url is
			String response = "";
			for (String url : urls) {
				// these lines are where the actual connection is made to the
				// remote
				// server and the content is passed to the string
				DefaultHttpClient client = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(url);
				try {
					HttpResponse execute = client.execute(httpGet);
					InputStream content = execute.getEntity().getContent();
					BufferedReader buffer = new BufferedReader(
							new InputStreamReader(content));
					String s = "";
					while ((s = buffer.readLine()) != null) {
						response += s;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// Whatever the content that was added to response String gets
			// returned
			// to be passed to the onPostExecute
			return response;
		}

		// once the contents of the http request are complete, this method is
		// called to do something with it
		@Override
		protected void onPostExecute(String result) {
			// send whatever the resulting content of the url to logcat for
			// diagnostic purposes.
			Log.v("aResult", result);
			try {
				// try to convert the string to a byte to be send to the method
				// that handles the Bluetooth transfer.
				results = Byte.decode(result);
				sendMessage((byte) results);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// you can call this method to manually get the remote content, used it
	// during debugging
	private int remote = 0;

	public void forward() {
		if (remote == 0) {
			// if remote is turned off, do nothing
		} else {
			DownloadWebPageTask task = new DownloadWebPageTask();
			task.execute(new String[] { theUrlToGet });
		}
	}

	// set x and y to the value of the sensor
	void Set_Neutral() {
		X_Init = Value_X;
		Y_Init = Value_Y;
	}

	// This method happens before any buttons get pressed so as to set up the
	// text values that
	// show on the screen
	void init() {
		device = (TextView) findViewById(R.id.device);

		Direction = (TextView) findViewById(R.id.Direction);
		Speed = (TextView) findViewById(R.id.Speed);

		Direction.setText("x");
		Speed.setText("0");
	}

	void Caculate(float Value_A, float Value_B) {
		float Gradient;
		int Radius;

		if (Value_A != 0) {
			Gradient = Value_B / Value_A;
		} else
			Gradient = 0;
		Radius = (int) Math.sqrt(Value_A * Value_A + Value_B * Value_B);

		if (Radius > 5) {
			if (Radius <= 64) {
				Speed.setText(String.valueOf(Radius / 4));
				y = (byte) (0x0000000f & (Radius / 4));
			} else if (Radius > 64) {
				Speed.setText("16(Max)");
				y = (byte) 0x0f;
			}
			if (Gradient < -0.25 && Gradient > -4) {
				if (Value_A > 0) {
					Direction.setText("¨J");
					y = (byte) (0x60 | y);
				} else {
					Direction.setText("¨L");
					y = (byte) (0x70 | y);
				}
			} else if (Gradient > 0.25 && Gradient < 4) {
				if (Value_A > 0) {
					Direction.setText("¨K");
					y = (byte) (0x80 | y);
				} else {
					Direction.setText("¨I");
					y = (byte) (0x50 | y);
				}
			} else if (Gradient >= -0.25 && Gradient <= 0.25) {
				if (Value_A > 0) {
					Direction.setText("¡ú");
					y = (byte) (0x40 | y);
				} else {
					Direction.setText("¡û");
					y = (byte) (0x30 | y);
				}
			} else if (Gradient <= -4 || Gradient >= 4) {
				if (Value_B > 0) {
					Direction.setText("¡ý");
					y = (byte) (0x20 | y);
				} else {
					Direction.setText("¡ü");
					y = (byte) (0x10 | y);
				}
			}
		}

		else {
			y = (byte) 0x00;
			Direction.setText("x");
			Speed.setText("0");
		}

		if (y != cd) {
			sendMessage(y);
			cd = y;
		}
	}

	// this method gets the tilt (a.k.a acceleration) of the phone and passes it
	// to the Caculate1 method
	// to convert the raw tilt data into usable values for the app
	SensorEventListener mSensorEevntLer = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
				return;

			Value_X = event.values[0];
			Value_Y = event.values[1];
			// Value_Z = event.values [2];

			Caculate1(Value_X, Value_Y);
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};

	// register a sensor (SensorEventListener) for receiving notifications from
	// the SensorManager when the accelerometer has changed
	private void regist() {
		mSensorManager.registerListener(mSensorEevntLer, mAccelerometer,
				SensorManager.SENSOR_DELAY_GAME);

	}

	// unregister the sensor (SensorEventListener) so that notifications are no
	// longer received from
	// the SensorManager
	private void unregist() {
		mSensorManager.unregisterListener(mSensorEevntLer);
	}

	// this method calculates the tilt of the accelerometer and returns a more
	// useful
	// number that can be sent to the Car
	void Caculate1(float Value_A, float Value_B) {
		float Gradient;
		int Radius;
		int n = 20;
		if (flag) {
			Set_Neutral();
			flag = false;
		}
		Value_A = Value_A - X_Init;
		Value_B = Value_B - Y_Init;
		Value_A = Value_A * n;
		Value_B = Value_B * n;

		if (Value_B != 0) {
			Gradient = Value_A / Value_B;
		} else
			Gradient = 0;
		Radius = (int) Math.sqrt(Value_A * Value_A + Value_B * Value_B);

		if (Radius > 17) {
			if (Radius <= 64) {
				Speed.setText(String.valueOf(Radius / 4));
				y = (byte) (0x0000000f & (Radius / 4));
			} else if (Radius > 64) {
				Speed.setText("16(MAX)");
				y = (byte) 0x0f;
			}

			if (Gradient < -0.33 && Gradient > -2.5) {
				if (Value_B > 0) {
					Direction.setText("¨J");
					y = (byte) (0x60 | y);
				} else {
					Direction.setText("¨L");
					y = (byte) (0x70 | y);
				}
			} else if (Gradient > 0.33 && Gradient < 2.5) {
				if (Value_B > 0) {
					Direction.setText("¨K");
					y = (byte) (0x80 | y);
				} else {
					Direction.setText("¨I");
					y = (byte) (0x50 | y);
				}
			} else if (Gradient >= -0.33 && Gradient <= 0.33) {
				if (Value_B > 0) {
					Direction.setText("¡ú");
					y = (byte) (0x40 | y);
				} else {
					Direction.setText("¡û");
					y = (byte) (0x30 | y);
				}
			} else if (Gradient <= -2.5 || Gradient >= 2.5) {
				if (Value_A > 0) {
					Direction.setText("¡ý");
					y = (byte) (0x20 | y);
				} else {
					Direction.setText("¡ü");
					y = (byte) (0x10 | y);
				}
			}
		} else {
			y = (byte) 0x00;
			Direction.setText("x");
			Speed.setText("0");
		}
		if (y != cd) {
			sendMessage(y);
			cd = y;
		}
	}

	// when an activity gets done figure out which activity it was that got
	// done,
	// and carryout another method.
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case Connect_Device:
			// called in the control method via "startActivityForResult"
			if (resultCode == Activity.RESULT_OK) {
				ConnectDevice(data);
			}
			break;

		case Enable_Bluetooth:
			// called in the onStart method
			if (resultCode == Activity.RESULT_OK) {
				control();
			} else {
				Toast.makeText(this, R.string.bt_not_enable, Toast.LENGTH_SHORT)
						.show();
				return;
			}

		}
	}

	// This is used in conjunction with the BluetoothService Class to 'handle'
	// the various
	// messages that might arise by the communication with the Car via Bluetooth
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case State_Change:
				switch (msg.arg1) {
				case BluetoothService.STATE_CONNECTED:
					device.setText(R.string.connected_to);
					device.append(mDeviceName);
					break;
				case BluetoothService.STATE_CONNECTING:
					device.setText(R.string.connecting);
					break;
				case BluetoothService.STATE_LISTEN:
				case BluetoothService.STATE_NONE:
					device.setText(R.string.not_connected);
					break;
				}
				break;
			case Message:
				byte[] readBuf = (byte[]) msg.obj;
				String readMessage = new String(readBuf, 0, msg.arg1);
				break;
			case Device_Name:
				mDeviceName = msg.getData().getString(DeviceName);
				Toast.makeText(getApplicationContext(),
						"The name of the device is: " + mDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case UImsg:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	// Get a BluetoothDevice object for the given Bluetooth hardware address and
	// pass it to the connect method of the BluetoothService Class
	private void ConnectDevice(Intent data) {
		String address = data.getExtras().getString(
				DeviceList.EXTRA_DEVICE_ADDRESS);
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		mBluetoothService.connect(device);
	}

	// create a menu that will contain some help content
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	// call the 'Help.java' class when it is selected from the menu
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.help:
			serverIntent = new Intent(this, Help.class);
			startActivity(serverIntent);
			return true;
		}
		return false;
	}

	// ask if you what to exit if you hit the back button
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			new AlertDialog.Builder(this)
					.setMessage(R.string.isout)
					.setNegativeButton(R.string.no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
								}
							})
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									Intent intent = new Intent();
									intent.setClass(Car.this, Car.class);
									stopService(intent);
									finish();
								}
							}).show();

			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}
}