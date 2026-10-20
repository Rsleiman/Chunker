package com.hivemc.chunker.conversion.encoding.java.base.writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.hivemc.chunker.conversion.encoding.base.Converter;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevelPlayer;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Writes a Java player file (a profile) for every player of the source world into the chunker-players folder, which
 * Java ignores, and installs a copy of each assigned player's profile where Java loads it for that account.
 * <p>
 * A profile is named after the player's derived UUID, has no UUID field (Java gives a loaded player the UUID of the
 * account which joins) and never changes afterwards, so a player who isn't assigned at conversion time can be given
 * their profile later by copying it (see the README written alongside).
 */
public final class JavaPlayerProfiles {
    /**
     * The folder in the world which holds the profiles.
     */
    public static final String FOLDER = "chunker-players";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();

    private JavaPlayerProfiles() {
    }

    /**
     * Creates the Java NBT of a player.
     */
    @FunctionalInterface
    public interface PlayerTagFactory {
        /**
         * Create the player NBT.
         *
         * @param player the player.
         * @return the NBT.
         * @throws Exception if it failed.
         */
        CompoundTag create(ChunkerLevelPlayer player) throws Exception;
    }

    /**
     * Refuse to write into a world which already has player files, as a leftover file from an earlier conversion
     * would be loaded by Java.
     *
     * @param outputFolder         the output world.
     * @param playersDataDirectory the folder Java loads player files from.
     * @throws IllegalStateException if either folder holds files.
     */
    public static void checkOutputHasNoPlayers(File outputFolder, File playersDataDirectory) {
        for (File directory : List.of(new File(outputFolder, FOLDER), playersDataDirectory)) {
            String[] files = directory.list();
            if (files != null && files.length > 0) {
                throw new IllegalStateException("The output world already has player files in " + directory
                        + ", convert into an empty folder so that no player file of an earlier conversion is kept");
            }
        }
    }

    /**
     * Write the profiles, install the assigned ones, and write mapping.json and README.md.
     *
     * @param outputFolder         the output world.
     * @param playersDataDirectory the folder Java loads player files from.
     * @param players              the players.
     * @param factory              creates the NBT of a player.
     * @param converter            the converter, for reporting and the input description.
     * @param levelName            the name of the level.
     * @throws Exception if writing failed.
     */
    public static void write(File outputFolder, File playersDataDirectory, List<ChunkerLevelPlayer> players, PlayerTagFactory factory, Converter converter, @Nullable String levelName) throws Exception {
        File folder = new File(outputFolder, FOLDER);
        folder.mkdirs();

        JsonObject playersJson = new JsonObject();
        Set<UUID> profiles = new HashSet<>();
        Set<UUID> accounts = new HashSet<>();
        for (ChunkerLevelPlayer player : players) {
            UUID profileUUID = player.getProfileUUID();
            if (profileUUID == null || player.getSourceId() == null) {
                converter.logNonFatalException(new Exception("A player without a UniqueID was not converted"));
                continue;
            }
            if (!profiles.add(profileUUID)) {
                converter.logNonFatalException(new Exception("Player " + player.getSourceId() + " has more than one record, only the first was converted"));
                continue;
            }

            // Write the profile
            CompoundTag tag = factory.create(player);
            tag.remove("UUID");
            File profile = new File(folder, profileUUID + ".dat");
            Tag.writeGZipJavaNBT(profile, tag);

            // Install a copy for the assigned account
            UUID account = player.getAccountUUID();
            if (account != null && !accounts.add(account)) {
                converter.logNonFatalException(new Exception("Player " + player.getSourceId() + " is mapped to an account which already has a player, their profile was not installed"));
                account = null;
            }
            if (account != null) {
                playersDataDirectory.mkdirs();
                Files.copy(profile.toPath(), new File(playersDataDirectory, account + ".dat").toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            }

            JsonObject entry = new JsonObject();
            entry.addProperty("profile", profile.getName());
            if (account != null) {
                entry.addProperty("javaUUID", account.toString());
            } else {
                entry.add("javaUUID", JsonNull.INSTANCE);
            }
            playersJson.add(player.getSourceId(), entry);
        }

        // The mapping, with a note of where the profiles came from
        JsonObject source = new JsonObject();
        source.addProperty("inputWorld", converter.getInputDescription());
        source.addProperty("levelName", levelName);
        source.addProperty("converted", Instant.now().toString());
        JsonObject mapping = new JsonObject();
        mapping.add("source", source);
        mapping.add("players", playersJson);
        writeText(new File(folder, "mapping.json"), GSON.toJson(mapping) + "\n");
        writeText(new File(folder, "README.md"), README);
    }

    private static void writeText(File file, String text) throws IOException {
        Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
    }

    /**
     * The README written into the folder, for whoever holds the world later.
     */
    public static final String README = """
            # chunker-players

            Chunker wrote this folder when it converted this world from Bedrock Edition. Java Edition ignores it.

            It holds one Java player file (a *profile*) for each Bedrock player in the world. A profile holds the
            player's own data as the Bedrock world had it: inventory, armour, off hand, ender chest, experience,
            health, hunger, effects, position, game mode and spawn point. Each profile is named after a placeholder
            UUID which no real account has, and the world's references to the player (the owners of their tamed
            animals, the mobs they hold on a lead) use that placeholder until the player is given to an account.
            Animals the player was riding, or carrying on their shoulders, are in the world where Bedrock left them.

            `mapping.json` lists every Bedrock player by their Bedrock UniqueID, with their profile and the UUID of the
            Java account they were given at conversion time (`javaUUID`, null if none). A profile given to an account
            at conversion time was also copied to `players/data/<javaUUID>.dat`, where Java loads it when that account
            joins.

            ## Giving a profile to a player later

            1. Stop the server and back up the world.
            2. Run `assign_player.py --world <this world> --from <Bedrock UniqueID> --to <UUID>`, where the UUID is the
               one the player will join with. The script is in `tools/players` of the conversion project that
               accompanies this Chunker fork.

            Copying a profile into `players/data/` by hand isn't enough: the player's pets and other references would
            still point at the placeholder UUID. The script replaces the placeholder everywhere in the world and
            records the assignment in `mapping.json`.

            Giving a profile to an account which has already played on this world replaces that account's Java
            progress, so the script refuses to overwrite an existing player file unless it is forced.
            """;
}
