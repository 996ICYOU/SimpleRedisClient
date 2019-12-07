package doggy.util;

import doggy.Model.ConfigWrapper;
import doggy.vo.ConfigVo;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class XmlUtil {
    /**
     * 配置默认保存
     * @param configWrapper
     * @return
     */
    public static boolean saveConfig(ConfigWrapper configWrapper){
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(configWrapper.getClass());
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(configWrapper, new File("RedisConfig.xml"));
            return true;
        } catch (Exception e){
            return false;
        }
    }

    /**
     * 加载默认配置
     * @return
     */
    public static List<ConfigVo> loadConfig(){
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ConfigWrapper.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ConfigWrapper configWrapper = (ConfigWrapper) unmarshaller.unmarshal(new File("RedisConfig.xml"));
            return configWrapper.getConfigList();
        }catch (Exception e){
            return new ArrayList<>();
        }
    }

    /**
     * 从磁盘导入配置
     * @param fromFile
     * @return
     */
    public static List<ConfigVo> importConfig(File fromFile){
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ConfigWrapper.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ConfigWrapper configWrapper = (ConfigWrapper) unmarshaller.unmarshal(fromFile);
            return configWrapper.getConfigList();
        }catch (Exception e){
            return new ArrayList<>();
        }
    }

    /**
     * 配置导出(另存为)
     * @param saveTo
     * @return
     */
    public static boolean exportConfig(File saveTo) throws IOException {
        File defaultConfig = new File("RedisConfig.xml");
        FileChannel outputChannel = new FileOutputStream(saveTo).getChannel();
        FileChannel inputChannel = new FileInputStream(defaultConfig).getChannel();
        outputChannel.transferFrom(inputChannel,0, inputChannel.size());
        return true;
    }
}
