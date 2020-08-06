package org.abstracthorizon.mercury.maildir.scala.util

import javax.mail.Flags;

object FlagUtilities {
    /**
     * Creates maildir string from given flags.
     * <ul>
     * <li>&quot;D&quot; for DRAFT</li>
     * <li>&quot;F&quot; for FLAGGED</li>
     * <li>&quot;R&quot; for ANSWERED</li>
     * <li>&quot;S&quot; for SEEN</li>
     * <li>&quot;T&quot; for DELETED</li>
     * </ul>
     *
     * @param flags flags to be converted
     * @return string in maildir format
     */
    def toMaildirString(flags: Flags): String = {
        if (flags == null) {
            return ""
        }
        val buf = new StringBuffer()
        if (flags.contains(javax.mail.Flags.Flag.DRAFT)) {
            buf.append('D')
        }
        if (flags.contains(javax.mail.Flags.Flag.FLAGGED)) {
            buf.append('F')
        }
        if (flags.contains(javax.mail.Flags.Flag.ANSWERED)) {
            buf.append('R')
        }
        if (flags.contains(javax.mail.Flags.Flag.SEEN)) {
            buf.append('S')
        }
        if (flags.contains(javax.mail.Flags.Flag.DELETED)) {
            buf.append('T')
        }
        return buf.toString()
    }

    /**
     * Creates set of flags from maildir flags string.
     * <ul>
     * <li>For &quot;D&quot; adds DRAFT</li>
     * <li>For &quot;F&quot; adds FLAGGED</li>
     * <li>For &quot;R&quot; adds ANSWERED</li>
     * <li>For &quot;S&quot; adds SEEN</li>
     * <li>For &quot;T&quot; adds DELETED</li>
     * </ul>
     * @param str maildir flags string
     * @return flags set
     */
    def fromMaildirString(str: String): Flags = {
        val flags = new Flags()
        val len = str.length()
        
        if ((str == null) || (len == 0)) {
            return flags
        }
        for (i <- 0 to (len - 1)) {
            val c = Character.toUpperCase(str.charAt(i))
            if (c == 'D') {
                flags.add(Flags.Flag.DRAFT)
            } else if (c == 'F') {
                flags.add(Flags.Flag.FLAGGED)
            } else if (c == 'R') {
                flags.add(Flags.Flag.ANSWERED)
            } else if (c == 'S') {
                flags.add(Flags.Flag.SEEN)
            } else if (c == 'T') {
                flags.add(Flags.Flag.DELETED)
            }
        }
        return flags
    }
    
    def main(args: Array[String]) = {
        println("Hello world 1")
    }

}

class FlagUtilities {

    def main(args: Array[String]) = {
        println("Hello world 2")
    }
    
}