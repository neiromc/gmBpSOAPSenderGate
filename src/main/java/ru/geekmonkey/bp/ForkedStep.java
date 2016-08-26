package ru.geekmonkey.bp;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by neiro on 18.08.16.
 */
public class ForkedStep {

    private Logger logger = Main.logger;

    private String jarFileName;
    private String configFileName;
    private int stepId;
    private Map<String,String> environments;

    public ForkedStep(final String jarFileName,
                      final String configFileName,
                      final int stepId,
                      final Map<String, String> environments)
    {
        this.jarFileName = jarFileName;
        this.configFileName = configFileName;
        this.stepId = stepId;
        this.environments = environments;
    }


    public void fork() {

        List<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-jar");
        //FORK :: get forkProcessLogConfig from Config.class (yml file)
        if ( ConfigFile.INSTANCE.getConfig().forkProcessLogConfig != null ) {
            commands.add("-Dlog4j.configuration=file:" + ConfigFile.INSTANCE.getConfig().forkProcessLogConfig);
        }
        commands.add(jarFileName);
        commands.add(configFileName);
        commands.add("fork_process.step_" + stepId);

        ProcessBuilder pb = new ProcessBuilder(commands);
        Map<String,String> env = pb.environment();

        for (String envName : environments.keySet()) {
            env.put(envName, environments.get(envName));
        }

        try {
            Process p = pb.start();
            logger.info("fork() method have been started ==> " + p.getOutputStream());


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

//    public static void main(String[] args) {
//        String workDir = System.getProperty("user.dir");
//
//        String configFileName = args[0];
//
//        Map<String,String> env = new HashMap<>();
//
//        int stepId = 2;
//        int stepsCount = 1;
//
//        String prefix = "SOAPGATE_";
//        env.put(prefix + "FORK_FLAG", "true");
//        env.put(prefix + "STEP_ID",String.valueOf(stepId));
//        env.put(prefix + "MACRO_LIST_COUNT", String.valueOf(stepsCount));
//
//        env.put(prefix + "MACRO_ITEM_NAME1", "MACROS_GLOBAL_eduid");
//        env.put(prefix + "MACRO_ITEM_VALUE1", "800000000110000009");
//
//        // SOAPGATE_FORK_FLAG=true;
//        // SOAPGATE_STEP_ID=2
//        // SOAPGATE_MACRO_LIST_COUNT=1;
//        // SOAPGATE_MACRO_ITEM_NAME1=MACROS_GLOBAL_eduid;
//        // SOAPGATE_MACRO_ITEM_VALUE1=1234567890AA0XX0987654321;
//
//
//        // Decode current JAR path
////        String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
////        String decodedPath = "";
////        try {
////            decodedPath = URLDecoder.decode(path, "UTF-8");
////        } catch (UnsupportedEncodingException e) {
////            e.printStackTrace();
////        }
////
////        System.out.println(decodedPath);
////        System.exit(0);
//        // stop forked here
//
//        ForkedStep forkedStep = new ForkedStep(
//                configFileName,
//                stepId,
//                env
//        );
//
//        forkedStep.fork();
//    }

}
