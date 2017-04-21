package harps.swanuniemailclient;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @file AttachmentsProvider.java
 * @author Ed Harper
 * @date 13/04/2017
 *
 * Content provider for attachments to allow URI access to private cache folder.
 */

public class AttachmentsProvider extends ContentProvider{
    @Override
    public boolean onCreate() {
        return false;
    }

    /**
     * The open file parcer. Takes passed uri and adds it to the cache directory.
     * Opens file using provider
     * @see <AndroidManifest.xml/.AttachmentsProvider>
     * @see <http://stackoverflow.com/questions/40703407/android-intent-action-send-from-internal-storage-with-content-provider>
     * @param uri
     * @param mode
     * @return
     * @throws FileNotFoundException
     */
    public ParcelFileDescriptor openFile(Uri uri, String mode)throws FileNotFoundException{
        File file = new File(getContext().getCacheDir(), uri.getPath());
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
