package motohud.fydp.com.motohud.utils

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by Shing on 2018-03-22.
 */

object StringUtils {
    fun validateMacAddress(mac: String): Boolean {
        val p = Pattern.compile("^([a-fA-F0-9][:-]){5}[a-fA-F0-9][:-]$")
        val m = p.matcher(mac)
        return m.find()
    }
}
