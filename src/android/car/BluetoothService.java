package android.car;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothService {
	// Debug
	private static final String TAG = "BTService";

	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private final BluetoothAdapter mAdapter;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;

	private final Handler mHandler;
	private int mState;
	public static final int STATE_NONE = 1;
	public static final int STATE_LISTEN = 2;
	public static final int STATE_CONNECTING = 3;
	public static final int STATE_CONNECTED = 4;
	//Main method where everything is initiated 
	public BluetoothService(Context context, Handler handler) {
		//Get a handle to the default local Bluetooth adapter. 
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;

	}
	//set the status of the bluetooth adapter
	private synchronized void setState(int state) {
		mState = state;
		mHandler.obtainMessage(Car.State_Change, state, -1).sendToTarget();
	}
	//get the status of the bluetooth adapter
	public synchronized int getState() {
		return mState;
	}
	
	public synchronized void start() {
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		setState(STATE_LISTEN);
	}
	public synchronized void connect(BluetoothDevice device) {
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
		Message msg = mHandler.obtainMessage(Car.Device_Name);
		Bundle bundle = new Bundle();
		bundle.putString(Car.DeviceName, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
	}
	public synchronized void stop() {
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		setState(STATE_NONE);
	}
	public void write(byte out) {
		ConnectedThread Thread;
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			Thread = mConnectedThread;
		}
		Thread.write(out);
	}
	//create a separate thread to handle the connection of the Bluetooth socket to the Car
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;
			try {
				//Create an RFCOMM BluetoothSocket ready to start a secure outgoing connection to the Car using service discovery protocol lookup of uuid. 
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				//catch the exception and write it to logcat
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}
		public void run() {
			setName("ConnectThread");
			//stop discovery because we are now going to connect and discovery is a heavyweight procedure 
			mAdapter.cancelDiscovery();
			try {
				//Attempt to connect to the car
				mmSocket.connect();
			} catch (IOException e) {
				//send a message back to the main Car.java class if connection has failed 
				connectionFailed();
				try {
					//Immediately close this socket, and release all associated resources
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close()socket during connection failure", e2);
				}
				BluetoothService.this.start();
				return;
			}
			synchronized (BluetoothService.this) {
				mConnectThread = null;
			}
			connected(mmSocket, mmDevice);
		}
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
	//send a message back to the main Car.java class if connection has failed 
	private void connectionFailed() {
		setState(STATE_NONE);
		Message msg = mHandler.obtainMessage(Car.UImsg);
		Bundle bundle = new Bundle();
		bundle.putString(Car.TOAST, "Unable to connect");
		msg.setData(bundle);
		mHandler.sendMessage(msg);

	}
	private void connectionLost() {
		setState(STATE_NONE);
		Message msg = mHandler.obtainMessage(Car.UImsg);
		Bundle bundle = new Bundle();
		bundle.putString(Car.TOAST, "No longer connected");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
		BluetoothService.this.start();
	}
	//once the connection is made; this class handles the actual sending and receiving of data
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "create ConnectedThread: ");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}
			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}
		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[1024];
			int bytes;

			while (true) {
				try {
					bytes = mmInStream.read(buffer);
					mHandler.obtainMessage(Car.Message, bytes, -1, buffer).sendToTarget();
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					connectionLost();
					break;
				}
			}
		}
		public void write(byte buffer) {
			try {
				mmOutStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

}
