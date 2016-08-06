package utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.SimpleFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtils {

    private static Logger classLog = null;

    static {
        String propertyLog = "log4j.configuration";
        String property = System.getProperty(propertyLog);

        if ((property == null || property.isEmpty())
                && FileUtils.exists("log4j.xml"))
            System.setProperty(propertyLog, "file:log4j.xml");
    }

    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    public static java.util.logging.Logger getLogger(String id, String filedir,
            String filename) throws SecurityException, IOException {
        java.util.logging.Logger logger = java.util.logging.Logger
                .getLogger(id);

        if (logger.getHandlers() != null && logger.getHandlers().length > 0)
            return logger;

        File file = new File(filedir);
        if (!file.exists())
            file.mkdirs();

        String filepath = filedir + File.separator + filename;
        file = new File(filepath);
        if (!file.exists())
            file.createNewFile();

        FileHandler handler = null;
        try {
            handler = new FileHandler(filepath, true);
        } catch (IOException e) {
            try {
                LogManager.getLogManager().reset();
                handler = new FileHandler(filepath, true);
            } catch (Exception e1) {
                LogManager.getLogManager().reset();
                if (classLog == null)
                    classLog = LoggerFactory.getLogger(LogUtils.class);
                classLog.error("Error to initialize log " + filename
                        + " with id " + id + " inside the directory " + filedir
                        + ". Error: " + e1.getMessage(), e1);
                return null;
            }
        }

        logger.addHandler(handler);
        handler.setFormatter(new SimpleFormatter());
        return logger;
    }
}
