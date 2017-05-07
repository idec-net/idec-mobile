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

package vit01.idecmobile.bugreports;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.acra.ACRAConstants;
import org.acra.ReportField;
import org.acra.collections.ImmutableSet;
import org.acra.collector.CrashReportData;
import org.acra.config.ACRAConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

public class CrashReportSender implements ReportSender {
    private final ACRAConfiguration config;

    public CrashReportSender(ACRAConfiguration config) {
        this.config = config;
    }

    public void send(@NonNull Context context, @NonNull CrashReportData errorContent)
            throws ReportSenderException {
        Intent emailIntent = new Intent("android.intent.action.SENDTO");
        emailIntent.setData(Uri.fromParts("mailto", this.config.mailTo(), null));
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String[] subjectBody = this.buildSubjectBody(context, errorContent);
        emailIntent.putExtra("android.intent.extra.SUBJECT", subjectBody[0]);
        emailIntent.putExtra("android.intent.extra.TEXT", subjectBody[1]);
        context.startActivity(emailIntent);
    }

    private String[] buildSubjectBody(Context context, CrashReportData errorContent) {
        ImmutableSet fields = this.config.getReportFields();
        if (fields.isEmpty()) {
            fields = new ImmutableSet<>(ACRAConstants.DEFAULT_MAIL_REPORT_FIELDS);
        }

        String subject = context.getPackageName() + " Crash Report";
        StringBuilder builder = new StringBuilder();

        for (Object field1 : fields) {
            ReportField field = (ReportField) field1;
            builder.append(field.toString()).append('=');
            builder.append(errorContent.get(field));
            builder.append('\n');
            if ("STACK_TRACE".equals(field.toString())) {
                String stackTrace = errorContent.get(field).toString();
                if (stackTrace != null) {
                    subject = context.getPackageName() + ": "
                            + stackTrace.substring(0, stackTrace.indexOf('\n'));
                    if (subject.length() > 72) {
                        subject = subject.substring(0, 72);
                    }
                }
            }
        }

        return new String[]{subject, builder.toString()};
    }
}