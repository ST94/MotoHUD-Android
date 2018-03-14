package motohud.fydp.com.motohud.navigation;

/**
 * Created by Shing on 2018-03-11.
 */

public class NavigationValue {
    public enum Direction {
        STRAIGHT, LEFT, RIGHT
    }

    private Direction direction;
    private int distance;

    NavigationValue(Direction direction, int distance) {
        this.direction = direction;
        this.distance = distance;
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

}
