package doggy;

import com.sun.imageio.plugins.jpeg.JPEGImageReader;
import doggy.Controls.AutoCompleteTextField;
import doggy.Model.ConfigMoael;
import doggy.controller.SrcController;
import doggy.jedis.JedisUtil;
import doggy.jedis.WrappedConnection;
import doggy.util.XmlUtil;
import doggy.vo.ConfigVo;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Properties;

public class MainApplication extends Application {
    private Stage rootStage;

    public AutoCompleteTextField getAutoCompleteTextField() {
        return autoCompleteTextField;
    }

    private AutoCompleteTextField autoCompleteTextField;
    public Stage getRootStage() {
        return rootStage;
    }
    private ObservableList<ConfigMoael> configMoaels = FXCollections.observableArrayList();

    public ObservableList<String> getMsgListView() {
        return msgListView;
    }

    private ObservableList<String> msgListView = FXCollections.observableArrayList();

    public ObservableList<ConfigMoael> getConfigMoaels() {
        return configMoaels;
    }
    private SrcController rootController;
    public List<ConfigVo> configVos;
    public String appRootPath;
    public Properties i18n;
    @Override
    public void start(Stage primaryStage) throws Exception{
        Image logo = new Image(getClass().getResourceAsStream("logo.jpg"));
        primaryStage.getIcons().add(logo);
        primaryStage.setTitle("Redis简易客户端");
        //舞台
        this.rootStage = primaryStage;
        primaryStage.setMinHeight(850);
        primaryStage.setMinWidth(1060);
        primaryStage.setResizable(true);
        //加载创建 舞台场景
        FXMLLoader rootLoader = new FXMLLoader();
        rootLoader.setLocation(getClass().getResource("view/src.fxml"));
        BorderPane rootPane = rootLoader.load();
        //初始化自动补全输入控件
        this.autoCompleteTextField = new AutoCompleteTextField(JedisUtil.getRedisCommand());
        this.autoCompleteTextField.setAlignment(Pos.CENTER);
        this.autoCompleteTextField.setPromptText("输入命令，按Enter发送，可自动补全");
        this.autoCompleteTextField.setFont(new Font("System Bold",14.00));
        rootPane.setBottom(this.autoCompleteTextField);
        //初始化数据
        this.rootController = rootLoader.getController();
        this.rootController.setMainApplication(this);
        Scene rootScene = new Scene(rootPane);
        //场景放入舞台
        primaryStage.setScene(rootScene);
        //大戏开幕
        primaryStage.show();
    }

    @Override
    public void init(){
        appRootPath = getClass().getResource("").getPath();
        msgListView.add("双击即可复制内容");
        configVos = XmlUtil.loadConfig();
        for (ConfigVo vo : configVos) {
            configMoaels.add(new ConfigMoael(vo.getName(),vo.getHost(),vo.getPort(),vo.getPass()));
        }
    }

    @Override
    public void stop() throws Exception {
        //程序关闭时销毁资源
        for (WrappedConnection connection : this.rootController.getWrappedConnectionMap().values()){
            connection.disconnect();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
