package ru.geekmonkey.bp.processors.runners.zabbix;

/**
 * Created by neiro on 26.07.16.
 */
public class ZabbixServer {

    private String zabbixServerIp;
    private int zabbixServerPort;
    private int zabbixServerConnTimeout;

    public ZabbixServer(final String zabbixServerIp,
                        final int zabbixServerPort,
                        final int zabbixServerConnTimeout)
    {
        this.zabbixServerIp = zabbixServerIp;
        this.zabbixServerPort = zabbixServerPort;
        this.zabbixServerConnTimeout = zabbixServerConnTimeout;
    }

    public String getZabbixServerIp() {
        return zabbixServerIp;
    }

    public int getZabbixServerPort() {
        return zabbixServerPort;
    }

    public int getZabbixServerConnTimeout() {
        return zabbixServerConnTimeout;
    }
}
