package org.abstracthorizon.mercury.common.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import javax.mail.internet.SharedInputStream;

public class FileSharedInputStream extends InputStream implements SharedInputStream {

    public static FileSharedInputStream newInstance(File file) {
        return new FileSharedInputStream(file, 0, -1);
    }
    
    private long start;
    private long position;
    private long end;
    private File file;
    private RandomAccessFile raf;

    protected FileSharedInputStream(File file, long start, long end) {
        this.file = file;
        this.start = start;
        this.position = start;
        this.end = end;
    }
    
    @Override
    public long getPosition() {
        return position;
    }

    @Override
    public InputStream newStream(long pos, long end) {
        pos = this.start + pos;
        if (end < 0) {
            return new FileSharedInputStream(file, pos, this.end);
        } else {
            end = this.start + end;
            return new FileSharedInputStream(file, pos, end);
        }
    }

    @Override
    public int read() throws IOException {
        checkOpen();
        int r = raf.read();
        position = position + 1;
        if (position >= end) {
            close();
        }
        return r;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkOpen();
        if (len + position > end) {
            len = (int)(end - position);
        }
        int r = raf.read(b, off, len);
        position = position + r;
        if (r == 0) {
            r = -1;
        }
        return r;
    }
    
    @Override
    public int available() throws IOException {
        checkOpen();
        return (int)(end - position);
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
    
    @Override
    public long skip(long n) throws IOException {
        checkOpen();
        if (position + n > end) {
            n = end - position;
        }
        n = raf.skipBytes((int)n);
        position = position + n;
        if (position >= end) {
            close();
        }
        return n;
    }

    protected void checkOpen() throws IOException {
        synchronized (this) {
            if (raf == null) {
                raf = new RandomAccessFile(file, "r");
                if (end == -1) {
                    end = raf.length();
                }
                if (position != 0) {
                    raf.seek(position);
                }
            }
        }
    }
}
