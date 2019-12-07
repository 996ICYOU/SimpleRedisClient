package doggy.vo;

import java.io.Serializable;

public class ConfigVo{
    private String name;
    private String host;
    private String port;
    private String pass;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public ConfigVo(String name, String host, String port, String pass) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.pass = pass == null?"":pass;
    }

    public ConfigVo() {
    }
}
