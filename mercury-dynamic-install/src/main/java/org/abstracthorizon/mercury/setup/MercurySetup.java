package org.abstracthorizon.mercury.setup;

import java.lang.reflect.Method;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class MercurySetup {

    private Object server;

    public MercurySetup() {
    }

    public void setServer(Object server) {
        this.server = server;
    }

    public Object getServer() {
        return server;
    }
    
    public void start() throws Exception {
        
        Method getHomeLocationMethod = getServer().getClass().getMethod("getHomeLocation", new Class[0]);
        URL homeURL = (URL)getHomeLocationMethod.invoke(getServer(), new Object[0]);
        File home = new File(homeURL.getFile());
    
        File deploy = new File(home, "deploy");
        File mercuryLogs = new File(deploy, "mercury-logs");
        if (!mercuryLogs.exists()) {
            mercuryLogs.mkdirs();
        }
        File logsLogs = new File(mercuryLogs, "logs");
        if (!logsLogs.exists()) {
            logsLogs.mkdirs();
        }
        File accessLogs = new File(mercuryLogs, "access");
        if (!accessLogs.exists()) {
            accessLogs.mkdirs();
        }
        
        File mercuryData = new File(deploy, "mercury-data");
        if (!mercuryData.exists()) {
            mercuryData.mkdirs();
        }

        File config = new File(mercuryData, "config");
        if (!config.exists()) {
            config.mkdirs();
        }
        File mailboxes = new File(mercuryData, "mailboxes");
        if (!mailboxes.exists()) {
            mailboxes.mkdirs();
        }
        
        File accountsKeystore = new File(config, "accounts.keystore");
        if (!accountsKeystore.exists()) {
            InputStream is = getClass().getResourceAsStream("/accounts.keystore");
            try {
                FileOutputStream os = new FileOutputStream(accountsKeystore);
                try {
                    copy(is, os);
                } finally {
                    os.close();
                }
            } finally {
                is.close();
            }
        }

        File accountsProperties = new File(config, "accounts.properties");
        if (!accountsProperties.exists()) {
            InputStream is = getClass().getResourceAsStream("/accounts.properties");
            try {
                FileOutputStream os = new FileOutputStream(accountsProperties);
                try {
                    copy(is, os);
                } finally {
                    os.close();
                }
            } finally {
                is.close();
            }
        }

        File mailSuiteXml = new File(deploy, "mercury-mail-suite.sar.xml");
        if (!mailSuiteXml.exists()) {
            InputStream is = getClass().getResourceAsStream("/mercury-mail-suite.sar.xml");
            try {
                FileOutputStream os = new FileOutputStream(mailSuiteXml);
                try {
                    copy(is, os);
                } finally {
                    os.close();
                }
            } finally {
                is.close();
            }
        }
    
    }

    public void copy(InputStream is, OutputStream os) throws Exception {
        byte[] buf = new byte[10240];
        
        int r = is.read(buf);
        while (r >= 0) {
            os.write(buf, 0, r);
            r = is.read(buf);
        }
    }
    
}
