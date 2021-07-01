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

internal object UUIDs {
    // HID related UUIDs
    const val SERVICE_HID = "00001812-0000-1000-8000-00805F9B34FB"
    const val CHAR_REPORT = "00002A4D-0000-1000-8000-00805F9B34FB"

    //    <!--Report Map-->
    //    <characteristic id="report_map" name="Report Map" sourceId="org.bluetooth.characteristic.report_map" uuid="2A4B">
    //      <informativeText>Summary:  The Report Map characteristic is used to define formatting information for Input Report, Output Report, and Feature Report data transferred between a HID Device and HID Host, information on how this data can be used, and other information regarding physical aspects of the device (i.e. that the device functions as a keyboard, for example, or has multiple functions such as a keyboard and volume controls).       Only a single instance of this characteristic exists as part of a HID Service.  </informativeText>
    //      <value length="45" type="hex" variable_length="false">05010906A101050719E029E71500250175019508810295017508810195057501050819012905910295017503910195067508150025650507190029658100C0</value>
    //      <properties encrypted_read="true" encrypted_read_requirement="mandatory" indicate="false" indicate_requirement="excluded" notify="false" notify_requirement="excluded" read="true" read_requirement="mandatory" reliable_write="false" reliable_write_requirement="excluded" write="false" write_no_response="false" write_no_response_requirement="excluded" write_requirement="excluded"/>
    //      <!--External Report Reference-->
    //      <descriptor id="external_report_reference" name="External Report Reference" sourceId="org.bluetooth.descriptor.external_report_reference" uuid="2907">
    //        <properties read="true" read_requirement="mandatory" write="false" write_requirement="excluded"/>
    //        <value length="2" type="hex" variable_length="false"/>
    //      </descriptor>
    //    </characteristic>
    const val CHAR_REPORT_MAP = "00002A4B-0000-1000-8000-00805F9B34FB"
    const val CHAR_HID_INFORMATION = "00002A4A-0000-1000-8000-00805F9B34FB"
    const val CHAR_HID_CONTROL_POINT = "00002A4C-0000-1000-8000-00805F9B34FB"
    const val DESC_REPORT_REFERENCE = "00002908-0000-1000-8000-00805F9B34FB"
    const val DESC_CCC = "00002902-0000-1000-8000-00805F9B34FB"

    // DIS related UUIDs
    const val SERVICE_DIS = "0000180A-0000-1000-8000-00805F9B34FB"
    const val CHAR_PNP_ID = "00002A50-0000-1000-8000-00805F9B34FB"

    // BAS related UUIDs
    const val SERVICE_BAS = "0000180F-0000-1000-8000-00805F9B34FB"
    const val CHAR_BATTERY_LEVEL = "00002A19-0000-1000-8000-00805F9B34FB"
}