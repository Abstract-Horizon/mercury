package org.abstracthorizon.mercury.maildir.scala

import javax.mail._
import java.io.File
import java.lang.ref.Reference
import java.lang.ref.WeakReference

import java.util.WeakHashMap


class MaildirStore(session: Session, urlName: URLName) extends Store(session, urlName) {
	

    /** Leading dot session attribute name */
    val LEADING_DOT = "maildir.leadingDot"

    /** Store's home directory session attribute name */
    val HOME = "maildir.home"

    /** Info separator session attribute name */
    val INFO_SEPARATOR = "maildir.infoSeparator"

    /** Http syntax session attribute name */
    val HTTP_SYNTAX = "maildir.httpSyntax"
	
    /** Amount of time folder is going to be kept in list of folders */
    val MAX_FOLDER_DATA_LIFE = 1000*60*60 // 1 hour

	var httpSyntax = false

	protected [scala] var base: File = null

    var isLeadingDot = true

    var infoSeparator: Char = ' '


//    static {
//        val commandMap = CommandMap.getDefaultCommandMap()
//        if (commandMap.isInstanceof[MailcapCommandMap]) {
//            val mailcapCommandMap = commandMap.asInstanceOf[MailcapCommandMap]
//            mailcapCommandMap.addMailcap("multipart/*;; x-java-content-handler=" + MaildirMimeMultipartDataContentHandler.class.getName())
//        }
//    }

    /** Cache */
    protected val directories = new WeakHashMap[File, Reference[MaildirFolderData]]


    
    var leadingDotString = session.getProperty(LEADING_DOT)
    if (leadingDotString != null) {
        isLeadingDot = "true".equals(leadingDotString)
    }

    var httpSyntaxString = session.getProperty(HTTP_SYNTAX)
    if (httpSyntaxString != null) {
        httpSyntax = "true".equalsIgnoreCase(httpSyntaxString)
    }

    var infoSeparatorString = session.getProperty(INFO_SEPARATOR)
    if ((infoSeparatorString != null) && (infoSeparatorString.length() > 0)) {
        infoSeparator = infoSeparatorString.charAt(0)
    } else {
        if (":".equals(System.getProperty("path.separator"))) {
            infoSeparator = ':'
        } else {
            infoSeparator = '.'
        }
    }

    if (httpSyntax) {
        parseURLName(urlName);
    } else {
        val baseFileName = urlName.getFile();
        setBaseFile(createBaseFile(urlName, baseFileName))
    }

    if (base == null) {
        val homeString = session.getProperty(HOME);
        if (homeString != null) {
            setBaseFile(new File(homeString))
        } else {

            val home = new File(System.getProperty("user.home"));
            base = new File(home, ".mail");
            if (!base.exists()) {
                val t = base;
                base = new File(home, "Mail");
                if (base.exists()) {
                    if (!"true".equals(session.getProperty(LEADING_DOT))) {
                        isLeadingDot = false;
                    }
                } else {
                    base = t;
                }
            }
        }
    }
    
    

    /**
     * Parses url name
     * @param urlname url name
     */
    protected def parseURLName(urlname: URLName) = {
        val file = urlname.getFile();
        var i = file.indexOf('?');
        if (i >= 0) {
            val params = file.substring(i+1);
            i = 0;
            var j = params.indexOf(',', i);
            while (j > 0) {
                processParam(urlname, params.substring(i, j));
                i = j+1;
                j = params.indexOf(',', i);
            }
            processParam(urlname, params.substring(i));
        }
    }
    
    /**
     * Processes singe parameter from url name
     * @param urlName url name
     * @param param parameter
     */
    protected def processParam(urlName: URLName, param: String) {
        if (param.startsWith("base=")) {
            val baseString = param.substring(5)
            setBaseFile(createBaseFile(urlName, baseString))
        }
    }

    protected def setBaseFile(file: File) = {
        base = file;
        if (!base.exists()) {
            base.mkdirs();
        }
    }
    

    /**
     * Creates base file and substitues <code>{user}</code>, <code>{port}</code>,
     * <code>{host}</code> and <code>{protocol}</code>
     * @param urlName url name
     * @param baseName directory path
     * @return created file that represents base directory of the store
     */
    protected def createBaseFile(urlName: URLName, baseName: String): File  = {
        var b = baseName. replace("{protocol}", urlName.getProtocol())
        b = b.replace("{host}", urlName.getHost())
        b = b.replace("{port}", Integer.toString(urlName.getPort()))
        b = b.replace("{user}", urlName.getUsername())

        return new File(b)
    }
    
    
    
    
    
    
    
    
    
    def getDefaultFolder(): Folder = getFolder("")

    def getFolder(name: String): Folder = {
        if (!isConnected()) {
            throw new IllegalStateException("Store is not connected");
        }
        val folderData = getFolderData(name)
        return createFolder(folderData)
	}

    def getFolder(urlName: URLName): Folder = { 
    	if (httpSyntax) {
    		getFolder(urlName.getFile)
    	} else {
    		getFolder(urlName.getRef)
    	}
    }

    protected def getFolderData(name: String): MaildirFolderData  = {
        if (!isConnected()) {
            throw new IllegalStateException("Store is not connected")
        }
        var n = name
        n = n.replace('/', '.').replace('\\', '.')
        if (n.startsWith(".")) {
            n = n.substring(1)
        }
        if ("inbox".equalsIgnoreCase(n)) {
            n = "inbox"
        }
        if ((n.length() > 0) && isLeadingDot) {
            n = '.' + n
        }
        val file = new File(base, name)
        return getFolderData(file)
    }
    

    /**
     * This method returns folder data needed for folder to operate on.
     * If first checks cache and if there is no folder data in it
     * new will be created and stored in the cache.
     * @param file directory
     * @return new folder data
     */
    protected def getFolderData(file: File): MaildirFolderData = {
        val ref: Reference[MaildirFolderData]= directories.get(file)
        var folderData: MaildirFolderData  = null
        if (ref != null) {
            folderData = ref.get()
            if ((System.currentTimeMillis() - folderData.lastAccessed) > MAX_FOLDER_DATA_LIFE) {
                folderData = null
                directories.remove(file)
            }
        }
        if (folderData == null) {
            folderData = createFolderData(file);
            directories.put(file, new WeakReference[MaildirFolderData](folderData));
        }

        return folderData;
    }

    /**
     * This implementation creates {@link MaildirFolderData} from supplied file.
     * @param file file
     * @return new maildir folder data
     */
    protected def createFolderData(file: File): MaildirFolderData = {
        return new MaildirFolderData(this, file);
    }
    
    /**
     * Creates new folder instance with given folder data. This metod is to be overriden by
     * class extensions.
     * @param folderData folder data
     * @return new folder instance
     */
    protected def createFolder(folderData: MaildirFolderData): MaildirFolder = {
        return new MaildirFolder(this, folderData);
    }    
}