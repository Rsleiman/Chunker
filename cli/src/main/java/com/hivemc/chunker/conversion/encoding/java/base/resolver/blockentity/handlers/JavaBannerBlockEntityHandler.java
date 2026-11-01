package com.hivemc.chunker.conversion.encoding.java.base.resolver.blockentity.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hivemc.chunker.conversion.encoding.base.resolver.blockentity.BlockEntityHandler;
import com.hivemc.chunker.conversion.encoding.base.resolver.blockentity.CustomItemNBTBlockEntityHandler;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.BannerBlockEntity;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerDyeColor;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.banner.ChunkerBannerPattern;
import com.hivemc.chunker.nbt.TagType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import com.hivemc.chunker.nbt.tags.primitive.StringTag;
import com.hivemc.chunker.util.JsonTextUtil;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handler for Banner Block Entities which also handles writing the component data.
 */
public class JavaBannerBlockEntityHandler extends BlockEntityHandler<JavaResolvers, CompoundTag, BannerBlockEntity> implements CustomItemNBTBlockEntityHandler<JavaResolvers, BannerBlockEntity> {
    /**
     * The translation key of the ominous banner's name.
     */
    private static final String OMINOUS_BANNER_NAME = "block.minecraft.ominous_banner";

    public JavaBannerBlockEntityHandler() {
        super("minecraft:banner", BannerBlockEntity.class, BannerBlockEntity::new);
    }

    @Override
    public void read(@NotNull JavaResolvers resolvers, @NotNull CompoundTag input, @NotNull BannerBlockEntity value) {
        // Parse base
        value.setBase(input.getOptionalValue("Base", Integer.class).flatMap(ChunkerDyeColor::getColorByID));

        // Parse patterns
        ListTag<CompoundTag, Map<String, Tag<?>>> patternTags = input.getList("patterns", CompoundTag.class, null);
        if (patternTags == null) {
            patternTags = input.getList("Patterns", CompoundTag.class, null);
        }

        if (patternTags != null) {
            // Loop through each pattern
            for (CompoundTag patternTag : patternTags) {
                Optional<ChunkerDyeColor> color;
                if (patternTag.contains("Color")) {
                    color = patternTag.getOptionalValue("Color", Integer.class)
                            .flatMap(ChunkerDyeColor::getColorByID);
                } else {
                    color = patternTag.getOptionalValue("color", String.class)
                            .flatMap(ChunkerDyeColor::getColorByName);
                }
                Optional<ChunkerBannerPattern> pattern;
                if (patternTag.contains("Pattern")) {
                    pattern = patternTag.getOptionalValue("Pattern", String.class)
                            .flatMap(resolvers.bannerPatternShortNameResolver()::to);
                } else {
                    pattern = patternTag.getOptionalValue("pattern", String.class)
                            .flatMap(resolvers.bannerPatternResolver()::to);
                }
                // If the color and pattern are present
                if (color.isPresent() && pattern.isPresent()) {
                    value.getPatterns().add(Pair.of(color.get(), pattern.get()));
                }
            }
        }

        value.setCustomName(input.getOptional("CustomName", Tag.class)
                .map(JsonTextUtil::fromNBT)
                .orElse(null));

        // A placed ominous banner keeps its item name in the components of the block entity
        CompoundTag components = input.getCompound("components");
        if (components != null && isOminous(components)) {
            value.setOminous(true);
        }
    }

    @Override
    public void write(@NotNull JavaResolvers resolvers, @NotNull CompoundTag output, @NotNull BannerBlockEntity value) {
        // Write base
        if (value.getBase().isPresent()) {
            output.put("Base", value.getBase().get().getID());
        }

        // Write patterns
        ListTag<CompoundTag, Map<String, Tag<?>>> patternTags = new ListTag<>(TagType.COMPOUND, value.getPatterns().size());
        for (Pair<ChunkerDyeColor, ChunkerBannerPattern> pair : value.getPatterns()) {
            CompoundTag patternTag = new CompoundTag(2);
            if (resolvers.dataVersion().getVersion().isGreaterThanOrEqual(1, 20, 5)) {
                Optional<String> patternIdentifier = resolvers.bannerPatternResolver().from(pair.second());
                if (patternIdentifier.isPresent()) {
                    patternTag.put("color", pair.first().getName());
                    patternTag.put("pattern", patternIdentifier.get());
                    patternTags.add(patternTag);
                }

            } else {
                Optional<String> patternShortName = resolvers.bannerPatternShortNameResolver().from(pair.second());
                if (patternShortName.isPresent()) {
                    patternTag.put("Color", pair.first().getID());
                    patternTag.put("Pattern", patternShortName.get());
                    patternTags.add(patternTag);
                }
            }
        }
        output.put(resolvers.dataVersion().getVersion().isGreaterThanOrEqual(1, 20, 5) ? "patterns" : "Patterns", patternTags);

        if (value.getCustomName() != null) {
            output.put("CustomName", JsonTextUtil.toNBT(value.getCustomName(), resolvers.dataVersion()));
        }

        // Java keeps the components a banner doesn't use itself on the block entity, so it drops as the ominous banner
        if (value.isOminous() && resolvers.dataVersion().getVersion().isGreaterThanOrEqual(1, 20, 5)) {
            writeOminousComponents(resolvers, output.getOrCreateCompound("components"));
        }
    }

    /**
     * Write the components which make a white banner the ominous banner (Raid.getBannerComponentPatch), a raid
     * captain is only recognised by a banner with exactly these.
     *
     * @param resolvers  the resolvers.
     * @param components the components to add to.
     */
    private static void writeOminousComponents(JavaResolvers resolvers, CompoundTag components) {
        JsonObject name = new JsonObject();
        name.addProperty("translate", OMINOUS_BANNER_NAME);
        components.put("minecraft:item_name", JsonTextUtil.toNBT(name, resolvers.dataVersion()));
        if (resolvers.dataVersion().getVersion().isGreaterThanOrEqual(1, 21, 5)) {
            CompoundTag tooltipDisplay = new CompoundTag();
            tooltipDisplay.put("hidden_components", new ListTag<>(TagType.STRING, List.of(new StringTag("minecraft:banner_patterns"))));
            components.put("minecraft:tooltip_display", tooltipDisplay);
        } else {
            components.put("minecraft:hide_additional_tooltip", new CompoundTag());
        }
        components.put("minecraft:rarity", "uncommon");
    }

    private static boolean isOminous(CompoundTag components) {
        Tag<?> name = components.get("minecraft:item_name");
        if (name == null) return false;
        JsonElement text = JsonTextUtil.fromNBT(name);
        return text.isJsonObject() && text.getAsJsonObject().has("translate")
                && OMINOUS_BANNER_NAME.equals(text.getAsJsonObject().get("translate").getAsString());
    }

    @Override
    public boolean generateFromItemNBT(@NotNull JavaResolvers resolvers, @NotNull ChunkerItemStack itemStack, @NotNull BannerBlockEntity output, @NotNull CompoundTag input) {
        if (resolvers.dataVersion().getVersion().isLessThan(1, 20, 5)) return false; // Components not needed

        CompoundTag components = input.getCompound("components");
        if (components == null) return false; // No components

        // Fetch the base color if it exists
        Optional<ChunkerDyeColor> baseColor = components.getOptionalValue("minecraft:base_color", String.class)
                .flatMap(ChunkerDyeColor::getColorByName);
        output.setBase(baseColor);

        // Fetch the base color if it exists
        ListTag<CompoundTag, Map<String, Tag<?>>> patterns = components.getList("minecraft:banner_patterns", CompoundTag.class, null);
        if (patterns != null) {
            // Loop through each pattern
            for (CompoundTag patternTag : patterns) {
                Optional<ChunkerDyeColor> color = patternTag.getOptionalValue("color", String.class)
                        .flatMap(ChunkerDyeColor::getColorByName);
                Optional<ChunkerBannerPattern> pattern = patternTag.getOptionalValue("pattern", String.class)
                        .flatMap(resolvers.bannerPatternResolver()::to);

                // If the color and pattern are present
                if (color.isPresent() && pattern.isPresent()) {
                    output.getPatterns().add(Pair.of(color.get(), pattern.get()));
                }
            }
        }

        if (isOminous(components)) {
            output.setOminous(true);
        }

        // If either are present it was successful
        return baseColor.isPresent() || !output.getPatterns().isEmpty();
    }

    @Override
    public boolean writeToItemNBT(@NotNull JavaResolvers resolvers, @NotNull ChunkerItemStack itemStack, @NotNull BannerBlockEntity input, @NotNull CompoundTag output) {
        if (resolvers.dataVersion().getVersion().isLessThan(1, 20, 5))
            return true; // Components not needed (write normally)

        // Base color
        if (input.getBase().isPresent()) {
            output.getOrCreateCompound("components").put("minecraft:base_color", input.getBase().get().getName());
        }

        // Patterns
        if (!input.getPatterns().isEmpty()) {
            ListTag<CompoundTag, Map<String, Tag<?>>> patternTags = new ListTag<>(TagType.COMPOUND, input.getPatterns().size());
            for (Pair<ChunkerDyeColor, ChunkerBannerPattern> pair : input.getPatterns()) {
                Optional<String> patternIdentifier = resolvers.bannerPatternResolver().from(pair.second());
                if (patternIdentifier.isPresent()) {
                    CompoundTag patternTag = new CompoundTag(2);
                    patternTag.put("color", pair.first().getName());
                    patternTag.put("pattern", patternIdentifier.get());
                    patternTags.add(patternTag);
                }
            }
            output.getOrCreateCompound("components").put("minecraft:banner_patterns", patternTags);
        }

        if (input.isOminous()) {
            writeOminousComponents(resolvers, output.getOrCreateCompound("components"));
        }

        return false; // Don't write the block entity data
    }
}
