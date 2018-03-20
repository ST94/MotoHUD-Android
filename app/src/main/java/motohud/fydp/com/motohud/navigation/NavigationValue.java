package motohud.fydp.com.motohud.navigation;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Shing on 2018-03-11.
 */

public class NavigationValue {
    public enum Direction {
        NONE, STRAIGHT, LEFT, RIGHT
    }

    private Direction direction;
    private int distance;
    private LatLng position;

    NavigationValue(Direction direction, int distance, LatLng position) {
        this.direction = direction;
        this.distance = distance;
        this.position = position;
    }


    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public LatLng getPosition() {
        return position;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

}
