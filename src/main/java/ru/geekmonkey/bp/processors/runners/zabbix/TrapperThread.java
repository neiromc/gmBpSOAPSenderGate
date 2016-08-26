package ru.geekmonkey.bp.processors.runners.zabbix;

import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

//import java.util.logging.Logger;

/**
 * Created by neiro on 19.07.16.
 */
public class TrapperThread implements Runnable {

//    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private long threadId = Thread.currentThread().getId();

    private Logger logger = ru.geekmonkey.bp.Main.logger;
    private ZabbixServer zabbixServer;
    private String host;
    private String item;
    private String value;

    public TrapperThread(final ZabbixServer zabbixServer,
                         final String host,
                         final String item,
                         final String value)
    {
        this.zabbixServer = zabbixServer;
        this.host = host;
        this.item = item;
        this.value = value;
    }

    public void run() {

        //build JSON request to zabbix server
        String report = buildJSonString(host, item, value);
        logger.info("Thread #" + threadId + " :: " + "ZABBIX_SENDER_JSON = " + report.replaceAll("\n",""));

        //create connection to zabbix_server if JSON Build Good
        try {
            logger.info("Thread #" + threadId + " :: " + "Trying to connect to Zabbix Server " +
                    zabbixServer.getZabbixServerIp() + ":" + zabbixServer.getZabbixServerPort());
            Socket clientSocket=new Socket();
            clientSocket.connect(new InetSocketAddress(
                    zabbixServer.getZabbixServerIp(),
                    zabbixServer.getZabbixServerPort()
            ), zabbixServer.getZabbixServerConnTimeout());

            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());

            //send message to zabbix trapper
            try {
                writeMessage(dos, report.getBytes());
            } catch (IOException e) {
                logger.error("Thread #" + threadId + " :: " + "Can't write to OutputStream while sending data to zabbix trapper");
                logger.error("Thread #" + threadId + " :: " + e);
                if ( logger.isDebugEnabled() )
                    e.printStackTrace();
            }


            logger.info("Thread #" + threadId + " :: " + String.valueOf(dis.readByte()));


            //close connection with zabbix server
            clientSocket.close();

        } catch (IOException e) {
            logger.error("Thread #" + threadId + " :: " + "Can't connect to Zabbix Server for sending results in trapper");
            logger.error("Thread #" + threadId + " :: " + e);
            if ( logger.isDebugEnabled() )
                e.printStackTrace();
        }
    }


    private String buildJSonString(final String host,
                                   final String item,
                                   final String value)
    {
        return "{"
                + "\"request\":\"sender data\",\n"
                + "\"data\":[\n"
                +        "{\n"
                +                "\"host\":\"" + host + "\",\n"
                +                "\"key\":\"" + item + "\",\n"
                +                "\"value\":\"" + value.replace("\\", "\\\\") + "\"}]}\n" ;
    }


    protected void writeMessage(OutputStream out, byte[] data) throws IOException {
        int length = data.length;

        out.write(new byte[] {
                'Z', 'B', 'X', 'D',
                '\1',
                (byte)(length & 0xFF),
                (byte)((length >> 8) & 0x00FF),
                (byte)((length >> 16) & 0x0000FF),
                (byte)((length >> 24) & 0x000000FF),
                '\0','\0','\0','\0'});

        out.write(data);
    }
    
}
