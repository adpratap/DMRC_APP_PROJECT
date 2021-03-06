package com.adpratap11.dmrcp1


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_attendence.*
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

val REQUEST_IMAGE_CAPTURE = 1
val MY_PERMISSIONS_REQUEST_CM = 0
lateinit var picurl: String
private const val PERMISSION_REQUEST = 10
lateinit var latitude : String
lateinit var longitude : String
lateinit var latitudenet : String
lateinit var longitudenet : String
lateinit var locationManager: LocationManager
val storageRef = FirebaseStorage.getInstance().reference
val user = FirebaseAuth.getInstance().currentUser
private var hasGps = false
private var hasNetwork = false
private var locationGps: Location? = null
private var locationNetwork: Location? = null
lateinit var user_address: String
lateinit var user_city: String
lateinit var user_country: String
lateinit var user_state: String
lateinit var user_postal: String
lateinit var user_knownloaction: String
lateinit var user_full_address: String
private var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.CAMERA)





class attendence : AppCompatActivity() {

    lateinit var usertime: String
    lateinit var userdate: String
    var anand = ""
    var pratap = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendence)


        //address start


        //address end


        val ha = Handler()
        ha.postDelayed(object : Runnable {

            override fun run() {

                if (anand == "" && pratap == "") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkPermission(permissions)) {
                            getLocation()
                        } else {
                            requestPermissions(permissions, PERMISSION_REQUEST)
                        }
                    } else {
                        getLocation()
                    }
                } else {

                    val toast =
                        Toast.makeText(applicationContext, "Searching your Location", Toast.LENGTH_SHORT)
                    toast.show()


                }

                if (anand == "" && pratap == "") {
                    ha.postDelayed(this, 10000)
                }
            }
        }, 10000)





        val login = FirebaseAuth.getInstance().currentUser
        if (login != null) {

            getdata()


        } else {

            // No user is signed in

            val toast = Toast.makeText(applicationContext, "NO USER PLZ TRY AGAIN", Toast.LENGTH_SHORT)
            toast.show()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }



        button_upload.setOnClickListener{


            usertime = getCurrentDateTime().toString()
            userdate = date()


            progressBar3.visibility = View.VISIBLE
            val picuser = storageRef.child(user!!.uid).child(userdate)
            val bitmap = (profilepic.drawable as BitmapDrawable).bitmap
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos as OutputStream?)
            val data = baos.toByteArray()
            val uploadTask = picuser.putBytes(data)

            uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                return@Continuation picuser.downloadUrl
            }).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    picurl = task.result.toString()

                    dataupload()


                } else {
                    // Handle failures


                }
            }

        }

        profilepic.setOnClickListener{


            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {


                    } else {

                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), MY_PERMISSIONS_REQUEST_CM)





                    }
                } else {

                    // Permission has already been granted

                    dispatchTakePictureIntent()


                }



        }

        history.setOnClickListener {

            val intent = Intent(this, admin::class.java)
            startActivity(intent)
        }

        imageView2.setOnClickListener {

            val intent = Intent(this, admin::class.java)
            startActivity(intent)

        }


    }


    private fun getdata() {



        val users = FirebaseAuth.getInstance().currentUser

        if (users!=null) {

            val ref = FirebaseDatabase.getInstance().getReference("DMRC USERS LIST/" + users.uid)
            ref.keepSynced(true)


            ref.addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                    Toast.makeText(applicationContext, "snapshot error", Toast.LENGTH_SHORT).show()


                }

                override fun onDataChange(p0: DataSnapshot) {


                    val mmm = p0.child("username").value.toString()


                    USRNAME.text = "Mr/Mrs $mmm"

                    getLocation()


                }

            })

        }


    }

    fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            profilepic.setImageBitmap(imageBitmap)
            textView5.visibility=View.GONE
            remarks.visibility=View.VISIBLE
            button_upload.visibility=View.VISIBLE





        }
    }

    fun dataupload(){


        val imurl = picurl
        val userlocation = user_full_address
        val nowTD = usertime
        val nowD = userdate
        val reff = FirebaseDatabase.getInstance().getReference("DMRC USERS DAILY DATA")
        val foradmin = FirebaseDatabase.getInstance().getReference("FOR ADMIN")
        reff.keepSynced(true)
        val remarksuser = remarks.text.toString()
        val userid = FirebaseAuth.getInstance().currentUser!!.uid
        val aname = USRNAME.text.toString()

        //uploading data
            val userdata = Record(imurl, userlocation, nowTD, remarksuser,aname)
        reff.child(userid).child(nowD).setValue(userdata)
                .addOnCompleteListener {


                    progressBar3.visibility = View.GONE

                    Toast.makeText(applicationContext, "SUCCESS upload data", Toast.LENGTH_SHORT).show()
                    USRNAME.visibility=View.GONE
                    button_upload.visibility=View.GONE
                    profilepic.visibility=View.GONE
                    remarks.visibility=View.GONE
                    thankyou.visibility=View.VISIBLE
                    textView6.visibility=View.GONE
                    gpslt.visibility=View.GONE
                    gpslg.visibility=View.GONE
                    history.visibility = View.VISIBLE




                }.addOnFailureListener {
                    progressBar3.visibility = View.GONE
                    Toast.makeText(applicationContext, "error upload data", Toast.LENGTH_SHORT).show()
                    return@addOnFailureListener
                }.addOnCanceledListener {
                    progressBar3.visibility = View.GONE
                    Toast.makeText(applicationContext, "error upload data canceled", Toast.LENGTH_SHORT).show()
                    return@addOnCanceledListener
                }
        foradmin.child(nowD).child(userid).setValue(userdata).addOnCanceledListener {
            Toast.makeText(applicationContext, "admin upload data canceled", Toast.LENGTH_SHORT).show()
            return@addOnCanceledListener
        }.addOnFailureListener {
            Toast.makeText(applicationContext, "admin upload data failed", Toast.LENGTH_SHORT).show()
            return@addOnFailureListener
        }



    }

    fun getCurrentDateTime(): Date {
        return Calendar.getInstance().time
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        progressBar3.visibility = View.VISIBLE
        //Toast.makeText(this, "Searching location", Toast.LENGTH_SHORT).show()


        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {

            val toast = Toast.makeText(applicationContext, "TURN ON GPS FIRST", Toast.LENGTH_SHORT)
            toast.show()


            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))


        } else {
            hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
        hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (hasGps || hasNetwork) {

            if (hasGps) {
                Log.d("CodeAndroidLocation", "hasGps")
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0F, object :
                    LocationListener {
                    override fun onLocationChanged(location: Location?) {
                        if (location != null) {
                            locationGps = location
                        }
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

                    }

                    override fun onProviderEnabled(provider: String?) {

                    }

                    override fun onProviderDisabled(provider: String?) {

                    }

                })

                val localGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (localGpsLocation != null){
                    locationGps = localGpsLocation
                }

            }
            if (hasNetwork) {
                Log.d("CodeAndroidLocation", "hasGps")
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0F, object : LocationListener {
                    override fun onLocationChanged(location: Location?) {
                        if (location != null) {
                            locationNetwork = location
                        }
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

                    }

                    override fun onProviderEnabled(provider: String?) {

                    }

                    override fun onProviderDisabled(provider: String?) {

                    }

                })

                val localNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (localNetworkLocation != null)
                    locationNetwork = localNetworkLocation
            }

            if(locationGps!= null && locationNetwork!= null){
                progressBar3.visibility = View.GONE
                if(locationGps!!.accuracy > locationNetwork!!.accuracy){
                    latitudenet=locationNetwork!!.latitude.toString()
                    longitudenet=locationNetwork!!.longitude.toString()
                    gpslg.text="GPS longitude : $longitude"
                    gpslt.text="GPS latitude : $latitude"
                    anand = latitude
                    pratap = longitude

                    val thegeo = Geocoder(this, Locale.getDefault()).getFromLocation(
                        latitude.toDouble(),
                        longitude.toDouble(),
                        1
                    )
                    user_address = thegeo.get(0).getAddressLine(0)

                    user_full_address = "$user_address "

                    Toast.makeText(applicationContext, user_full_address, Toast.LENGTH_LONG).show()



                }else{
                    latitude=locationGps!!.latitude.toString()
                    longitude=locationGps!!.longitude.toString()
                    gpslg.text="GPS longitude : $longitude"
                    gpslt.text="GPS latitude : $latitude"
                    anand = latitude
                    pratap = longitude

                    val thegeo = Geocoder(this, Locale.getDefault()).getFromLocation(
                        latitude.toDouble(),
                        longitude.toDouble(),
                        1
                    )
                    user_address = thegeo.get(0).getAddressLine(0)
                    //user_country = thegeo.get(0).countryName
                    //user_city = thegeo.get(0).locality
                    //user_state = thegeo.get(0).adminArea
                    //user_postal = thegeo.get(0).postalCode
                    //user_knownloaction = thegeo.get(0).featureName

                    user_full_address = "$user_address"

                    Toast.makeText(applicationContext, user_full_address, Toast.LENGTH_LONG).show()

                }
            }

        } else {

            progressBar3.visibility = View.GONE
            Toast.makeText(applicationContext, "plz turn on gps and net", Toast.LENGTH_SHORT).show()
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

            if( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {

                val toast = Toast.makeText(applicationContext, "TURN ON GPS FIRST", Toast.LENGTH_SHORT)
                toast.show()

                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))


            }



        }

    }

    private fun checkPermission(permissionArray: Array<String>): Boolean {
        var allSuccess = true
        for (i in permissionArray.indices) {
            if (checkCallingOrSelfPermission(permissionArray[i]) == PackageManager.PERMISSION_DENIED)
                allSuccess = false
        }
        return allSuccess
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            var allSuccess = true
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    allSuccess = false
                    val requestAgain = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(permissions[i])
                    if (requestAgain) {
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Go to settings and enable the permission", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            if (allSuccess)

               getLocation()

        }
    }

    fun date(): String {

        val sdf = SimpleDateFormat("dd MM yyyy")
        val currentDate = sdf.format(Date())
        return currentDate
    }

}
