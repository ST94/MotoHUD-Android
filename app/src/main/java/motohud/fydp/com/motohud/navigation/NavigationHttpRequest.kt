package motohud.fydp.com.motohud.navigation

import android.graphics.Color
import android.os.AsyncTask
import android.util.Log
import org.json.JSONObject
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions

/**
 * Created by Shing on 2018-02-09.
 */
class NavigationHttpRequest : AsyncTask<String, kotlin.Int, NavigationResult>() {

    interface AsyncResponse {
        fun processFinish(output: PolylineOptions, navigationValues : List<NavigationValue>)
    }

    var delegate: AsyncResponse? = null

    // Parsing the data in non-ui thread
     override fun doInBackground(vararg jsonData: String): NavigationResult? {
        val jObject: JSONObject
        var navigationResult : NavigationResult? = null
        var routes: List<List<HashMap<String, String>>>? = null

        try {
            jObject = JSONObject(jsonData[0])
            Log.d("ParserTask", jsonData[0].toString())
            val parser = NavigationHttpParser()
            Log.d("ParserTask", parser.toString())

            // Starts parsing data
            navigationResult = parser.parse(jObject)

            Log.d("ParserTask", "Executing routes")
            Log.d("ParserTask", routes.toString())

        } catch (e: Exception) {
            Log.d("ParserTask", e.toString())
            e.printStackTrace()
        }

        return navigationResult
    }

    // Executes in UI thread, after the parsing process
    override fun onPostExecute(result: NavigationResult) {
        var points: ArrayList<LatLng>
        var lineOptions: PolylineOptions? = null

        // Traversing through all the routes
        for (i in result.polylineList.indices) {
            points = ArrayList()
            lineOptions = PolylineOptions()

            // Fetching i-th route
            val path = result.polylineList[i]

            // Fetching all the points in i-th route
            for (j in path.indices) {
                val point = path[j]
                val lat = java.lang.Double.parseDouble(point["lat"])
                val lng = java.lang.Double.parseDouble(point["lng"])
                val position = LatLng(lat, lng)
                points.add(position)
            }

            // Adding all the points in the route to LineOptions
            lineOptions.addAll(points)
            lineOptions.width(10f)
            lineOptions.color(Color.RED)

            Log.d("onPostExecute", "onPostExecute lineoptions decoded")
        }

        // Drawing polyline in the Google Map for the i-th route
        if (lineOptions != null) {
            delegate!!.processFinish(lineOptions, result.navigationValues);
        } else {
            Log.d("onPostExecute", "without Polylines drawn")
        }
    }
}