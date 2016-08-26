package ru.geekmonkey.bp.processors;

import org.apache.log4j.Logger;
import ru.geekmonkey.bp.Main;
import ru.geekmonkey.bp.processors.runners.shell.ShellCommandRunner;
import ru.geekmonkey.bp.yaml.Macros;

import java.util.ArrayList;

/**
 * Created by neiro on 25.07.16.
 */
public class MacrosProcessor {

    private Logger logger = Main.logger;
    private String data;
    private ArrayList<Macros> macrosList;

    public MacrosProcessor(final String data,
                           final ArrayList<Macros> macrosList) {
        this.data = data;
        this.macrosList = macrosList;
    }

    private String runCommand( final String cmdType,
                             final String cmdString) {

        if ( "external".equals(cmdType.trim().toLowerCase())) {
            return (new ShellCommandRunner().runCmdExternal(cmdString));
        } else {
            return "INTERNAL_COMMANDS_NOT_SUPPORTED";
        }

    }

    public String processingData() {
        for (Macros m : macrosList ) {
            logger.info(String.format("MACROS: name=%s, value=%s, isCommand=%s, commanType=%s",
                    m.name, m.value, m.isCommand, m.commandType));

            if ( ! m.isCommand ) {
                data = data.replaceAll(m.name, m.value);
            } else {
                data = data.replaceAll(m.name, runCommand(m.commandType, m.value));
            }
        }

        return data;
    }

}
