package org.abstracthorizon.mercury.maildir.scala

import java.io.File
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;


class MaildirFolderData(store: MaildirStore, base: File) {

    /**
     * This constant defines a filename of a zero length
     * flag file that denotes no subfolders are suppose
     * to be created for this folder */
    val NO_SUBFOLDERS_FILENAME = ".nosubfolders"

	var lastAccessed = System.currentTimeMillis


    /** Permanent flags for root are user defined &quot;\\Noselect&quot; */
    val rootPermanentFlags = new Flags("\\Noselect")

    /** Permanent flags cache */
    protected var permanentFlags: Flags = null

    /** Flag to denote is this root folder or not */
    protected val isRootFolder = base.equals(store.base);

    /** Type of the folder. See {@link javax.mail.Folder#HOLDS_FOLDERS} and {@link javax.mail.Folder#HOLDS_MESSAGES}. */
    protected [scala] var typ = -1;

    /** Tmp subdirectory */
    protected val tmp: File = new File(base, "tmp")

    /** Cur subdirectory */
    protected val cur: File = new File(base, "cur")

    /** New subdirectory */
    protected val nw: File = new File(base, "new")

    /** Amount of time between two accesses. TODO - make this as an attribute */
    protected val delay = 1000

    /** Delay factor - amount of time needed for reading directory vs delay. TODO - make this as an attribute */
    protected val delayFactor = 3

    /** List of open folders */
    protected val openedFolders = new WeakHashMap[Folder, Any]

    /** Folder's data */
    protected var data: Data = null;

    /** Weak reference to data when there are no open folders */
    protected val closedRef: Reference[Data] = null

    /** Count of open folders. When count reaches zero, storage may remove this folder data */
    protected var openCount = 0
    
    protected def extractName = {
        if (isRootFolder) {
            ""
        } else {
	    	var n = base.getName()
	        var i = n.lastIndexOf('.')
	        if (i > 0) {
	            n = n.substring(i+1);
	        }
	        if (store.isLeadingDot && n.startsWith(".")) {
	            n = n.substring(1);
	        }
	        n
        }
    }

    val name = extractName

    
    /**
     * Returns folder's full name (path and name)
     * @return folder's full name
     */
    protected def extractFullName = {
        if (isRootFolder) {
            ""
        } else {
            var name = base.getName();
            if (store.isLeadingDot && name.startsWith(".")) {
                name = name.substring(1);
            }
            name.replace('.', '/')
        }
    }

    val fullName = extractFullName

    def exists = base.exists
    
}

/**
 * Folders data class. This class keeps list of messages and map for files, messages pair.
 * It is moved to separate class just for easier handling of moving instance of this class
 * under weak reference regime when there are no open folders. That means garbage collector
 * can more easily remove not used messages.
 */
protected class Data {

	/** Messages in this directory (folder(s)) */
    protected var messages: List[MaildirMessage] = null

    /** Map from files to message objects */
    protected var files: HashMap[String, MaildirMessage]  = null
}
