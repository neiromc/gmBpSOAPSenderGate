package ru.geekmonkey.bp.processors.runners.shell;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by neiro on 11.08.16.
 */
public class ShellCommandRunner {

    public String runCmdExternal(String command) {
        StringBuilder output = new StringBuilder();
        Process p;
        String line;

        try {
            if ( command.contains("|") ) {
                // Detect Linux and Pipeline - bad solution
                p = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
            } else {
                p = Runtime.getRuntime().exec(command);
            }
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            while ((line = reader.readLine())!= null) {
                output.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();
    }


}
