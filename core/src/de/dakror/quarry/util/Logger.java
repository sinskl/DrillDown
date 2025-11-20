/*******************************************************************************
 * Copyright 2018 Maximilian Stark | Dakror <mail@dakror.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.dakror.quarry.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/**
 * Logger utility that writes logs both to console and file
 * Useful for mobile platforms where console is not accessible
 *
 * @author Maximilian Stark | Dakror
 */
public class Logger {
    public static boolean fileLoggingEnabled = true;
    private static FileWriter fileWriter;
    private static BufferedWriter bufferedWriter;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Object lock = new Object();

    /**
     * Initialize file logging to the specified file
     */
    public static void initFileLogging(String logFilePath) {
        if (!fileLoggingEnabled || Gdx.app == null)
            return;

        try {
            FileHandle logFile = Gdx.files.external(logFilePath);
            logFile.parent().mkdirs();

            fileWriter = new FileWriter(logFile.file(), true);
            bufferedWriter = new BufferedWriter(fileWriter);

            log("Logger", "=== Logging started at " + dateFormat.format(new Date()) + " ===", LogLevel.INFO);
        } catch (Exception e) {
            Gdx.app.error("Logger", "Failed to initialize file logging", e);
            fileLoggingEnabled = false;
        }
    }

    /**
     * Close the log file
     */
    public static void dispose() {
        synchronized (lock) {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    // Ignore on dispose
                }
                bufferedWriter = null;
            }
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    // Ignore on dispose
                }
                fileWriter = null;
            }
        }
    }

    /**
     * Log an INFO level message
     */
    public static void info(String tag, String message) {
        log(tag, message, LogLevel.INFO);
    }

    /**
     * Log a DEBUG level message
     */
    public static void debug(String tag, String message) {
        log(tag, message, LogLevel.DEBUG);
    }

    /**
     * Log an ERROR level message
     */
    public static void error(String tag, String message) {
        log(tag, message, LogLevel.ERROR);
    }

    /**
     * Log an ERROR level message with exception
     */
    public static void error(String tag, String message, Throwable exception) {
        log(tag, message + "\n" + getStackTrace(exception), LogLevel.ERROR);
    }

    /**
     * Internal log method that writes to both console and file
     */
    private static void log(String tag, String message, LogLevel level) {
        // Write to console/system
        if (Gdx.app != null) {
            switch (level) {
                case INFO:
                    Gdx.app.log(tag, message);
                    break;
                case DEBUG:
                    Gdx.app.debug(tag, message);
                    break;
                case ERROR:
                    Gdx.app.error(tag, message);
                    break;
            }
        } else {
            // Fallback if Gdx.app not available
            System.out.println("[" + level.name() + "] " + tag + ": " + message);
        }

        // Write to file
        if (fileLoggingEnabled && bufferedWriter != null) {
            synchronized (lock) {
                try {
                    String timestamp = dateFormat.format(new Date());
                    String logLine = String.format("%s [%s] %s: %s",
                            timestamp, level.name(), tag, message);
                    bufferedWriter.write(logLine);
                    bufferedWriter.newLine();
                    bufferedWriter.flush(); // Flush immediately to capture logs even on crash
                } catch (IOException e) {
                    // If we can't write to file, disable file logging to avoid spam
                    fileLoggingEnabled = false;
                    if (Gdx.app != null) {
                        Gdx.app.error("Logger", "Failed to write to log file, disabling file logging", e);
                    }
                }
            }
        }
    }

    /**
     * Convert exception stack trace to string
     */
    private static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Log levels
     */
    public enum LogLevel {
        INFO, DEBUG, ERROR
    }
}
