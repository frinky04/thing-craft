/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package paulscode.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class SoundSystemLogger {
    public void message(String string, int i) {
        String string3 = "";
        for (int j = 0; j < i; ++j) {
            string3 = string3 + "    ";
        }
        String string2 = string3 + string;
        System.out.println(string2);
    }

    public void importantMessage(String string, int i) {
        String string3 = "";
        for (int j = 0; j < i; ++j) {
            string3 = string3 + "    ";
        }
        String string2 = string3 + string;
        System.out.println(string2);
    }

    public boolean errorCheck(boolean bl, String string, String string2, int i) {
        if (bl) {
            this.errorMessage(string, string2, i);
        }
        return bl;
    }

    public void errorMessage(String string, String string2, int i) {
        String string5 = "";
        for (int j = 0; j < i; ++j) {
            string5 = string5 + "    ";
        }
        String string3 = string5 + "Error in class '" + string + "'";
        String string4 = "    " + string5 + string2;
        System.out.println(string3);
        System.out.println(string4);
    }

    public void printStackTrace(Exception exception, int i) {
        this.printExceptionMessage(exception, i);
        this.importantMessage("STACK TRACE:", i);
        if (exception == null) {
            return;
        }
        StackTraceElement[] stackTraceElements = exception.getStackTrace();
        if (stackTraceElements == null) {
            return;
        }
        for (int j = 0; j < stackTraceElements.length; ++j) {
            StackTraceElement stackTraceElement = stackTraceElements[j];
            if (stackTraceElement == null) continue;
            this.message(stackTraceElement.toString(), i + 1);
        }
    }

    public void printExceptionMessage(Exception exception, int i) {
        this.importantMessage("ERROR MESSAGE:", i);
        if (exception.getMessage() == null) {
            this.message("(none)", i + 1);
        } else {
            this.message(exception.getMessage(), i + 1);
        }
    }
}

