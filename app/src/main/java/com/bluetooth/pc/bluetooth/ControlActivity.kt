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
import android.hardware.Camera
import android.net.Uri
import android.os.Environment
import android.provider.Contacts
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
import android.util.Log
import com.bluetooth.pc.bluetooth.R
import kotlinx.android.synthetic.main.control_layout.*
import android.system.Os.socket
import android.widget.FrameLayout
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.doAsync
import java.io.*
import java.nio.file.Files.exists
import java.text.SimpleDateFormat

class ControlActivity: AppCompatActivity() {

    companion object {
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var m_isConnected: Boolean = false
        lateinit var m_address: String
    }

    // Camera setup
    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null

    private val TAG: String = "CameraV2"

    private val mPicture = Camera.PictureCallback { data, _ ->
        val pictureFile: File = getOutputMediaFile(MEDIA_TYPE_IMAGE) ?: run {
            Log.d(TAG, ("Error creating media file, check storage permissions"))
            return@PictureCallback
        }

        try {
            val fos = FileOutputStream(pictureFile)
            fos.write(data)
            fos.close()
        } catch (e: FileNotFoundException) {
            Log.d(TAG, "File not found: ${e.message}")
        } catch (e: IOException) {
            Log.d(TAG, "Error accessing file: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.control_layout)
        // m_address = intent.getStringExtra(SelectDeviceActivity.EXTRA_ADDRESS)
        // ConnectToDevice(this).execute()


        // Control Buttons
        control_led_on.setOnClickListener { sendCommand("a") }
        control_led_off.setOnClickListener { readDataFromBT() }
        /// control_led_disconnect.setOnClickListener { disconnect() }

        // Camera
        mCamera = CameraPreview.getCameraInstance()
        mPreview = mCamera?.let {
            // Create our Preview view
            CameraPreview(this, it)
        }

        // Set the Preview view as the content of our activity.
        mPreview?.also {
            val preview: FrameLayout = findViewById(R.id.camera_preview)
            preview.addView(it)
        }


        // This button will be used
        control_led_disconnect.setOnClickListener { mCamera?.takePicture(null, null, mPicture)}

    }

    private fun getOutputMediaFileUri(type: Int): Uri {
        return Uri.fromFile(getOutputMediaFile(type))
    }
    private fun getOutputMediaFile(type: Int): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        val mediaStorageDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "MyCameraApp"
        )

        Log.d(TAG, mediaStorageDir.absolutePath)
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        mediaStorageDir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    Log.d(TAG, "failed to create directory")
                    return null
                }
            }
        }

        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return when (type) {
            MEDIA_TYPE_IMAGE -> {
                File("${mediaStorageDir.path}${File.separator}IMG_$timeStamp.jpg")
            }
            else -> null
        }
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
        doAsync {
            var buffer = ByteArray(256)
            var bytes: Int

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

    override fun onPause() {
        super.onPause()
        releaseCamera()
    }

    private fun releaseCamera() {
        mCamera?.release() // release the camera for other applications
        mCamera = null
    }


}