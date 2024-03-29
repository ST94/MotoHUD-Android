package motohud.fydp.com.motohud.maps

import android.Manifest
import android.annotation.SuppressLint
import android.app.DialogFragment
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Criteria
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.ramimartin.multibluetooth.activity.BluetoothActivity
import com.ramimartin.multibluetooth.bluetooth.manager.BluetoothManager
import kotlinx.android.synthetic.main.activity_maps.*
import motohud.fydp.com.motohud.BuildConfig
import motohud.fydp.com.motohud.R
import motohud.fydp.com.motohud.bluetooth.BluetoothConstants
import motohud.fydp.com.motohud.dongle.MotorcycleState
import motohud.fydp.com.motohud.maps.MapConstants.DC_LOCATION_LATITUDE
import motohud.fydp.com.motohud.maps.MapConstants.DC_LOCATION_LONGITUDE
import motohud.fydp.com.motohud.navigation.NavigationHttpRequest
import motohud.fydp.com.motohud.navigation.NavigationValue
import motohud.fydp.com.motohud.navigation.ui.DirectionDialogFragment
import motohud.fydp.com.motohud.utils.LocationUtils
import motohud.fydp.com.motohud.utils.PermissionUtils
import motohud.fydp.com.motohud.utils.StringUtils.validateMacAddress
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

class MapsActivity : BluetoothActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener, DirectionDialogFragment.DirectionDialogListener {

    override fun onClientConnectionFail() {
    }

    private val TAG = "MapsActivity"

    private lateinit var mMap: GoogleMap
    private val markerPoints = ArrayList<Marker>()
    private val geocoder = Geocoder(this)
    private var dongleMacAddress = ""
    private var helmetMacAddress = ""
    private var latestMotorcycleState : MotorcycleState? = null
    private var hudNavigationValues = ArrayList<NavigationValue>()

    private var sendNavigationState = false
    private var sendMotorcycleState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = fragmentManager
                .findFragmentById(R.id.map) as MapFragment
        mapFragment.getMapAsync(this)

        fab.setOnClickListener {
            showDirectionDialog()
        }

        setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DICOVERY_3600_SEC)
        selectServerMode()
        maps_scan_button.setOnClickListener {
            Log.d(TAG, "Scanning for bluetooth devices")
            scanAllBluetoothDevice()
        }

        maps_transmit_navigation_button.setOnClickListener {
            Log.d(TAG, "Checking if navigation transmission is permissible")
            sendNavigationState = !sendNavigationState
            //latestMotorcycleState = MotorcycleState(1,2,3)
            if (hudNavigationValues.size > 0 && isConnected && helmetMacAddress != "" && sendNavigationState) {
                maps_transmit_state_button.isEnabled = false
                maps_transmit_state_button.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
                Log.d(TAG, "Requirements met, starting transmit thread")
                if (latestMotorcycleState == null) {
                    latestMotorcycleState = MotorcycleState(0, 0, 0)
                }
                Thread(Runnable {
                    for (i in 0 until hudNavigationValues.size) {
                        if (!sendNavigationState) {
                            break
                        }

                        SystemClock.sleep(500)
                        Log.d(TAG, "Sending value to helmet: " + generateHelmetInfoString(hudNavigationValues[i], latestMotorcycleState!!, true))
                        sendMessageString(helmetMacAddress, generateHelmetInfoString(hudNavigationValues[i], latestMotorcycleState!!, true))

                        runOnUiThread {
                            try {
                                animateMarker(markerPoints[0], hudNavigationValues[i].position, false)
                            } catch (ex : IndexOutOfBoundsException) {
                                Log.d(TAG, "Ignoring index out of bounds")
                            }
                        }
                        SystemClock.sleep(500)
                        if (i == (hudNavigationValues.size - 1)) {
                            sendNavigationState = false
                        }
                    }
                    runOnUiThread {
                        maps_transmit_navigation_button.text = resources.getString(R.string.maps_transmit_button_disabled_navigation_text)
                        maps_transmit_navigation_button.setTextColor(Color.BLACK)
                        //maps_transmit_navigation_button.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
                        maps_transmit_navigation_button.setBackgroundColor(Color.WHITE)
                        maps_transmit_state_button.isEnabled = true
                        maps_transmit_state_button.setBackgroundColor(Color.WHITE)
                    }
                    //hudNavigationValues.clear()
                }).start()
                maps_transmit_navigation_button.text = resources.getString(R.string.maps_transmitting_button_text)
                maps_transmit_navigation_button.setTextColor(Color.WHITE)
                maps_transmit_navigation_button.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            }
        }

        maps_transmit_state_button.setOnClickListener {
            sendMotorcycleState = !sendMotorcycleState
            if (sendMotorcycleState && latestMotorcycleState != null && isConnected && helmetMacAddress != "") {
                maps_transmit_navigation_button.isEnabled = false
                maps_transmit_navigation_button.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
                Thread(Runnable {
                    while (sendMotorcycleState) {
                        SystemClock.sleep(1000)
                        Log.d(TAG, "Sending value to helmet: " + generateHelmetInfoString(null, latestMotorcycleState!!, false))
                        sendMessageString(helmetMacAddress, generateHelmetInfoString(null, latestMotorcycleState!!, false))
                    }
                    runOnUiThread({
                        maps_transmit_state_button.text = resources.getString(R.string.maps_transmit_button_disabled_state_text)
                        maps_transmit_navigation_button.isEnabled = true
                        if (hudNavigationValues.size > 0) {
                            maps_transmit_navigation_button.setBackgroundColor(Color.WHITE)
                        } else {
                            maps_transmit_navigation_button.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
                        }
                    })
                }).start()

                maps_transmit_state_button.text = resources.getString(R.string.maps_transmit_button_stop_state_text)
                maps_transmit_state_button.setTextColor(Color.WHITE)
                maps_transmit_state_button.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            }

            if (!sendMotorcycleState) {
                maps_transmit_state_button.text = resources.getString(R.string.maps_transmit_button_disabled_state_text)
                maps_transmit_state_button.setBackgroundColor(Color.WHITE)
                maps_transmit_state_button.setTextColor(Color.BLACK)
            }
        }
    }

    private fun generateHelmetInfoString(navValue : NavigationValue?, mState : MotorcycleState, includeNavigation : Boolean) : String  {
        var builder = StringBuilder()
        builder.append(mState.speed)
        builder.append(",")
        builder.append(mState.rpm)
        builder.append(",")
        builder.append(mState.gearNumber)
        builder.append(",")

        if (includeNavigation) {
            builder.append(navValue!!.distance)
            builder.append(",")
            builder.append(Integer.toString(navValue.direction.ordinal))
        } else {
            builder.append("0")
            builder.append(",")
            builder.append("0")
        }
        builder.append(";")
        return builder.toString()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "Registered event bus")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BluetoothManager.REQUEST_DISCOVERABLE_CODE) {
            when (resultCode) {
                BluetoothManager.BLUETOOTH_REQUEST_REFUSED -> {
                }
                BluetoothManager.BLUETOOTH_REQUEST_ACCEPTED -> onBluetoothStartDiscovery()
                else -> {
                    mBluetoothManager.yourBtMacAddress
                }
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case, by
     * default the user is placed at their current location.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setOnMyLocationButtonClickListener(this)
        mMap.setOnMyLocationClickListener(this)
        enableMyLocation()

        // Getting Current Location
        try {
            // Getting LocationManager object from System Service LOCATION_SERVICE
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Creating a criteria object to retrieve provider
            val criteria = Criteria()

            // Getting the name of the best provider
            val provider = locationManager.getBestProvider(criteria, true)
            moveToCurrentLocation(locationManager, provider)

        } catch (ex : SecurityException) {
            PermissionUtils.requestPermission(this, MapConstants.LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true)
        }

        mMap.setOnMapClickListener { point ->
            if (markerPoints.size == 2) {
                markerPoints.clear()
                mMap.clear()
            }
            val options = MarkerOptions()
            options.position(point)
            if (markerPoints.size == 0) {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            } else if (markerPoints.size == 1) {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            }
            markerPoints.add(mMap.addMarker(options))

            if (markerPoints.size > 1) {
                generateDirectionMarkers(markerPoints[0].position, markerPoints[1].position)
            }
        }
    }

    private fun showDirectionDialog() {
        // Create an instance of the dialog fragment and show it
        val dialog = DirectionDialogFragment()
        dialog.show(fragmentManager, "DirectionDialogFragment")
    }

    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface
    override fun onDialogPositiveClick(dialog: DialogFragment) {
        // User touched the dialog's positive button

        val startField = dialog.dialog.findViewById<EditText>(R.id.start_location)
        val endField = dialog.dialog.findViewById<EditText>(R.id.end_location)
        try {
            val startGeocode = geocoder.getFromLocationName(startField.text.toString(), 1)
            Log.d("MapsActivity", "startGeocode address: " + startGeocode[0].toString())
            val endGeocode = geocoder.getFromLocationName(endField.text.toString(), 1)
            Log.d("MapsActivity", "endGeocode address: " + endGeocode[0].toString())

            val startLatLng = LatLng(startGeocode[0].latitude, startGeocode[0].longitude)
            val endLatLng = LatLng(endGeocode[0].latitude, endGeocode[0].longitude)

            generateDirectionMarkers(startLatLng, endLatLng)
        } catch (ex : Exception) {
            ex.printStackTrace()
            Toast.makeText(this, "Failed to find location:", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // User touched the dialog's negative button
    }

    private fun generateDirectionMarkers(start: LatLng, end:LatLng) {
        if (markerPoints.size == 2) {
            markerPoints.clear()
            mMap.clear()
        }

        for (i in 0 .. 1) {
            val options = MarkerOptions()
            if (i == 0) {
                options.position(start)
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            } else if (i == 1) {
                options.position(end)
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            }
            markerPoints.add(mMap.addMarker(options))
        }

        // Checks, whether start and end locations are captured
        if (markerPoints.size >= 2) {
            // Getting URL to the Google Directions API
            val url = URL("https://maps.googleapis.com/maps/api/directions/json?origin="
                    +start.latitude+","+start.longitude +"&destination="+end.latitude+","
                    +end.longitude+"&key=" + BuildConfig.googleMapsKey)
            Log.d("onMapClick", url.toString())
            val fetchUrl = FetchUrl()

            // Start downloading json data from Google Directions API
            fetchUrl.execute(url.toString())
            //move map camera
            mMap.moveCamera(CameraUpdateFactory.newLatLng(start))
            mMap.animateCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition.zoom))
        }
    }

    private fun animateMarker(marker:Marker, toPosition:LatLng, hideMarker:Boolean) {
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val proj = mMap.projection
        val startPoint = proj.toScreenLocation(marker.position)
        val startLatLng = proj.fromScreenLocation(startPoint)
        val duration:Long = 500
        val interpolator = LinearInterpolator()
        handler.post(object:Runnable {
             override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t = interpolator.getInterpolation((elapsed.toFloat() / duration))
                val lng = (t * toPosition.longitude + ((1 - t) * startLatLng.longitude))
                val lat = (t * toPosition.latitude + ((1 - t) * startLatLng.latitude))
                 marker.position = LatLng(lat, lng)
                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16)
                }
                else {
                    marker.isVisible = !hideMarker
                }
            }
        })
    }


    @Throws(SecurityException::class)
    private fun moveToCurrentLocation(locationManager: LocationManager, provider : String) {
        var location = locationManager.getLastKnownLocation(provider)
        if (location == null) {
            // Attempt to use location utils method
            location = LocationUtils.getLastKnownLocation(this)
        }

        if (location != null) {
            // Getting latitude of the current location
            val latitude = location.latitude
            // Getting longitude of the current location
            val longitude = location.longitude
            // Creating a LatLng object for the current location
            val currentPosition = LatLng(latitude, longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15f))
        } else {
            // Default location to DC
            val latitude = DC_LOCATION_LATITUDE
            val longitude = DC_LOCATION_LONGITUDE
            val currentPosition = LatLng(latitude, longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15f))
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show()
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false
    }

    override fun onMyLocationClick(p0: Location) {
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, MapConstants.LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true)
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.isMyLocationEnabled = true
        }
    }

    override fun onBluetoothMsgObjectReceived(message: Any?, clientAddress: String) {
        Log.d(TAG, "Received bluetooth object")
        Toast.makeText(this, message.toString(), Toast.LENGTH_SHORT).show()
    }

    override fun setUUIDappIdentifier(): String {
        return "f520cf2c-6487-11e7-907b"
    }

    override fun onBluetoothStartDiscovery() {
    }

    override fun onServeurConnectionFail() {
        Toast.makeText(this, "Server Connection failed", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Server connection failed")
    }

    override fun onBluetoothMsgStringReceived(message: String?, clientAddress: String) {
        Log.d(TAG, "Received bluetooth string")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBluetoothNotAviable() {
    }

    override fun onClientConnectionSuccess() {
    }

    override fun onServeurConnectionSuccess(clientAddress : String) {
//        if (clientAddress == BluetoothConstants.M_HELMET_MAC_ADDRRESS) {
//            helmetMacAddress = clientAddress
//            Log.d(TAG, "Connected to helmet with mac address$clientAddress")
//        }
        //helmetMacAddress = clientAddress
        Toast.makeText(this, "===> Server Connection succeeded", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Connected to a client device$clientAddress")
    }

    override fun myNbrClientMax(): Int {
        return 7
    }

    override fun onBluetoothMsgBytesReceived(message: ByteArray?, clientAddress: String) {
        //Should only be receiving from motorcycle dongle

        //Log.d(TAG, "Received bluetooth byte array")
        if (message != null) {
            val motorcycleStateData = message.toString(Charset.defaultCharset())
            val splitData = motorcycleStateData.split(",")
            if (splitData.size == 3) {
                latestMotorcycleState = MotorcycleState(Integer.parseInt(splitData[0]), Integer.parseInt(splitData[1]), Integer.parseInt(splitData[2]))
                maps_transmit_state_button.isEnabled = true
                if (!sendMotorcycleState) {
                    maps_transmit_state_button.setBackgroundColor(Color.WHITE)
                }
            } else if (splitData.size == 1) {
                if (splitData[0] == BluetoothConstants.M_HELMET_BT_NAME) {
                    // Assume this is the helmet's mac address
                    helmetMacAddress = clientAddress
                }
            }
            else {
                Toast.makeText(this,
                        "malformed motorcycle dongle message:" + message.toString(Charset.defaultCharset()),
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBluetoothDeviceFound(device: BluetoothDevice?) {
        Log.d(TAG, "Discovered device with mac address" + device!!.address)
//        if (device.address == BluetoothConstants.M_DONGLE_MAC_ADDRESS) {
//            Toast.makeText(this,
//                    "===> Device detected and Thread Server created for this address : " + device.address,
//                    Toast.LENGTH_SHORT).show()
//            Log.d(TAG, "Found motor cycle dongle")
//            dongleMacAddress = device.address
//            maps_transmit_state_button.isEnabled = true
//            maps_transmit_state_button.setBackgroundColor(Color.WHITE)
//        } else if (device.address == BluetoothConstants.M_HELMET_MAC_ADDRRESS){
//            helmetMacAddress = device.address
//            Toast.makeText(this,
//                    "===> Device detected and Thread Server created for this address : " + device.address,
//                    Toast.LENGTH_SHORT).show()
//            Log.d(TAG, "Found motor cycle helmet")
//        }
        //helmetMacAddress = device.address
        //dongleMacAddress = device.address
    }

    @Throws(IOException::class)
    private fun downloadUrl(strUrl: String): String {
        var data = ""
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(strUrl)

            // Creating an http connection to communicate with url
            urlConnection = url.openConnection() as HttpURLConnection

            // Connecting to url
            urlConnection.connect()

            // Reading data from url
            iStream = urlConnection.inputStream

            val status = urlConnection.responseCode

            val br = BufferedReader(InputStreamReader(iStream))
            val sb = StringBuffer()

            when (status) {
                200, 201 -> {
                    var line = urlConnection.inputStream.bufferedReader().use(BufferedReader::readText)
                    sb.append(line)
                }
            }

            data = sb.toString()
            Log.d("downloadUrl", data.toString())
            br.close()

        } catch (e: Exception) {
            Log.d("Exception", e.toString())
            e.printStackTrace()
        } finally {
            iStream!!.close()
            urlConnection!!.disconnect()
        }
        return data
    }

    // Fetches data from url passed
    @SuppressLint("StaticFieldLeak")
    private inner class FetchUrl : AsyncTask<String, Void, String>(), NavigationHttpRequest.AsyncResponse{
        override fun processFinish(output: PolylineOptions, navigationValues : List<NavigationValue>) {
            mMap.addPolyline(output)
            hudNavigationValues = ArrayList(navigationValues)
            maps_transmit_navigation_button.isEnabled = true
            maps_transmit_navigation_button.setBackgroundColor(Color.WHITE)
        }

        override fun doInBackground(vararg url: String): String {
            // For storing data from web service
            var data = ""
            try {
                // Fetching the data from web service
                data = downloadUrl(url[0])
                Log.d("Background Task data", data)
            } catch (e: Exception) {
                Log.d("Background Task", e.toString())
            }

            return data
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            val parserTask = NavigationHttpRequest()
            // Invokes the thread for parsing the JSON data
            parserTask.delegate = this
            parserTask.execute(result)
        }
    }
}
