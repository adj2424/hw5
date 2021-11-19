package com.example.hw5

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storageMetadata
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.jar.Manifest

private const val CODE = 111
private const val PERMISSION_ID = 123
private const val FILE_NAME = "temp"
private lateinit var photoFile: File
class MainActivity : AppCompatActivity(), OnItemListener {

    private var recyclerView: RecyclerView?= null
    private var addPhoto: FloatingActionButton?= null

    private var testImage: ImageView?= null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var model: SharedViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        recyclerView = findViewById(R.id.recyclerView)
        addPhoto = findViewById(R.id.floatingActionButton)


        var locationItemList = mutableListOf(
            LocationItem("address", "coordinates", "time", 0),
            LocationItem("address", "coordinates", "time", 0),
            LocationItem("address", "coordinates", "time", 0),
            LocationItem("address", "coordinates", "time", 0),
            LocationItem("address", "coordinates", "time", 0)
        )
        val adapter = Adapter(locationItemList, this)
        recyclerView?.adapter = adapter
        recyclerView?.layoutManager = LinearLayoutManager(this)
        var itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        itemDecoration.setDrawable(getDrawable(R.drawable.custom_divider)!!)
        recyclerView?.addItemDecoration(itemDecoration)
        adapter.notifyDataSetChanged()

        addPhoto?.setOnClickListener{
            val takePicIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            photoFile = getPhotoFile(FILE_NAME)
            val fileProvider = FileProvider.getUriForFile(this, "com.example.fileprovider", photoFile)
            takePicIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
            startActivityForResult(takePicIntent, CODE)

        }
    }
    private fun getPhotoFile(fileName: String): File {
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDir)
    }

    private fun getLocation() {
        if(checkPermission()) {
            if (checkLocationService()) {
                fusedLocationProviderClient.lastLocation.addOnCompleteListener{
                    var location = it.result
                    if (location == null) { // create new last location
                        getNewLocation()
                    }
                    else {
                        Log.d("Test", location.latitude.toString() + location.longitude.toString())
                    }
                }
            }
            else {
                Log.d("test", "you must enable location")
            }
        }
        else {
            requestPermission()
        }
    }
    @SuppressLint("MissingPermission")
    private fun getNewLocation() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates = 2
        val locationCallback = object :  LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                var location = locationResult.lastLocation
                Log.d("Test", location.latitude.toString() + location.longitude.toString())
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.myLooper()
        )
    }



    private fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERMISSION_ID
        )
    }

    private fun checkLocationService(): Boolean {
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("test", "you have permissions pogu")
            }
        }
        else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CODE && resultCode == Activity.RESULT_OK) {
            //var img = data?.extras?.get("data") as Bitmap

            val storageRef: StorageReference = FirebaseStorage.getInstance().reference
            val file: Uri = Uri.fromFile(File(photoFile.absolutePath))

            val current = LocalDateTime.now()

            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            val formatted = current.format(formatter)

            val metadata = storageMetadata{
                contentType = "image/jpg"
                setCustomMetadata("timeUpload", formatted)
                getLocation()

            }
            val uploadTask = storageRef.child("images/" + file.lastPathSegment).putFile(file, metadata)
            uploadTask.addOnFailureListener {
                // Handle unsuccessful uploads
            }.addOnSuccessListener {
                Log.d("test", "upload successful")
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onItemClick(item: LocationItem, position: Int) {
        Log.d("test", "clicked")
    }
}