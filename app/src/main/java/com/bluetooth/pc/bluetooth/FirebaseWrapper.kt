package com.bluetooth.pc.bluetooth

import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.File
import java.io.FileInputStream
import java.net.URI


class FirebaseWrapper {
    lateinit var storage: FirebaseStorage
    lateinit var storageRef: StorageReference
    lateinit var captured: StorageReference

    val TAG: String = "CameraV3"
    init {
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference
        captured = storageRef.child("captured")
        Log.d(TAG, "fire Storage ready !")
    }


    fun uploadFileByPath(path: String, callback : (String) -> Unit) {
        Log.d(TAG, "uploading $path")
        val file = Uri.fromFile(File(path))
        val fileRef = storageRef.child("captured/${file.lastPathSegment}")

        var uploadTask = fileRef.putFile(file)




        val urlTask = uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }

            return@Continuation fileRef.downloadUrl
        }).addOnCompleteListener {task ->
            if(task.isSuccessful) {
                val downloadUrl = task.result
                // callback
                Log.d("CameraV3", downloadUrl.path)
                Log.d("CameraV3", "Upload Success")
                callback(downloadUrl.path)
            } else {
                Log.d(TAG, "Uploading failed")
            }
        }

    }
}