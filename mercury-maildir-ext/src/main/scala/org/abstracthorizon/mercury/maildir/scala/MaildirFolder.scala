package org.abstracthorizon.mercury.maildir.scala

import javax.mail._

class MaildirFolder(val maildirStore: MaildirStore, val data: MaildirFolderData) extends Folder(maildirStore) {

    protected var opened = false
	
    def getName() = data.name

    def getFullName() = data.fullName

    def getParent(): Folder = { null }

    def exists() = data.exists

    def list(list: String): Array[Folder] = { null }

    def getSeparator() = '/'

    def getType() = data.typ

    def create(mode: Int): Boolean = { false }

    def hasNewMessages(): Boolean = { false }

    def getFolder(name: String): Folder = { null }

    def delete(recursive: Boolean): Boolean = { false }

    def renameTo(folder: Folder): Boolean = { false }

    def open(mode: Int): Unit = {  }

    def close(arg0: Boolean): Unit = {  }

    def isOpen(): Boolean = { false }

    def getPermanentFlags(): Flags = { null }

    def getMessageCount(): Int = { 0 }

    def getMessage(arg0: Int): Message = { null }

    def appendMessages(messages: Array[Message]): Unit = {  }

    def expunge(): Array[Message] = { null }

}