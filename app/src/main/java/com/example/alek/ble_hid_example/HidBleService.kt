/*
 * Copyright 2018-2019 Aleksander Drewnicki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.alek.ble_hid_example

import android.app.Service
import com.example.alek.ble_hid_example.ReportField
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.os.IBinder
import com.example.alek.ble_hid_example.HidBleService.LocalBinder
import com.example.alek.ble_hid_example.NotificationData
import com.example.alek.ble_hid_example.MainActivity
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.os.BatteryManager
import com.example.alek.ble_hid_example.SendTo
import com.example.alek.ble_hid_example.UUIDs
import com.example.alek.ble_hid_example.HidBleService
import com.example.alek.ble_hid_example.KeyboardUsage
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.AdvertiseData
import android.os.ParcelUuid
import android.bluetooth.BluetoothGattDescriptor
import com.example.alek.ble_hid_example.ApplicationConfiguration
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Binder
import android.util.Log
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or

internal enum class SendTo {
    SEND_TO_FIRST,
    SEND_TO_LAST,
    SEND_TO_ALL
}

internal enum class SecurityLevel {
    SECURITY_LEVEL_1,  // None
    SECURITY_LEVEL_2,  // Encryption
    SECURITY_LEVEL_3   // Authentication
}

enum class ReportField(var byte_offset: Int, val byte_size: Int) {
    /* Consumer */
    REPORT_FIELD_CONSUMER_CONTROL(0, 2),
    REPORT_FIELD_LAUNCHER_BUTTON(2, 2),
    REPORT_FIELD_CONTROL_BUTTON(4, 2),  /* Keyboard */
    REPORT_FIELD_KEYBOARD_META_KEYS(6, 1),
    REPORT_FIELD_KEYBOARD_KEYS(1, 1),
    REPORT_FIELD_KEYBOARD_ALL(0, 2),  // REPORT_FIELD_KEYBOARD_META_KEYS + REPORT_FIELD_KEYBOARD_KEYS

    /* Mouse */
    REPORT_FIELD_MOUSE_BUTTONS(8, 1),
    REPORT_FIELD_MOUSE_X(9, 1),
    REPORT_FIELD_MOUSE_Y(10, 1),
    REPORT_FIELD_MOUSE_BUTTONS_XY(8, 3),
    REPORT_FIELD_MOUSE_XY(9, 2),
    REPORT_FIELD_MOUSE_SCROLL(11, 1);

    companion object {
        const val REP_CONSUMER = 0x01
        const val REP_MOUSE = 0x02
        const val REP_KEYBOARD = 0x04
        const val REP_BASIC = 0x08
        fun updateValues(features: Int) {
            /* Restore original value */
            REPORT_FIELD_CONSUMER_CONTROL.byte_offset = 0
            REPORT_FIELD_LAUNCHER_BUTTON.byte_offset = 2
            REPORT_FIELD_CONTROL_BUTTON.byte_offset = 4
            REPORT_FIELD_KEYBOARD_META_KEYS.byte_offset = 0
            REPORT_FIELD_KEYBOARD_KEYS.byte_offset = 1
            REPORT_FIELD_KEYBOARD_ALL.byte_offset = 0
            REPORT_FIELD_MOUSE_BUTTONS.byte_offset = 8
            REPORT_FIELD_MOUSE_X.byte_offset = 9
            REPORT_FIELD_MOUSE_Y.byte_offset = 10
            REPORT_FIELD_MOUSE_BUTTONS_XY.byte_offset = 8
            REPORT_FIELD_MOUSE_XY.byte_offset = 9
            REPORT_FIELD_MOUSE_SCROLL.byte_offset = 11
            if (features and REP_CONSUMER == 0) {
                REPORT_FIELD_CONSUMER_CONTROL.byte_offset = -1
                REPORT_FIELD_LAUNCHER_BUTTON.byte_offset = -1
                REPORT_FIELD_CONTROL_BUTTON.byte_offset = -1
                REPORT_FIELD_KEYBOARD_META_KEYS.byte_offset -= 6
                REPORT_FIELD_KEYBOARD_KEYS.byte_offset -= 6
                REPORT_FIELD_KEYBOARD_ALL.byte_offset -= 6
                REPORT_FIELD_MOUSE_BUTTONS.byte_offset -= 6
                REPORT_FIELD_MOUSE_X.byte_offset -= 6
                REPORT_FIELD_MOUSE_Y.byte_offset -= 6
                REPORT_FIELD_MOUSE_BUTTONS_XY.byte_offset -= 6
                REPORT_FIELD_MOUSE_XY.byte_offset -= 6
                REPORT_FIELD_MOUSE_SCROLL.byte_offset -= 6
            } else if (features and REP_BASIC == REP_BASIC) {
                REPORT_FIELD_LAUNCHER_BUTTON.byte_offset = -1
                REPORT_FIELD_CONTROL_BUTTON.byte_offset = -1
                REPORT_FIELD_KEYBOARD_META_KEYS.byte_offset -= 4
                REPORT_FIELD_KEYBOARD_KEYS.byte_offset -= 4
                REPORT_FIELD_KEYBOARD_ALL.byte_offset -= 4
                REPORT_FIELD_MOUSE_BUTTONS.byte_offset -= 4
                REPORT_FIELD_MOUSE_X.byte_offset -= 4
                REPORT_FIELD_MOUSE_Y.byte_offset -= 4
                REPORT_FIELD_MOUSE_BUTTONS_XY.byte_offset -= 4
                REPORT_FIELD_MOUSE_XY.byte_offset -= 4
                REPORT_FIELD_MOUSE_SCROLL.byte_offset -= 4
            }
            if (features and REP_KEYBOARD == 0) {
                REPORT_FIELD_KEYBOARD_META_KEYS.byte_offset = -1
                REPORT_FIELD_KEYBOARD_KEYS.byte_offset = -1
                REPORT_FIELD_KEYBOARD_ALL.byte_offset = -1
                REPORT_FIELD_MOUSE_BUTTONS.byte_offset -= 2
                REPORT_FIELD_MOUSE_X.byte_offset -= 2
                REPORT_FIELD_MOUSE_Y.byte_offset -= 2
                REPORT_FIELD_MOUSE_BUTTONS_XY.byte_offset -= 2
                REPORT_FIELD_MOUSE_XY.byte_offset -= 2
                REPORT_FIELD_MOUSE_SCROLL.byte_offset -= 2
            }
            if (features and REP_MOUSE == 0) {
                REPORT_FIELD_MOUSE_BUTTONS.byte_offset = -1
                REPORT_FIELD_MOUSE_X.byte_offset = -1
                REPORT_FIELD_MOUSE_Y.byte_offset = -1
                REPORT_FIELD_MOUSE_BUTTONS_XY.byte_offset = -1
                REPORT_FIELD_MOUSE_XY.byte_offset = -1
                REPORT_FIELD_MOUSE_SCROLL.byte_offset = -1
            }
        }
    }
}

internal class NotificationData(
    val device: BluetoothDevice,
    var characteristic: BluetoothGattCharacteristic,
    val value: ByteArray
) {
    val responseNeeded = false
}

class HidBleService : Service() {
    private val mBinder: IBinder = LocalBinder()
    val devices = ArrayList<BluetoothDevice>()
    private val pendingNotifications = ArrayList<NotificationData>()

    /* Call proper methods on some BLE events  */
    private var mainActivity: MainActivity? = null
    private var mGattServerCallback: BluetoothGattServerCallback? = null
    private var notificationPossible = false
    private var gattServer: BluetoothGattServer? = null
    private var check_thread: Thread? = null
    private val serviceList: MutableList<String> = mutableListOf()
    private val pendingServices: MutableList<BluetoothGattService> = mutableListOf()
    private val advertisingCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i("BLE", "Advertisement started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.i("BLE", "Advertisement not started - error: $errorCode")
        }
    }

    private fun readBatteryLevel(): Byte {
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        println("battery: $batteryLevel")
        return 75.toByte()
    }

    fun sendNotification(rf: ReportField, value: Int) {
        sendNotification(rf, value, SendTo.SEND_TO_ALL)
    }

    fun sendNotification(rf: ReportField, rawBytes: ByteArray?) {
        sendNotification(rf, 0, SendTo.SEND_TO_ALL, rawBytes)
    }

    private fun getArrayFromValue(rf: ReportField, value: Int, st: SendTo): ByteArray? {
        var value = value
        Log.d("BYTE", "offset: " + rf.byte_offset.toString())
        Log.d("BYTE", "size: " + rf.byte_size.toString())
        val array = ByteArray(rf.byte_offset + rf.byte_size)
        if (devices.size == 0) {
            return null
        }
        if (rf.byte_offset < 0) {
            return null
        }
        //        System.out.println(gattServer);
//        for (BluetoothGattService i: gattServer.getServices()) {
//            System.out.println(i.getUuid().toString());
//        }
//        System.out.println(gattServer.getService(UUID.fromString(UUIDs.SERVICE_HID)));


//        System.out.println(report);
        Arrays.fill(array, 0.toByte())
        //        array[0] = (byte) 0x02;
//        array[1] = (byte) 0x00;
//        array[2] = (byte) (value & 0xff);
        for (i in rf.byte_offset until rf.byte_offset + rf.byte_size) {
            array[i] = (value and 0xff).toByte()
            value = value shr 8
        }
        return array
    }

    private fun sendNotification(rf: ReportField, value: Int, st: SendTo, rawBytes: ByteArray? = null) {
        val array: ByteArray?
        array = rawBytes ?: getArrayFromValue(rf, value, st)
        if (array == null) return
        Log.d("NOTIF", "real notif starting")
        val report = gattServer!!.getService(UUID.fromString(UUIDs.SERVICE_HID)).getCharacteristic(
            UUID.fromString(UUIDs.CHAR_REPORT)
        )
        when (st) {
            SendTo.SEND_TO_FIRST -> pendingNotifications.add(NotificationData(devices[0], report, array))
            SendTo.SEND_TO_LAST -> pendingNotifications.add(
                NotificationData(
                    devices[devices.size - 1],
                    report, array
                )
            )
            SendTo.SEND_TO_ALL -> {
                var i = 0
                while (i < devices.size) {
                    pendingNotifications.add(
                        NotificationData(
                            devices[i], report, array
                        )
                    )
                    i++
                }
            }
        }
        if (notificationPossible) {
            val toSend = pendingNotifications.removeAt(0)
            //            toSend.characteristic.setValue(boop);
            toSend.characteristic.value = toSend.value
            Log.e("NOTIF1", "sending notification")
            Log.d("NOTIF", bytesToHex(toSend.characteristic.value))
            gattServer!!.notifyCharacteristicChanged(
                toSend.device, toSend.characteristic,  //                    toSend.responseNeeded
                true
            )
            notificationPossible = false
            return
        }
        if (check_thread != null) {
            return
        }
        check_thread = object : Thread() {
            override fun run() {
                Log.e("THD", "thread running")
                val toSend: NotificationData
                while (!notificationPossible) {
                    if (devices.size == 0) {
                        pendingNotifications.clear()
                        return
                    }
                }
                if (pendingNotifications.size < 1 || devices.size == 0) {
                    check_thread = null
                    return
                }
                toSend = pendingNotifications.removeAt(0)
                if (devices.contains(toSend.device)) {
                    /* Device could disconnect in the mean time */
                    toSend.characteristic.value = toSend.value
                    gattServer!!.notifyCharacteristicChanged(
                        toSend.device, toSend.characteristic,
                        toSend.responseNeeded
                    )
                }
                notificationPossible = false
            }
        }
        check_thread!!.start()
    }

    fun sendNotification(s: String) {
        sendNotification(s, SendTo.SEND_TO_ALL)
    }

    private fun sendNotification(s: String, st: SendTo) {
        for (i in 0 until s.length) {
            val c = s[i]
            for (j in KeyboardUsage.KEYBOARD_USAGES.indices) {

                var m: Byte
                var k: Byte
                if (Character.toLowerCase(c) == KeyboardUsage.KEYBOARD_USAGES[j].character) {
                    m = KeyboardUsage.KEYBOARD_USAGES[j].meta
                    k = KeyboardUsage.KEYBOARD_USAGES[j].usage
                    if (Character.isLetter(c) && Character.isUpperCase(c)) {
                        m = m or KeyboardUsage.META_LEFT_SHIFT
                    }
                    val value = (m + (k as Int shl 8)) as Short
                    Log.d("NOTIF", "sending here")
                    sendNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, value.toInt(), st)
                    sendNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0, st)
                    break
                }
            }
        }
        Log.d("NOTIF", "sending here too")
        sendNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0, st)
    }

    private fun startAdvertising() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        Log.i("ADV1", bluetoothAdapter.isLe2MPhySupported.toString())
        Log.i("ADV2", bluetoothAdapter.isLeCodedPhySupported.toString())
        Log.i("ADV3", bluetoothAdapter.isLeExtendedAdvertisingSupported.toString())
        Log.i("ADV4", bluetoothAdapter.isLePeriodicAdvertisingSupported.toString())
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()
        val advData = AdvertiseData.Builder()
            .setIncludeDeviceName(true) // Human Interface Device Service's UUID
            .addServiceUuid(ParcelUuid(UUID.fromString(UUIDs.SERVICE_HID))) // Device Information Service's UUID
            .addServiceUuid(ParcelUuid(UUID.fromString(UUIDs.SERVICE_DIS))) // Battery Service's UUID
            .addServiceUuid(ParcelUuid(UUID.fromString(UUIDs.SERVICE_BAS)))
            .build()

        //bluetoothAdapter.setName(deviceName);
        advertiser.startAdvertising(settings, advData, advertisingCallback)
    }

    private fun createGattDatabase(gattServer: BluetoothGattServer?, level: Int, features: Int) {
        // Set characteristics' permissions
        val PERM_READ =
            if (level == SecurityLevel.SECURITY_LEVEL_3.ordinal) BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM else if (level == SecurityLevel.SECURITY_LEVEL_2.ordinal) BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED else BluetoothGattCharacteristic.PERMISSION_READ
        val PERM_WRITE =
            if (level == SecurityLevel.SECURITY_LEVEL_3.ordinal) BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM else if (level == SecurityLevel.SECURITY_LEVEL_2.ordinal) BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED else BluetoothGattCharacteristic.PERMISSION_WRITE
        val PERM_READ_WRITE = PERM_READ or PERM_WRITE

        // Clear current attributes database - avoid adding the same services to database
        gattServer!!.clearServices()

        ////////////////////////////////////////////////////////////////////////////////////////////
        // HID Service
        //           -> Report Map
        //           -> HID Information
        //           -> HID Control Point
        //           -> Report
        //                   -> Client Characteristic Configuration
        //                   -> Report Reference
        //           ... (another Report characteristics and their descriptors)
        ////////////////////////////////////////////////////////////////////////////////////////////
        // Report Map - for basic mode
        val USAGE_PAGE = 0x05.toByte()
        val USAGE = 0x09.toByte()
        val COLLECTION = 0xA1.toByte()
        val REPORT_ID = 0x05.toByte()
        val END_COLLECTION = 0xC0.toByte()
        val LOGICAL_MIN = 0x15.toByte()
        val LOGICAL_MAX = 0x25.toByte()
        val REPORT_SIZE = 0x75.toByte()
        val REPORT_COUNT = 0x95.toByte()
        val INPUT = 0x81.toByte()
        val USAGE_MIN = 0x19.toByte()
        val USAGE_MAX = 0x29.toByte()
        val REPORT_MAP_DEFAULT_KEYBOARD = byteArrayOf(
            0x05.toByte(), 0x01.toByte(),  // USAGE_PAGE (Generic Desktop)
            0x09.toByte(), 0x06.toByte(),  // USAGE (Keyboard)
            0xa1.toByte(), 0x01.toByte(),  // COLLECTION (Application)
            0x85.toByte(), 0x02.toByte(),  /*        Report ID=2                         */
            0x05.toByte(), 0x07.toByte(),  //   USAGE_PAGE (Keyboard)
            0x19.toByte(), 0xe0.toByte(),  //   USAGE_MINIMUM (Keyboard LeftControl)
            0x29.toByte(), 0xe7.toByte(),  //   USAGE_MAXIMUM (Keyboard Right GUI)
            0x15.toByte(), 0x00.toByte(),  //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x01.toByte(),  //   LOGICAL_MAXIMUM (1)
            0x75.toByte(), 0x01.toByte(),  //   REPORT_SIZE (1)
            0x95.toByte(), 0x08.toByte(),  //   REPORT_COUNT (8)
            0x81.toByte(), 0x02.toByte(),  //   INPUT (Data,Var,Abs)
            0x95.toByte(), 0x01.toByte(),  //   REPORT_COUNT (1)
            0x75.toByte(), 0x08.toByte(),  //   REPORT_SIZE (8)
            0x81.toByte(), 0x03.toByte(),  //   INPUT (Cnst,Var,Abs)
            //                (byte) 0x95, (byte) 0x05,                    //   REPORT_COUNT (5)
            //                (byte) 0x75, (byte) 0x01,                    //   REPORT_SIZE (1)
            //                (byte) 0x05, (byte) 0x08,                    //   USAGE_PAGE (LEDs)
            //                (byte) 0x19, (byte) 0x01,                    //   USAGE_MINIMUM (Num Lock)
            //                (byte) 0x29, (byte) 0x05,                    //   USAGE_MAXIMUM (Kana)
            //                (byte) 0x91, (byte) 0x02,                    //   OUTPUT (Data,Var,Abs)
            //                (byte) 0x95, (byte) 0x01,                    //   REPORT_COUNT (1)
            //                (byte) 0x75, (byte) 0x03,                    //   REPORT_SIZE (3)
            //                (byte) 0x91, (byte) 0x03,                    //   OUTPUT (Cnst,Var,Abs)
            0x95.toByte(), 0x06.toByte(),  //   REPORT_COUNT (6)
            0x75.toByte(), 0x08.toByte(),  //   REPORT_SIZE (8)
            0x15.toByte(), 0x00.toByte(),  //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x65.toByte(),  //   LOGICAL_MAXIMUM (101)
            0x05.toByte(), 0x07.toByte(),  //   USAGE_PAGE (Keyboard)
            0x19.toByte(), 0x00.toByte(),  //   USAGE_MINIMUM (Reserved (no event indicated))
            0x29.toByte(), 0x65.toByte(),  //   USAGE_MAXIMUM (Keyboard Application)
            0x81.toByte(), 0x00.toByte(),  //   INPUT (Data,Ary,Abs)
            0xc0.toByte() // END_COLLECTION
        )
        val REPORT_MOUSE_DEFAULT = byteArrayOf(
            0x05.toByte(), 0x01.toByte(),  // USAGE_PAGE (Generic Desktop)
            0x09.toByte(), 0x02.toByte(),  // USAGE (Mouse)
            0xa1.toByte(), 0x01.toByte(),  // COLLECTION (Application)
            0x09.toByte(), 0x01.toByte(),  //   USAGE (Pointer)
            0xa1.toByte(), 0x00.toByte(),  //   COLLECTION (Physical)
            //                (byte) 0x85, (byte) 0x02, /*        Report ID=2                         */
            0x05.toByte(), 0x09.toByte(),  //     USAGE_PAGE (Button)
            0x19.toByte(), 0x01.toByte(),  //     USAGE_MINIMUM (Button 1)
            0x29.toByte(), 0x03.toByte(),  //     USAGE_MAXIMUM (Button 3)
            0x15.toByte(), 0x00.toByte(),  //     LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x01.toByte(),  //     LOGICAL_MAXIMUM (1)
            0x95.toByte(), 0x03.toByte(),  //     REPORT_COUNT (3)
            0x75.toByte(), 0x01.toByte(),  //     REPORT_SIZE (1)
            0x81.toByte(), 0x02.toByte(),  //     INPUT (Data,Var,Abs)
            0x95.toByte(), 0x01.toByte(),  //     REPORT_COUNT (1)
            0x75.toByte(), 0x05.toByte(),  //     REPORT_SIZE (5)
            0x81.toByte(), 0x03.toByte(),  //     INPUT (Cnst,Var,Abs)
            0x05.toByte(), 0x01.toByte(),  //     USAGE_PAGE (Generic Desktop)
            0x09.toByte(), 0x30.toByte(),  //     USAGE (X)
            0x09.toByte(), 0x31.toByte(),  //     USAGE (Y)
            0x15.toByte(), 0x81.toByte(),  //     LOGICAL_MINIMUM (-127)
            0x25.toByte(), 0x7f.toByte(),  //     LOGICAL_MAXIMUM (127)
            0x75.toByte(), 0x08.toByte(),  //     REPORT_SIZE (8)
            0x95.toByte(), 0x02.toByte(),  //     REPORT_COUNT (2)
            0x81.toByte(), 0x06.toByte(),  //     INPUT (Data,Var,Rel)
            0xc0.toByte(),  //   END_COLLECTION
            0xc0.toByte() // END_COLLECTION
        )
        val REPORT_MAP_RYAN = byteArrayOf(
            USAGE_PAGE, 0x01.toByte(),  // Usage Page (Generic Desktop)
            USAGE, 0x06.toByte(),  // Usage (Keyboard)
            COLLECTION, 0x01.toByte(),  // Collection (Application)
            COLLECTION, 0x00.toByte(),  //   Collection (Physical)
            REPORT_ID, 0x02.toByte(),  //   Report ID 2
            USAGE_PAGE, 0x07.toByte(),  //   Usage Page (Key Codes)
            USAGE_MIN, 0xE0.toByte(),
            USAGE_MAX, 0xE7.toByte(),
            LOGICAL_MIN, 0x00.toByte(),  //   Logical Minimum (0)
            LOGICAL_MAX, 0x01.toByte(),  //   Logical Maximum (1)
            REPORT_SIZE, 0x01.toByte(),  //   Report Size (1)
            REPORT_COUNT, 0x08.toByte(),  //   Report Count (8)
            //                USAGE, (byte) 0xE0,            //   Usage (LeftControl)
            //                USAGE, (byte) 0xE1,            //   Usage (LeftShift)
            //                USAGE, (byte) 0xE2,            //   Usage (LeftAlt)
            //                USAGE, (byte) 0xE3,            //   Usage (LeftGUI)
            //                USAGE, (byte) 0xE4,            //   Usage (RightControl)
            //                USAGE, (byte) 0xE5,            //   Usage (RightShift)
            //                USAGE, (byte) 0xE6,            //   Usage (RightAlt)
            //                USAGE, (byte) 0xE7,            //   Usage (RightGUI)
            INPUT, 0x02.toByte(),  //   Input (Data, Variable, Absolute)
            USAGE_PAGE, 0x07.toByte(),  //   Usage Page (Keyboard/Keypad)
            REPORT_COUNT, 0x01.toByte(),  //   Report Count (1)
            REPORT_SIZE, 0x08.toByte(),  //   Report Size (8)
            LOGICAL_MIN, 0x00.toByte(),  //   Logical Minimum (4) now 0
            LOGICAL_MAX, 0x65.toByte(),  //   Logical Maximum (223) now 101
            //                USAGE, 0x00,
            //                USAGE, 0x2c,
            USAGE_PAGE, 0x07.toByte(),  //   Usage Page (Key codes)
            USAGE_MIN, 0x00.toByte(),  //   Usage Minimum 0x04 (4) now 0
            USAGE_MAX, 0x65.toByte(),  //   Usage Maximum 0xDF (223) now 101
            INPUT, 0x00.toByte(),  //   Input 0x00 (Data, Array) now (Data, Variable, Absolute)
            END_COLLECTION,
            END_COLLECTION
        )
        val REPORT_MAP_BASIC = byteArrayOf(
            0x05.toByte(), 0x0C.toByte(),  /*        Usage Page (Consumer Devices)       */
            0x09.toByte(), 0x01.toByte(),  /*        Usage (Consumer Control)            */
            0xA1.toByte(), 0x01.toByte(),  /*        Collection (Application)            */
            0x85.toByte(), 0x02.toByte(),  /*        Report ID=2                         */
            0x05.toByte(), 0x0C.toByte(),  /*        Usage Page (Consumer Devices)       */
            0x15.toByte(), 0x00.toByte(),  /*        Logical Minimum (0)                 */
            0x25.toByte(), 0x01.toByte(),  /*        Logical Maximum (1)                 */
            0x75.toByte(), 0x01.toByte(),  /*        Report Size (1)                     */
            0x95.toByte(), 0x0B.toByte(),  /*        Report Count (11)                   */
            0x09.toByte(), 0x6F.toByte(),  /* 1       Usage (Bright Up)                  */
            0x09.toByte(), 0x70.toByte(),  /* 2       Usage (Bright Down)                */
            0x09.toByte(), 0xB5.toByte(),  /* 3       Usage (Scan Next Track)            */
            0x09.toByte(), 0xB6.toByte(),  /* 4       Usage (Scan Previous Track)        */
            0x09.toByte(), 0xB7.toByte(),  /* 5       Usage (Stop)                       */
            0x09.toByte(), 0xCD.toByte(),  /* 6       Usage (Play / Pause)               */
            0x09.toByte(), 0xE2.toByte(),  /* 7       Usage (Mute)                       */
            0x09.toByte(), 0xE9.toByte(),  /* 8       Usage (Volume Up)                  */
            0x09.toByte(), 0xEA.toByte(),  /* 9       Usage (Volume Down)                */
            0x09.toByte(), 0xB8.toByte(),  /* 10      Usage (Eject)                      */
            0x09.toByte(), 0xb8.toByte(),  /* 11      Usage (Snapshot)                   */
            0x81.toByte(), 0x02.toByte(),  /*        Input (Data, Variable, Absolute)    */
            0x95.toByte(), 0x05.toByte(),  /*        Report Count (5)                    */
            0x81.toByte(), 0x01.toByte(),  /*        Input (Constant)                    */
            0xC0.toByte()
        )

        // Report Map - concatenation of few arrays
        val REPORT_MAP_START = byteArrayOf(
            0x05.toByte(), 0x0C.toByte(),  /*        Usage Page (Consumer Devices)       */
            0x09.toByte(), 0x01.toByte(),  /*        Usage (Consumer Control)            */
            0xA1.toByte(), 0x01.toByte(),  /*        Collection (Application)            */
            0x85.toByte(), 0x02.toByte()
        )
        val REPORT_MAP_CONSUMER =
            if (features and ReportField.REP_CONSUMER == ReportField.REP_CONSUMER) byteArrayOf( /*========================== Consumer control ==========================*/
                0xA1.toByte(),
                0x02.toByte(),  /*        Collection (Logical)            */
                0x05.toByte(),
                0x0C.toByte(),  /*        Usage Page (Consumer Devices)       */
                0x15.toByte(),
                0x00.toByte(),  /*        Logical Minimum (0)                 */
                0x25.toByte(),
                0x01.toByte(),  /*        Logical Maximum (1)                 */
                0x75.toByte(),
                0x01.toByte(),  /*        Report Size (1)                     */
                0x95.toByte(),
                0x10.toByte(),  /*        Report Count (16)                   */
                0x09.toByte(),
                0x6F.toByte(),  /* 1       Usage (Bright Up)                  */
                0x09.toByte(),
                0x70.toByte(),  /* 2       Usage (Bright Down)                */
                0x09.toByte(),
                0xB5.toByte(),  /* 3       Usage (Scan Next Track)            */
                0x09.toByte(),
                0xB6.toByte(),  /* 4       Usage (Scan Previous Track)        */
                0x09.toByte(),
                0xB7.toByte(),  /* 5       Usage (Stop)                       */
                0x09.toByte(),
                0xCD.toByte(),  /* 6       Usage (Play / Pause)               */
                0x09.toByte(),
                0xE2.toByte(),  /* 7       Usage (Mute)                       */
                0x09.toByte(),
                0xE9.toByte(),  /* 8       Usage (Volume Up)                  */
                0x09.toByte(),
                0xEA.toByte(),  /* 9       Usage (Volume Down)                */
                0x09.toByte(),
                0xB8.toByte(),  /* 10      Usage (Eject)                      */
                0x09.toByte(),
                0x65.toByte(),  /* 11      Usage (Snapshot)                   */
                0x05.toByte(),
                0x01.toByte(),  /*        Usage Page (Generic Desktop)        */
                0x09.toByte(),
                0x82.toByte(),  /* 12      Usage (System Sleep)               */
                0x09.toByte(),
                0xA8.toByte(),  /* 13      Usage (System Hibernate)           */
                0x09.toByte(),
                0x81.toByte(),  /* 14      Usage (System Power Down)          */
                0x09.toByte(),
                0x8E.toByte(),  /* 15      Usage (System Cold Restart)        */
                0x09.toByte(),
                0x8F.toByte(),  /* 16      Usage (System Warm Restart)        */
                0x81.toByte(),
                0x02.toByte(),  /*        Input (Data, Variable, Absolute)    */ //                (byte) 0x95, (byte) 0x00, /*        Report Count (1)                    */
                //                (byte) 0x81, (byte) 0x01, /*        Input (Constant)                    */
                /*==================== Application Launcher Buttons ====================*/
                0x05.toByte(),
                0x0C.toByte(),  /* Usage Page (Consumer Devices) */
                0x95.toByte(),
                0x01.toByte(),  /* Report Count (1)              */
                0x75.toByte(),
                0x10.toByte(),  /* Report Size (16)              */
                0x16.toByte(),
                0x81.toByte(),
                0x01.toByte(),  /* Logical Minimum (385)         */
                0x26.toByte(),
                0xC7.toByte(),
                0x01.toByte(),  /* Logical Maximum (455)         */
                0x05.toByte(),
                0x0C.toByte(),  /* Usage Page (Consumer Devices) */
                0x1a.toByte(),
                0x81.toByte(),
                0x01.toByte(),  /* Usage Minimum (385)           */
                0x2a.toByte(),
                0xC7.toByte(),
                0x01.toByte(),  /* Usage Maximum (455)           */
                0x81.toByte(),
                0x00.toByte(),  /* Input (Data, Array)           */ /*==================== Application Control Buttons =====================*/
                0x05.toByte(),
                0x0C.toByte(),  /* Usage Page (Consumer Devices) */
                0x95.toByte(),
                0x01.toByte(),  /* Report Count (1)              */
                0x75.toByte(),
                0x10.toByte(),  /* Report Size (16)              */
                0x16.toByte(),
                0x01.toByte(),
                0x02.toByte(),  /* Logical Minimum (385)         */
                0x26.toByte(),
                0x9C.toByte(),
                0x02.toByte(),  /* Logical Maximum (455)         */
                0x05.toByte(),
                0x0C.toByte(),  /* Usage Page (Consumer Devices) */
                0x1a.toByte(),
                0x01.toByte(),
                0x02.toByte(),  /* Usage Minimum (385)           */
                0x2a.toByte(),
                0x9C.toByte(),
                0x02.toByte(),  /* Usage Maximum (455)           */
                0x81.toByte(),
                0x00.toByte(),  /* Input (Data, Array)           */
                0xC0.toByte()
            ) else byteArrayOf()
        val REPORT_MAP_KEYBOARD =
            if (features and ReportField.REP_KEYBOARD == ReportField.REP_KEYBOARD) byteArrayOf( /*============================== Keyboard ==============================*/
                0x05.toByte(), 0x07.toByte(),  /*        Usage Page (Keyboard/Keypad)        */
                0x15.toByte(), 0x00.toByte(),  /*        Logical Minimum (0)                 */
                0x25.toByte(), 0x01.toByte(),  /*        Logical Maximum (1)                 */
                0x75.toByte(), 0x01.toByte(),  /*        Report Size (1)                     */
                0x95.toByte(), 0x08.toByte(),  /*        Report Count (8)                    */
                0x09.toByte(), 0xE0.toByte(),  /* 1       Usage (LeftControl)                */
                0x09.toByte(), 0xE1.toByte(),  /* 2       Usage (LeftShift)                  */
                0x09.toByte(), 0xE2.toByte(),  /* 3       Usage (LeftAlt)                    */
                0x09.toByte(), 0xE3.toByte(),  /* 4       Usage (LeftGUI)                    */
                0x09.toByte(), 0xE4.toByte(),  /* 5       Usage (RightControl)               */
                0x09.toByte(), 0xE5.toByte(),  /* 6       Usage (RightShift)                 */
                0x09.toByte(), 0xE6.toByte(),  /* 7       Usage (RightAlt)                   */
                0x09.toByte(), 0xE7.toByte(),  /* 8       Usage (RightGUI)                   */
                0x81.toByte(), 0x02.toByte(),  /*        Input (Data, Variable, Absolute)    */
                0x05.toByte(), 0x07.toByte(),  /*        Usage Page (Keyboard/Keypad)        */
                0x95.toByte(), 0x01.toByte(),  /*        Report Count (1)                    */
                0x75.toByte(), 0x08.toByte(),  /*        Report Size (8)                     */
                0x15.toByte(), 0x04.toByte(),  /*        Logical Minimum (4)                 */
                0x25.toByte(), 0xDF.toByte(),  /*        Logical Maximum (223)               */
                0x05.toByte(), 0x07.toByte(),  /*        Usage Page (Key codes)              */
                0x19.toByte(), 0x04.toByte(),  /*        Usage Minimum (4)                   */
                0x29.toByte(), 0xDF.toByte(),  /*        Usage Maximum (223)                 */
                0x81.toByte(), 0x00.toByte()
            ) else byteArrayOf()
        val REPORT_MAP_MOUSE =
            if (features and ReportField.REP_MOUSE == ReportField.REP_MOUSE) byteArrayOf( /*================================ Mouse ===============================*/
                0x05.toByte(), 0x01.toByte(),  /*        Usage Page (Generic Desktop)        */
                0x09.toByte(), 0x02.toByte(),  /*        Usage (Mouse)                       */
                0xa1.toByte(), 0x01.toByte(),  /*         Collection (Application)            */
                0x85.toByte(), 0x02.toByte(),
                0x09.toByte(), 0x01.toByte(),  /*        Usage (Consumer Control)            */
                0xa1.toByte(), 0x00.toByte(),  /*        Collection (Physical)               */
                0x05.toByte(), 0x09.toByte(),  /*        Usage Page (Button)                 */
                0x19.toByte(), 0x01.toByte(),  /*        Usage Minimum (1)                   */
                0x29.toByte(), 0x05.toByte(),  /*        Usage Maximum (5)                   */
                0x15.toByte(), 0x00.toByte(),  /*        Logical Minimum (0)                 */
                0x25.toByte(), 0x01.toByte(),  /*        Logical Maximum (1)                 */
                0x95.toByte(), 0x05.toByte(),  /*        Report Count (5)                    */
                0x75.toByte(), 0x01.toByte(),  /*        Report Size (1)                     */
                0x81.toByte(), 0x02.toByte(),  /*        Input (Variable, Absolute)          */
                0x95.toByte(), 0x01.toByte(),  /*        Report Count (1)                    */
                0x75.toByte(), 0x03.toByte(),  /*        Report Size (3)                     */
                0x81.toByte(), 0x01.toByte(),  /*        Input (Constant)                    */
                0x05.toByte(), 0x01.toByte(),  /*        Usage Page (Generic Desktop)        */
                0x09.toByte(), 0x30.toByte(),  /*        Usage (X)                           */
                0x09.toByte(), 0x31.toByte(),  /*        Usage (Y)                           */
                0x15.toByte(), 0x81.toByte(),  /*        Logical Minimum (-127)              */
                0x25.toByte(), 0x7f.toByte(),  /*        Logical Maximum (127)               */
                0x75.toByte(), 0x08.toByte(),  /*        Report Size (8)                     */
                0x95.toByte(), 0x02.toByte(),  /*        Report Count (2)                    */
                0x81.toByte(), 0x06.toByte(),  /*        Input (Variable, Relative)          */
                0x09.toByte(), 0x38.toByte(),  /*        Usage (Wheel)                       */
                0x15.toByte(), 0x81.toByte(),  /*        Logical Minimum (-127)              */
                0x25.toByte(), 0x7f.toByte(),  /*        Logical Maximum (127)               */
                0x75.toByte(), 0x08.toByte(),  /*        Report Size (8)                     */
                0x95.toByte(), 0x01.toByte(),  /*        Report Count (1)                    */
                0x81.toByte(), 0x06.toByte(),  /*        Input (Variable, Relative)          */
                0xC0.toByte(),  /*       End Collection                       */
                0xC0.toByte()
            ) else byteArrayOf()
        val REPORT_MAP_END = byteArrayOf(
            0xC0.toByte()
        )
        val position = 0
        val REPORT_MAP: ByteArray


//        if ((features & ReportField.REP_BASIC) == ReportField.REP_BASIC) {
//            REPORT_MAP = REPORT_MAP_BASIC;
////            REPORT_MAP = REPORT_MAP_RYAN;
//        } else {
//            REPORT_MAP = new byte[REPORT_MAP_START.length + REPORT_MAP_CONSUMER.length +
//                    REPORT_MAP_KEYBOARD.length + REPORT_MAP_MOUSE.length + REPORT_MAP_END.length];
//
//            System.arraycopy(REPORT_MAP_START, 0, REPORT_MAP, position, REPORT_MAP_START.length);
//            position += REPORT_MAP_START.length;
//            System.arraycopy(REPORT_MAP_CONSUMER, 0, REPORT_MAP, position, REPORT_MAP_CONSUMER.length);
//            position += REPORT_MAP_CONSUMER.length;
//            System.arraycopy(REPORT_MAP_KEYBOARD, 0, REPORT_MAP, position, REPORT_MAP_KEYBOARD.length);
//            position += REPORT_MAP_KEYBOARD.length;
//            System.arraycopy(REPORT_MAP_MOUSE, 0, REPORT_MAP, position, REPORT_MAP_MOUSE.length);
//            position += REPORT_MAP_MOUSE.length;
//            System.arraycopy(REPORT_MAP_END, 0, REPORT_MAP, position, REPORT_MAP_END.length);
//        }
        REPORT_MAP = REPORT_MAP_DEFAULT_KEYBOARD

        // HID Service
        val serviceHid = BluetoothGattService(
            UUID.fromString(UUIDs.SERVICE_HID),
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // Report Map characteristic
        val charReportMap = BluetoothGattCharacteristic(
            UUID.fromString(UUIDs.CHAR_REPORT_MAP),
            BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ
        )
        charReportMap.value = REPORT_MAP

        // HID Information characteristic
        val charHidInformation = BluetoothGattCharacteristic(
            UUID.fromString(UUIDs.CHAR_HID_INFORMATION),
            BluetoothGattCharacteristic.PROPERTY_READ, PERM_READ
        )
        charHidInformation.value = byteArrayOf(0x01, 0x11, 0x00, 0x03)

        // HID Control Point characteristic
        val charHidControlPoint = BluetoothGattCharacteristic(
            UUID.fromString(UUIDs.CHAR_HID_CONTROL_POINT),
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, PERM_WRITE
        )
        charHidControlPoint.value = "\u0000".toByteArray()

        // Report characteristic
        val charReport1 = BluetoothGattCharacteristic(
            UUID.fromString(UUIDs.CHAR_REPORT),
            BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY, PERM_READ_WRITE
        )
        charReport1.value = "\u0000\u0000".toByteArray()

        /// Report Reference descriptor
        val descReportReference1 = BluetoothGattDescriptor(
            UUID.fromString(UUIDs.DESC_REPORT_REFERENCE), PERM_READ
        )
        descReportReference1.value = "\u0002\u0001".toByteArray()

        /// Client Characteristic Configuration descriptor
        val descCCC1 = BluetoothGattDescriptor(
            UUID.fromString(UUIDs.DESC_CCC), PERM_READ_WRITE
        )
        descCCC1.value = "\u0000\u0000".toByteArray()
        charReport1.addDescriptor(descReportReference1)
        charReport1.addDescriptor(descCCC1)
        serviceHid.addCharacteristic(charReportMap)
        serviceHid.addCharacteristic(charHidInformation)
        serviceHid.addCharacteristic(charHidControlPoint)
        serviceHid.addCharacteristic(charReport1)
        //        gattServer.addService(serviceHid);
        ////////////////////////////////////////////////////////////////////////////////////////////
        // Device Information Service
        //                          -> PnP ID
        ////////////////////////////////////////////////////////////////////////////////////////////
        val serviceDIS = BluetoothGattService(
            UUID.fromString(UUIDs.SERVICE_DIS),
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // PnP ID characteristic
        val charPnpId = BluetoothGattCharacteristic(
            UUID.fromString(UUIDs.CHAR_PNP_ID),
            BluetoothGattCharacteristic.PROPERTY_READ, PERM_READ
        )
        charPnpId.value = "\u0002\u0000\u0000\u0000\u0000\u0000\u0000".toByteArray()
        serviceDIS.addCharacteristic(charPnpId)
        //        gattServer.addService(serviceDIS);
        ////////////////////////////////////////////////////////////////////////////////////////////
        // Battery Service
        //               -> Battery Level
        ////////////////////////////////////////////////////////////////////////////////////////////
        val serviceBAS = BluetoothGattService(
            UUID.fromString(UUIDs.SERVICE_BAS),
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // PBattery Level characteristic
        val charBatteryLevel = BluetoothGattCharacteristic(
            UUID.fromString(UUIDs.CHAR_BATTERY_LEVEL),
            BluetoothGattCharacteristic.PROPERTY_READ, PERM_READ
        )
        charBatteryLevel.value = byteArrayOf(readBatteryLevel())
        serviceBAS.addCharacteristic(charBatteryLevel)
        //        gattServer.addService(serviceBAS);
        pendingServices.add(serviceBAS)
        pendingServices.add(serviceDIS)

        gattServer.addService(serviceHid)
    }

    private fun gattServerCbInit() {
        mGattServerCallback = object : BluetoothGattServerCallback() {
            private val DEFAULT_MTU = 23
            private var currentMtu = DEFAULT_MTU
            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                Log.e("BLE", "onConnectionStateChange $device $status $newState")
                if (newState == BluetoothAdapter.STATE_CONNECTED) {
                    currentMtu = DEFAULT_MTU
                    if (!devices.contains(device)) {
                        devices.add(device)
                    }
                    if (devices.size == 1) {
                        notificationPossible = true
                    }
                    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val advertiser = bluetoothAdapter.bluetoothLeAdvertiser
                    if (ApplicationConfiguration.getConfigurationField(
                            applicationContext,
                            ApplicationConfiguration.FORCE_BOND_FEAT
                        )
                    ) {
                        if (device.bondState != BluetoothDevice.BOND_BONDING &&
                            device.bondState != BluetoothDevice.BOND_BONDED
                        ) {
                            device.createBond()
                        } else if (device.bondState == BluetoothDevice.BOND_BONDING) {
                            device.setPairingConfirmation(true)
                        }
                    }
                } else if (newState == BluetoothAdapter.STATE_DISCONNECTED) {
                    if (devices.contains(device)) {
                        // If some notification waiting for this device then they will be removed in
                        // proper task
                        devices.remove(device)
                    }
                    if (devices.size == 0) {
                        notificationPossible = false
                    }
                }
                if (mainActivity != null) {
                    mainActivity!!.onConnectionStateChange(devices)
                }
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice, requestId: Int, offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
                val charLength = characteristic.value.size
                val partLength = if (currentMtu > charLength - offset) charLength - offset else currentMtu
                val bytes = ByteArray(partLength)
                if (characteristic.uuid.toString() == UUIDs.CHAR_BATTERY_LEVEL) {
                    characteristic.value = byteArrayOf(readBatteryLevel())
                }
                for (i in 0 until partLength) {
                    bytes[i] = characteristic.value[i + offset]
                }
                gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, bytes)
                Log.e(
                    "BLE", "onCharacteristicReadRequest " + characteristic.uuid.toString() +
                            " offset " + offset + " " + String(bytes)
                )
            }

            override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
                currentMtu = mtu
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice, requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean, responseNeeded: Boolean,
                offset: Int, value: ByteArray
            ) {
                Log.e(
                    "BLE", "onDescriptorWriteRequest " + descriptor.uuid.toString() + "value"
                            + value[0] + value[1]
                )
                descriptor.value = value
                gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice, requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean, responseNeeded: Boolean,
                offset: Int, value: ByteArray
            ) {
                Log.e(
                    "BLE", "onCharacteristicWriteRequest " + characteristic.uuid.toString() +
                            " offset " + offset
                )
                characteristic.value = value
                gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
            }

            override fun onDescriptorReadRequest(
                device: BluetoothDevice, requestId: Int,
                offset: Int, descriptor: BluetoothGattDescriptor
            ) {
                Log.e("BLE", "onDescriptorReadRequest " + descriptor.uuid.toString())
                gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, descriptor.value)
            }

            override fun onNotificationSent(device: BluetoothDevice, status: Int) {
                Log.e("BLE", "onNotificationSent - left: " + pendingNotifications.size)
                if (pendingNotifications.size > 0) {
                    val toSend = pendingNotifications.removeAt(0)
                    toSend.characteristic.value = toSend.value
                    gattServer!!.notifyCharacteristicChanged(
                        toSend.device, toSend.characteristic,
                        toSend.responseNeeded
                    )
                    notificationPossible = false
                } else {
                    notificationPossible = true
                }
            }

            override fun onServiceAdded(status: Int, service: BluetoothGattService) {
                Log.e("BLE", "Pending services: " + pendingServices.size.toString())
                super.onServiceAdded(status, service)
                serviceList.add(service.uuid.toString())
                if (pendingServices.size == 0) {
                    Log.i("BLE", "All services added")
                    for (s in serviceList) {
                        Log.i("BLE", s!!)
                    }
                    startAdvertising()
                    println("ADvertising")
                } else {
                    Log.w("BLE", "Not all services added yet.")
                    Log.w("BLE", pendingServices.toString())
                    //                    if (pendingServices.size() > 0) {
                    gattServer!!.addService(pendingServices.removeAt(0))
                    //                    }
                }
            }
        }
    }

    fun setActivity(ma: MainActivity?) {
        mainActivity = ma
    }

    /* Service handling-related methods */
    fun initializeLE() {
        val mManager: BluetoothManager
        var features = 0
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            BluetoothAdapter.getDefaultAdapter().enable()
            while (true) {
                if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
                    break
                }
            }
            //TODO: Add timeout
        }
        if (ApplicationConfiguration.getConfigurationField(
                applicationContext,
                ApplicationConfiguration.CONSUMER_FEAT
            )
        ) {
            features = features or ReportField.REP_CONSUMER
        }
        if (ApplicationConfiguration.getConfigurationField(
                applicationContext,
                ApplicationConfiguration.MOUSE_FEAT
            )
        ) {
            features = features or ReportField.REP_MOUSE
        }
        if (ApplicationConfiguration.getConfigurationField(
                applicationContext,
                ApplicationConfiguration.KEYBOARD_FEAT
            )
        ) {
            features = features or ReportField.REP_KEYBOARD
        }
        if (ApplicationConfiguration.getConfigurationField(
                applicationContext,
                ApplicationConfiguration.BASIC_FEAT
            )
        ) {
            features = features or ReportField.REP_BASIC
        }
        ReportField.updateValues(features)
        gattServerCbInit()
        mManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        gattServer = mManager.openGattServer(baseContext, mGattServerCallback)
        println("gatt")
        println(gattServer)
        createGattDatabase(gattServer, SecurityLevel.SECURITY_LEVEL_2.ordinal, features)
        pendingNotifications.clear()
    }

    override fun onCreate() {
        super.onCreate()
        initializeLE()
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        mainActivity = null
        return false
    }

    inner class LocalBinder : Binder() {
        val service: HidBleService
            get() = this@HidBleService
    }

    companion object {
        private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
        fun bytesToHex(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2)
            for (j in bytes.indices) {
                val v: Int = bytes[j].toInt() and 0xFF
                hexChars[j * 2] = HEX_ARRAY[v ushr 4]
                hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
            }
            return String(hexChars)
        }
    }
}