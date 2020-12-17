package pcp.com.bttemperature.database;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

public class CSVWriter {
    public static final char DEFAULT_ESCAPE_CHARACTER = '\"';
    public static final String DEFAULT_LINE_END = "\n";
    public static final char DEFAULT_QUOTE_CHARACTER = '\"';
    public static final char DEFAULT_SEPARATOR = ',';
    public static final char NO_ESCAPE_CHARACTER = 0;
    public static final char NO_QUOTE_CHARACTER = 0;
    private char escapechar;
    private String lineEnd;

    /* renamed from: pw */
    private PrintWriter f16pw;
    private char quotechar;
    private char separator;

    public CSVWriter(Writer writer) {
        this(writer, DEFAULT_SEPARATOR, '\"', '\"', DEFAULT_LINE_END);
    }

    public CSVWriter(Writer writer, char separator2, char quotechar2, char escapechar2, String lineEnd2) {
        this.f16pw = new PrintWriter(writer);
        this.separator = separator2;
        this.quotechar = quotechar2;
        this.escapechar = escapechar2;
        this.lineEnd = lineEnd2;
    }

    public void writeNext(String[] nextLine) {
        if (nextLine != null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < nextLine.length; i++) {
                if (i != 0) {
                    sb.append(this.separator);
                }
                String nextElement = nextLine[i];
                if (nextElement != null) {
                    if (this.quotechar != 0) {
                        sb.append(this.quotechar);
                    }
                    for (int j = 0; j < nextElement.length(); j++) {
                        char nextChar = nextElement.charAt(j);
                        if (this.escapechar != 0 && nextChar == this.quotechar) {
                            sb.append(this.escapechar).append(nextChar);
                        } else if (this.escapechar == 0 || nextChar != this.escapechar) {
                            sb.append(nextChar);
                        } else {
                            sb.append(this.escapechar).append(nextChar);
                        }
                    }
                    if (this.quotechar != 0) {
                        sb.append(this.quotechar);
                    }
                }
            }
            sb.append(this.lineEnd);
            this.f16pw.write(sb.toString());
        }
    }

    public void flush() throws IOException {
        this.f16pw.flush();
    }

    public void close() throws IOException {
        this.f16pw.flush();
        this.f16pw.close();
    }
}
