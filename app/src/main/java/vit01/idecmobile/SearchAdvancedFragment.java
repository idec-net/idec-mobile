/*
 * Copyright (c) 2016-2017 Viktor Fedenyov <me@ii-net.tk> <https://ii-net.tk>
 *
 * This file is part of IDEC Mobile.
 *
 * IDEC Mobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IDEC Mobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IDEC Mobile.  If not, see <http://www.gnu.org/licenses/>.
 */

package vit01.idecmobile;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TextInputEditText;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import vit01.idecmobile.Core.SimpleFunctions;

public class SearchAdvancedFragment extends BottomSheetDialogFragment {
    String DATE_EMPTY = "00-00-0000";
    String DATE_FORMAT = "dd-MM-yyyy";

    Button advSearchButton, date1, date2;
    boolean showButton = true;

    String subjKey = null, echoareas = null, senders = null, receivers = null,
            addresses = null, time1_string = null, time2_string = null;
    boolean is_favorite = false;

    TextInputEditText edit_echoareas, edit_senders, edit_receivers, edit_addresses, edit_subj;
    CheckBox is_favorite_checkbox;
    private android.support.design.widget.BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    public SearchAdvancedFragment() {
    }

    public static SearchAdvancedFragment newInstance() {
        return new SearchAdvancedFragment();
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View v = View.inflate(getContext(), R.layout.content_search, null);

        final Calendar c = Calendar.getInstance();
        final int mYear = c.get(Calendar.YEAR);
        final int mMonth = c.get(Calendar.MONTH);
        final int mDay = c.get(Calendar.DAY_OF_MONTH);

        date1 = (Button) v.findViewById(R.id.search_advanced_date_first);
        date2 = (Button) v.findViewById(R.id.search_advanced_date_second);

        View.OnClickListener dateSelect = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TextView tv = (TextView) v;

                DatePickerDialog dpd = new DatePickerDialog(getActivity(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {
                                tv.setText(
                                        ((dayOfMonth < 10) ? "0" : "") + dayOfMonth
                                                + "-" + ((monthOfYear < 9) ? "0" : "") + (monthOfYear + 1)
                                                + "-" + year);
                            }
                        }, mYear, mMonth, mDay);
                dpd.show();
            }
        };

        date1.setOnClickListener(dateSelect);
        date2.setOnClickListener(dateSelect);

        int buttonColor = SimpleFunctions.colorFromTheme(getActivity(), android.R.attr.textColorPrimary);
        advSearchButton = (Button) v.findViewById(R.id.search_advanced_start_search);
        advSearchButton.setCompoundDrawablesWithIntrinsicBounds(new IconicsDrawable(getContext(), GoogleMaterial.Icon.gmd_search)
                .sizeDp(24).color(buttonColor), null, null, null);

        if (!showButton) advSearchButton.setVisibility(View.GONE);

        advSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getData();
                getActivity().triggerSearch("___query_empty", new Bundle());
            }
        });

        ImageView clearDateButton = (ImageView) v.findViewById(R.id.search_advanced_date_clear);
        clearDateButton.setImageDrawable(new IconicsDrawable(getContext(), GoogleMaterial.Icon.gmd_close)
                .sizeDp(24).color(buttonColor));

        clearDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                date1.setText(DATE_EMPTY);
                date2.setText(DATE_EMPTY);
            }
        });

        edit_echoareas = (TextInputEditText) v.findViewById(R.id.search_advanced_echoareas);
        edit_senders = (TextInputEditText) v.findViewById(R.id.search_advanced_senders);
        edit_receivers = (TextInputEditText) v.findViewById(R.id.search_advanced_receivers);
        edit_addresses = (TextInputEditText) v.findViewById(R.id.search_advanced_addresses);
        edit_subj = (TextInputEditText) v.findViewById(R.id.search_advanced_subj);
        is_favorite_checkbox = (CheckBox) v.findViewById(R.id.search_advanced_favorites);

        dialog.setContentView(v);

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) v.getParent()).getLayoutParams();
        BottomSheetBehavior behavior = (BottomSheetBehavior) params.getBehavior();
        if (behavior != null) behavior.setBottomSheetCallback(mBottomSheetBehaviorCallback);

        setData();
    }

    public Bundle getDataBundle() {
        Bundle bundle = new Bundle();
        if (subjKey != null && subjKey.equals("")) subjKey = null;

        bundle.putString("subj", subjKey);

        bundle.putStringArrayList("echoareas", splitToList(echoareas));
        bundle.putStringArrayList("senders", splitToList(senders));
        bundle.putStringArrayList("receivers", splitToList(receivers));
        bundle.putStringArrayList("addresses", splitToList(addresses));

        Long[] timekeys = getTimeKeys(time1_string, time2_string);
        bundle.putSerializable("time1", timekeys[0]);
        bundle.putSerializable("time2", timekeys[1]);
        bundle.putBoolean("is_favorite", is_favorite);

        return bundle;
    }

    public void setData() {
        edit_subj.setText((subjKey != null) ? subjKey : "");
        edit_echoareas.setText((echoareas != null) ? echoareas : "");
        edit_senders.setText((senders != null) ? senders : "");
        edit_receivers.setText((receivers != null) ? receivers : "");
        edit_addresses.setText((echoareas != null) ? addresses : "");

        date1.setText((time1_string != null) ? time1_string : DATE_EMPTY);
        date2.setText((time2_string != null) ? time2_string : DATE_EMPTY);

        is_favorite_checkbox.setChecked(is_favorite);
    }

    public void getData() {
        subjKey = edit_subj.getText().toString();
        echoareas = edit_echoareas.getText().toString();
        senders = edit_senders.getText().toString();
        receivers = edit_receivers.getText().toString();
        addresses = edit_addresses.getText().toString();

        time1_string = date1.getText().toString();
        time2_string = date2.getText().toString();

        is_favorite = is_favorite_checkbox.isChecked();
    }

    Long[] getTimeKeys(String date1, String date2) {
        Long keys[] = new Long[2];

        if (date1 == null || date2 == null) {
            keys[0] = null;
            keys[1] = null;
            return keys;
        }

        if (date1.equals(DATE_EMPTY) && date2.equals(DATE_EMPTY)) {
            keys[0] = null;
            keys[1] = null;
            return keys;
        }

        Calendar date1_calendar, date2_calendar;
        date1_calendar = dateParse(date1);
        date2_calendar = dateParse(date2);

        if (date1_calendar.getTime().getTime() == date2_calendar.getTime().getTime()) {
            date2_calendar.add(Calendar.DATE, 1);
        } else if (date2_calendar.getTime().getTime() < date1_calendar.getTime().getTime()) {
            // меняем местами
            Calendar tmp = date1_calendar;
            date1_calendar = date2_calendar;
            date2_calendar = tmp;
        }

        keys[0] = date1_calendar.getTime().getTime() / 1000;
        keys[1] = date2_calendar.getTime().getTime() / 1000;

        return keys;
    }

    Calendar dateParse(String date) {
        Calendar c = Calendar.getInstance();
        try {
            if (date.equals(DATE_EMPTY)) {
                c.setTime(new Date(0));
            } else {
                Date dateParsed = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).parse(date);
                c.setTime(dateParsed);
            }
        } catch (ParseException e) {
            SimpleFunctions.debug(e.getMessage());
            e.printStackTrace();
            c.setTime(new Date(0));
        }
        return c;
    }

    ArrayList<String> splitToList(String str) {
        if (str == null || TextUtils.isEmpty(str)) return SimpleFunctions.emptyList;

        if (!str.contains("||")) return new ArrayList<>(Collections.singletonList(str));
        String[] keys = str.split("||");
        ArrayList<String> result = new ArrayList<>();
        Collections.addAll(result, keys);
        if (result.size() > 0) return result;
        else return SimpleFunctions.emptyList;
    }

    @Override
    public void dismiss() {
        getData();
        super.dismiss();
    }
}