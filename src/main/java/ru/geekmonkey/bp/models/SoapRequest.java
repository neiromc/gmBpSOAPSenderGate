package ru.geekmonkey.bp.models;

/**
 * Created by neiro on 26.07.16.
 */
public class SoapRequest {

    private String serverUrl;
    private int timeoutConn;
    private int timeoutRead;
    private String soapMessageText;

    public SoapRequest(final String serverUrl,
                       final int timeoutConn,
                       final int timeoutRead,
                       final String soapMessageText)
    {
        this.serverUrl = serverUrl;
        this.timeoutConn = timeoutConn;
        this.timeoutRead = timeoutRead;
        this.soapMessageText = soapMessageText;
    }


    public String getServerUrl() {
        return serverUrl;
    }

    public int getTimeoutConn() {
        return timeoutConn;
    }

    public int getTimeoutRead() {
        return timeoutRead;
    }

    public String getSoapMessageText() {
        return soapMessageText;
    }

}
