package pcp.com.bttemperature.parse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/* access modifiers changed from: package-private */
public class ParseFileUtils {
    private static final long FILE_COPY_BUFFER_SIZE = 31457280;
    public static final long ONE_KB = 1024;
    public static final long ONE_MB = 1048576;

    ParseFileUtils() {
    }

    public static byte[] readFileToByteArray(File file) throws IOException {
        InputStream in = null;
        try {
            in = openInputStream(file);
            return ParseIOUtils.toByteArray(in);
        } finally {
            ParseIOUtils.closeQuietly(in);
        }
    }

    public static FileInputStream openInputStream(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File '" + file + "' does not exist");
        } else if (file.isDirectory()) {
            throw new IOException("File '" + file + "' exists but is a directory");
        } else if (file.canRead()) {
            return new FileInputStream(file);
        } else {
            throw new IOException("File '" + file + "' cannot be read");
        }
    }

    public static void writeByteArrayToFile(File file, byte[] data) throws IOException {
        OutputStream out = null;
        try {
            out = openOutputStream(file);
            out.write(data);
        } finally {
            ParseIOUtils.closeQuietly(out);
        }
    }

    public static FileOutputStream openOutputStream(File file) throws IOException {
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("File '" + file + "' could not be created");
            }
        } else if (file.isDirectory()) {
            throw new IOException("File '" + file + "' exists but is a directory");
        } else if (!file.canWrite()) {
            throw new IOException("File '" + file + "' cannot be written to");
        }
        return new FileOutputStream(file);
    }

    public static void moveFile(File srcFile, File destFile) throws IOException {
        if (srcFile == null) {
            throw new NullPointerException("Source must not be null");
        } else if (destFile == null) {
            throw new NullPointerException("Destination must not be null");
        } else if (!srcFile.exists()) {
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        } else if (srcFile.isDirectory()) {
            throw new IOException("Source '" + srcFile + "' is a directory");
        } else if (destFile.exists()) {
            throw new IOException("Destination '" + destFile + "' already exists");
        } else if (destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' is a directory");
        } else if (!srcFile.renameTo(destFile)) {
            copyFile(srcFile, destFile);
            if (!srcFile.delete()) {
                deleteQuietly(destFile);
                throw new IOException("Failed to delete original file '" + srcFile + "' after copy to '" + destFile + "'");
            }
        }
    }

    public static void copyFile(File srcFile, File destFile) throws IOException {
        copyFile(srcFile, destFile, true);
    }

    public static void copyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
        if (srcFile == null) {
            throw new NullPointerException("Source must not be null");
        } else if (destFile == null) {
            throw new NullPointerException("Destination must not be null");
        } else if (!srcFile.exists()) {
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        } else if (srcFile.isDirectory()) {
            throw new IOException("Source '" + srcFile + "' exists but is a directory");
        } else if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
            throw new IOException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
        } else {
            File parentFile = destFile.getParentFile();
            if (parentFile != null && !parentFile.mkdirs() && !parentFile.isDirectory()) {
                throw new IOException("Destination '" + parentFile + "' directory cannot be created");
            } else if (!destFile.exists() || destFile.canWrite()) {
                doCopyFile(srcFile, destFile, preserveFileDate);
            } else {
                throw new IOException("Destination '" + destFile + "' exists but is read-only");
            }
        }
    }

    private static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
        Throwable th;
        FileOutputStream fos;
        long count;
        if (!destFile.exists() || !destFile.isDirectory()) {
            FileInputStream fis = null;
            FileOutputStream fos2 = null;
            try {
                FileInputStream fis2 = new FileInputStream(srcFile);
                try {
                    fos = new FileOutputStream(destFile);
                } catch (Throwable th2) {
                    th = th2;
                    fis = fis2;
                    ParseIOUtils.closeQuietly((Closeable) null);
                    ParseIOUtils.closeQuietly((OutputStream) fos2);
                    ParseIOUtils.closeQuietly((Closeable) null);
                    ParseIOUtils.closeQuietly((InputStream) fis);
                    throw th;
                }
                try {
                    FileChannel input = fis2.getChannel();
                    FileChannel output = fos.getChannel();
                    long size = input.size();
                    long pos = 0;
                    while (pos < size) {
                        long remain = size - pos;
                        if (remain > FILE_COPY_BUFFER_SIZE) {
                            count = FILE_COPY_BUFFER_SIZE;
                        } else {
                            count = remain;
                        }
                        long bytesCopied = output.transferFrom(input, pos, count);
                        if (bytesCopied == 0) {
                            break;
                        }
                        pos += bytesCopied;
                    }
                    ParseIOUtils.closeQuietly(output);
                    ParseIOUtils.closeQuietly((OutputStream) fos);
                    ParseIOUtils.closeQuietly(input);
                    ParseIOUtils.closeQuietly((InputStream) fis2);
                    long srcLen = srcFile.length();
                    long dstLen = destFile.length();
                    if (srcLen != dstLen) {
                        throw new IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile + "' Expected length: " + srcLen + " Actual: " + dstLen);
                    } else if (preserveFileDate) {
                        destFile.setLastModified(srcFile.lastModified());
                    }
                } catch (Throwable th3) {
                    th = th3;
                    fos2 = fos;
                    fis = fis2;
                    ParseIOUtils.closeQuietly((Closeable) null);
                    ParseIOUtils.closeQuietly((OutputStream) fos2);
                    ParseIOUtils.closeQuietly((Closeable) null);
                    ParseIOUtils.closeQuietly((InputStream) fis);
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                ParseIOUtils.closeQuietly((Closeable) null);
                ParseIOUtils.closeQuietly((OutputStream) fos2);
                ParseIOUtils.closeQuietly((Closeable) null);
                ParseIOUtils.closeQuietly((InputStream) fis);
                //throw th;     //Modify by Joey
            }
        } else {
            throw new IOException("Destination '" + destFile + "' exists but is a directory");
        }
    }

    public static void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            if (!isSymlink(directory)) {
                cleanDirectory(directory);
            }
            if (!directory.delete()) {
                throw new IOException("Unable to delete directory " + directory + ".");
            }
        }
    }

    public static boolean deleteQuietly(File file) {
        if (file == null) {
            return false;
        }
        try {
            if (file.isDirectory()) {
                cleanDirectory(file);
            }
        } catch (Exception e) {
        }
        try {
            return file.delete();
        } catch (Exception e2) {
            return false;
        }
    }

    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            throw new IllegalArgumentException(directory + " does not exist");
        } else if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory + " is not a directory");
        } else {
            File[] files = directory.listFiles();
            if (files == null) {
                throw new IOException("Failed to list contents of " + directory);
            }
            IOException exception = null;
            for (File file : files) {
                try {
                    forceDelete(file);
                } catch (IOException ioe) {
                    exception = ioe;
                }
            }
            if (exception != null) {
                throw exception;
            }
        }
    }

    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
            return;
        }
        boolean filePresent = file.exists();
        if (file.delete()) {
            return;
        }
        if (!filePresent) {
            throw new FileNotFoundException("File does not exist: " + file);
        }
        throw new IOException("Unable to delete file: " + file);
    }

    public static boolean isSymlink(File file) throws IOException {
        File fileInCanonicalDir;
        if (file == null) {
            throw new NullPointerException("File must not be null");
        }
        if (file.getParent() == null) {
            fileInCanonicalDir = file;
        } else {
            fileInCanonicalDir = new File(file.getParentFile().getCanonicalFile(), file.getName());
        }
        if (fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile())) {
            return false;
        }
        return true;
    }

    public static String readFileToString(File file, Charset encoding) throws IOException {
        return new String(readFileToByteArray(file), encoding);
    }

    public static String readFileToString(File file, String encoding) throws IOException {
        return readFileToString(file, Charset.forName(encoding));
    }

    public static void writeStringToFile(File file, String string, Charset encoding) throws IOException {
        writeByteArrayToFile(file, string.getBytes(encoding));
    }

    public static void writeStringToFile(File file, String string, String encoding) throws IOException {
        writeStringToFile(file, string, Charset.forName(encoding));
    }

    public static JSONObject readFileToJSONObject(File file) throws IOException, JSONException {
        return new JSONObject(readFileToString(file, "UTF-8"));
    }

    public static void writeJSONObjectToFile(File file, JSONObject json) throws IOException {
        writeByteArrayToFile(file, json.toString().getBytes(Charset.forName("UTF-8")));
    }
}
