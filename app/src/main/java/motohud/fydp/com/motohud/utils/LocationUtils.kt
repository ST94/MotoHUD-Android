package motohud.fydp.com.motohud.utils

import android.app.Activity
import android.content.Context
import android.location.Location
import android.location.LocationManager

import android.content.Context.LOCATION_SERVICE

/**
 * Created by Shing on 2018-03-22.
 */

class LocationUtils {
    companion object {
        @Throws(SecurityException::class)
        fun getLastKnownLocation(context: Activity): Location? {
            val mLocationManager = context.applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = mLocationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val l = mLocationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    // Found best last known location: %s", l);
                    bestLocation = l
                }
            }
            return bestLocation
        }
    }
}
