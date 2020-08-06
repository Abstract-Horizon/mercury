package org.abstracthorizon.mercury.maildir.scala.util

import java.io._
import java.util.Date

import javax.activation.DataHandler
import javax.mail._
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import org.abstracthorizon.mercury.maildir.scala._

class MessageBase(folder: MaildirFolder, msgNumber: Int) extends MimeMessage(folder, msgNumber) {

    /**
     * Sets message number
     * @param num message number
     */
    override def setMessageNumber(num: Int) = {
        super.setMessageNumber(num);
    }
	
	
}

class LazyParsingMessage(folder: MaildirFolder, msgNumber: Int) extends MessageBase(folder, msgNumber) {

    /** Flag to show is message parsed or not */
    var parsed = false

    /** Cached input stream */
    protected var inputStream: InputStream = null

    /**
     * Returns <code>true</code> if is parsed
     * @return <code>true</code> if is parsed
     */
    protected  def isParsed() = parsed
    
    /**
     * Stores input stream for later invoking of superclass' parse method
     * @param is input stream
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    protected override def parse(is: InputStream) = {
        inputStream = is
    }

    /**
     * Parses message. It calls superclass' parse method.
     *
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    protected def parseImpl() = {
        if (!parsed) {
            super.parse(inputStream);
            parsed = true
        }
    }

    /**
     * Returns headers
     * @param inputStream input stream
     * @return fixed internet headers
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    protected override def createInternetHeaders(inputStream: InputStream): InternetHeaders = new InternetHeadersImpl(inputStream)

    // ----------------------------------------------------------------------------------

    /**
     * Adds from address
     * @param addresses array of addresses
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def addFrom(addresses: Array[Address]) = {
        if (!parsed) { parseImpl() }
        super.addFrom(addresses)
    }

    /**
     * Adds new header
     * @param name header name
     * @param value value
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def addHeader(name: String, value: String) = {
        if (!parsed) { parseImpl() }
        super.addHeader(name, value)
    }

    /**
     * Adds header line
     * @param line header line
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def addHeaderLine(line: String) = {
        if (!parsed) { parseImpl() }
        super.addHeaderLine(line);
    }

    /**
     * Adds recipients
     * @param type recipient type (see {@link javax.mail.Message.RecipientType})
     * @param addresses addresses
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def addRecipients(typ: Message.RecipientType, addresses: Array[Address]) = {
        if (!parsed) { parseImpl() }
        super.addRecipients(typ, addresses)
    }

    /**
     * Adds recipients
     * @param type recipient type (see {@link javax.mail.Message.RecipientType})
     * @param addresses addresses
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def addRecipients(typ: Message.RecipientType, addresses: String) = {
        if (!parsed) { parseImpl() }
        super.addRecipients(typ, addresses)
    }

    /**
     * Returns header lines
     * @return enumeration
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getAllHeaderLines() = {
        if (!parsed) { parseImpl() }
        super.getAllHeaderLines()
    }

    /**
     * Returns headers
     * @return enumeration
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getAllHeaders() = {
        if (!parsed) { parseImpl() }
        super.getAllHeaders()
    }

    /**
     * Returns all recipients
     * @array of addresses
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getAllRecipients() = {
        if (!parsed) { parseImpl() }
        super.getAllRecipients()
    }

    /**
     * Returns content
     * @content
     * @throws IOException
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    @throws(classOf[IOException])
    override def getContent() = {
        if (!parsed) { parseImpl() }
        super.getContent()
    }

    /**
     * Returns content id
     * @content id
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getContentID() = {
        if (!parsed) { parseImpl() }
        super.getContentID()
    }

    /**
     * Returns content language
     * @content language
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getContentLanguage() = {
        if (!parsed) { parseImpl() }
        super.getContentLanguage()
    }

    /**
     * Returns content md3
     * @content md
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getContentMD5() = {
        if (!parsed) { parseImpl() }
        super.getContentMD5()
    }

    /**
     * Returns content stream
     * @content stream
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    protected override def getContentStream() = {
        if (!parsed) { parseImpl() }
        super.getContentStream()
    }

    /**
     * Returns content type
     * @content type
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getContentType() = {
        if (!parsed) { parseImpl() }
        super.getContentType()
    }

    /**
     * Returns data handler
     * @data handler
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getDataHandler() = {
        if (!parsed) { parseImpl() }
        super.getDataHandler()
    }

    /**
     * Returns description
     * @description
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getDescription() = {
        if (!parsed) { parseImpl() }
        super.getDescription()
    }

    /**
     * Returns disposition
     * @disposition
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getDisposition() = {
        if (!parsed) { parseImpl() }
        super.getDisposition()
    }

    /**
     * Returns encoding
     * @encoding
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getEncoding() = {
        if (!parsed) { parseImpl() }
        super.getEncoding()
    }

    /**
     * Returns file name
     * @file name
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getFileName() = {
        if (!parsed) { parseImpl() }
        super.getFileName()
    }

    /**
     * Returns from
     * @array of from addresses
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getFrom() = {
        if (!parsed) { parseImpl() }
        super.getFrom()
    }

    /**
     * Returns header
     * @param name name of header
     * @array of header values
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getHeader(name: String) = {
        if (!parsed) { parseImpl() }
        super.getHeader(name)
    }

    /**
     * Returns header
     * @param name name
     * @param delimiter delimiter
     * @header
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getHeader(name: String, delimiter: String) = {
        if (!parsed) { parseImpl() }
        super.getHeader(name, delimiter)
    }

    /**
     * Returns input stream
     * @input stream
     * @throws IOException
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    @throws(classOf[IOException])
    override def getInputStream() = {
        if (!parsed) { parseImpl() }
        super.getInputStream()
    }

    /**
     * Returns line count
     * @line count
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getLineCount() = {
        if (!parsed) { parseImpl() }
        super.getLineCount()
    }

    /**
     * Returns matching header lines
     * @param names array of names
     * @enumeration
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getMatchingHeaderLines(names: Array[String]) = {
        if (!parsed) { parseImpl() }
        super.getMatchingHeaderLines(names)
    }

    /**
     * Returns matching headers
     * @param names header names
     * @enumeration
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getMatchingHeaders(names: Array[String]) = {
        if (!parsed) { parseImpl() }
        super.getMatchingHeaders(names)
    }

    /**
     * Returns message id
     * @message id
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getMessageID() = {
        if (!parsed) { parseImpl() }
        super.getMessageID()
    }

    /**
     * Returns non matching header lines
     * @param names array of names
     * @enumeration
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getNonMatchingHeaderLines(names: Array[String]) = {
        if (!parsed) { parseImpl() }
        super.getNonMatchingHeaderLines(names)
    }

    /**
     * Returns non matching headers
     * @param names header names
     * @enumeration
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getNonMatchingHeaders(names: Array[String]) = {
        if (!parsed) { parseImpl() }
        super.getNonMatchingHeaders(names)
    }

    /**
     * Returns raw input stream
     * @raw input stream
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getRawInputStream() = {
        if (!parsed) { parseImpl() }
        super.getRawInputStream()
    }

    /**
     * Returns recipients
     * @param type recipitents' type
     * @array of recipients
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getRecipients(typ: Message.RecipientType) = {
        if (!parsed) { parseImpl() }
        super.getRecipients(typ)
    }

    /**
     * Returns reply to
     * @array of recipients
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getReplyTo() = {
        if (!parsed) { parseImpl() }
        super.getReplyTo()
    }

    /**
     * Returns sender
     * @sender
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getSender() = {
        if (!parsed) { parseImpl() }
        super.getSender()
    }

    /**
     * Returns sent date
     * @sent date
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getSentDate() = {
        if (!parsed) { parseImpl() }
        super.getSentDate()
    }

    /**
     * Returns size
     * @size
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getSize() = {
        if (!parsed) { parseImpl() }
        super.getSize()
    }

    /**
     * Returns subject
     * @subject
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def getSubject() = {
        if (!parsed) { parseImpl() }
        super.getSubject()
    }

    /**
     * Returns <code>true</code> if is of supplied mime type
     * @param mimeType mime type to be checked
     * @<code>true</code> if is of supplied mime type
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def isMimeType(mimeType: String) = {
        if (!parsed) { parseImpl() }
        super.isMimeType(mimeType)
    }

    /**
     * Removes header
     * @param name header's name
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def removeHeader(name: String) = {
        if (!parsed) { parseImpl() }
        super.removeHeader(name)
    }

    /**
     * Makes reply message
     * @param replyToAll should it reply to all
     * @new message
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def reply(replyToAll: Boolean) = {
        if (!parsed) { parseImpl() }
        super.reply(replyToAll)
    }

    /**
     * Saves changes in message
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def saveChanges() = {
        if (!parsed) { parseImpl() }
        super.saveChanges()
    }

    /**
     * Sets contnet as multipart
     * @param mp multipart content
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setContent(mp: Multipart) = {
        if (!parsed) { parseImpl() }
        super.setContent(mp)
    }

    /**
     * Sets content
     * @param o content object
     * @param type mime type
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setContent(o: Object, typ: String) = {
        if (!parsed) { parseImpl() }
        super.setContent(o, typ)
    }

    /**
     * Sets content id
     * @param cid content id
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setContentID(cid: String) = {
        if (!parsed) { parseImpl() }
        super.setContentID(cid)
    }

    /**
     * Sets languages
     * @param languages array of language strings
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setContentLanguage(languages: Array[String]) = {
        if (!parsed) { parseImpl() }
        super.setContentLanguage(languages)
    }

    /**
     * Sets content md5
     * @param md5 content md5
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setContentMD5(md5: String) = {
        if (!parsed) { parseImpl() }
        super.setContentMD5(md5)
    }

    /**
     * Sets data handler
     * @param dh data handler
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setDataHandler(dh: DataHandler) = {
        if (!parsed) { parseImpl() }
        super.setDataHandler(dh)
    }

    /**
     * Sets description
     * @param description description
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setDescription(description: String) = {
        if (!parsed) { parseImpl() }
        super.setDescription(description)
    }

    /**
     * Sets description
     * @param description description
     * @param charset character set
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setDescription(description: String, charset: String) = {
        if (!parsed) { parseImpl() }
        super.setDescription(description, charset)
    }

    /**
     * Sets disposition
     * @param disposition content disposition
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setDisposition(disposition: String) = {
        if (!parsed) { parseImpl() }
        super.setDisposition(disposition)
    }

    /**
     * Sets file name
     * @param filename file name
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setFileName(filename: String) = {
        if (!parsed) { parseImpl() }
        super.setFileName(filename)
    }

    /**
     * Sets from
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setFrom() = {
        if (!parsed) { parseImpl() }
        super.setFrom()
    }

    /**
     * Sets from
     * @param address from address
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setFrom(address: Address) = {
        if (!parsed) { parseImpl() }
        super.setFrom(address)
    }

    /**
     * Set header
     * @param name header name
     * @param value header value
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setHeader(name: String, value: String) = {
        if (!parsed) { parseImpl() }
        super.setHeader(name, value)
    }

    /**
     * Sets recipients
     * @param type recipients' type
     * @param addresses addresses
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setRecipients(typ: Message.RecipientType, addresses: Array[Address]) = {
        if (!parsed) { parseImpl() }
        super.setRecipients(typ, addresses)
    }

    /**
     * Sets recipients
     * @param type recipients' type
     * @param addresses addresses
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setRecipients(typ: Message.RecipientType, addresses: String) = {
        if (!parsed) { parseImpl() }
        super.setRecipients(typ, addresses)
    }

    /**
     * Sets reply to address
     * @param addresses addresses
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setReplyTo(addresses: Array[Address]) = {
        if (!parsed) { parseImpl() }
        super.setReplyTo(addresses)
    }

    /**
     * Sets sender's address
     * @param address sender's address
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setSender(address: Address) = {
        if (!parsed) { parseImpl() }
        super.setSender(address);
    }

    /**
     * Sets sent date
     * @param d date
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setSentDate(d: Date) = {
        if (!parsed) { parseImpl() }
        super.setSentDate(d)
    }

    /**
     * Sets subject
     * @param subject subject
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setSubject(subject: String) = {
        if (!parsed) { parseImpl() }
        super.setSubject(subject)
    }

    /**
     * Sets subject
     * @param subject subject
     * @param charset character set
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setSubject(subject: String, charset: String) = {
        if (!parsed) { parseImpl() }
        super.setSubject(subject, charset)
    }

    /**
     * Sets body as text
     * @param text body text
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setText(text: String) = {
        if (!parsed) { parseImpl() }
        super.setText(text)
    }

    /**
     * Sets body as text
     * @param text body text
     * @param charset character set
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setText(text: String, charset: String) = {
        if (!parsed) { parseImpl() }
        super.setText(text, charset)
    }

    /**
     * Updates headers
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    protected override def updateHeaders() = {
        if (!parsed) { parseImpl() }
        super.updateHeaders()
    }

    /**
     * Writes content of the message to output stream
     * @param os output stream
     * @throws IOException
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    @throws(classOf[IOException])
    override def writeTo(os: OutputStream) = {
        if (!parsed) { parseImpl() }
        super.writeTo(os)
    }

    /**
     * Writes content of the message to output stream ignoring supplied headers
     * @param os output stream
     * @param ignoreList array of headers to be ignored
     * @throws IOException
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    @throws(classOf[IOException])
    override def writeTo(os: OutputStream, ignoreList: Array[String]) = {
        if (!parsed) { parseImpl() }
        super.writeTo(os, ignoreList)
    }

    /**
     * Adds new recipient to the message
     * @param type recipient type
     * @param address address
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def addRecipient(typ: Message.RecipientType, address: Address) = {
        if (!parsed) { parseImpl() }
        super.addRecipient(typ, address)
    }

//    /**
//     * Matches message
//     * @param term term to be used for matching
//     * @<code>true</code> if matched
//     * @throws MessagingException
//     */
//    @throws(classOf[MessagingException])
//    override def xmatch(term: SearchTerm) = {
//        if (!parsed) { parseImpl() }
//        super.xmatch(term)
//    }

    /**
     * Sets recipient to the message
     * @param type recipient type
     * @param address address
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def setRecipient(typ: Message.RecipientType, address: Address) = {
        if (!parsed) { parseImpl() }
        super.setRecipient(typ, address)
    }

    /**
     * Returns <code>true</code> if message is expunged
     * @<code>true</code> if message is expunged
     */
    @throws(classOf[MessagingException])
    override def isExpunged() = { super.isExpunged() }

}
