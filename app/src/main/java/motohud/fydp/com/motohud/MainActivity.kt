package motohud.fydp.com.motohud

import android.Manifest.permission.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import motohud.fydp.com.motohud.maps.MapConstants.BLUETOOTH_ADMIN_PERMISSION_REQUEST_CODE
import motohud.fydp.com.motohud.maps.MapConstants.BLUETOOTH_PERMISSION_REQUEST_CODE
import motohud.fydp.com.motohud.maps.MapConstants.LOCATION_PERMISSION_REQUEST_CODE
import motohud.fydp.com.motohud.maps.MapConstants.PERMISSION_ARRAY
import motohud.fydp.com.motohud.utils.PermissionUtils
import motohud.fydp.com.motohud.utils.PermissionUtils.isPermissionGranted
import motohud.fydp.com.motohud.maps.MapsActivity
import motohud.fydp.com.motohud.utils.ui.SupportPermissionDeniedDialog


class MainActivity : AppCompatActivity() {
    private var mPermissionDenied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var permissionsGranted = true

        if(ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                // No explanation needed, we can request the permission.
            permissionsGranted = false
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    ACCESS_FINE_LOCATION, true)
        }
        if (ContextCompat.checkSelfPermission(this, BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsGranted = false
            PermissionUtils.requestPermission(this, BLUETOOTH_PERMISSION_REQUEST_CODE,
                    BLUETOOTH, true)
        }
        if (ContextCompat.checkSelfPermission(this, BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsGranted = false
            PermissionUtils.requestPermission(this, BLUETOOTH_ADMIN_PERMISSION_REQUEST_CODE,
                    BLUETOOTH_ADMIN, true)
        }

        if (permissionsGranted){
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE
                || requestCode != BLUETOOTH_PERMISSION_REQUEST_CODE
                || requestCode != BLUETOOTH_ADMIN_PERMISSION_REQUEST_CODE) {
            return
        }

        if (checkPermissions(PERMISSION_ARRAY, grantResults)) {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError()
            mPermissionDenied = false
        }
    }

    private fun checkPermissions(permissionArray: Array<String> , grantResults: IntArray) : Boolean {
        if (isPermissionGranted(permissionArray, grantResults, ACCESS_FINE_LOCATION)
                && isPermissionGranted(permissionArray, grantResults, BLUETOOTH)
                && isPermissionGranted(permissionArray, grantResults, BLUETOOTH_ADMIN)) {
            return true
        }
        return false
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private fun showMissingPermissionError() {
        SupportPermissionDeniedDialog.newInstance(true).show(supportFragmentManager, "dialog")
    }
}
