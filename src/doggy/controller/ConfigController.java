package doggy.controller;

import doggy.MainApplication;
import doggy.Model.ConfigMoael;
import doggy.Model.ConfigWrapper;
import doggy.jedis.JedisUtil;
import doggy.jedis.WrappedConnection;
import doggy.util.XmlUtil;
import doggy.vo.ConfigVo;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class ConfigController {
    @FXML
    private TextField name;
    @FXML
    private TextField host;
    @FXML
    private TextField port;
    @FXML
    private PasswordField password;

    public void setConfigView(TableView<ConfigMoael> configView) {
        this.configView = configView;
    }

    private TableView<ConfigMoael> configView;

    public void setRoot(Stage root) {
        this.root = root;
    }

    private Stage root;

    public void setName(String name) {
        this.name.setText(name);
    }

    public void setHost(String host) {
        this.host.setText(host);
    }

    public void setPort(String port) {
        this.port.setText(port);
    }

    public void setPassword(String password) {
        this.password.setText(password);
    }

    @FXML
    private Label tips;

    public void setMainApplication(MainApplication mainApplication) {
        this.mainApplication = mainApplication;
    }

    private MainApplication mainApplication;

    public String getName() {
        return name.getText();
    }

    public String getHost() {
        return host.getText();
    }

    public String getPort() {
        return port.getText();
    }

    public String getPassword() {
        return password.getText();
    }
    public void setHostReadonly(){
        this.host.setEditable(false);
    }
    public void setPortReadonly(){
        this.port.setEditable(false);
    }
    @FXML
    public void test(){
        if (doCheck()){
            doTest();
        }
    }
    private boolean doTest(){
        try {
            WrappedConnection connection = JedisUtil.establishConnection(getHost(), getPort());
            if (getPassword() != null && getPassword().length()>0){
                String auth = JedisUtil.doAuth(connection,getPassword());
                if (!"OK".equals(auth)){
                    tips.setText("╭(╯^╰)╮ 密码错误："+auth);
                    return false;
                } else {
                    tips.setText("连接成功 (๑*◡*๑)");
                    connection.disconnect();
                    return true;
                }
            } else {
                tips.setText("连接成功 (๑*◡*๑)");
                connection.disconnect();
                return true;
            }
        } catch (JedisConnectionException e){
            tips.setText("(╯﹏╰)b 连接失败："+e.getLocalizedMessage());
            return false;
        }
    }
    @FXML
    public void save(){
        if (doCheck() && doTest()){
            if ("添加配置".equals(root.getTitle())){
                mainApplication.configVos.add(new ConfigVo(getName(),getHost(),getPort(),getPassword()));
                if (XmlUtil.saveConfig(new ConfigWrapper(mainApplication.configVos))){
                    tips.setText("配置保存成功ヾ(✿ﾟ▽ﾟ)ノ");
                    mainApplication.getConfigMoaels().add(new ConfigMoael(getName(),getHost(),getPort(),getPassword()));
                } else {
                    mainApplication.configVos.remove(mainApplication.configVos.size()-1);
                    tips.setText("配置保存失败(｡•́︿•̀｡)");
                }
        } else {//修改配置
                int index = configView.getSelectionModel().getSelectedIndex();
                mainApplication.configVos.get(index).setName(getName());
                mainApplication.configVos.get(index).setPass(getPassword());
                mainApplication.getConfigMoaels().get(index).setPassword(getPassword());
                mainApplication.getConfigMoaels().get(index).setServerName(getName());
                XmlUtil.saveConfig(new ConfigWrapper(mainApplication.configVos));
                tips.setText("配置修改成功ヾ(✿ﾟ▽ﾟ)ノ");
            }
        }
    }
    private boolean doCheck(){
        if (name.getText() == null || name.getText().length()==0){
            tips.setText("请输入服务名称 (〃´-ω･) ");
            return false;
        } else if (host.getText() == null || host.getText().length()==0){
            tips.setText("请输入服务地址 (>ω･* )ﾉ");
            return false;
        }else if (port.getText() == null || port.getText().length()==0){
            tips.setText("请输入服务端口 ￣ω￣=");
            return false;
        } else
            return true;
    }
}
