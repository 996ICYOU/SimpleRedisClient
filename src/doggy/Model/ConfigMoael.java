package doggy.Model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ConfigMoael {
    private final StringProperty serverName = new SimpleStringProperty("");
    private final StringProperty serverHost = new SimpleStringProperty("");
    private final StringProperty serverPort =  new SimpleStringProperty("");

    public String getPassword() {
        return password.get();
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public void setPassword(String password) {
        this.password.set(password);
    }

    private final StringProperty password = new SimpleStringProperty("");

    public ConfigMoael(String serverName, String serverHost, String serverPort, String password) {
        setServerHost(serverHost);
        setServerName(serverName);
        setServerPort(serverPort);
        if (password != null && password.length()>0){
            setPassword(password);
        }
    }

    public String getServerName() {
        return serverName.get();
    }

    public StringProperty serverNameProperty() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName.set(serverName);
    }

    public String getServerHost() {
        return serverHost.get();
    }

    public StringProperty serverHostProperty() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost.set(serverHost);
    }

    public String getServerPort() {
        return serverPort.get();
    }

    public StringProperty serverPortProperty() {
        return serverPort;
    }

    public void setServerPort(String serverPort) {
        this.serverPort.set(serverPort);
    }
}
