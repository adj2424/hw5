package com.example.hw5

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storageMetadata
import com.google.gson.Gson
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import java.io.File
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

private const val CODE = 111
private const val FILE_NAME = "temp"
private lateinit var photoFile: File

@ExperimentalStdlibApi
class MainActivity : AppCompatActivity(), OnItemListener, EasyPermissions.PermissionCallbacks {

    private var recyclerView: RecyclerView?= null
    private var addPhoto: FloatingActionButton?= null
    private var lat = 0.0
    private var long = 0.0
    private var latlong = ""
    private var address = ""
    private var locationItemList = mutableListOf<LocationItem>()
    private val myAdapter = Adapter(locationItemList, this)
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        addPhoto = findViewById(R.id.floatingActionButton)
        recyclerView = findViewById(R.id.recyclerView)

        val storageRef = FirebaseStorage.getInstance().reference.child("images")
        locationItemList.clear()
        val listAllTask: Task<ListResult> = storageRef.listAll()
        listAllTask.addOnCompleteListener { result ->
            val items: List<StorageReference> = result.result!!.items
            items.forEachIndexed { index, item ->
                item.downloadUrl.addOnSuccessListener { url ->
                    Log.d("Test", "$url")
                    item.metadata.addOnSuccessListener { metaData ->
                        val timeUpload = metaData.getCustomMetadata("timeUpload")
                        val strlatlong = metaData.getCustomMetadata("latlong")
                        val templat = strlatlong!!.substringBefore(", ").toDouble()
                        val templong = strlatlong!!.substringAfter(", ").toDouble()
                        val locationList = Geocoder(this).getFromLocation(templat, templong, 1)
                        val tempaddress = locationList[0].getAddressLine(0)

                        Log.d("test", timeUpload + strlatlong + tempaddress)

                        val newItem = LocationItem(tempaddress, strlatlong!!, timeUpload!!, url.toString())
                        locationItemList.add(newItem)
                    }.addOnCompleteListener {
                        recyclerView?.adapter = myAdapter
                        recyclerView?.layoutManager = LinearLayoutManager(this)
                        var itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
                        itemDecoration.setDrawable(getDrawable(R.drawable.custom_divider)!!)
                        recyclerView?.addItemDecoration(itemDecoration)
                        myAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
        recyclerView?.adapter = myAdapter
        recyclerView?.layoutManager = LinearLayoutManager(this)
        var itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        itemDecoration.setDrawable(getDrawable(R.drawable.custom_divider)!!)
        recyclerView?.addItemDecoration(itemDecoration)
        myAdapter.notifyDataSetChanged()
        addPhoto?.setOnClickListener{
            getLocation()
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

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        if (hasLocationPermission()) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                lat = it.latitude
                long = it.longitude
                latlong = "${lat.toString()}, ${long.toString()}"
                val locationList = Geocoder(this).getFromLocation(lat, long, 1)
                address = locationList[0].getAddressLine(0)
            }
        }
        else {
            requestLocationPermission()
        }
    }
    private fun hasLocationPermission() =
        EasyPermissions.hasPermissions(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        )
    private fun requestLocationPermission() {
        Log.d("test", "requesting")
        EasyPermissions.requestPermissions(
            this, "I need permission", 101, Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms.first())) {
            SettingsDialog.Builder(this).build().show()
        }
        else {
            requestLocationPermission()
        }
    }
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        TODO("Not yet implemented")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CODE && resultCode == Activity.RESULT_OK) {
            val storageRef: StorageReference = FirebaseStorage.getInstance().reference
            val file: Uri = Uri.fromFile(File(photoFile.absolutePath))

            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            val formattedTime = current.format(formatter)

            val metadata = storageMetadata{
                contentType = "image/jpg"
                setCustomMetadata("timeUpload", formattedTime)

                Thread.sleep(1_000)
                setCustomMetadata("latlong", latlong)
            }

            val uploadTask = storageRef.child("images/" + file.lastPathSegment).putFile(file, metadata)
            uploadTask.addOnSuccessListener {
                storageRef.child("images/" + file.lastPathSegment).downloadUrl.addOnSuccessListener {
                    val newItem = LocationItem(address, latlong, formattedTime, it.toString())
                    locationItemList.add(newItem)
                    myAdapter.notifyDataSetChanged()
                }
                Log.d("test", "upload successful")
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onItemClick(item: LocationItem, position: Int) {
        Log.d("test", "clicked")
        val intent = Intent(Intent.ACTION_VIEW)
        val str = "geo:" + item.coordinates
        intent.data = Uri.parse(str)
        val chooser = Intent.createChooser(intent, "Launch Maps")
        startActivity(chooser)
    }
}