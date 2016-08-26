package ru.geekmonkey.bp.processors;

import org.apache.log4j.Logger;
import ru.geekmonkey.bp.ForkedStep;
import ru.geekmonkey.bp.Main;
import ru.geekmonkey.bp.models.ResponseResultList;
import ru.geekmonkey.bp.models.SoapRequest;
import ru.geekmonkey.bp.processors.runners.zabbix.TrapperThread;
import ru.geekmonkey.bp.processors.runners.zabbix.ZabbixServer;
import ru.geekmonkey.bp.processors.runners.zabbix.ZabbixTrapper;
import ru.geekmonkey.bp.yaml.GlobalMacros;

import javax.xml.soap.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

/**
 * Created by neiro on 19.07.16.
 */
public class StepProcessor implements Runnable {

    private long threadId = Thread.currentThread().getId();
    private Logger logger = Main.logger;

    private int stepId;
    private Long sleepBeforeRun;
    private ResponseResultList responseResultList;
    private SoapRequest soapRequest;
    private ZabbixServer zabbixServer;
    private ZabbixTrapper zabbixTrapper;
    private Boolean mustDoForkProcess;

    public StepProcessor(final Boolean mustDoForkProcess,
                         final int stepId,
                         final Long sleepBeforeRun,
                         final ResponseResultList responseResultList,
                         final SoapRequest soapRequest,
                         final ZabbixServer zabbixServer,
                         final ZabbixTrapper zabbixTrapper)
    {
        this.mustDoForkProcess = mustDoForkProcess;
        this.stepId = stepId;
        this.sleepBeforeRun = sleepBeforeRun;
        this.responseResultList = responseResultList;
        this.soapRequest = soapRequest;
        this.zabbixServer = zabbixServer;
        this.zabbixTrapper = zabbixTrapper;
    }

    private ArrayList<GlobalMacros> globalMacrosList;
    private String configFileName;
    private String jarFileName;

    public StepProcessor(final Boolean mustDoForkProcess,
                         final int stepId,
                         final Long sleepBeforeRun,
                         final ArrayList<GlobalMacros> globalMacrosList,
                         final String jarFileName,
                         final String configFileName)
    {
        this.mustDoForkProcess = mustDoForkProcess;
        this.stepId = stepId;
        this.sleepBeforeRun = sleepBeforeRun;
        this.globalMacrosList = globalMacrosList;
        this.jarFileName = jarFileName;
        this.configFileName = configFileName;
    }

    public void run() {

        if ( mustDoForkProcess ) {
            // ===== RUN STEP PROCESSING AS FORK PROCESS =====
            logger.warn("=============== FORK RUNNED ================ ");

            String workDir = System.getProperty("user.dir");

            Map<String,String> env = new HashMap<>();

            String prefix = "SOAPGATE_";
            env.put(prefix + "FORK_FLAG", "true");
            env.put(prefix + "STEP_ID",String.valueOf(stepId));
            env.put(prefix + "MACRO_LIST_COUNT", String.valueOf(globalMacrosList.size()));

            for (int i = 0; i < globalMacrosList.size(); i++) {
                GlobalMacros gm = globalMacrosList.get(i);
                env.put(String.format("%sMACRO_ITEM_NAME%s", prefix, i), gm.name);
                env.put(String.format("%sMACRO_ITEM_VALUE%s", prefix, i), gm.value);
            }
//            env.put(prefix + "MACRO_ITEM_NAME1", "MACROS_GLOBAL_eduid");
//            env.put(prefix + "MACRO_ITEM_VALUE1", "800000000110000009");

            ForkedStep forkedStep = new ForkedStep(
                    jarFileName,
                    configFileName,
                    stepId,
                    env
            );

            forkedStep.fork();


        } else {

            // ===== RUN STEP PROCESSING AS NORMAL =====

            if (sleepBeforeRun != null) {
                if (sleepBeforeRun > 0) {
                    try {
                        logger.info(String.format("SLEEP STEP %s, before run thread = %sms",
                                stepId, sleepBeforeRun)
                        );

                        sleep(sleepBeforeRun);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }


            logger.info("Thread #" + threadId + " :: " + "("+stepId+") Request SOAP URL = [" + soapRequest.getServerUrl() + "]...");
            logger.info("Thread #" + threadId + " :: " + "("+stepId+") Request SOAP Message = " + soapRequest.getSoapMessageText());

            //Start thread for SOAP Request
            SOAPMessage soapResponse = null;

            try {
                // Create SOAP connection
                SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
                SOAPConnection soapConnection = soapConnectionFactory.createConnection();

                // Send SOAP Message to SOAP Server
                MessageFactory factory = MessageFactory.newInstance();
                SOAPMessage message = factory.createMessage(new MimeHeaders(),
                        new ByteArrayInputStream(soapRequest.getSoapMessageText().getBytes("UTF-8"))
                );

                // URL Connection - change connection and set Timeouts (Conn, Read)
                URL endpointUrl = new URL(new URL(soapRequest.getServerUrl()), "",
                        new URLStreamHandler() {
                            @Override
                            protected URLConnection openConnection(URL url) throws IOException {
                                URL target = new URL(url.toString());
                                URLConnection connection = target.openConnection();
                                // Connection settings
                                connection.setConnectTimeout(soapRequest.getTimeoutConn() * 1000);
                                connection.setReadTimeout(soapRequest.getTimeoutRead() * 1000);
                                return (connection);
                            }
                        });


                soapResponse = soapConnection.call(message, endpointUrl);

                // Process the SOAP Response
                logger.info("Thread #" + threadId + " :: " + "("+stepId+") Response SOAP Message = " + getResponseAsText(soapResponse));

                soapConnection.close();


            } catch (Exception e) {
                logger.error("Thread #" + threadId + " :: " + e);
                if (logger.isDebugEnabled())
                    e.printStackTrace();
            }

            // save response
            String currentResponseResult;
            currentResponseResult = ((soapResponse != null) ? getResponseResult(soapResponse) : "0");
            responseResultList.add(currentResponseResult);

            //run zabbix_sender Thread
            if (zabbixTrapper.getZabbixSendEnabled() != null)
                if (zabbixTrapper.getZabbixSendEnabled()) {
                    Thread trapperThread;

                    trapperThread = new Thread(new TrapperThread(
                            zabbixServer,
                            zabbixTrapper.getZabbixHost(),
                            zabbixTrapper.getZabbixKey(),
                            currentResponseResult
                    ));

                    trapperThread.start();
                }
        }
    }

    public String getResponseResult(SOAPMessage soapMessage) {
        String response;

        switch ( zabbixTrapper.getResultCheckMethod() ) {
            //true if we get response XML
            case "httpStatus"       : response = checkByHttpStatus(soapMessage);
                                      break;
            //works such as linux grep
            case "grep"             : response = checkByGrep(soapMessage);
                                      break;
            //find by regexp magic - i don't know how it's work :)
            case "regex"            : response = checkByRegex(soapMessage);
                                      break;
            //true if all strings in Expression Array finds in response XML. All strings!
            case "stringArray"      : response = checkByStringArray(soapMessage);
                                      break;
            //get last result and use in next request
            case "getLastResult"    : response = getResponseAsText(soapMessage);
                                      break;
            default                 : response = checkByHttpStatus(soapMessage);
                                      break;
        }
        return response;
    }

    private String checkByHttpStatus(SOAPMessage soapMessage) {
        Boolean result = false;

        try {
            result = ! soapMessage.getSOAPPart().getEnvelope().getBody().hasFault();
        } catch (SOAPException e) {
            logger.error("Thread #" + threadId + " :: " + e);
            if ( logger.isDebugEnabled() )
                e.printStackTrace();
        }

        return ( result ? "1" : "0" );
    }


    private String checkByGrep(SOAPMessage soapMessage) {
        Boolean result;
        result = getResponseAsText(soapMessage).contains(zabbixTrapper.getResultCheckExpression());
        return ( result ? "1" : "0" );
    }


    private String checkByRegex(SOAPMessage soapMessage) {
        Boolean result;
        Pattern p = Pattern.compile (zabbixTrapper.getResultCheckExpression(), Pattern.UNICODE_CASE);
        Matcher m = p.matcher(getResponseAsText(soapMessage));
        result = m.find();

        return ( result ? "1" : "0" );
    }


    private String checkByStringArray(SOAPMessage soapMessage) {
        Boolean result = true;

        //Get Expression string, and split it to String[] Array. Divided by ";;"
        String[] strMatch = zabbixTrapper.getResultCheckExpression().split(";;");

        //This part only for log. Show array as string
        String strMatch2Log = "";
        for ( String s : strMatch ) strMatch2Log += s + ", ";
        logger.info("Thread #" + threadId + " :: EXPR_SPLIT_ARRAY = \"" + strMatch2Log + "\"");

        //Check, if all fields find in string. Only All!
        for ( String str : strMatch ) {
            if ( ! getResponseAsText(soapMessage).contains(str)) {
                result = false;
                break;
            }
        }

        return ( result ? "1" : "0" );
    }


    private String getResponseAsText(SOAPMessage soapResponse) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        String result = "<NO_RESULT>";
        try {
            soapResponse.writeTo(stream);
            result = new String(stream.toByteArray(), "utf-8");
        } catch (Exception e) {
            logger.error("Thread #" + threadId + " :: " + e);
        }
        return result;
    }

}
