package motohud.fydp.com.motohud.utils

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by Shing on 2018-03-22.
 */

object StringUtils {
    const val MAC_ADDRESS_REGEX = "^([a-fA-F0-9][:-]){5}[a-fA-F0-9][:-]$"
    fun validateMacAddress(mac: String): Boolean {
        val p = Pattern.compile(MAC_ADDRESS_REGEX)
        val m = p.matcher(mac)
        return m.find()
    }
}
