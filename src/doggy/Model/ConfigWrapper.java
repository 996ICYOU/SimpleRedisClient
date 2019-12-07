package doggy.Model;

import doggy.vo.ConfigVo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
@XmlRootElement(name = "RedisConfigList")
public class ConfigWrapper {
    private List<ConfigVo> configList;
    @XmlElement(name = "RedisConfig")
    public List<ConfigVo> getConfigList() {
        return configList;
    }

    public void setConfigList(List<ConfigVo> configList) {
        this.configList = configList;
    }

    public ConfigWrapper(List<ConfigVo> configList) {
        this.configList = configList;
    }

    public ConfigWrapper() {
    }
}
