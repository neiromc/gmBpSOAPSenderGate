package ru.geekmonkey.bp.processors.runners.zabbix;

/**
 * Created by neiro on 19.07.16.
 */
public class ZabbixTrapper {

    private ZabbixServer zabbixServer;
    private Boolean zabbixSendEnabled;
    private String zabbixHost;
    private String zabbixKey;
    private String resultCheckMethod;
    private String resultCheckExpression;

    public ZabbixTrapper(final ZabbixServer zabbixServer,
                         final Boolean zabbixEnabled,
                         final String zabbixHost,
                         final String zabbixKey,
                         final String resultCheckMethod,
                         final String resultCheckExpression)
    {
        this.zabbixServer = zabbixServer;
        this.zabbixSendEnabled = zabbixEnabled;
        this.zabbixHost = zabbixHost;
        this.zabbixKey = zabbixKey;
        this.resultCheckMethod = resultCheckMethod;
        this.resultCheckExpression = resultCheckExpression;

    }

    public Boolean getZabbixSendEnabled() {
        return zabbixSendEnabled;
    }

    public String getZabbixHost() {
        return zabbixHost;
    }

    public String getZabbixKey() {
        return zabbixKey;
    }

    public String getResultCheckMethod() {
        return resultCheckMethod;
    }

    public String getResultCheckExpression() {
        return resultCheckExpression;
    }
}
