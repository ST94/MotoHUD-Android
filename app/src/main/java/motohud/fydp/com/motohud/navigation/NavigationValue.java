package motohud.fydp.com.motohud.navigation;

/**
 * Created by Shing on 2018-03-11.
 */

public class NavigationValue {
    private enum Direction {
        STRAIGHT, LEFT, RIGHT
    }

    private Direction direction;
    private int distance;

    NavigationValue(Direction direction, int distance) {
        this.direction = direction;
        this.distance = distance;
    }
}
