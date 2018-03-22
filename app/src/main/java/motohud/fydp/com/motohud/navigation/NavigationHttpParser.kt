package motohud.fydp.com.motohud.navigation

import com.google.android.gms.maps.model.LatLng
import motohud.fydp.com.motohud.navigation.NavigationValue.Direction
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONArray
import java.lang.Math.*

/**
 * Created by Shing on 2018-02-09.
 */

class NavigationHttpParser {
    /**
     * Receives a JSONObject and returns a list of lists containing latitude and longitude
     */
    fun parse(jObject: JSONObject): NavigationResult {

        val routes = ArrayList<List<HashMap<String,String>>>()
        val navigationValues = ArrayList<NavigationValue>()
        val jRoutes: JSONArray
        var jLegs: JSONArray
        var jSteps: JSONArray

        try {
            jRoutes = jObject.getJSONArray("routes")

            /** Traversing all routes  */
            for (i in 0 until jRoutes.length()) {
                jLegs = (jRoutes.get(i) as JSONObject).getJSONArray("legs")
                val path = ArrayList<HashMap<String,String>>()

                /** Traversing all legs  */
                for (j in 0 until jLegs.length()) {
                    jSteps = (jLegs.get(j) as JSONObject).getJSONArray("steps")

                    /** Traversing all steps  */
                    for (k in 0 until jSteps.length()) {
                        val polyline = ((jSteps.get(k) as JSONObject).get("polyline") as JSONObject).get("points") as String
                        val list = decodePoly(polyline)

                        /** Traversing all points  */
                        for (l in list.indices) {
                            val hm = HashMap<String, String>()
                            hm.put("lat", java.lang.Double.toString(list[l].latitude))
                            hm.put("lng", java.lang.Double.toString(list[l].longitude))
                            path.add(hm)
                        }

                        var maneuver = Direction.STRAIGHT
                        try {
                            val maneuverString = (jSteps.get(k) as JSONObject).get("maneuver") as String
                            if (maneuverString == "turn-left") {
                                maneuver = Direction.LEFT
                            } else if (maneuverString == "turn-right") {
                                maneuver = Direction.RIGHT
                            }
                        } catch (jsonEx: JSONException) {
                            //maneuver does not exist, keep going straight
                        }

                        val distance = ((jSteps.get(k) as JSONObject).get("distance") as JSONObject).get("value") as Int
                        if (distance > 100) {
                            for (splitDistance in distance downTo 0 step 100) {
                                val point = findPointBetweenPath(list[0], list.last(), (distance - splitDistance.toDouble()))
                                if (splitDistance < 100) {
                                    navigationValues.add(NavigationValue(maneuver, splitDistance, list.last()))
                                } else {
                                    navigationValues.add(NavigationValue(Direction.STRAIGHT, splitDistance, point))
                                }
                            }
                        } else {
                            navigationValues.add(NavigationValue(maneuver, distance, list.last()))
                        }
                    }
                    routes.add(path)
                }
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: Exception) {
        }
        return NavigationResult(routes, navigationValues)
    }

    private fun findPointBetweenPath(start: LatLng, end : LatLng, distanceInMetres: Double) : LatLng {
        val brngRad = bearingInRadians(start, end)
        val latRad = Math.toRadians(start.latitude)
        val lonRad = Math.toRadians(start.longitude)
        val earthRadiusInMetres = 6371000
        val distFrac = distanceInMetres / earthRadiusInMetres

        val latitudeResult = asin(sin(latRad) * cos(distFrac) + cos(latRad) * sin(distFrac) * cos(brngRad))
        val a = atan2(sin(brngRad) * sin(distFrac) * cos(latRad), cos(distFrac) - sin(latRad) * sin(latitudeResult))
        val longitudeResult = (lonRad + a + 3 * PI) % (2 * PI) - PI
        return LatLng(toDegrees(latitudeResult), toDegrees(longitudeResult))
    }

    private fun bearingInRadians(src: LatLng, dst: LatLng): Double {
        val srcLat = Math.toRadians(src.latitude)
        val dstLat = Math.toRadians(dst.latitude)
        val dLng = Math.toRadians(dst.longitude - src.longitude)

        return Math.atan2(Math.sin(dLng) * Math.cos(dstLat),
                Math.cos(srcLat) * Math.sin(dstLat) - Math.sin(srcLat) * Math.cos(dstLat) * Math.cos(dLng))
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * @returns Distance in Meters
     */
    private fun distanceBetweenPoints(start : LatLng, end: LatLng, el1: Double = 0.0, el2: Double = 0.0): Double {
        val R = 6371 // Radius of the earth
        val latDistance = Math.toRadians(end.latitude - start.latitude)
        val lonDistance = Math.toRadians(end.longitude - start.longitude)
        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + (Math.cos(Math.toRadians(start.latitude)) * Math.cos(Math.toRadians(end.latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2))
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        var distance = R.toDouble() * c * 1000.0 // convert to meters
        val height = el1 - el2
        distance = Math.pow(distance, 2.0) + Math.pow(height, 2.0)
        return Math.sqrt(distance)
    }

    /**
     * Method to decode polyline points
     * Courtesy : https://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
     */
    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5,
                    lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

}
