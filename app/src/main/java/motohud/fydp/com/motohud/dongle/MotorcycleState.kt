package motohud.fydp.com.motohud.dongle

/**
 * Created by Shing on 2018-03-14.
 */

class MotorcycleState(var speed: Int, var rpm: Int, var gearNumber: Int) {

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append(speed)
        builder.append(",")
        builder.append(rpm)
        builder.append(",")
        builder.append(gearNumber)
        return builder.toString()
    }
}
