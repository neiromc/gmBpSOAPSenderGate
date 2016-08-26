package ru.geekmonkey.bp.yaml;

import java.util.ArrayList;

/**
 * Created by neiro on 18.07.16.
 */
public class Config {

    public String name;
    public String description;
    public String version;
    public String authors;
    public Boolean useThreads;
    public String zabbixServerIp;
    public String zabbixServerPort;
    public String zabbixServerConnTimeout;
    public String forkProcessLogConfig;
    public ArrayList globalMacrosList;
    public ArrayList steps;

}
