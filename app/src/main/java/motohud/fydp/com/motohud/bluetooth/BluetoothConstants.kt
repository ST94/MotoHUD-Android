/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package motohud.fydp.com.motohud.bluetooth

interface BluetoothConstants {
    companion object {

        // Message types sent from the BluetoothManagerThread Handler
        val MESSAGE_STATE_CHANGE = 1
        val MESSAGE_READ = 2
        val MESSAGE_WRITE = 3
        val MESSAGE_DEVICE_NAME = 4
        val MESSAGE_TOAST = 5

        // Key names received from the BluetoothManagerThread Handler
        val DEVICE_NAME = "device_name"
        val TOAST = "toast"

        val M_DONGLE_BT_NAME = "M_DONGLE"
        val M_HELMET_BT_NAME = "HELMET"
        val M_SERVER_BT_NAME = "M_SERVER"
    }
}
