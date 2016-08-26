package ru.geekmonkey.bp;

import org.apache.log4j.Logger;
import ru.geekmonkey.bp.models.ResponseResultList;
import ru.geekmonkey.bp.models.SoapRequest;
import ru.geekmonkey.bp.processors.GlobalMacrosProcessor;
import ru.geekmonkey.bp.processors.LastResultMatcherProcessor;
import ru.geekmonkey.bp.processors.MacrosProcessor;
import ru.geekmonkey.bp.processors.StepProcessor;
import ru.geekmonkey.bp.processors.runners.zabbix.ZabbixServer;
import ru.geekmonkey.bp.processors.runners.zabbix.ZabbixTrapper;
import ru.geekmonkey.bp.yaml.*;

import java.util.ArrayList;
import java.util.Date;

import static java.lang.Integer.parseInt;

/**
 * Created by neiro on 18.08.16.
 */
public class MainRunner {

    private Logger logger = Main.logger;
    private ResponseResultList responseResultList = Main.responseResultList;
    private Config config = ConfigFile.INSTANCE.getConfig();

    private String[] cmdArgs;
    private String jarFileName;

    public MainRunner(final String[] cmdArgs,
                      final String jarFileName) {
        this.cmdArgs = cmdArgs;
        this.jarFileName = jarFileName;
    }

    public void run() {
//        String configFileName;
        Boolean LL_DISCOVERY;
        StringBuilder llSB = new StringBuilder();


        logger.info("=== START MAIN PROCESS ===");

//        configFileName = cmdArgs[0];
        // if args[1] sets to LL_DISCOVERY - run discovery out
        try {
            LL_DISCOVERY = cmdArgs[1].trim().toLowerCase().equals("ll_discovery");
            llSB.append("{\n \"data\":[\n");
        } catch (ArrayIndexOutOfBoundsException e) {
            LL_DISCOVERY = false;
        }


        logger.info(String.format("CONFIG_HEADER: Name = %s, Description = \"%s\", Version = %s, Author(s) = %s",
                config.name, config.description, config.version, config.authors)
        );
        logger.info(String.format("ZABBIX_SERVER = %s:%s, Connection Timeout = %s", config.zabbixServerIp, config.zabbixServerPort, config.zabbixServerConnTimeout));
        logger.info(String.format("STEPS Count = %s", config.steps.size()));


        //Start Processing All GLOBAL Macroses ---
        logger.info("=== GLOBAL MACROS PROCESSING === START ===");

//            Boolean hasGlobalMacroses = false;
        ArrayList<GlobalMacros> globalMacrosList = new ArrayList<>();
        if ( config.globalMacrosList != null ) {
//                hasGlobalMacroses = true;
            for (int n = 0; n < config.globalMacrosList.size(); n++) {
                GlobalMacros macros = (GlobalMacros) config.globalMacrosList.get(n);
                globalMacrosList.add(macros);
            }
        }

        GlobalMacrosProcessor globalMacrosProcessor = new GlobalMacrosProcessor(globalMacrosList);

        globalMacrosProcessor.processing();
        logger.info("=== GLOBAL MACROS PROCESSING === END ===");

        logger.info("START STEPS...");

        //useThread True as default
        if ( config.useThreads == null) {
            config.useThreads = true;
        }


        for (int i = 0; i < config.steps.size(); i++) {
                Step step = (Step) config.steps.get(i);

                if (step.enabled) {
                    if ( ! LL_DISCOVERY) {

                        logger.info(String.format("URL=\"%s\", timeoutCONN=%s, timeoutREAD=%s",
                                step.url, step.timeoutConn, step.timeoutRead)
                        );
                        logger.info(String.format("id=%s, [%s@%s], description=\"%s\"",
                                step.id, step.zabbixHost, step.zabbixKey, step.description)
                        );

                        logger.info(String.format("CHECK_TYPE=%s, CHECK_EXPR=%s",
                                step.resultCheckMethod, step.resultCheckExpression)
                        );

                        //-------- Start lastResultMatcher Processing --------------
                        //Put all Matching values from config to array
                        ArrayList<LastResultMatching> lastResultMatchingList = new ArrayList<>();
                        if (step.lastResultMatching != null) {
                            for (int n = 0; n < step.lastResultMatching.size(); n++) {
                                LastResultMatching match = (LastResultMatching) step.lastResultMatching.get(n);
                                lastResultMatchingList.add(match);
                            }
                        }

                        //Check: if Previous Step has errors and current step has Matches from this previous steps -> skip step
                        int lastResultMatchingError = responseResultList.hasErrors(lastResultMatchingList);
                        if ( lastResultMatchingError == 0 ) {

                            if ( lastResultMatchingList.size() > 0 ) {
                                //Create lastResultMatcher Processor
                                LastResultMatcherProcessor matcherProcessor = new LastResultMatcherProcessor(
                                        i, step.data, responseResultList, lastResultMatchingList
                                );
                                step.data = matcherProcessor.processingData();
                            }


                            //Start Processing All Macroses ---
                            if ( step.macrosList != null ) {
                                ArrayList<Macros> macrosList = new ArrayList<>();

//                            for (int n = 0; n < step.macrosList.size(); n++) {
//                                macrosList.add( (Macros) step.macrosList.get(n));
//                            }
                                for (Object macrosObject : step.macrosList ) {
                                    macrosList.add( (Macros) macrosObject);
                                }

                                // Create MacrosProcessor Object
                                // Change LOCAL macros names by values
                                MacrosProcessor macrosProcessor = new MacrosProcessor(
                                        step.data, macrosList
                                );
                                step.data = macrosProcessor.processingData();

                            }

                            // Change GLOBAL macros names by values
                            if ( globalMacrosList.size() > 0 ) {
//                                step.data = globalMacrosProcessor.processingData(step.data, globalMacrosList);
                                step.data = globalMacrosProcessor.processingData(step.data);
                            }

                            //End processing macros ---


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
                            StepProcessor stepProcessor = null;

                            if ( (step.sleepBeforeRun != null) ) {
                                if (step.sleepBeforeRun > 0) {
                                    logger.info("(" + step.id + ") NEED Fork!!!");

                                    //fork_process.step_999
//                                    for (GlobalMacros gm : globalMacrosList) {
//                                        System.out.printf("name=%s, value=%s",
//                                                gm.name, gm.value);
//                                    }
//                                    System.exit(0);
                                    stepProcessor = new StepProcessor(true, step.id, step.sleepBeforeRun, globalMacrosList, jarFileName, ConfigFile.INSTANCE.getFileName());
                                }
                            } else {
                                logger.info("(" + step.id + ") DOES NOT NEED Fork!!!");
                                stepProcessor = new StepProcessor(false, step.id, step.sleepBeforeRun, responseResultList, soapRequest, zabbixServer, zabbixTrapper);
                            }

                            Thread reqThread = new Thread(stepProcessor);
                            reqThread.start();

                            if ( ! config.useThreads ) {
                                while (reqThread.isAlive()) {}
                            }


                        } else {
                            logger.info(String.format("SKIP STEP id=%s ==> Reason: Can't run step cause responseResultList has errors on STEP = %s",
                                    step.id,
                                    lastResultMatchingError
                            ));
                        }
                        //End ---



                    } else {
                        //LL_DISCOVERY is TRUE
                        llSB.append("\t{ \"{#STEPNAME}\":\"");
                        llSB.append(step.description);
                        llSB.append("\",\t\t\t \"{#STEPKEY}\":\"");
                        llSB.append(step.zabbixKey);
                        llSB.append("\"" + " }\n");
//                        llSB.append("\t{ \"{#STEPNAME}\":\"" + step.description + "\",\t\t\t \"{#STEPKEY}\":\"" + step.zabbixKey + "\"" + " }\n");
                    }
                } else {
                    logger.info(String.format("SKIP Step id=%s ==> Reason: step set's as disabled", step.id));
                }

            }

            if (LL_DISCOVERY) {
                //console out Zabbix DISCOVERY Json
                llSB.append(" ]\n}");
                System.out.println(llSB.toString());
                System.exit(0);
            } else {
                //console out current date/time - last running date/time in (UNIXTIME in sec / 1000)
                System.out.println(new Date().getTime()/1000);
            }



    }

}
