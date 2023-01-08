package org.kingdoms.peacetreaties.config;

import org.kingdoms.config.EnumConfig;
import org.kingdoms.config.KeyedConfigAccessor;
import org.kingdoms.config.implementation.KeyedYamlConfigAccessor;
import org.kingdoms.config.managers.ConfigManager;
import org.kingdoms.config.managers.ConfigWatcher;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.peacetreaties.PeaceTreatiesAddon;
import org.kingdoms.peacetreaties.terms.TermRegistry;
import org.kingdoms.utils.config.ConfigPath;
import org.kingdoms.utils.config.adapters.YamlResource;
import org.kingdoms.utils.string.StringUtils;

public enum PeaceTreatyConfig implements EnumConfig {
    DURATION,
    MIN_TERMS,
    UNFINISHED_CONTRACT_REMINDER,
    WAR_POINTS_MAX(2),
    WAR_POINTS_SCORES_KILL(2, 3),
    WAR_POINTS_SCORES_INVADE(2, 3),
    ;

    public static final YamlResource PEACE_TREATIES = new YamlResource(PeaceTreatiesAddon.get(), Kingdoms.getPath("peace-treaties.yml").toFile(), "peace-treaties.yml").load();

    static {
        ConfigWatcher.register(PEACE_TREATIES.getFile().toPath().getParent(), ConfigWatcher::handleNormalConfigs);
        ConfigManager.registerNormalWatcher("peace-treaties", (event) -> {
            ConfigWatcher.reload(PEACE_TREATIES, "peace-treaties.yml");
            TermRegistry.loadTermGroupings();
        });
    }

    private final ConfigPath option;

    PeaceTreatyConfig() {
        this.option = new ConfigPath(StringUtils.configOption(this));
    }

    PeaceTreatyConfig(String option) {
        this.option = new ConfigPath(option);
    }

    PeaceTreatyConfig(int... grouped) {
        this.option = new ConfigPath(this.name(), grouped);
    }

    @Override
    public KeyedConfigAccessor getManager() {
        return new KeyedYamlConfigAccessor(PEACE_TREATIES, option);
    }

    public static YamlResource getConfig() {
        return PEACE_TREATIES;
    }
}
