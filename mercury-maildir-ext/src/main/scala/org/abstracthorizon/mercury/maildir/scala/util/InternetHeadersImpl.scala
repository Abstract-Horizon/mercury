package org.abstracthorizon.mercury.maildir.scala.util

import java.io.IOException;
import java.io.InputStream;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

class InternetHeadersImpl(inputStream: InputStream) extends InternetHeaders(inputStream) {

	// def InternetHeadersImpl = super

    /**
     * Loads headers from the input stream
     * @param inputStream input stream
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def load(inputStream: InputStream) = {

		var ss: String = null
        val stringbuffer = new StringBuffer()

        var run = true

        while (run) {
            try {
                val s = readLine(inputStream)
                
                if ((s != null) && (s.startsWith(" ") || s.startsWith("\t"))) {
                    if (ss != null) {
                        stringbuffer.append(ss)
                        ss = null
                    }
                    stringbuffer.append("\r\n")
                    stringbuffer.append(s)
                } else {
                    if (ss != null) {
                        addHeaderLine(ss)
                    } else {
                        if (stringbuffer.length() > 0) {
                            addHeaderLine(stringbuffer.toString())
                            stringbuffer.setLength(0)
                        }
                    }
                    ss = s
                }

                if (s == null) {
                    run = false
                } else {
                    if (s.length() <= 0) {
                        run = false
                    }
                }
            } catch {
            	case e: IOException => throw new MessagingException("Error in input stream", e)
            }
        }
    }

    /**
     * This method reads a line from the input stream
     * @param in input stream
     * @return new string representing the read line
     * @throws IOException
     */
    @throws(classOf[IOException])
    def readLine(in: InputStream): String = {
        var line = new Array[Byte](1024)
        var l = 0
        var maybeEOL = false

        while (true) {
            val c = in.read()

            var k = -1

            if (c == -1) {
                if (l == 0) {
                    return ""
                } else {
                    return new String(line, 0, l, "ISO-8859-1")
                }
            } else if (c == 13) {
                if (maybeEOL) {
                    k = c
                } else {
                    maybeEOL = true
                }
            } else if (c == 10) {
                //if (maybeEOL) {
                    return new String(line, 0, l)
                //} else {
                //    k = c;
                //    maybeEOL = false;
                //}
            } else {
                k = c
                maybeEOL = false
            }
            if (k != -1) {
                if (l == line.length) {
                    val linet = new Array[Byte](line.length*2)
                    System.arraycopy(line, 0, linet, 0, line.length)
                    line = linet
                }
                line(l) = k.toByte
                l = l + 1
            }
        }
        ""
    }
}
