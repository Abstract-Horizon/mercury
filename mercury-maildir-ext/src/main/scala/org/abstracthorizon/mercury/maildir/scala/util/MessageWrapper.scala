package org.abstracthorizon.mercury.maildir.scala.util

import java.io.InputStream
import java.io.OutputStream

import java.util.Date

import javax.activation.DataHandler

import javax.mail._
import javax.mail.internet._

import org.abstracthorizon.mercury.maildir.scala._

object MessageWrapper {
	
	
	
}

class MessageWrapper(folder: MaildirFolder, val message: MimeMessage, msgnum: Int) extends MessageBase(folder, msgnum) {

    /**
     * Returns wrapped messaage
     * @return wrapped messaage
     */
    def getMessage(): MimeMessage = message

    // ----------------------------------------------------------------------------------

    /**
     * Adds from address
     * @param addresses array of addresses
     * @throws MessagingException
     */
    @throws(classOf[MessagingException])
    override def addFrom(addresses: Array[Address]) = message.addFrom(addresses)

    /**
     * Adds new header
     * @param name header name
     * @param value value
     * @throws MessagingException
     */
    override def addHeader(name: String, value: String) = message.addHeader(name, value)

    /**
     * Adds header line
     * @param line header line
     * @throws MessagingException
     */
    override def addHeaderLine(line: String) = message.addHeaderLine(line)

    /**
     * Adds recipients
     * @param type recipient type (see {@link javax.mail.Message.RecipientType})
     * @param addresses addresses
     * @throws MessagingException
     */
    override def addRecipients(typ: Message.RecipientType, addresses: Array[Address]) = message.addRecipients(typ, addresses)

    /**
     * Adds recipients
     * @param type recipient type (see {@link javax.mail.Message.RecipientType})
     * @param addresses addresses
     * @throws MessagingException
     */
    override def addRecipients(typ: Message.RecipientType, addresses: String) = message.addRecipients(typ, addresses)

    /**
     * Returns header lines
     * @return enumeration
     * @throws MessagingException
     */
    override def getAllHeaderLines() = message.getAllHeaderLines()

    /**
     * Returns headers
     * @return enumeration
     * @throws MessagingException
     */
    override def getAllHeaders() = message.getAllHeaders()

    /**
     * Returns all recipients
     * @return array of addresses
     * @throws MessagingException
     */
    override def getAllRecipients() = message.getAllRecipients()

    /**
     * Returns content
     * @return content
     * @throws IOException
     * @throws MessagingException
     */
    override def getContent() = message.getContent()

    /**
     * Returns content id
     * @return content id
     * @throws MessagingException
     */
    override def getContentID() = message.getContentID()

    /**
     * Returns content language
     * @return content language
     * @throws MessagingException
     */
    override def getContentLanguage() = message.getContentLanguage()

    /**
     * Returns content md3
     * @return content md
     * @throws MessagingException
     */
    override def getContentMD5() = message.getContentMD5()

// No need to be implemented since it is used from writeTo method and that
// method is overridden anyway
//    /**
//     * Returns content stream
//     * @return content stream
//     * @throws MessagingException
//     */
//    protected InputStream getContentStream() = {
//        return null;
//    }

    /**
     * Returns content type
     * @return content type
     * @throws MessagingException
     */
    override def getContentType() = message.getContentType()

    /**
     * Returns data handler
     * @return data handler
     * @throws MessagingException
     */
    override def getDataHandler() = message.getDataHandler()

    /**
     * Returns description
     * @return description
     * @throws MessagingException
     */
    override def getDescription() = message.getDescription()

    /**
     * Returns disposition
     * @return disposition
     * @throws MessagingException
     */
    override def getDisposition() = message.getDisposition()

    /**
     * Returns encoding
     * @return encoding
     * @throws MessagingException
     */
    override def getEncoding() = message.getEncoding()

    /**
     * Returns file name
     * @return file name
     * @throws MessagingException
     */
    override def getFileName() = message.getFileName()

    /**
     * Returns from
     * @return array of from addresses
     * @throws MessagingException
     */
    override def getFrom() = message.getFrom()

    /**
     * Returns header
     * @param name name of header
     * @return array of header values
     * @throws MessagingException
     */
    override def getHeader(name: String) = message.getHeader(name)

    /**
     * Returns header
     * @param name name
     * @param delimiter delimiter
     * @return header
     * @throws MessagingException
     */
    override def getHeader(name: String, delimiter: String) = message.getHeader(name, delimiter)

    /**
     * Returns input stream
     * @return input stream
     * @throws IOException
     * @throws MessagingException
     */
    override def getInputStream() = message.getInputStream()

    /**
     * Returns line count
     * @return line count
     * @throws MessagingException
     */
    override def getLineCount() = message.getLineCount()

    /**
     * Returns matching header lines
     * @param names array of names
     * @return enumeration
     * @throws MessagingException
     */
    override def getMatchingHeaderLines(names: Array[String]) = message.getMatchingHeaderLines(names)

    /**
     * Returns matching headers
     * @param names header names
     * @return enumeration
     * @throws MessagingException
     */
    override def getMatchingHeaders(names: Array[String]) = message.getMatchingHeaders(names)

    /**
     * Returns message id
     * @return message id
     * @throws MessagingException
     */
    override def getMessageID() = message.getMessageID()

    /**
     * Returns non matching header lines
     * @param names array of names
     * @return enumeration
     * @throws MessagingException
     */
    override def getNonMatchingHeaderLines(names: Array[String]) = message.getNonMatchingHeaderLines(names)

    /**
     * Returns non matching headers
     * @param names header names
     * @return enumeration
     * @throws MessagingException
     */
    override def getNonMatchingHeaders(names: Array[String]) = message.getNonMatchingHeaders(names)

    /**
     * Returns raw input stream
     * @return raw input stream
     * @throws MessagingException
     */
    override def getRawInputStream() = message.getRawInputStream()

    /**
     * Returns recipients
     * @param type recipitents' type
     * @return array of recipients
     * @throws MessagingException
     */
    override def getRecipients(typ: Message.RecipientType) = message.getRecipients(typ)

    /**
     * Returns reply to
     * @return array of recipients
     * @throws MessagingException
     */
    override def getReplyTo() = message.getReplyTo()

    /**
     * Returns sender
     * @return sender
     * @throws MessagingException
     */
    override def getSender() = message.getSender()

    /**
     * Returns sent date
     * @return sent date
     * @throws MessagingException
     */
    override def getSentDate() = message.getSentDate()

    /**
     * Returns size
     * @return size
     * @throws MessagingException
     */
    override def getSize() = message.getSize()

    /**
     * Returns subject
     * @return subject
     * @throws MessagingException
     */
    override def getSubject() = message.getSubject()

    /**
     * Returns <code>true</code> if is of supplied mime type
     * @param mimeType mime type to be checked
     * @return <code>true</code> if is of supplied mime type
     * @throws MessagingException
     */
    override def isMimeType(mimeType: String) = message.isMimeType(mimeType)

    /**
     * Removes header
     * @param name header's name
     * @throws MessagingException
     */
    override def removeHeader(name: String) = message.removeHeader(name)

    /**
     * Makes reply message
     * @param replyToAll should it reply to all
     * @return new message
     * @throws MessagingException
     */
    override def reply(replyToAll: Boolean) = message.reply(replyToAll)

    /**
     * Saves changes in message
     * @throws MessagingException
     */
    override def saveChanges() = message.saveChanges()

    /**
     * Sets contnet as multipart
     * @param mp multipart content
     * @throws MessagingException
     */
    override def setContent(mp: Multipart) = message.setContent(mp)

    /**
     * Sets content
     * @param o content object
     * @param type mime type
     * @throws MessagingException
     */
    override def setContent(o: Object, typ: String) = message.setContent(o, typ)

    /**
     * Sets content id
     * @param cid content id
     * @throws MessagingException
     */
    override def setContentID(cid: String) = message.setContentID(cid)

    /**
     * Sets languages
     * @param languages array of language strings
     * @throws MessagingException
     */
    override def setContentLanguage(languages: Array[String]) = message.setContentLanguage(languages)

    /**
     * Sets content md5
     * @param md5 content md5
     * @throws MessagingException
     */
    override def setContentMD5(md5: String) = message.setContentMD5(md5)

    /**
     * Sets data handler
     * @param dh data handler
     * @throws MessagingException
     */
    override def setDataHandler(dh: DataHandler) = message.setDataHandler(dh)

    /**
     * Sets description
     * @param description description
     * @throws MessagingException
     */
    override def setDescription(description: String) = message.setDescription(description)

    /**
     * Sets description
     * @param description description
     * @param charset character set
     * @throws MessagingException
     */
    override def setDescription(description: String, charset: String) = message.setDescription(description, charset)

    /**
     * Sets disposition
     * @param disposition content disposition
     * @throws MessagingException
     */
    override def setDisposition(disposition: String) = message.setDisposition(disposition)

    /**
     * Sets file name
     * @param filename file name
     * @throws MessagingException
     */
    override def setFileName(filename: String) = message.setFileName(filename)

    /**
     * Sets from
     * @throws MessagingException
     */
    override def setFrom() = message.setFrom()

    /**
     * Sets from
     * @param address from address
     * @throws MessagingException
     */
    override def setFrom(address: Address) = message.setFrom(address)

    /**
     * Set header
     * @param name header name
     * @param value header value
     * @throws MessagingException
     */
    override def setHeader(name: String, value: String) = message.setHeader(name, value)

    /**
     * Sets recipients
     * @param type recipients' type
     * @param addresses addresses
     * @throws MessagingException
     */
    override def setRecipients(typ: Message.RecipientType, addresses: Array[Address]) = message.setRecipients(typ, addresses)

    /**
     * Sets recipients
     * @param type recipients' type
     * @param addresses addresses
     * @throws MessagingException
     */
    override def setRecipients(typ: Message.RecipientType, addresses: String) = message.setRecipients(typ, addresses)

    /**
     * Sets reply to address
     * @param addresses addresses
     * @throws MessagingException
     */
    override def setReplyTo(addresses: Array[Address]) = message.setReplyTo(addresses)

    /**
     * Sets sender's address
     * @param address sender's address
     * @throws MessagingException
     */
    override def setSender(address: Address) = message.setSender(address)

    /**
     * Sets sent date
     * @param d date
     * @throws MessagingException
     */
    override def setSentDate(d: Date) = message.setSentDate(d)

    /**
     * Sets subject
     * @param subject subject
     * @throws MessagingException
     */
    override def setSubject(subject: String) = message.setSubject(subject)

    /**
     * Sets subject
     * @param subject subject
     * @param charset character set
     * @throws MessagingException
     */
    override def setSubject(subject: String, charset: String) = message.setSubject(subject, charset)

    /**
     * Sets body as text
     * @param text body text
     * @throws MessagingException
     */
    override def setText(text: String) = message.setText(text)

    /**
     * Sets body as text
     * @param text body text
     * @param charset character set
     * @throws MessagingException
     */
    override def setText(text: String, charset: String) = message.setText(text, charset)

// No need to be implemented since it is used from saveChanges method and that
// method is overridden anyway
//    /**
//     * Updates headers
//     * @throws MessagingException
//     */
//    protected void updateHeaders() = {
//    }

    /**
     * Writes content of the message to output stream
     * @param os output stream
     * @throws IOException
     * @throws MessagingException
     */
    override def writeTo(os: OutputStream) = message.writeTo(os)

    /**
     * Writes content of the message to output stream ignoring supplied headers
     * @param os output stream
     * @param ignoreList array of headers to be ignored
     * @throws IOException
     * @throws MessagingException
     */
    override def writeTo(os: OutputStream, ignoreList: Array[String]) = message.writeTo(os, ignoreList)

    /**
     * Adds new recipient to the message
     * @param type recipient type
     * @param address address
     * @throws MessagingException
     */
    override def addRecipient(typ: Message.RecipientType, address: Address) = message.addRecipient(typ, address)

    /**
     * Matches message
     * @param term term to be used for matching
     * @return <code>true</code> if matched
     * @throws MessagingException
     */
//    override def matchx(term: SearchTerm) = message.matchx(term)

    /**
     * Sets recipient to the message
     * @param type recipient type
     * @param address address
     * @throws MessagingException
     */
    override def setRecipient(typ: Message.RecipientType, address: Address) = message.setRecipient(typ, address)

    /**
     * Returns <code>true</code> if message is expunged
     * @return <code>true</code> if message is expunged
     */
    override def isExpunged() = message.isExpunged()
    
    /**
     * Returns flags
     * @return flags
     * @throws MessagingException
     */
    override def getFlags() = message.getFlags()

    /**
     * Returns received date
     * @return received date
     * @throws MessagingException
     */
    override def getReceivedDate() = message.getReceivedDate()

    /**
     * Checks if flag is set
     * @param flag flag
     * @return <code>true</code> if flag is set
     * @throws MessagingException
     */
    override def isSet(flag: Flags.Flag) = message.isSet(flag)

    /**
     * Sets or resets a flag
     * @param flag flag to be set
     * @param set should flag be set or reset
     * @throws MessagingException
     */
    override def setFlag(flag: Flags.Flag, set: Boolean) = message.setFlag(flag, set)

    /**
     * Sets or resets a flags
     * @param flags flags to be set
     * @param set should flag be set or reset
     * @throws MessagingException
     */
    override def setFlags(flags: Flags, set: Boolean) = message.setFlags(flags, set)

    /**
     * Compares two objects. If supplied object is of this type then it compares
     * if both are pointing to the same message
     * @param o object to be compared with
     * @return <code>true</code> if both objects are pointing to the same message object
     */
    override def equals(o: Any): Boolean = {
        if (o.isInstanceOf[MessageWrapper]) {
            val msg = (o.asInstanceOf[MessageWrapper]).message;
            if (message == msg) {
                return true;
            }
            if (message != null) {
                return message.equals(msg);
            }
        }
        if (message == o) {
            return true;
        }
        if (message != null) {
            return message.equals(o);
        }
        return false;
    }

    /**
     * Wrapper around wrapped message's <code>compareTo</code> method.
     * @param o another object
     * @return wrapped message's <code>compareTo</code> method result or -1
     */
    def compareTo(o: Message): Int = {
        if (message.isInstanceOf[Comparable[_]]) {
        	val comparable = message.asInstanceOf[Comparable[Message]]
        	return comparable.compareTo(o)
        }
        return -1
    }

}
