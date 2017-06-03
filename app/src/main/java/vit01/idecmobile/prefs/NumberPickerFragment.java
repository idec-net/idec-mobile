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

package vit01.idecmobile.prefs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

public class NumberPickerFragment extends PreferenceDialogFragmentCompat {
    NumberPickerPreference targetPreference;
    NumberPicker picker;

    public static NumberPickerFragment newInstance(NumberPickerPreference preference) {
        NumberPickerFragment fragment = new NumberPickerFragment();
        Bundle bundle = new Bundle(1);
        bundle.putString("key", preference.getKey());
        fragment.setArguments(bundle);
        fragment.targetPreference = preference;

        return fragment;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            picker.clearFocus();
            int newValue = picker.getValue();
            if (targetPreference.callChangeListener(newValue)) {
                targetPreference.setValue(newValue);
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstance) {
        picker = new NumberPicker(this.getContext());
        picker.setMinValue(targetPreference.minValue);
        picker.setMaxValue(targetPreference.maxValue);
        picker.setValue(targetPreference.getValue());
        picker.setWrapSelectorWheel(targetPreference.wrapSelectorWheel);
        picker.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        final LinearLayout linearLayout = new LinearLayout(this.getContext());
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.addView(picker);

        return new AlertDialog.Builder(getContext())
                .setMessage(targetPreference.getDialogMessage())
                .setTitle(targetPreference.getDialogTitle())
                .setView(linearLayout)
                .create();
    }
}
