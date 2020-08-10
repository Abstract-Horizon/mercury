package org.abstracthorizon.mercury.adminconsole;

import org.abstracthorizon.mercury.accounts.spring.MaildirKeystoreStorageManager;

public interface RequiresStorageManager {

    void setStorageManager(MaildirKeystoreStorageManager storageManager);

}
