package com.hivemc.chunker.conversion.intermediate.column.blockentity;

import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerDyeColor;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.banner.ChunkerBannerPattern;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a Banner Block Entity.
 */
public class BannerBlockEntity extends BlockEntity {
    /**
     * The patterns of the ominous banner on its white base, in the order of Java 26.3 (Raid.getBannerComponentPatch).
     */
    public static final List<Pair<ChunkerDyeColor, ChunkerBannerPattern>> OMINOUS_PATTERNS = List.of(
            Pair.of(ChunkerDyeColor.CYAN, ChunkerBannerPattern.RHOMBUS_MIDDLE),
            Pair.of(ChunkerDyeColor.LIGHT_GRAY, ChunkerBannerPattern.STRIPE_BOTTOM),
            Pair.of(ChunkerDyeColor.GRAY, ChunkerBannerPattern.STRIPE_CENTER),
            Pair.of(ChunkerDyeColor.LIGHT_GRAY, ChunkerBannerPattern.BORDER),
            Pair.of(ChunkerDyeColor.BLACK, ChunkerBannerPattern.STRIPE_MIDDLE),
            Pair.of(ChunkerDyeColor.LIGHT_GRAY, ChunkerBannerPattern.HALF_HORIZONTAL),
            Pair.of(ChunkerDyeColor.LIGHT_GRAY, ChunkerBannerPattern.CIRCLE_MIDDLE),
            Pair.of(ChunkerDyeColor.BLACK, ChunkerBannerPattern.BORDER)
    );

    private final List<Pair<ChunkerDyeColor, ChunkerBannerPattern>> patterns = new ArrayList<>();
    private Optional<ChunkerDyeColor> base = Optional.empty(); // On Java base is only for shields
    private boolean ominous = false;

    /**
     * Get a list of patterns used on the banner.
     *
     * @return a list with a pair of the color to the pattern type.
     */
    public List<Pair<ChunkerDyeColor, ChunkerBannerPattern>> getPatterns() {
        return patterns;
    }

    /**
     * Get the base color.
     *
     * @return optional of the base color if it's present.
     */
    public Optional<ChunkerDyeColor> getBase() {
        return base;
    }

    /**
     * Set the base color of the banner.
     *
     * @param base an optional holding the color, if absent the base color isn't present.
     */
    public void setBase(Optional<ChunkerDyeColor> base) {
        this.base = base;
    }

    /**
     * Whether this is the ominous banner (a raid captain's banner), which Java identifies by its name and rarity.
     *
     * @return true if the banner is ominous.
     */
    public boolean isOminous() {
        return ominous;
    }

    /**
     * Set whether this is the ominous banner.
     *
     * @param ominous true if the banner is ominous.
     */
    public void setOminous(boolean ominous) {
        this.ominous = ominous;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BannerBlockEntity that)) return false;
        if (!super.equals(o)) return false;
        return isOminous() == that.isOminous() && Objects.equals(getBase(), that.getBase()) && Objects.equals(getPatterns(), that.getPatterns());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getBase(), getPatterns(), isOminous());
    }

    @Override
    public String toString() {
        return "BannerBlockEntity{" +
                "base=" + getBase() +
                ", patterns=" + getPatterns() +
                ", ominous=" + isOminous() +
                '}';
    }
}
