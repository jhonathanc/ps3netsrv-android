package android.provider;
import android.net.Uri;
public class DocumentsContract {
  public static String getDocumentId(Uri uri) { return uri.toString(); }
  public static Uri buildChildDocumentsUriUsingTree(Uri uri,String id) { return Uri.parse(id); }
  public static Uri buildDocumentUriUsingTree(Uri uri,String id) { return Uri.parse(id); }
  public static class Document {
    public static final String COLUMN_DOCUMENT_ID="id", COLUMN_DISPLAY_NAME="name",
      COLUMN_MIME_TYPE="mime", COLUMN_SIZE="size", MIME_TYPE_DIR="directory";
  }
}
