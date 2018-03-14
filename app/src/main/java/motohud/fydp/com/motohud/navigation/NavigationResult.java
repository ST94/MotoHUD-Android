package motohud.fydp.com.motohud.navigation;

import java.util.HashMap;
import java.util.List;

/** Store navigation values
 *
 * Created by Shing on 2018-03-11.
 */

public class NavigationResult {
    private List<List<HashMap<String, String>>> polylineList;
    private List<NavigationValue> navigationValues;

    public NavigationResult(List<List<HashMap<String, String>>> polylineList, List<NavigationValue> navigationValues) {
        this.polylineList = polylineList;
        this.navigationValues = navigationValues;
    }

    public List<List<HashMap<String, String>>> getPolylineList() {
        return polylineList;
    }

    public void setPolylineList(List<List<HashMap<String, String>>> polylineList) {
        this.polylineList = polylineList;
    }

    public List<NavigationValue> getNavigationValues() {
        return navigationValues;
    }

    public void setNavigationValues(List<NavigationValue> navigationValues) {
        this.navigationValues = navigationValues;
    }
}
