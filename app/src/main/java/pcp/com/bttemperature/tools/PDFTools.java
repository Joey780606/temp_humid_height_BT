package pcp.com.bttemperature.tools;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import java.io.File;

import pcp.com.bttemperature.R;

public class PDFTools {
    private static final String GOOGLE_DRIVE_PDF_READER_PREFIX = "http://drive.google.com/viewer?url=";
    private static final String HTML_MIME_TYPE = "text/html";
    private static final String PDF_MIME_TYPE = "application/pdf";

    public static void showPDFUrl(Context context, String pdfUrl, ProgressDialog progress) {
        if (isPDFSupported(context)) {
            downloadAndOpenPDF(context, pdfUrl, progress);
            return;
        }
        progress.dismiss();
        askToOpenPDFThroughGoogleDrive(context, pdfUrl);
    }

    @TargetApi(9)
    public static void downloadAndOpenPDF(Context context, String pdfUrl, final ProgressDialog progress) {
        String filename = pdfUrl.substring(pdfUrl.lastIndexOf("/") + 1);
        final File tempFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename);
        if (tempFile.exists()) {
            tempFile.delete();
        }
        DownloadManager.Request r = new DownloadManager.Request(Uri.parse(pdfUrl));
        r.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, filename);
        final DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        context.registerReceiver(new BroadcastReceiver() {
            /* class com.celaer.android.ambient.tools.PDFTools.C03021 */

            public void onReceive(Context context, Intent intent) {
                if (progress.isShowing()) {
                    context.unregisterReceiver(this);
                    progress.dismiss();
                    long downloadId = intent.getLongExtra("extra_download_id", -1);
                    Cursor c = dm.query(new DownloadManager.Query().setFilterById(downloadId));
                    if (c.moveToFirst() && c.getInt(c.getColumnIndex("status")) == 8) {
                        PDFTools.openPDF(context, Uri.fromFile(tempFile));
                    }
                    c.close();
                }
            }
        }, new IntentFilter("android.intent.action.DOWNLOAD_COMPLETE"));
        dm.enqueue(r);
    }

    public static void askToOpenPDFThroughGoogleDrive(final Context context, final String pdfUrl) {
        new AlertDialog.Builder(context).setTitle(R.string.pdf_show_online_dialog_title).setMessage(R.string.pdf_show_online_dialog_question).setNegativeButton(R.string.no, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            /* class com.celaer.android.ambient.tools.PDFTools.DialogInterface$OnClickListenerC03032 */

            public void onClick(DialogInterface dialog, int which) {
                PDFTools.openPDFThroughGoogleDrive(context, pdfUrl);
            }
        }).show();
    }

    public static void openPDFThroughGoogleDrive(Context context, String pdfUrl) {
        Intent i = new Intent("android.intent.action.VIEW");
        i.setDataAndType(Uri.parse(GOOGLE_DRIVE_PDF_READER_PREFIX + pdfUrl), HTML_MIME_TYPE);
        context.startActivity(i);
    }

    public static final void openPDF(Context context, Uri localUri) {
        Intent i = new Intent("android.intent.action.VIEW");
        i.setDataAndType(localUri, PDF_MIME_TYPE);
        context.startActivity(i);
    }

    public static boolean isPDFSupported(Context context) {
        Intent i = new Intent("android.intent.action.VIEW");
        i.setDataAndType(Uri.fromFile(new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "test.pdf")), PDF_MIME_TYPE);
        return context.getPackageManager().queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }
}
