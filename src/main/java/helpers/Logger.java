package helpers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    private static final String LOG_FILE = "server.log";
    private static PrintWriter fileWriter;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Object lock = new Object();

    static {
        try {
            fileWriter = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE, true)), true);
            log("=".repeat(80));

        } catch (IOException e) {
            System.err.println("Failed to initialize log file: " + e.getMessage());
            System.err.println("Logging will continue to console only");
        }
    }

    public static void log(String message) {
        synchronized (lock) {
            String timestamp = dateFormat.format(new Date());
            String logMessage = "[" + timestamp + "] " + message;

            System.out.println(logMessage);

            if (fileWriter != null) {
                fileWriter.println(logMessage);
            }
        }
    }

    public static void logWithThread(String threadName, String message) {
        synchronized (lock) {
            String timestamp = dateFormat.format(new Date());
            String logMessage = "[" + timestamp + "] [" + threadName + "] " + message;

            System.out.println(logMessage);

            if (fileWriter != null) {
                fileWriter.println(logMessage);
            }
        }
    }

    public static void error(String message) {
        synchronized (lock) {
            String timestamp = dateFormat.format(new Date());
            String logMessage = "[" + timestamp + "] [ERROR] " + message;

            System.err.println(logMessage);

            if (fileWriter != null) {
                fileWriter.println(logMessage);
            }
        }
    }

    public static void errorWithThread(String threadName, String message) {
        synchronized (lock) {
            String timestamp = dateFormat.format(new Date());
            String logMessage = "[" + timestamp + "] [" + threadName + "] [ERROR] " + message;

            System.err.println(logMessage);

            if (fileWriter != null) {
                fileWriter.println(logMessage);
            }
        }
    }

    public static void logException(String message, Exception e) {
        synchronized (lock) {
            String timestamp = dateFormat.format(new Date());
            String logMessage = "[" + timestamp + "] [EXCEPTION] " + message + ": " + e.getMessage();

            System.err.println(logMessage);
            e.printStackTrace(System.err);

            if (fileWriter != null) {
                fileWriter.println(logMessage);
                e.printStackTrace(fileWriter);
            }
        }
    }

    public static void security(String threadName, String message) {
        synchronized (lock) {
            String timestamp = dateFormat.format(new Date());
            String logMessage = "[" + timestamp + "] [" + threadName + "] [SECURITY] " + message;

            System.out.println(logMessage);

            if (fileWriter != null) {
                fileWriter.println(logMessage);
            }
        }
    }

    public static void logWithLevel(String level, String message) {
        synchronized (lock) {
            String timestamp = dateFormat.format(new Date());
            String logMessage = "[" + timestamp + "] [" + level + "] " + message;

            System.out.println(logMessage);

            if (fileWriter != null) {
                fileWriter.println(logMessage);
            }
        }
    }

    public static void close() {
        synchronized (lock) {
            if (fileWriter != null) {
                log("=".repeat(80));
                log("Logger closing - Session ended");
                log("=".repeat(80));
                fileWriter.close();
                fileWriter = null;
            }
        }
    }

    public static void flush() {
        synchronized (lock) {
            if (fileWriter != null) {
                fileWriter.flush();
            }
        }
    }


}
