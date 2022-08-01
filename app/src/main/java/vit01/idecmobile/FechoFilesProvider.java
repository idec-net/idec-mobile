/*
 * Copyright (c) 2016-2022 Viktor Fedenyov <me@alicorn.tk> <https://alicorn.tk>
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

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.webkit.MimeTypeMap;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.ExternalStorage;
import vit01.idecmobile.Core.FEchoFile;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.SimpleFunctions;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class FechoFilesProvider extends DocumentsProvider {
    private final static String[] DEFAULT_ROOT_PROJECTION =
            new String[]{
                    DocumentsContract.Root.COLUMN_ROOT_ID,
                    DocumentsContract.Root.COLUMN_ICON, DocumentsContract.Root.COLUMN_TITLE,
                    DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.COLUMN_DOCUMENT_ID};

    private final static String[] DEFAULT_ENTRY_PROJECTION =
            new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_FLAGS, DocumentsContract.Document.COLUMN_LAST_MODIFIED};

    @Override
    public Cursor queryRoots(String[] strings) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(strings != null ?
                strings : DEFAULT_ROOT_PROJECTION);

        MatrixCursor.RowBuilder row = result.newRow();

        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, "vit01.idecmobile");
        row.add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher);
        row.add(DocumentsContract.Root.COLUMN_TITLE, "IDEC Mobile");
        row.add(DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.FLAG_LOCAL_ONLY);
        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, "vit01.idecmobile.root");
        return result;
    }

    @Override
    public Cursor queryDocument(String id, String[] strings) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(strings != null ?
                strings : DEFAULT_ENTRY_PROJECTION);

        AbstractTransport transport = GlobalTransport.transport(getContext());

        if (id.equals("vit01.idecmobile.root")) {
            MatrixCursor.RowBuilder row = result.newRow();

            row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, "vit01.idecmobile.root");
            row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, "IDEC files");
            row.add(DocumentsContract.Document.COLUMN_FLAGS, null);
            row.add(DocumentsContract.Document.COLUMN_MIME_TYPE,
                    android.provider.DocumentsContract.Document.MIME_TYPE_DIR);
            row.add(DocumentsContract.Document.COLUMN_SIZE, null);
            row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, null);
        }

        if (id.startsWith("fecho://")) {
            id = id.substring(8);

            queryDir(result, id);
        } else if (id.startsWith("idecfile://")) {
            id = id.substring(11);

            queryFile(transport, result, id);
        }

        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parent_id, String[] strings, String s1) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(strings != null ?
                strings : DEFAULT_ENTRY_PROJECTION);

        AbstractTransport transport = GlobalTransport.transport(getContext());

        if (parent_id.equals("vit01.idecmobile.root")) {
            for (String fecho : transport.fullFEchoList()) {
                queryDir(result, fecho);
            }
        } else if (parent_id.startsWith("fecho://")) {
            parent_id = parent_id.substring(8);
            ArrayList<String> ids = transport.getFileList(parent_id, 0, 0, "number");

            for (String fid : ids) {
                queryFile(transport, result, fid);
            }
        }
        return result;
    }

    public MatrixCursor.RowBuilder queryDir(MatrixCursor result, String fecho) {
        MatrixCursor.RowBuilder row = result.newRow();

        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, "fecho://" + fecho);
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, fecho);
        row.add(DocumentsContract.Document.COLUMN_FLAGS, null);
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE,
                android.provider.DocumentsContract.Document.MIME_TYPE_DIR);
        row.add(DocumentsContract.Document.COLUMN_SIZE, null);
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, null);

        return row;
    }

    public MatrixCursor.RowBuilder queryFile(AbstractTransport transport,
                                             MatrixCursor result, String id) {

        FEchoFile file_entry = transport.getFileMeta(id);
        String filename = file_entry.filename;

        if (!filename.contains(".") || !file_entry.existsLocally()) return null;
        else {
            MatrixCursor.RowBuilder row = result.newRow();

            int dotindex = filename.lastIndexOf(".") + 1;
            String extension = filename.substring(dotindex).toLowerCase();
            String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

            row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, "idecfile://" + id);
            row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, filename);
            row.add(DocumentsContract.Document.COLUMN_FLAGS, null);
            row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, mimetype);
            row.add(DocumentsContract.Document.COLUMN_SIZE, file_entry.serverSize);
            row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, null);

        }
        return null;
    }

    @Override
    public ParcelFileDescriptor openDocument(String fid, String mode,
                                             CancellationSignal cancellationSignal) throws FileNotFoundException {
        AbstractTransport transport = GlobalTransport.transport(getContext());
        fid = fid.substring(11);

        File file = transport.getFileMeta(fid).getLocalFile();

        SimpleFunctions.debug("query " + file.getAbsolutePath());

        return ParcelFileDescriptor.open(file,
                ParcelFileDescriptor.parseMode(mode));
    }

    @Override
    public boolean onCreate() {
        ExternalStorage.initStorage();
        return false;
    }
}
