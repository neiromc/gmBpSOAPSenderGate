package ru.geekmonkey.bp;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import org.apache.log4j.Logger;
import ru.geekmonkey.bp.yaml.Config;

import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by neiro on 19.08.16.
 */
public enum ConfigFile {

    INSTANCE;

    private Logger logger = Main.logger;
    private Config config = null;
    private String configFileName;

    public Config getConfig() {
        return config;
    }

    public String getFileName() {
        return configFileName;
    }

    public void load(final String configFN) {
        this.configFileName = configFN;

        try {

            YamlReader yamlReader = new YamlReader(
                    new FileReader(configFileName)
            );

            logger.info("Config file " + configFileName + " found.");

            config = yamlReader.read(Config.class);

        } catch (FileNotFoundException e) {
            logger.error("Config file " + configFileName + " not found.");
            logger.error(e);
            if ( logger.isDebugEnabled() )
                e.printStackTrace();
        } catch (YamlException e) {
            logger.error("Can't open config file " + configFileName);
            logger.error(e);
            if ( logger.isDebugEnabled() )
                e.printStackTrace();
        }

    }

}
