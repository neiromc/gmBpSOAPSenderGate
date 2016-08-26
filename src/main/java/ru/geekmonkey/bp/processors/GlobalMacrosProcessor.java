package ru.geekmonkey.bp.processors;

import org.apache.log4j.Logger;
import ru.geekmonkey.bp.Main;
import ru.geekmonkey.bp.processors.runners.shell.ShellCommandRunner;
import ru.geekmonkey.bp.yaml.GlobalMacros;

import java.util.ArrayList;

/**
 * Created by neiro on 25.07.16.
 */
public class GlobalMacrosProcessor {

    private Logger logger = Main.logger;
    private ArrayList<GlobalMacros> globalMacrosLists;

    public GlobalMacrosProcessor(final ArrayList<GlobalMacros> globalMacrosList) {
        this.globalMacrosLists = globalMacrosList;
    }

    private String runCommand( final String cmdType,
                             final String cmdString) {

        if ( "external".equals(cmdType.trim().toLowerCase())) {
            return (new ShellCommandRunner().runCmdExternal(cmdString));
        } else {
            return "INTERNAL_NOT_SUPPORTED";
        }

    }

    public ArrayList<GlobalMacros> processing() {

//        for (int i = 0; i < globalMacrosLists.size(); i++) {
//            GlobalMacros m = globalMacrosLists.get(i);
        for (GlobalMacros m : globalMacrosLists) {
            logger.info(String.format("MACROS: name=%s, value=%s, isCommand=%s, commanType=%s",
                    m.name, m.value, m.isCommand, m.commandType));

            if ( m.isCommand ) {
                m.value = runCommand(m.commandType, m.value);
                logger.info(String.format("MACROS-PROCESSING: name=%s, value=%s",
                        m.name, m.value));
            }

        }

        return globalMacrosLists;

    }

//    public String processingData(String data, ArrayList<GlobalMacros> globalMacrosLists) {
    public String processingData(String data) {
//        System.out.println(data);
//        printList();

//            logger.info(String.format("MACROS: name=%s, value=%s, isCommand=%s, commanType=%s",
//                    m.name, m.value, m.isCommand, m.commandType));

        for ( GlobalMacros m : globalMacrosLists) {
            data = data.replaceAll(m.name, m.value);
        }

        return data;

//        System.out.println(data);
    }

}
