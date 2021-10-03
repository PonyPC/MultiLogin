package moe.caa.multilogin.core.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.SneakyThrows;
import moe.caa.multilogin.core.auth.response.HasJoinedResponse;
import moe.caa.multilogin.core.auth.response.Property;
import moe.caa.multilogin.core.auth.yggdrasil.serialize.HasJoinedResponseSerializer;
import moe.caa.multilogin.core.auth.yggdrasil.serialize.PropertySerializer;
import moe.caa.multilogin.core.data.config.AdvancedSetting;
import moe.caa.multilogin.core.data.config.GeneralConfig;
import moe.caa.multilogin.core.data.database.SQLManager;
import moe.caa.multilogin.core.impl.IPlugin;
import moe.caa.multilogin.core.language.LanguageHandler;
import moe.caa.multilogin.core.logger.LoggerLevel;
import moe.caa.multilogin.core.logger.MultiLogger;
import moe.caa.multilogin.core.util.IOUtil;
import moe.caa.multilogin.core.util.YamlReader;
import moe.caa.multilogin.core.yggdrasil.YggdrasilServicesHandler;

import java.io.File;
import java.io.FileInputStream;

@Getter
public class MultiCore {
    @Getter
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(HasJoinedResponse.class, new HasJoinedResponseSerializer())
            .registerTypeAdapter(Property.class, new PropertySerializer()).create();

    private final IPlugin plugin;
    private final MultiLogger logger;
    private final LanguageHandler languageHandler;
    private final YggdrasilServicesHandler yggdrasilServicesHandler;
    private final SQLManager sqlManager;
    private final GeneralConfig config;
    private final AdvancedSetting setting;

    private final File generalConfig;
    private final File dataFolder;

    /**
     * 构建插件核心
     *
     * @param plugin 插件实例
     */
    @SneakyThrows
    public MultiCore(IPlugin plugin) {
        this.plugin = plugin;
        dataFolder = plugin.getDataFolder();
        generalConfig = new File(dataFolder, "config.yml");
        setting = new AdvancedSetting();
        config = new GeneralConfig();
        logger = new MultiLogger(this, setting.isLogger_debug());
        languageHandler = new LanguageHandler();
        yggdrasilServicesHandler = new YggdrasilServicesHandler();
        sqlManager = new SQLManager(this);
    }

    /**
     * 初始化
     *
     * @return 初始化是否成功
     */
    public boolean init() {
        try {
            return init0();
        } catch (Throwable e) {
            logger.log(LoggerLevel.ERROR, "A fatal error was encountered while loading the plugin.", e);
            return false;
        }
    }

    /**
     * 初始化操作
     *
     * @return 初始化是否成功
     */
    private boolean init0() throws Throwable {
        // 数据文件夹
        IOUtil.createNewFileOrFolder(dataFolder, true);
        // 初始化和读取高级配置文件
        generateSettingFile();
        // 重新设置DEBUG属性
        logger.setDebug(setting.isLogger_debug());
        // 初始化文件日志记录器
        logger.init();
        // 读取一般配置文件
        if (!generateConfigFile()) return false;
        // 读取语言文件
        if (!languageHandler.init(this, "message.properties")) return false;
        // 加载 Yggdrasil 账户验证服务器配置
        yggdrasilServicesHandler.init(config.getReader().get("services", YamlReader.class));
        // 连接数据库操作
        if (!sqlManager.init(config.getReader().get("sql", YamlReader.class))) return false;

        // 后端实现任务
        plugin.initService();
        plugin.initOther();
        logger.log(LoggerLevel.INFO, "插件已加载");
        return true;
    }

    public void disable() {
        sqlManager.close();
        plugin.getRunServer().getScheduler().shutdown();
    }

    /**
     * 读取外置高级配置文件
     */
    private void generateSettingFile() {
        File file = new File(dataFolder, "advanced_setting.properties");
        if (!file.exists()) return;
        try {
            setting.load(new FileInputStream(file));
        } catch (Exception e) {
            logger.log(LoggerLevel.WARN, String.format("Failed to read advanced configuration file, Will keep the default values. (%s)", file.getAbsolutePath()), e);
        }
    }

    /**
     * 读取一般配置文件
     *
     * @return 成功标志
     */
    public boolean generateConfigFile() {
        try {
            IOUtil.saveResource(IOUtil.getJarResource("config.yml"), generalConfig, false);
            config.reader(generalConfig);
            return true;
        } catch (Exception e) {
            logger.log(LoggerLevel.ERROR, String.format("Failed to read general configuration file. (%s)", generalConfig.getAbsolutePath()), e);
            return false;
        }
    }
}
