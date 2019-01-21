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

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraDialog;
import org.acra.annotation.AcraMailSender;
import org.acra.data.StringFormat;

import vit01.idecmobile.prefs.Config;

@AcraCore(buildConfigClass = BuildConfig.class,
        reportFormat = StringFormat.KEY_VALUE_LIST)
@AcraMailSender(mailTo = "me@ii-net.tk",
        reportAsFile = false)
@AcraDialog(reportDialogClass = vit01.idecmobile.bugreports.CrashReportActivity.class)
public class ApplicationMain extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // Initialize bug reports
        ACRA.init(this);

        // Грузим некоторые строки с переводами
        Strings.initStrings(base);

        // Подгружаем наш любимый конфиг
        Config.loadConfig(this);
    }
}