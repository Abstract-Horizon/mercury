package org.abstracthorizon.mercury.maildir.scala

import java.io.File
import java.util.Random
import java.net.InetAddress

import javax.mail._
import javax.mail.internet._

import org.abstracthorizon.mercury.maildir.scala.util._

object MaildirMessage {
    
    
    /** Flags (info) separator */
    val FLAGS_SEPERATOR = "2,"

    /** Random number generator */
    val randomGenerator = new Random()

    /** Number of retries when creating new file */
    val CREATE_FILE_RETRIES = 6
    
    /** Host name cache */
    private var h:String = "localhost"
    try {
        h = InetAddress.getLocalHost().getHostName()
    } catch {
        case _ =>
    }
    
    val host = h
    
    def apply(folder: MaildirFolder, msgNumber: Int, file: File) = new MaildirMessage(folder, msgNumber, file, true)
    def apply(folder: MaildirFolder, msgNumber: Int, file: File, initialise: Boolean) = new MaildirMessage(folder, msgNumber, file, initialise)
    
    def apply(folder: MaildirFolder, message: MimeMessage, msgNumber: Boolean) = {
        
    }
    
}

class MaildirMessage(folder: MaildirFolder, val msgNumber: Int, var file: File, initialise: Boolean) extends LazyParsingMessage(folder, msgNumber) {

    /** Message's base name, name without info (flags) */
    var baseName = ""

    /** Cached info separator */
    val infoSeparator = folder.maildirStore.infoSeparator

    /** Cached file size or -1 */
    var fileSize: Long = -1

    /** Flag to show is file in <i>new</i> subdirectory or not */
    var isNew = true


    if (!file.exists()) {
        throw new MessagingException("File not found; "+file.getAbsolutePath());
    }

        setFile(file);

//        if (initialise) {
//            initialise();
//        }    
//        createFile(null);
//        storeMessage(message);
//        setFlags(message.getFlags(), true);
//        initialise();

        
    def setFile(file: File) = {
        this.file =  file
        
        val parentFile = file.getParentFile()
        isNew = parentFile.getAbsolutePath().endsWith(File.pathSeparator + "new")

        baseName = file.getName()
        val i = baseName.lastIndexOf(MaildirMessage.FLAGS_SEPERATOR)

        var flags: Flags = null;
        if (i > 0) {
            val flagsStr = baseName.substring(i+2);
            baseName = baseName.substring(0, i-1);
            flags = FlagUtilities.fromMaildirString(flagsStr);
        } else {
            flags = new Flags();
        }
        if (isNew) {
            flags.add(Flags.Flag.RECENT);
        }

        super.setFlags(flags, true);

        fileSize = file.length();    }
}