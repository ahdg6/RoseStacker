package dev.rosewood.rosestacker.stack.settings.entity;

import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosestacker.stack.EntityStackComparisonResult;
import dev.rosewood.rosestacker.stack.StackedEntity;
import dev.rosewood.rosestacker.stack.settings.EntityStackSettings;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Snowman;

public class SnowmanStackSettings extends EntityStackSettings {

    private final boolean dontStackIfNoPumpkin;

    public SnowmanStackSettings(CommentedFileConfiguration entitySettingsFileConfiguration) {
        super(entitySettingsFileConfiguration);

        this.dontStackIfNoPumpkin = this.settingsConfiguration.getBoolean("dont-stack-if-no-pumpkin");
    }

    @Override
    protected EntityStackComparisonResult canStackWithInternal(StackedEntity stack1, StackedEntity stack2) {
        Snowman snowman1 = (Snowman) stack1.getEntity();
        Snowman snowman2 = (Snowman) stack2.getEntity();

        if (this.dontStackIfNoPumpkin && (snowman1.isDerp() || snowman2.isDerp()))
            return EntityStackComparisonResult.NO_PUMPKIN;

        return EntityStackComparisonResult.CAN_STACK;
    }

    @Override
    protected void setDefaultsInternal() {
        this.setIfNotExists("dont-stack-if-no-pumpkin", false);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SNOWMAN;
    }

    @Override
    public Material getSpawnEggMaterial() {
        return null;
    }

    @Override
    public List<String> getDefaultSpawnRequirements() {
        return Collections.singletonList("on-ground");
    }

}
