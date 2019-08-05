package com.oak.obd2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import static android.content.ContentValues.TAG;


public class Bluetooth {
    protected BluetoothAdapter mBluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
    private ConnectThread mConnectThread = null;
    private AcceptThread mAcceptThread = null;
    private WorkerThread mWorkerThread = null;
    private BluetoothDevice mOBDDevice = null;
    private BluetoothSocket mSocket = null;
    private String uuid;

    Bluetooth() {
        mBluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices;


        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
            return;

        pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                //TODO: check whether this is OBD and whether it is connected
                //by sending a command and check response
                if (deviceName.contains("OBD")) {
                    mOBDDevice = device;
                    uuid = device.getUuids()[0].toString();
                    break;
                }
            }
        }
        if (mOBDDevice == null) {
            return;
        }
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a session
     * in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void connect()
    {
        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            mSocket = mOBDDevice.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }
    }

    /**
     * start connection to specified device
     *
     * @param device The device to connect
     */
    public synchronized void connect(Object device)
    {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null)
        {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mWorkerThread != null)
        {
            mWorkerThread.cancel();
            mWorkerThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread((BluetoothDevice)device);
        mConnectThread.run();
    }

    /**
     * Start the BtWorkerThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     */
    public synchronized void startWorkerThread(BluetoothSocket socket)
    {
        Log.d(TAG, "Start worker thread");

        // Start the thread to manage the connection and perform transmissions
        mWorkerThread = new WorkerThread(socket);
        mWorkerThread.run();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop()
    {
        Log.d(TAG, "stop");

        if (mConnectThread != null)
        {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mWorkerThread != null)
        {
            mWorkerThread.cancel();
            mWorkerThread = null;
        }

        if (mAcceptThread != null)
        {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
    }

    /**
     * Write to the BtWorkerThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see BtWorkerThread#write(byte[])
     **/
    public synchronized void write(byte[] out)
    {
        // Perform the write unsynchronized
        mWorkerThread.write(out);
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("Server", UUID.fromString("42819fbd-bcd7-4393-96f4-5a9969c4bfe9"));
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    //manageMyConnectedSocket(socket);
                    //mServerSocket.close();
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mDevice = device;

            try {
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("42819fbd-bcd7-4393-96f4-5a9969c4bfe9"));
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();
            try {
                mSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                Log.e(TAG, "Failed to connect to device");
                try {
                    mSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            startWorkerThread(mSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device. It handles all
     * incoming and outgoing transmissions.
     */
    private class WorkerThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public WorkerThread(BluetoothSocket socket)
        {
            Log.d(TAG, "create BtWorkerThread: ");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e)
            {
                Log.d(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        /**
         * run the main communication loop
         */
        public void run()
        {
            Log.d(TAG, "BEGIN mBtWorkerThread");
            try
            {
                // run the communication thread
                //ser.run();
            } catch (Exception ex)
            {
                // Intentionally ignore
                Log.d(TAG,"Comm thread aborted", ex);
            }
            //connectionLost();
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer)
        {
            //ser.writeTelegram(new String(buffer).toCharArray());
        }

        public void cancel()
        {
            try
            {
                mmSocket.close();
            } catch (IOException e)
            {
                Log.d(TAG, "close() of connect socket failed", e);
            }
        }

    }

}
