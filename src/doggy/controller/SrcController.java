package doggy.controller;

import doggy.MainApplication;
import doggy.Model.ConfigMoael;
import doggy.Model.ConfigWrapper;
import doggy.jedis.JedisUtil;
import doggy.jedis.WrappedConnection;
import doggy.util.XmlUtil;
import doggy.vo.ConfigVo;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SrcController {
    @FXML
    private TableView<ConfigMoael> configView;
    @FXML
    private TableColumn<ConfigMoael,String> nameColumn;
    @FXML
    private ListView<String> msgListView;

    private MainApplication mainApplication;

    public Map<String, WrappedConnection> getWrappedConnectionMap() {
        return wrappedConnectionMap;
    }

    private Map<String, WrappedConnection> wrappedConnectionMap = new HashMap<>(5);

    public void setMainApplication(MainApplication mainApplication) {
        this.mainApplication = mainApplication;
        configView.setItems(mainApplication.getConfigMoaels());
        msgListView.setItems(mainApplication.getMsgListView());
        //绑定命令输入框 Entry键执行命令
        this.mainApplication.getAutoCompleteTextField().setOnKeyPressed(keyEvent->{
            if (keyEvent.getCode() == KeyCode.ENTER){
                ConfigMoael vo = configView.getSelectionModel().getSelectedItem();
                if (vo != null) {
                    String cacheKey = String.format("%s:%s",vo.getServerHost(),vo.getServerPort());
                    WrappedConnection connection = wrappedConnectionMap.get(cacheKey);
                    if(connection != null) {
                        msgListView.getItems().add("执行命令->" + this.mainApplication.getAutoCompleteTextField().getText());
                        List<String> cmdResult = JedisUtil.executeCmd(connection,
                                this.mainApplication.getAutoCompleteTextField().getText());
                        if ( cmdResult.size() != 0)
                            msgListView.getItems().addAll(cmdResult);
                        //发送quit命令后清除当前选中状态
                        if (this.mainApplication.getAutoCompleteTextField().getText().equals("QUIT")){
                            configView.getSelectionModel().clearSelection();
                            connection.disconnect();
                            wrappedConnectionMap.remove(cacheKey);
                        }
                    }

                }
            }
        });
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(model-> model.getValue().serverNameProperty());
        nameColumn.setCellFactory(callback -> {
            TableCell<ConfigMoael, String> cell = new TextFieldTableCell<>();
            cell.setVisible(true);
            cell.setOnMouseClicked(event -> {
                //鼠标双击配置项来连接服务
                if (event.getClickCount() == 2){
                    //建立Redis连接并且进行Ping测试
                    ConfigMoael selectedOne = configView.getSelectionModel().getSelectedItem();
                    String cacheKey = String.format("%s:%s",selectedOne.getServerHost(),selectedOne.getServerPort());
                    try{
                        if (wrappedConnectionMap.containsKey(cacheKey)){
                            WrappedConnection wrappedConnection = wrappedConnectionMap.get(cacheKey);
                            String p = JedisUtil.doPing(wrappedConnection);
                            if ("PONG".equals(p)) {
                                mainApplication.getMsgListView().add("Ping 检测ok");
                            } else {
                                mainApplication.getMsgListView().add("Ping 检测异常："+p);
                                wrappedConnectionMap.remove(cacheKey);
                                configView.getSelectionModel().clearSelection();
                            }
                        } else {
                            mainApplication.getMsgListView().add(String.format("开始连接服务：%s(%s)", selectedOne.getServerName(), cacheKey));
                            WrappedConnection wrappedConnection = JedisUtil.establishConnection(selectedOne.getServerHost(),
                                    selectedOne.getServerPort());
                            if (selectedOne.getPassword().length() > 0) {
                                String auth = JedisUtil.doAuth(wrappedConnection, selectedOne.getPassword());
                                mainApplication.getMsgListView().add("密码授权：" + auth);
                            }
                            String pong = JedisUtil.doPing(wrappedConnection);
                            if ("PONG".equals(pong)) {
                                mainApplication.getMsgListView().add("成功建立连接！Ping 检测ok");
                                wrappedConnectionMap.put(cacheKey, wrappedConnection);
                            } else {
                                mainApplication.getMsgListView().add("TCP连接成功，但Ping失败：" + pong);
                            }
                        }
                    } catch (JedisConnectionException e){
                        mainApplication.getMsgListView().add("连接异常："+e.getLocalizedMessage());
                    }
                }
            });
            return cell;
        });

        msgListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2){//双击复制
                String copyText = msgListView.getSelectionModel().getSelectedItem();
                Map<DataFormat, Object> map = new HashMap<>();
                map.put(DataFormat.PLAIN_TEXT, copyText);
                //清除粘贴板历史内容
                Clipboard.getSystemClipboard().clear();
                Clipboard.getSystemClipboard().setContent(map);
                msgListView.getItems().add("❤内容已复制到粘贴板❤");
            }
        });
    }

    @FXML
    public void importConfig(){
        try{
            FileChooser fileChooser = new FileChooser();
            FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("导入XML配置","*.xml");
            fileChooser.getExtensionFilters().add(extensionFilter);
            fileChooser.setTitle("Simple Redis Cilent");
            File fromFile = fileChooser.showOpenDialog(mainApplication.getRootStage());
            if (fromFile != null){
              List<ConfigVo> list = XmlUtil.importConfig(fromFile);
              if (list.size()>0){
                  mainApplication.configVos.addAll(list);
                  for (ConfigVo vo : list) {
                      mainApplication.getConfigMoaels().add(new ConfigMoael(vo.getName(),vo.getHost(),vo.getPort(),vo.getPass()));
                  }
                  XmlUtil.saveConfig(new ConfigWrapper(mainApplication.configVos));
              }
              msgListView.getItems().add("从 "+fromFile.getAbsolutePath()+" 导入配置 "+list.size()+" 条");
            } else {
                msgListView.getItems().add("未选择任何文件");
            }
        } catch (Exception e){

        }
    }
    @FXML
    public void exportConfig(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simple Redis Cilent");
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("导出XML配置","*.xml");
        fileChooser.getExtensionFilters().add(extensionFilter);
        fileChooser.setInitialFileName("RedisConfig.xml");
        File exportTo = fileChooser.showSaveDialog(mainApplication.getRootStage());
        if(exportTo != null){
            try {
                XmlUtil.exportConfig(exportTo);
                msgListView.getItems().add("文件保存成功！路径："+exportTo.getAbsolutePath());
            } catch (IOException e) {
                msgListView.getItems().add("没有配置文件！请先添加配置后再来操作。");
            }
        } else {
            msgListView.getItems().add("未选择保存路径");
        }
    }
    @FXML
    public void openConfigWindow() throws IOException {
        //搭建舞台
        Stage stage = new Stage();
        stage.getIcons().add(mainApplication.getRootStage().getIcons().get(0));
        stage.setResizable(false);
        stage.setTitle("添加配置");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(mainApplication.getRootStage());
        //场景加载
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(mainApplication.getClass().getResource("view/addConfig.fxml"));
        stage.setScene(new Scene(fxmlLoader.load()));
        ConfigController configController =fxmlLoader.getController();
        configController.setMainApplication(mainApplication);
        configController.setRoot(stage);
        configController.setConfigView(configView);
        //大戏上演
        stage.show();
    }
    @FXML
    public void editConfigWindow() throws IOException {
        if (configView.getSelectionModel().getSelectedItem() == null){

            msgListView.getItems().add("请先选中要修改的数据。");
            return;
        }
        //搭建舞台
        Stage stage = new Stage();
        stage.getIcons().add(mainApplication.getRootStage().getIcons().get(0));
        stage.setResizable(false);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(mainApplication.getRootStage());
        //场景加载
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(mainApplication.getClass().getResource("view/addConfig.fxml"));
        stage.setScene(new Scene(fxmlLoader.load()));
        ConfigController configController =fxmlLoader.getController();
        configController.setMainApplication(mainApplication);
        //数据初始化
        if (configView.getSelectionModel().getSelectedItem() != null){
            stage.setTitle("修改配置");
            ConfigMoael moael = configView.getSelectionModel().getSelectedItem();
            configController.setHost(moael.getServerHost());
            configController.setName(moael.getServerName());
            configController.setPort(moael.getServerPort());
            configController.setPassword(moael.getPassword());
            configController.setHostReadonly();
            configController.setPortReadonly();
        }
        configController.setRoot(stage);
        configController.setConfigView(configView);
        //大戏上演
        stage.show();
    }
    @FXML
    public void deleteConfirm(){
        ConfigMoael vo = configView.getSelectionModel().getSelectedItem();
        if(vo == null){
            msgListView.getItems().add("请先选中要删除的数据");
            return;
        }
        //搭建舞台
        Stage stage = new Stage();
        stage.getIcons().add(mainApplication.getRootStage().getIcons().get(0));
        stage.setResizable(false);
        stage.setTitle("确认删除数据");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(mainApplication.getRootStage());
        //场景加载
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(mainApplication.getClass().getResource("view/confirm.fxml"));
        try {
            stage.setScene(new Scene(fxmlLoader.load()));
            ConfirmController confirmController = fxmlLoader.getController();
            confirmController.setConfigView(configView);
            confirmController.setMainApplication(mainApplication);
            confirmController.setRoot(stage);
            confirmController.setMsg(String.format("确定删除 %s(%s:%s)", vo.getServerName(), vo.getServerHost(), vo.getServerPort()));
            stage.show();
        } catch (IOException e) {
           msgListView.getItems().add("删除异常："+e.getLocalizedMessage());
        }
    }
}
