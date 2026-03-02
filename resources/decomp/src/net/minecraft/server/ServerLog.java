/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.SERVER)
public class ServerLog {
    public static Logger LOGGER = Logger.getLogger("Minecraft");

    public static void init() {
        ConsoleFormatter consoleFormatter = new ConsoleFormatter();
        LOGGER.setUseParentHandlers(false);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(consoleFormatter);
        LOGGER.addHandler(consoleHandler);
        try {
            FileHandler fileHandler = new FileHandler("server.log", true);
            fileHandler.setFormatter(consoleFormatter);
            LOGGER.addHandler(fileHandler);
        }
        catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to log to server.log", exception);
        }
    }

    @Environment(value=EnvType.SERVER)
    static final class ConsoleFormatter
    extends Formatter {
        private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        ConsoleFormatter() {
        }

        public String format(LogRecord logRecord) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.dateFormat.format(logRecord.getMillis()));
            Level level = logRecord.getLevel();
            if (level == Level.FINEST) {
                stringBuilder.append(" [FINEST] ");
            } else if (level == Level.FINER) {
                stringBuilder.append(" [FINER] ");
            } else if (level == Level.FINE) {
                stringBuilder.append(" [FINE] ");
            } else if (level == Level.INFO) {
                stringBuilder.append(" [INFO] ");
            } else if (level == Level.WARNING) {
                stringBuilder.append(" [WARNING] ");
            } else if (level == Level.SEVERE) {
                stringBuilder.append(" [SEVERE] ");
            } else if (level == Level.SEVERE) {
                stringBuilder.append(" [" + level.getLocalizedName() + "] ");
            }
            stringBuilder.append(logRecord.getMessage());
            stringBuilder.append('\n');
            Throwable throwable = logRecord.getThrown();
            if (throwable != null) {
                StringWriter stringWriter = new StringWriter();
                throwable.printStackTrace(new PrintWriter(stringWriter));
                stringBuilder.append(stringWriter.toString());
            }
            return stringBuilder.toString();
        }
    }
}

