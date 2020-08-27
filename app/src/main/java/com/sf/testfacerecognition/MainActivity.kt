package com.sf.testfacerecognition

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var uriData: Uri? = null
    private var isLoading = false
    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getImage()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 808 && data != null) {
            uriData = data.data
            bitmap = uriData?.let { getBitmapFromUri(uriData!!) }
            imagePerson.setImageBitmap(bitmap)
            textImageName.text = getFileName(uriData!!)
        }
    }

    private fun getBitmapFromUri(uri: Uri) : Bitmap {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        } else {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        }
    }
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor =
                contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor!!.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }


    private fun getImage() {
        btnGetImage.setOnClickListener {
            getPermission()
        }
        btnStartRecognition.setOnClickListener {
            bitmap?.let { data -> initRecognition(data) }
            loadingState(isLoading)
        }
    }

    private fun getPermission() {
        val permissionAll = 1
        val permission = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (!hasPermission(this, permission)) {
            ActivityCompat.requestPermissions(this, permission, permissionAll)
        } else {
            intentToGetImage()
        }
    }


    private fun hasPermission(context: Context, permission: Array<String>): Boolean {
        return permission.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permission granted
                intentToGetImage()
            }
        }
    }

    private fun intentToGetImage() {
        val intent = Intent().apply {
            type = "*/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(Intent.createChooser(intent, "Select File"), 808)
    }

    private fun initRecognition(bitmap: Bitmap) {
        isLoading = true
        val highAccuracyOpts = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .build()

        val detector = FirebaseVision.getInstance()
            .getVisionFaceDetector(highAccuracyOpts)

        val scaleBitmap = Bitmap.createScaledBitmap(bitmap, 480, 480, true)
        val mBitmap = scaleBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mBitmap)
        val paint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 4F
        }

        // Memulai proses recognition dari gambar dan mencari gambar wajah
        val image = FirebaseVisionImage.fromBitmap(mBitmap)
        detector.detectInImage(image)
            .addOnSuccessListener { faces ->
                isLoading = false
                loadingState(isLoading)
                Log.i("ASDF", faces.toString())
                if (faces.isEmpty()) {
                    textStatus.text = ": GAGAL"
                } else {
                    textStatus.text = ": BERHASIL"
                    for (face in faces) {
                        val bound = face.boundingBox
                        canvas.drawRect(bound, paint)
                    }
                    imagePerson.setImageBitmap(mBitmap)
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                loadingState(isLoading)
                e.printStackTrace()
                Toast.makeText(this, "FAILURE", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadingState(procces: Boolean) {
        if (procces) {
            loadingBar.visibility = View.VISIBLE
        } else {
            loadingBar.visibility = View.GONE
        }
    }
}