package motohud.fydp.com.motohud.navigation

import com.google.android.gms.maps.model.LatLng
import motohud.fydp.com.motohud.navigation.NavigationValue.Direction
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONArray



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
                        var distance = ((jSteps.get(k) as JSONObject).get("distance") as JSONObject).get("value") as Int
                        var maneuver = Direction.STRAIGHT
                        try {
                            val maneuverString = (jSteps.get(k) as JSONObject).get("maneuver") as String
                            if (maneuverString == "turn-left") {
                                maneuver = Direction.LEFT
                            } else if (maneuverString == "turn-right"){
                                maneuver = Direction.RIGHT
                            }
                        } catch (jsonEx : JSONException) {
                            //maneuver does not exist, keep going straight
                        }
                        navigationValues.add(NavigationValue(maneuver, distance))

                        var polyline = ((jSteps.get(k) as JSONObject).get("polyline") as JSONObject).get("points") as String
                        val list = decodePoly(polyline)

                        /** Traversing all points  */
                        for (l in list.indices) {
                            val hm = HashMap<String, String>()
                            hm.put("lat", java.lang.Double.toString(list[l].latitude))
                            hm.put("lng", java.lang.Double.toString(list[l].longitude))
                            path.add(hm)
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
