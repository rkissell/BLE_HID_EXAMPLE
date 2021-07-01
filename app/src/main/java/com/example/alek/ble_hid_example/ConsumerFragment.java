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
package com.example.alek.ble_hid_example;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.view.View.INVISIBLE;

public class ConsumerFragment extends Fragment implements Button.OnTouchListener,
        AdapterView.OnItemSelectedListener {

    int testVal = 0;
    Thread testThread;
    boolean testThreadRun = true;

    public static ConsumerFragment newInstance() {
        return new ConsumerFragment();
    }


    private void setOnTouchListenerForEach(LinearLayout layout) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View v = layout.getChildAt(i);

            if (v instanceof LinearLayout) {
                setOnTouchListenerForEach((LinearLayout) v);
            } else if (v instanceof ImageButton) {
                v.setOnTouchListener(this);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.consumer_fragment, container, false);

        ConsumerControlUsage.bindButtonId(R.id.cons_button_bright_plus,
                ConsumerControlUsage.BRIGHT_UP);
        ConsumerControlUsage.bindButtonId(R.id.cons_button_bright_minus,
                ConsumerControlUsage.BRIGHT_DOWN);
        ConsumerControlUsage.bindButtonId(R.id.cons_button_next,
                ConsumerControlUsage.SCAN_NEXT_TRACK);
        ConsumerControlUsage.bindButtonId(R.id.cons_button_previous,
                ConsumerControlUsage.SCAN_PREVIOUS_TRACK);
        ConsumerControlUsage.bindButtonId(R.id.cons_button_stop,
                ConsumerControlUsage.STOP);
        ConsumerControlUsage.bindButtonId(R.id.cons_button_start_stop,
                ConsumerControlUsage.PLAY_PAUSE);
        ConsumerControlUsage.bindButtonId(R.id.cons_button_mute,
                ConsumerControlUsage.MUTE);
        ConsumerControlUsage.bindButtonId(R.id.cons_button_vol_plus,
                ConsumerControlUsage.VOLUME_UP);
        ConsumerControlUsage.bindButtonId(R.id.cons_button_vol_minus,
                ConsumerControlUsage.VOLUME_DOWN);
        ConsumerControlUsage.bindButtonId(R.id.cons_button_eject,
                ConsumerControlUsage.EJECT);
        ConsumerControlUsage.bindButtonId(R.id.cons_button_snapshot,
                ConsumerControlUsage.SNAPSHOT);
        ConsumerControlUsage.bindButtonId(R.id.cons_button_system_sleep,
                ConsumerControlUsage.SYSTEM_SLEEP);
        ConsumerControlUsage.bindButtonId(R.id.cons_button_system_hibernation,
                ConsumerControlUsage.SYSTEM_HIBERNATE);
        ConsumerControlUsage.bindButtonId(R.id.cons_button_system_power_down,
                ConsumerControlUsage.SYSTEM_POWER_DOWN);
        ConsumerControlUsage.bindButtonId(R.id.cons_button_system_restart,
                ConsumerControlUsage.SYSTEM_WARM_RESTART);

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        setOnTouchListenerForEach((LinearLayout) getActivity().findViewById(R.id.cons_sub_main_layout));

        // Application launch spinner configuration
        List<String> lal = new ArrayList<>();
        lal.add("SELECT VALUE");
        lal.addAll(ApplicationLaunchButtonsUsage.getUsageNames());

        Spinner spinner_al = (Spinner) getActivity().findViewById(R.id.cons_spinner_al_hidden);
        spinner_al.setOnItemSelectedListener(this);
        ArrayAdapter<String> dataAdapter_al = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, lal);
        dataAdapter_al.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_al.setAdapter(dataAdapter_al);
        spinner_al.setSelection(0);

        // Application control spinner configuration
        List<String> lac = new ArrayList<>();
        lac.add("SELECT VALUE");
        lac.addAll(ApplicationControlUsage.getUsageNames());

        Spinner spinner_ac = (Spinner) getActivity().findViewById(R.id.cons_spinner_ac_hidden);
        spinner_ac.setOnItemSelectedListener(this);
        ArrayAdapter<String> dataAdapter_ac = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, lac);
        dataAdapter_ac.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_ac.setAdapter(dataAdapter_ac);
        spinner_ac.setSelection(0);

        if (ApplicationConfiguration.getConfigurationField(getContext(),
                ApplicationConfiguration.BASIC_FEAT)) {
            getActivity().findViewById(R.id.cons_system_text_hidden).setVisibility(INVISIBLE);
            getActivity().findViewById(R.id.cons_system_button_layout_hidden).setVisibility(INVISIBLE);
            getActivity().findViewById(R.id.cons_launch_text_hidden).setVisibility(INVISIBLE);
            getActivity().findViewById(R.id.cons_control_text_hidden).setVisibility(INVISIBLE);
            getActivity().findViewById(R.id.cons_spinner_al_hidden).setVisibility(INVISIBLE);
            getActivity().findViewById(R.id.cons_spinner_ac_hidden).setVisibility(INVISIBLE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Spinner spinner_al = (Spinner) getActivity().findViewById(R.id.cons_spinner_al_hidden);
        Spinner spinner_ac = (Spinner) getActivity().findViewById(R.id.cons_spinner_ac_hidden);

        spinner_al.setSelection(0);
        spinner_ac.setSelection(0);
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int toSend = 0;
        int value = ConsumerControlUsage.getUsage(v.getId());

        Log.d("CONSUMER", "value: " + String.valueOf(value));
        final MainActivity activity = (MainActivity) getActivity();

        if (value == 0) {
            return false;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            testVal+=256;
//            value=testVal;
//            activity.sendNotification(ReportField.REPORT_FIELD_CONSUMER_CONTROL, new byte[] {0, 0x05, 0x05});
//            activity.sendNotification(ReportField.REPORT_FIELD_CONSUMER_CONTROL, new byte[] {0, 0, 0});
            if (testThread == null) {
                testThread = new Thread() {

                    public void sendBytes(byte[] bytes) throws InterruptedException {
                        byte [] empty = new byte[bytes.length];
                        Arrays.fill(empty, (byte) 0);
                        Log.d("BYTES", bytesToHex(bytes));
                        activity.sendNotification(ReportField.REPORT_FIELD_CONSUMER_CONTROL, bytes);
//                        sleep(500);
                        activity.sendNotification(ReportField.REPORT_FIELD_CONSUMER_CONTROL, empty);
//                        sleep(500);
                    }

                    @Override
                    public void run() {
                        int ct = 0;
                        try {
                            sendBytes(new byte[]{
                                    (byte) 0x02,
                                    (byte) 0x00,
                                    (byte) 0x0b,
                                    (byte) 0x00,
                                    (byte) 0x00,
                                    (byte) 0x00,
                                    (byte) 0x00,
                                    (byte) 0x00,
                            });
                            sendBytes(new byte[]{
                                    (byte) 0x00,
                                    (byte) 0x00,
                                    (byte) 0x08,
                                    (byte) 0x0f,
                                    (byte) 0x00,
                                    (byte) 0x0f,
                                    (byte) 0x12,
                                    (byte) 0x2c,
                            });
                            sendBytes(new byte[]{
                                    (byte) 0x02,
                                    (byte) 0x00,
                                    (byte) 0x07,
                                    (byte) 0x00,
                                    (byte) 0x00,
                                    (byte) 0x00,
                                    (byte) 0x00,
                                    (byte) 0x00,
                            });
                            sendBytes(new byte[]{
                                    (byte) 0x00,
                                    (byte) 0x00,
                                    (byte) 0x15,
                                    (byte) 0x37,
                                    (byte) 0x2c,
                                    (byte) 0x00,
                                    (byte) 0x00,
                                    (byte) 0x00,
                            });
                            sendBytes(new byte[]{
                                    (byte) 0x02,
                                    (byte) 0x00,
                                    (byte) 0x10,
                                    (byte) 0x00,
                                    (byte) 0x00,
                                    (byte) 0x00,
                                    (byte) 0x00,
                                    (byte) 0x00,
                            });
                            sendBytes(new byte[]{
                                    (byte) 0x00,
                                    (byte) 0x00,
                                    (byte) 0x0c,
                                    (byte) 0x10,
                                    (byte) 0x0c,
                                    (byte) 0x28,
                                    (byte) 0x00,
                                    (byte) 0x00,
                            });

//                            for (int i = 101; i > 3; i--) {
//                                for (int j = 101; j > 3; j--) {
//                                    if (testThreadRun) {
//                                        byte[] bytes = new byte[]{
//                                                (byte) 0x00,
//                                                (byte) 0x00,
//                                                (byte) 0x08,
//                                                (byte) 0x00,
//                                                (byte) 0x00,
//                                                (byte) 0x00,
//                                                (byte) 0x00,
//                                                (byte) 0x00,
//                                        };
//                                        byte [] empty = new byte[bytes.length];
//                                        Arrays.fill(empty, (byte) 0);
//                                        Log.d("BYTES", bytesToHex(bytes));
//                                        activity.sendNotification(ReportField.REPORT_FIELD_CONSUMER_CONTROL, bytes);
//                                        sleep(500);
//                                        activity.sendNotification(ReportField.REPORT_FIELD_CONSUMER_CONTROL, empty);
//                                        sleep(500);
//                                    }
//                                }
//                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                testThread.start();
            } else {
                testThreadRun = false;
                testThread = null;
            }


            toSend |= value;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            toSend &= ~value;
            toSend &= 0xffff;
        } else {
            return false;
        }
//        Log.e("CONSUMER", "toSend: " + String.valueOf(toSend));
//        activity.sendNotification(ReportField.REPORT_FIELD_CONSUMER_CONTROL, new byte[] {0x00, 0x01});
//        activity.sendNotification(ReportField.REPORT_FIELD_CONSUMER_CONTROL, new byte[] {0x00, 0x00});

        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        MainActivity activity = (MainActivity) getActivity();
        short value;

        if (parent.getChildAt(0) != null) {
            ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);

            switch (parent.getId()) {
                case R.id.cons_spinner_al_hidden:
                    if (position == 0) {
                        /* Dummy item */
                        return;
                    }

                    value = ApplicationLaunchButtonsUsage.AL_USAGES[position - 1].usage;
                    activity.sendNotification(ReportField.REPORT_FIELD_LAUNCHER_BUTTON, (int) value);
                    activity.sendNotification(ReportField.REPORT_FIELD_LAUNCHER_BUTTON, 0);
                    break;
                case R.id.cons_spinner_ac_hidden:
                    if (position == 0) {
                        /* Dummy item */
                        return;
                    }

                    value = ApplicationControlUsage.AC_USAGES[position - 1].usage;
                    activity.sendNotification(ReportField.REPORT_FIELD_CONTROL_BUTTON, (int) value);
                    activity.sendNotification(ReportField.REPORT_FIELD_CONTROL_BUTTON, 0);
                    break;
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        if (parent.getChildAt(0) != null) {
            ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
        }
    }
}
