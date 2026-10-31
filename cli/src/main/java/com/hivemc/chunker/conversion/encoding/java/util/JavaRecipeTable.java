package com.hivemc.chunker.conversion.encoding.java.util;

import com.google.common.io.Resources;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerRecipeStation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Java recipes by the station they are made at and the item they make, from the recipes of the Java 26.3 server
 * (tools/recipe-tables in the project that maintains this fork). Special recipes, which Java never keeps in the
 * recipe book, armour trims and brewing aren't included.
 */
public final class JavaRecipeTable {
    private static final Map<ChunkerRecipeStation, Map<String, List<String>>> RECIPES = new EnumMap<>(ChunkerRecipeStation.class);

    static {
        try (InputStream stream = Resources.getResource("java/recipes_26_3.json").openStream();
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject stations = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("recipes");
            for (Map.Entry<String, JsonElement> station : stations.entrySet()) {
                Map<String, List<String>> byResult = new HashMap<>();
                for (Map.Entry<String, JsonElement> result : station.getValue().getAsJsonObject().entrySet()) {
                    List<String> ids = new ArrayList<>();
                    for (JsonElement id : result.getValue().getAsJsonArray()) {
                        ids.add(id.getAsString());
                    }
                    byResult.put(result.getKey(), List.copyOf(ids));
                }
                RECIPES.put(ChunkerRecipeStation.fromName(station.getKey()), byResult);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JavaRecipeTable() {
    }

    /**
     * Get the recipes made at a station that make an item.
     *
     * @param station the station.
     * @param item    the Java item ID.
     * @return the recipe IDs, empty if there are none.
     */
    public static List<String> get(ChunkerRecipeStation station, String item) {
        return RECIPES.getOrDefault(station, Map.of()).getOrDefault(item, List.of());
    }
}
