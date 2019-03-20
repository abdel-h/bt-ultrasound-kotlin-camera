package com.bluetooth.pc.bluetooth

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import java.util.*
import android.bluetooth.BluetoothSocket
import android.provider.Contacts
import android.util.Log
import com.bluetooth.pc.bluetooth.R
import kotlinx.android.synthetic.main.control_layout.*
import java.io.IOException
import android.system.Os.socket
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.doAsync
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream

class ControlActivity: AppCompatActivity() {

    companion object {
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var m_isConnected: Boolean = false
        lateinit var m_address: String
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.control_layout)
        m_address = intent.getStringExtra(SelectDeviceActivity.EXTRA_ADDRESS)

        try {
            ConnectToDevice(this).execute()
        }catch (e: Exception) {
            Log.d("Exception", "Error")
        }


        control_led_on.setOnClickListener { sendCommand("a") }
        control_led_off.setOnClickListener {
            doAsync {
                readDataFromBT()
            }
        }
        control_led_disconnect.setOnClickListener { disconnect() }



    }


    private fun sendCommand(input: String) {
        if (m_bluetoothSocket != null) {
            try{
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch(e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun disconnect() {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.close()
                m_bluetoothSocket = null
                m_isConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        finish()
    }

    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>() {
        private var connectSuccess: Boolean = true
        private val context: Context

        init {
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context, "Connecting...", "please wait")
        }

        override fun doInBackground(vararg p0: Void?): String? {
            try {
                if (m_bluetoothSocket == null || !m_isConnected) {
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    m_bluetoothSocket!!.connect()
                }
            } catch (e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectSuccess) {
                Log.i("BT_Socket", "couldn't connect")
            } else {
                m_isConnected = true

            }
            m_progress.dismiss()
        }



    }
    fun readDataFromBT( ) {
        var buffer = ByteArray(256)
        var bytes: Int

        // m_bluetoothSocket!!.close()

        var tmpIn = m_bluetoothSocket!!.getInputStream()

            if(m_bluetoothSocket == null) {
                Log.d("BT_Socket", "Bt Socket null")
            }
        while(m_bluetoothSocket != null) {
            val mmInStream = DataInputStream(tmpIn)
            // here you can use the Input Stream to take the string from the client  whoever is connecting
            //similarly use the output stream to send the data to the client

            // Read from the InputStream
            bytes = mmInStream.read(buffer)
            val readMessage = String(buffer, 0, bytes)
            // Send the obtained bytes to the UI Activity
            Log.d("BT_Socket", readMessage)
        }

    }


}