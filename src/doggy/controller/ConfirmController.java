package doggy.controller;

import doggy.MainApplication;
import doggy.Model.ConfigMoael;
import doggy.Model.ConfigWrapper;
import doggy.jedis.JedisUtil;
import doggy.util.XmlUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class ConfirmController {
    public void setMsg(String msg) {
        this.msg.setText(msg);
    }

    @FXML
    private Label msg;
    @FXML
    private Button yes;
    @FXML
    private Button no;

    public void setConfigView(TableView<ConfigMoael> configView) {
        this.configView = configView;
    }

    private TableView<ConfigMoael> configView;

    public void setMainApplication(MainApplication mainApplication) {
        this.mainApplication = mainApplication;
    }

    private MainApplication mainApplication;

    public void setRoot(Stage root) {
        this.root = root;
    }

    private Stage root;

    @FXML
    public void handelYES(){
        int index = configView.getSelectionModel().getSelectedIndex();
        mainApplication.configVos.remove(index);
        configView.getItems().remove(index);
        XmlUtil.saveConfig(new ConfigWrapper(mainApplication.configVos));
        mainApplication.getMsgListView().add("数据已删除");
        root.close();
    }
    @FXML
    public void handleNo(){
        mainApplication.getMsgListView().add("您已取消删除。");
        root.close();
    }
}
