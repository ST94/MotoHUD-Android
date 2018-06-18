package motohud.fydp.com.motohud.maps

/**
 * Created by Shing on 2018-02-08.
 */

object MapConstants {
    const val LOCATION_PERMISSION_REQUEST_CODE = 1
    const val BLUETOOTH_PERMISSION_REQUEST_CODE = 2
    const val BLUETOOTH_ADMIN_PERMISSION_REQUEST_CODE = 3
    const val BLUETOOTH_REQUEST_ENABLE = 4

    val PERMISSION_ARRAY = arrayOf (android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION)

    // Location defaults to the university
    const val DC_LOCATION_LATITUDE = 43.473110
    const val DC_LOCATION_LONGITUDE = -80.541529

}
