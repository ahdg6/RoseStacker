package dev.rosewood.rosestacker.stack.settings.entity;

import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosestacker.stack.EntityStackComparisonResult;
import dev.rosewood.rosestacker.stack.StackedEntity;
import dev.rosewood.rosestacker.stack.settings.EntityStackSettings;
import dev.rosewood.rosestacker.stack.settings.spawner.ConditionTags;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class CowStackSettings extends EntityStackSettings {

    public CowStackSettings(CommentedFileConfiguration entitySettingsFileConfiguration) {
        super(entitySettingsFileConfiguration);
    }

    @Override
    protected EntityStackComparisonResult canStackWithInternal(StackedEntity stack1, StackedEntity stack2) {
        return EntityStackComparisonResult.CAN_STACK;
    }

    @Override
    protected void setDefaultsInternal() {

    }

    @Override
    public EntityType getEntityType() {
        return EntityType.COW;
    }

    @Override
    public Material getSpawnEggMaterial() {
        return Material.COW_SPAWN_EGG;
    }

    @Override
    public List<String> getDefaultSpawnRequirements() {
        return ConditionTags.ANIMAL_TAGS;
    }

}
