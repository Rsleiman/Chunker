package com.hivemc.chunker.conversion.encoding.bedrock.util;

import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerRecipeStation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Bedrock recipes by ID: the stations each is made at and the item it makes.
 * <p>
 * Bedrock saves a player's unlocked recipes by ID. Many recipes are built into the game rather than defined in data
 * (the dye recipes, such as harness_color_1_3, and older ones such as TrapdoorBirch_recipeId), so the table comes
 * from the crafting_data packet BDS 1.26.61.1 sends to a joining player (tools/recipe-tables in the project that
 * maintains this fork). The result is the item name and data value as BDS sends them.
 */
public final class BedrockRecipeTable {
    private static final Map<String, Entry> RECIPES = new HashMap<>();

    static {
        try (InputStream stream = Resources.getResource("bedrock/recipes_1_26_60.json").openStream();
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject recipes = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("recipes");
            for (Map.Entry<String, JsonElement> recipe : recipes.entrySet()) {
                JsonArray value = recipe.getValue().getAsJsonArray();
                List<ChunkerRecipeStation> stations = new ArrayList<>();
                for (JsonElement station : value.get(0).getAsJsonArray()) {
                    stations.add(ChunkerRecipeStation.fromName(station.getAsString()));
                }
                RECIPES.put(recipe.getKey(), new Entry(List.copyOf(stations), value.get(1).getAsString(), value.get(2).getAsInt()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private BedrockRecipeTable() {
    }

    /**
     * Get a recipe by its ID.
     *
     * @param id the recipe ID as Bedrock saves it.
     * @return the recipe or null if it isn't known.
     */
    @Nullable
    public static Entry get(String id) {
        return RECIPES.get(id);
    }

    /**
     * A Bedrock recipe.
     *
     * @param stations the stations it is made at (a furnace recipe is also made in a smoker or on a campfire).
     * @param item     the Bedrock name of the item it makes.
     * @param data     the data value of the item it makes.
     */
    public record Entry(List<ChunkerRecipeStation> stations, String item, int data) {
    }
}
