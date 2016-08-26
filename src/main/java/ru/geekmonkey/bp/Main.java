package ru.geekmonkey.bp;

import org.apache.log4j.Logger;
import ru.geekmonkey.bp.models.ResponseResultList;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Created by neiro on 18.07.16.
 */
public class Main {

    public static final Logger logger = Logger.getLogger(Main.class.getName());
    public static final ResponseResultList responseResultList = new ResponseResultList();
    private static String jarFileName;

//    private static String configFileName = "bpConfig01.yml";
//    private static FileHandler fileHandler;

    public static void main(String[] args) {

        //Try to Detect main JAR file. Need to run forks!
//        jarFileName = "/Users/neiro/Projects/IntelliJIdea/sandbox/bpSoapSender/target/gmZabbixSOAPSenderGate-jar-with-dependencies.jar";
        try {
            jarFileName = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).toString();
            if ( jarFileName.contains("jar") ) {
                logger.info("Detect jarFileName succesful = " + jarFileName);
            } else {
                logger.error("Detect jarFileName unsuccesful = " + jarFileName);
                System.exit(0);
            }
        } catch (URISyntaxException e) {
            logger.error("Can't detect current jarFileName");
            e.printStackTrace();
        }


        if (args.length > 0 ) {

            String configFileName = args[0];
            Boolean fork_process = false;

            //Load config file (singleton class)
            ConfigFile.INSTANCE.load(configFileName);

            //FORK :: Check, if must run as Step ForkProcess
            if (args.length > 1)
                fork_process = args[1].trim().toLowerCase().contains("fork_process.step_");

            if ( fork_process ) {

                logger.info("FORK :: Start ForkRunner");
                Map<String, String> env = System.getenv();
                ForkRunner forkRunner = new ForkRunner(jarFileName, configFileName, env);
                forkRunner.run();

            } else {

                //MAIN :: Run mainRunner if run not in FORK Process Mode
                MainRunner mainRunner = new MainRunner(args, jarFileName);
                mainRunner.run();

            }

            logger.info("MAIN COMLETE!");
        } else {
            logger.warn("Usage: java -jar <jar_file> config.yml");
        }
    }



}
