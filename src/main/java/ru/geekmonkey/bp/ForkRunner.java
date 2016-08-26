package ru.geekmonkey.bp;

import org.apache.log4j.Logger;
import ru.geekmonkey.bp.models.ResponseResultList;
import ru.geekmonkey.bp.models.SoapRequest;
import ru.geekmonkey.bp.processors.GlobalMacrosProcessor;
import ru.geekmonkey.bp.processors.MacrosProcessor;
import ru.geekmonkey.bp.processors.StepProcessor;
import ru.geekmonkey.bp.processors.runners.zabbix.ZabbixServer;
import ru.geekmonkey.bp.processors.runners.zabbix.ZabbixTrapper;
import ru.geekmonkey.bp.yaml.Config;
import ru.geekmonkey.bp.yaml.GlobalMacros;
import ru.geekmonkey.bp.yaml.Macros;
import ru.geekmonkey.bp.yaml.Step;

import java.util.ArrayList;
import java.util.Map;

import static java.lang.Integer.parseInt;

/**
 * Created by neiro on 18.08.16.
 */
public class ForkRunner {

    private Logger logger = Main.logger;
    private ResponseResultList responseResultList = Main.responseResultList;

    private String jarFileName;
    private String configFileName;
    private Map<String,String> environments;

    private Config config = ConfigFile.INSTANCE.getConfig();

    public ForkRunner(final String jarFileName,
                      final String configFileName,
                      final Map<String, String> environments)
    {
        this.jarFileName = jarFileName;
        this.configFileName = configFileName;
        this.environments = environments;
    }

    public void run() {
        String prefix = "SOAPGATE_";

        if (environments.get(prefix + "FORK_FLAG").toLowerCase().equals("true")) {
//            logger.info("Trying to run fork process...");
            logger.info("FORK :: " + prefix + "FORK_FLAG => " + environments.get(prefix + "FORK_FLAG"));

            int stepId = parseInt(environments.get(prefix + "STEP_ID"));
            int globalMacroListCount = parseInt(environments.get(prefix + "MACRO_LIST_COUNT"));


//            logger.info("--------");
//            logger.info(environments);
//            logger.info("--------");

            ArrayList<GlobalMacros> globalMacrosList = new ArrayList<>();

            if (globalMacroListCount > 0) {
                for (int i = 0; i < globalMacroListCount; i++) {
                    //SOAPGATE_MACRO_ITEM_NAME1
                    String itemName  = prefix + "MACRO_ITEM_NAME" + i;
                    String itemValue = prefix + "MACRO_ITEM_VALUE" + i;
                    GlobalMacros globalMacrosItem = new GlobalMacros();
                        globalMacrosItem.name = environments.get(itemName);
                        globalMacrosItem.value = environments.get(itemValue);
                        globalMacrosItem.commandType = "internal";
                        globalMacrosItem.isCommand = false;
                    globalMacrosList.add(globalMacrosItem);
                }

            }

            logger.info(String.format("FORK :: globalMacrosList.size=%s",
                    globalMacrosList.size())
            );

            logger.info(String.format("FORK :: STEP_ID=%s, MACRO_LIST_COUNT=%s",
                    stepId,
                    globalMacrosList.size()
            ));

            logger.info(String.format("FORK :: CONFIG_FILE=%s", configFileName));


            Step step = (Step) config.steps.get(stepId - 1);
            if (step.enabled) {
                logger.info(String.format("FORK-STEP :: URL=\"%s\", timeoutCONN=%s, timeoutREAD=%s",
                        step.url, step.timeoutConn, step.timeoutRead)
                );
                logger.info(String.format("FORK-STEP :: id=%s, [%s@%s], description=\"%s\"",
                        step.id, step.zabbixHost, step.zabbixKey, step.description)
                );

                logger.info(String.format("FORK-STEP :: CHECK_TYPE=%s, CHECK_EXPR=%s",
                        step.resultCheckMethod, step.resultCheckExpression)
                );

                //Start Processing All Macroses ---
                if ( step.macrosList != null ) {
                    ArrayList<Macros> macrosList = new ArrayList<>();

                    for (Object macrosObject : step.macrosList ) {
                        macrosList.add( (Macros) macrosObject);
                    }

                    // Create MacrosProcessor Object
                    // Change LOCAL macros names by values
                    MacrosProcessor macrosProcessor = new MacrosProcessor(
                            step.data,
                            macrosList
                    );
                    step.data = macrosProcessor.processingData();
                }

                GlobalMacrosProcessor globalMacrosProcessor = new GlobalMacrosProcessor(
                        globalMacrosList
                );

//                logger.info("========");
//                for (GlobalMacros gm : globalMacrosList) {
//                    logger.info(String.format("name=%s, value=%s",
//                            gm.name, gm.value));
//                }
//                logger.info("========");

                if ( globalMacrosList.size() > 0 ) {
                    step.data = globalMacrosProcessor.processingData(step.data);
                }

//                logger.info("========");
//                logger.info(step.data);
//                logger.info("========");

                // --- end macro processing

                //Run thread for processing SOAP req/resp
                ZabbixServer zabbixServer = new ZabbixServer(config.zabbixServerIp,
                        parseInt(config.zabbixServerPort),
                        parseInt(config.zabbixServerConnTimeout)
                );
                ZabbixTrapper zabbixTrapper = new ZabbixTrapper(
                        zabbixServer,
                        step.zabbixSendEnabled,
                        step.zabbixHost,
                        step.zabbixKey,
                        step.resultCheckMethod,
                        step.resultCheckExpression
                );
                SoapRequest soapRequest = new SoapRequest(step.url, step.timeoutConn, step.timeoutRead, step.data);
                StepProcessor stepProcessing = new StepProcessor(false, step.id, step.sleepBeforeRun, responseResultList, soapRequest, zabbixServer, zabbixTrapper);
                Thread reqThread = new Thread(stepProcessing);
                reqThread.start();

                logger.info("FORK-STEP :: Thread Started...");

                while (reqThread.isAlive()) {}

            }

            logger.info("FORK :: COMLETE!");
//            System.exit(0);


        }

    }


}
