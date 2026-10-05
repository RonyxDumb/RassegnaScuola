package com.ronyxdumb.rassegnascuola;
import android.content.*;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
final class AndroidDocuments {
    static Uri save(Context context, NewsEngine.Result result) throws Exception {
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HHmmss", Locale.ITALY);
        date.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
        ContentValues v = new ContentValues();
        v.put(MediaStore.Downloads.DISPLAY_NAME, "Rassegna_" + result.sector.key + "_" + date.format(new Date()) + ".docx");
        v.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RassegnaScuola");
        v.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
        if (uri == null) throw new java.io.IOException("Impossibile creare il documento.");
        try {
            try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new java.io.IOException("Impossibile salvare il documento.");
                out.write(DocxExporter.build(result.items, result.notes, result.sector));
            }
            v.clear(); v.put(MediaStore.Downloads.IS_PENDING, 0); context.getContentResolver().update(uri,v,null,null); return uri;
        } catch(Exception e) { context.getContentResolver().delete(uri,null,null); throw e; }
    }
}
