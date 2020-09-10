package dev.rosewood.rosestacker.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.manager.Manager;
import dev.rosewood.rosestacker.stack.settings.BlockStackSettings;
import dev.rosewood.rosestacker.stack.settings.EntityStackSettings;
import dev.rosewood.rosestacker.stack.settings.ItemStackSettings;
import dev.rosewood.rosestacker.stack.settings.SpawnerStackSettings;
import dev.rosewood.rosestacker.stack.settings.spawner.ConditionTags;
import dev.rosewood.rosestacker.utils.ClassUtils;
import dev.rosewood.rosestacker.utils.StackerUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

public class StackSettingManager extends Manager {

    private static final String PACKAGE_PATH = "dev.rosewood.rosestacker.stack.settings.entity";

    private Map<Material, BlockStackSettings> blockSettings;
    private Map<EntityType, EntityStackSettings> entitySettings;
    private Map<Material, ItemStackSettings> itemSettings;
    private Map<EntityType, SpawnerStackSettings> spawnerSettings;

    public StackSettingManager(RosePlugin rosePlugin) {
        super(rosePlugin);

        this.blockSettings = new HashMap<>();
        this.entitySettings = new HashMap<>();
        this.itemSettings = new HashMap<>();
        this.spawnerSettings = new HashMap<>();
    }

    @Override
    public void reload() {
        // Settings files
        File blockSettingsFile = this.getBlockSettingsFile();
        File entitySettingsFile = this.getEntitySettingsFile();
        File itemSettingsFile = this.getItemSettingsFile();
        File spawnerSettingsFile = this.getSpawnerSettingsFile();

        // Flags for if we should save the files
        AtomicBoolean saveBlockSettingsFile = new AtomicBoolean(false);
        AtomicBoolean saveEntitySettingsFile = new AtomicBoolean(false);
        AtomicBoolean saveItemSettingsFile = new AtomicBoolean(false);
        AtomicBoolean saveSpawnerSettingsFile = new AtomicBoolean(false);

        // Load block settings
        CommentedFileConfiguration blockSettingsConfiguration = CommentedFileConfiguration.loadConfiguration(blockSettingsFile);
        StackerUtils.getPossibleStackableBlockMaterials().forEach(x -> {
            BlockStackSettings blockStackSettings = new BlockStackSettings(blockSettingsConfiguration, x);
            this.blockSettings.put(x, blockStackSettings);
            if (blockStackSettings.hasChanges())
                saveBlockSettingsFile.set(true);
        });

        // Load entity settings
        CommentedFileConfiguration entitySettingsConfiguration = CommentedFileConfiguration.loadConfiguration(entitySettingsFile);
        try {
            List<Class<EntityStackSettings>> classes = ClassUtils.getClassesOf(this.rosePlugin, PACKAGE_PATH, EntityStackSettings.class);
            List<String> ignoredLoading = new ArrayList<>();
            for (Class<EntityStackSettings> clazz : classes) {
                try {
                    EntityStackSettings entityStackSetting = clazz.getConstructor(CommentedFileConfiguration.class).newInstance(entitySettingsConfiguration);
                    this.entitySettings.put(entityStackSetting.getEntityType(), entityStackSetting);
                    if (entityStackSetting.hasChanges())
                        saveEntitySettingsFile.set(true);
                } catch (Exception e) {
                    // Log entity settings that failed to load
                    // This should only be caused by version incompatibilities
                    String className = clazz.getSimpleName();
                    ignoredLoading.add(className.substring(0, className.length() - 13));
                }
            }

            if (!ignoredLoading.isEmpty())
                this.rosePlugin.getLogger().warning("Ignored loading stack settings for entities: " + ignoredLoading);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load item settings
        CommentedFileConfiguration itemSettingsConfiguration = CommentedFileConfiguration.loadConfiguration(itemSettingsFile);
        Stream.of(Material.values()).sorted(Comparator.comparing(Enum::name)).forEach(x -> {
            ItemStackSettings itemStackSettings = new ItemStackSettings(itemSettingsConfiguration, x);
            this.itemSettings.put(x, itemStackSettings);
            if (itemStackSettings.hasChanges())
                saveItemSettingsFile.set(true);
        });

        // Load spawner settings
        boolean addSpawnerHeaderComments = !spawnerSettingsFile.exists();
        CommentedFileConfiguration spawnerSettingsConfiguration = CommentedFileConfiguration.loadConfiguration(spawnerSettingsFile);
        if (addSpawnerHeaderComments) {
            saveSpawnerSettingsFile.set(true);
            Map<String, String> conditionTags = ConditionTags.getTagDescriptionMap();
            spawnerSettingsConfiguration.addComments("Available Spawn Requirements:", "");
            for (Entry<String, String> entry : conditionTags.entrySet()) {
                String tag = entry.getKey();
                String description = entry.getValue();
                spawnerSettingsConfiguration.addComments(tag + " - " + description);
            }

            spawnerSettingsConfiguration.addComments(
                    "",
                    "Valid Blocks: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html",
                    "Valid Biomes: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/block/Biome.html"
            );
        }

        StackerUtils.getAlphabeticalStackableEntityTypes().forEach(x -> {
            SpawnerStackSettings spawnerStackSettings = new SpawnerStackSettings(spawnerSettingsConfiguration, x);
            this.spawnerSettings.put(x, spawnerStackSettings);
            if (spawnerStackSettings.hasChanges())
                saveSpawnerSettingsFile.set(true);
        });

        // Save files if changes were made
        if (saveBlockSettingsFile.get())
            blockSettingsConfiguration.save(true);
        if (saveEntitySettingsFile.get())
            entitySettingsConfiguration.save(true);
        if (saveItemSettingsFile.get())
            itemSettingsConfiguration.save(true);
        if (saveSpawnerSettingsFile.get())
            spawnerSettingsConfiguration.save(true);
    }

    @Override
    public void disable() {
        this.blockSettings.clear();
        this.entitySettings.clear();
        this.itemSettings.clear();
        this.spawnerSettings.clear();
    }

    public File getBlockSettingsFile() {
        return new File(this.rosePlugin.getDataFolder(), "block_settings.yml");
    }

    public File getEntitySettingsFile() {
        return new File(this.rosePlugin.getDataFolder(), "entity_settings.yml");
    }

    public File getItemSettingsFile() {
        return new File(this.rosePlugin.getDataFolder(), "item_settings.yml");
    }

    public File getSpawnerSettingsFile() {
        return new File(this.rosePlugin.getDataFolder(), "spawner_settings.yml");
    }

    public BlockStackSettings getBlockStackSettings(Material material) {
        return this.blockSettings.get(material);
    }

    public BlockStackSettings getBlockStackSettings(Block block) {
        return this.getBlockStackSettings(block.getType());
    }

    public EntityStackSettings getEntityStackSettings(EntityType entityType) {
        return this.entitySettings.get(entityType);
    }

    public EntityStackSettings getEntityStackSettings(LivingEntity entity) {
        return this.getEntityStackSettings(entity.getType());
    }

    public EntityStackSettings getEntityStackSettings(Material material) {
        if (!StackerUtils.isSpawnEgg(material))
            return null;

        for (EntityType key : this.entitySettings.keySet()) {
            EntityStackSettings settings = this.entitySettings.get(key);
            if (settings.getSpawnEggMaterial() == material)
                return settings;
        }

        return null;
    }

    public ItemStackSettings getItemStackSettings(Material material) {
        return this.itemSettings.get(material);
    }

    public ItemStackSettings getItemStackSettings(Item item) {
        return this.getItemStackSettings(item.getItemStack().getType());
    }

    public SpawnerStackSettings getSpawnerStackSettings(EntityType entityType) {
        return this.spawnerSettings.get(entityType);
    }

    public SpawnerStackSettings getSpawnerStackSettings(CreatureSpawner creatureSpawner) {
        return this.getSpawnerStackSettings(creatureSpawner.getSpawnedType());
    }

    public Set<EntityType> getStackableEntityTypes() {
        return this.entitySettings.values().stream()
                .filter(EntityStackSettings::isStackingEnabled)
                .map(EntityStackSettings::getEntityType)
                .collect(Collectors.toSet());
    }

    public Set<Material> getStackableItemTypes() {
        return this.itemSettings.values().stream()
                .filter(ItemStackSettings::isStackingEnabled)
                .map(ItemStackSettings::getType)
                .collect(Collectors.toSet());
    }

    public Set<Material> getStackableBlockTypes() {
        return this.blockSettings.values().stream()
                .filter(BlockStackSettings::isStackingEnabled)
                .map(BlockStackSettings::getType)
                .collect(Collectors.toSet());
    }

    public Set<EntityType> getStackableSpawnerTypes() {
        return this.spawnerSettings.values().stream()
                .filter(SpawnerStackSettings::isStackingEnabled)
                .map(SpawnerStackSettings::getEntityType)
                .collect(Collectors.toSet());
    }

}
