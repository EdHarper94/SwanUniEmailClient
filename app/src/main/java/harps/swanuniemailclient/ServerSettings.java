package harps.swanuniemailclient;

/**
 * @file ServerSettings.java
 * @author Ed Harper
 * @date 29/03/2017
 *
 * Static IMAP server settings.
 */

public class ServerSettings {

    private static final String SERVER_ADDRESS = "mobile.swansea.ac.uk";
    private static final String INC_SETTINGS = "SSL/TLS";
    private static final Boolean INC_SLL = true;
    private static final int IN_PORT = 993;
    private static final String IN_PROTOCOL = "IMAP";
    private static final String OUT_SETTINGS = "STARTTLS";
    private static final int OUT_PORT = 587;

    public String getServerAddress(){
        return SERVER_ADDRESS;
    }

    public String getIncSettings(){
        return INC_SETTINGS;
    }

    public Boolean getIncSll(){
        return INC_SLL;
    }

    public int getInPort(){
        return IN_PORT;
    }

    public String getInProtocol(){
        return IN_PROTOCOL;
    }

    public String getOutSettings(){
        return OUT_SETTINGS;
    }

    public int getOutPort(){
        return OUT_PORT;
    }
}

