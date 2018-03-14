package motohud.fydp.com.motohud.maps

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.ramimartin.multibluetooth.activity.BluetoothActivity
import com.ramimartin.multibluetooth.bluetooth.manager.BluetoothManager
import kotlinx.android.synthetic.main.activity_maps.*
import motohud.fydp.com.motohud.bluetooth.BluetoothConstants
import motohud.fydp.com.motohud.R
import motohud.fydp.com.motohud.dongle.MotorcycleState
import motohud.fydp.com.motohud.navigation.NavigationHttpRequest
import motohud.fydp.com.motohud.navigation.ui.DirectionDialogFragment
import motohud.fydp.com.motohud.utils.PermissionUtils
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
    private val markerPoints = ArrayList<LatLng>()
    private val geocoder = Geocoder(this)
    private var dongleMacAddress = ""
    private var helmetMacAddress = ""
    private lateinit var latestMotorcycleState : MotorcycleState

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
//        mBluetoothManager = BluetoothManager(this)
//        mBluetoothManager!!.setUUIDappIdentifier(setUUIDappIdentifier())

//        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter() //Assume device supports bluetooth
//
//        if (!mBluetoothAdapter.isEnabled) {
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            startActivityForResult(enableBtIntent, MapConstants.BLUETOOTH_REQUEST_ENABLE)
//        }
//
//        val pairedDevices = mBluetoothAdapter.bondedDevices
//
//        if (pairedDevices.size > 0) {
//            // There are paired devices. Get the name and address of each paired device.
//            for (device in pairedDevices) {
//                val deviceName = device.name
//                val deviceHardwareAddress = device.address // MAC address
//            }
//        }
//        var numConnectedDevices = 0

        setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DICOVERY_3600_SEC)
        selectServerMode()

        maps_scan_button.setOnClickListener {
            scanAllBluetoothDevice()
        }
    }

    override fun onStart() {
        super.onStart()
//        if(!EventBus.getDefault().isRegistered(this))
//            EventBus.getDefault().register(this)
        Log.d(TAG, "Registered event bus")
        //mBluetoothManager!!.nbrClientMax = 2
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BluetoothManager.REQUEST_DISCOVERABLE_CODE) {
            when (resultCode) {
                BluetoothManager.BLUETOOTH_REQUEST_REFUSED -> {
                }
                BluetoothManager.BLUETOOTH_REQUEST_ACCEPTED -> onBluetoothStartDiscovery()
                else -> {
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
            markerPoints.add(point)
            val options = MarkerOptions()
            options.position(point)
            if (markerPoints.size == 1) {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            } else if (markerPoints.size == 2) {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            }
            mMap.addMarker(options)

            if (markerPoints.size > 1) {
                generateDirectionMarkers(markerPoints[0], markerPoints[1])
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

        val startGeocode = geocoder.getFromLocationName(startField.text.toString(), 1)
        Log.d("MapsActivity", "startGeocode address: " + startGeocode[0].toString())
        val endGeocode = geocoder.getFromLocationName(endField.text.toString(), 1)
        Log.d("MapsActivity", "endGeocode address: " + endGeocode[0].toString())

        val startLatLng = LatLng(startGeocode[0].latitude, startGeocode[0].longitude)
        val endLatLng = LatLng(endGeocode[0].latitude, endGeocode[0].longitude)

        generateDirectionMarkers(startLatLng, endLatLng)
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // User touched the dialog's negative button
    }

    private fun generateDirectionMarkers(start: LatLng, end:LatLng) {
        if (markerPoints.size == 2) {
            markerPoints.clear()
            mMap.clear()
        }
        markerPoints.add(start)
        markerPoints.add(end)

        for (i in 0 .. 1) {
            val options = MarkerOptions()
            options.position(markerPoints[i])
            if (i == 0) {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            } else if (i == 1) {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            }
            mMap.addMarker(options)
        }

        // Checks, whether start and end locations are captured
        if (markerPoints.size >= 2) {
            // Getting URL to the Google Directions API
            val url = URL("https://maps.googleapis.com/maps/api/directions/json?origin="
                    +start.latitude+","+start.longitude +"&destination="+end.latitude+","
                    +end.longitude+"&key=AIzaSyDwysEfPjHbKyzW-ZC5mDqi_s3bMY1-2E8")
            Log.d("onMapClick", url.toString())
            val fetchUrl = FetchUrl()

            // Start downloading json data from Google Directions API
            fetchUrl.execute(url.toString())
            //move map camera
            mMap.moveCamera(CameraUpdateFactory.newLatLng(start))
            mMap.animateCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition.zoom))
        }

    }

    @Throws(SecurityException::class)
    private fun moveToCurrentLocation(locationManager: LocationManager, provider : String) {
        val location = locationManager.getLastKnownLocation(provider)

        if (location != null) {
            // Getting latitude of the current location
            val latitude = location.latitude
            // Getting longitude of the current location
            val longitude = location.longitude
            // Creating a LatLng object for the current location
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

    override fun onBluetoothMsgObjectReceived(message: Any?) {
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
    }

    override fun onBluetoothMsgStringReceived(message: String?) {
        Log.d(TAG, "Received bluetooth string")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBluetoothNotAviable() {
    }

    override fun onClientConnectionSuccess() {
    }

    override fun onServeurConnectionSuccess() {
        Toast.makeText(this, "===> Server Connection succeeded", Toast.LENGTH_SHORT).show()
    }

    override fun myNbrClientMax(): Int {
        return 2
    }

    override fun onBluetoothMsgBytesReceived(message: ByteArray?) {
        //Should only be receiving from motorcycle dongle

        Log.d(TAG, "Received bluetooth byte array")
        if (message != null) {
            val motorcycleStateData = message.toString(Charset.defaultCharset())
            val splitData = motorcycleStateData.split(",")
            if (splitData.size == 3) {
                latestMotorcycleState = MotorcycleState(Integer.parseInt(splitData[0]), Integer.parseInt(splitData[1]), Integer.parseInt(splitData[2]))
            } else {
                Toast.makeText(this,
                        "malformed motorcycle dongle message:" + message.toString(Charset.defaultCharset()),
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBluetoothDeviceFound(device: BluetoothDevice?) {
        if (device!!.name == BluetoothConstants.M_DONGLE_BT_NAME) {
            Toast.makeText(this,
                    "===> Device detected and Thread Server created for this address : " + device.address,
                    Toast.LENGTH_SHORT).show()

            dongleMacAddress = device.address
        } else if (device.name == BluetoothConstants.M_HELMET_BT_NAME){
            helmetMacAddress = device.address
            Toast.makeText(this,
                    "===> Device detected and Thread Server created for this address : " + device.address,
                    Toast.LENGTH_SHORT).show()
        }
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

    override fun onDestroy() {
        super.onDestroy()
//        EventBus.getDefault().unregister(this)
//        closeAllConnections()
    }

//    private fun closeAllConnections() {
//        mBluetoothManager!!.closeAllConnexion()
//    }
//
//    private fun setTimeDiscoverable(timeInSec: Int) {
//        mBluetoothManager!!.setTimeDiscoverable(timeInSec)
//    }
//
//    private fun startDiscovery() {
//        mBluetoothManager!!.startDiscovery()
//    }
//
//    fun scanAllBluetoothDevices() {
//        mBluetoothManager!!.scanAllBluetoothDevice()
//    }
//
//    fun disconnectServer() {
//        mBluetoothManager!!.disconnectServer(true)
//    }
//
//    private fun createServer(address: String) {
//        mBluetoothManager!!.createServeur(address)
//    }
//
//    fun selectServerMode() {
//        mBluetoothManager!!.selectServerMode()
//    }
//
//    fun getTypeBluetooth(): BluetoothManager.TypeBluetooth {
//        return mBluetoothManager!!.mType
//    }
//
//    fun getBluetoothMode(): BluetoothManager.TypeBluetooth {
//        return mBluetoothManager!!.mType
//    }
//
//    fun createClient(addressMac: String) {
//        mBluetoothManager!!.createClient(addressMac)
//    }
//
//    fun setMessageMode(messageMode: BluetoothManager.MessageMode) {
//        mBluetoothManager!!.setMessageMode(messageMode)
//    }
//
//    fun sendMessageStringToAll(message: String) {
//        mBluetoothManager!!.sendStringMessageForAll(message)
//    }
//
//    fun sendMessageString(addressMacTarget: String, message: String) {
//        mBluetoothManager!!.sendStringMessage(addressMacTarget, message)
//    }
//
//    fun sendMessageObjectToAll(message: Any) {
//        mBluetoothManager!!.sendObjectForAll(message)
//    }
//
//    fun sendMessageObject(adressMacTarget: String, message: Any) {
//        mBluetoothManager!!.sendObject(adressMacTarget, message)
//    }
//
//    fun sendMessageBytesForAll(message: ByteArray) {
//        mBluetoothManager!!.sendBytesForAll(message)
//    }
//
//    fun sendMessageBytes(adressMacTarget: String, message: ByteArray) {
//        mBluetoothManager!!.sendBytes(adressMacTarget, message)
//    }
//
//    fun isConnected(): Boolean {
//        return mBluetoothManager!!.isConnected
//    }
//
//    fun onEventMainThread(device: BluetoothDevice) {
//        onBluetoothDeviceFound(device)
//        createServer(device.address)
//    }
//
//    private fun onEventMainThread( event : ClientConnectionSuccess){
//        mBluetoothManager!!.isConnected = true
//        onClientConnectionSuccess()
//    }
//
//    private fun onEventMainThread(event : ClientConnectionFail){
//        mBluetoothManager!!.isConnected = false
//        onClientConnectionFail()
//    }
//
//    private fun onEventMainThread(event : ServeurConnectionSuccess){
//        mBluetoothManager!!.isConnected = true
//        mBluetoothManager!!.onServerConnectionSuccess(event.mClientAdressConnected);
//        onServerConnectionSuccess()
//    }
//
//    private fun onEventMainThread(event : ServeurConnectionFail){
//        mBluetoothManager!!.onServerConnectionFailed(event.mClientAdressConnectionFail);
//        onServerConnectionFail()
//    }
//
//    private fun onEventMainThread(event: BluetoothCommunicatorString){
//        onBluetoothMsgStringReceived(event.mMessageReceive);
//    }
//
//    private fun onEventMainThread(event : BluetoothCommunicatorObject){
//        onBluetoothMsgObjectReceived(event.mObject);
//    }
//
//    private fun onEventMainThread(event : BluetoothCommunicatorBytes){
//        onBluetoothMsgBytesReceived(event.mBytesReceive)
//    }

    // Fetches data from url passed
    @SuppressLint("StaticFieldLeak")
    private inner class FetchUrl : AsyncTask<String, Void, String>(), NavigationHttpRequest.AsyncResponse{
        override fun processFinish(output: PolylineOptions) {
            mMap.addPolyline(output)
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
