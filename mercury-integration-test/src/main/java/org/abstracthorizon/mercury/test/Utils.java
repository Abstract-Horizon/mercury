package org.abstracthorizon.mercury.test;

import java.io.IOException;

public class Utils {

    public static interface RunWithRetry {
        void run() throws Exception;
    }

    public static interface FunctionWithRetry<T> {
        T run() throws Exception;
    }

    public static void runWithRetry(RunWithRetry runWithRetry) throws IOException {
        runWithRetry(() -> {
            runWithRetry.run();
            return null;
        }, 3, 1000);
    }

    public static <T> T functionWithRetry(FunctionWithRetry<T> runWithRetry) throws IOException {
        return runWithRetry(runWithRetry, 3, 1000);
    }

    public static <T> T runWithRetry(FunctionWithRetry<T> runWithRetry, int numberOfRetries, int backoffTimeout) throws IOException {
        int i = 0;
        Exception firstException = null;
        while (i < numberOfRetries) {
            try {
                return runWithRetry.run();
            } catch (Exception e) {
                if (firstException == null) {
                    firstException = e;
                }
            }
            i++;
            try {
                Thread.sleep(backoffTimeout);
            } catch (InterruptedException ignore) {}
        }

        if (firstException instanceof IOException) {
            throw (IOException) firstException;
        }
        throw new IOException(firstException);
    }

}
