package ru.geekmonkey.bp.yaml;

import java.util.ArrayList;

/**
 * Created by neiro on 18.07.16.
 */
public class Step {

    public Boolean enabled;
    public int id;
    public String description;
    public Long sleepBeforeRun;
    public String url;
    public int timeoutConn;
    public int timeoutRead;
    public String dataType;
    public String data;
    public ArrayList lastResultMatching;
    public ArrayList macrosList;
    public Boolean zabbixSendEnabled;
    public String zabbixHost;
    public String zabbixKey;
    public String resultCheckMethod;
    public String resultCheckExpression;

}
