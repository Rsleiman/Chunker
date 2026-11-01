package com.hivemc.chunker.conversion.encoding.bedrock.base.reader;

import com.hivemc.chunker.conversion.encoding.base.Converter;
import com.hivemc.chunker.conversion.encoding.base.Version;
import com.hivemc.chunker.conversion.encoding.base.reader.LevelReader;
import com.hivemc.chunker.conversion.encoding.bedrock.base.BedrockReaderWriter;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.encoding.bedrock.util.BedrockChunkCoordinateSet;
import com.hivemc.chunker.conversion.encoding.bedrock.util.BedrockStructureParser;
import com.hivemc.chunker.conversion.encoding.bedrock.util.BedrockVillageParser;
import com.hivemc.chunker.conversion.encoding.bedrock.util.LevelDBChunkType;
import com.hivemc.chunker.conversion.encoding.bedrock.util.LevelDBKey;
import com.hivemc.chunker.conversion.handlers.LevelConversionHandler;
import com.hivemc.chunker.conversion.handlers.WorldConversionHandler;
import com.hivemc.chunker.conversion.intermediate.column.biome.ChunkerBiome;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerLodestoneData;
import com.hivemc.chunker.conversion.intermediate.level.*;
import com.hivemc.chunker.conversion.intermediate.level.map.ChunkerMap;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerGlobalPosition;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerVillagerMemory;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.conversion.intermediate.world.DimensionRegistry;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import com.hivemc.chunker.nbt.tags.primitive.IntTag;
import com.hivemc.chunker.scheduling.task.FutureTask;
import com.hivemc.chunker.scheduling.task.ProgressiveTask;
import com.hivemc.chunker.scheduling.task.Task;
import com.hivemc.chunker.scheduling.task.TaskWeight;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.iq80.leveldb.*;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.iq80.leveldb.table.BloomFilterPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A reader for Bedrock levels.
 */
public class BedrockLevelReader implements LevelReader, BedrockReaderWriter {
    public static final float PLAYER_HEIGHT = 1.62001f; // Height + 0.00001 offset (based on save data)
    public static final String VOID_WORD_STRING = "{ " +
            "\"biome_id\" : 1, " +
            "\"block_layers\" : [{" +
            "\"block_data\" : 0, " +
            "\"block_name\" : \"minecraft:air\", " +
            "\"count\" : 1 " +
            "}], " +
            "\"encoding_version\" : 4,  " +
            "\"structure_options\" : null" +
            "}";
    protected final File inputDirectory;
    protected final Version inputVersion;
    protected final Converter converter;
    protected final BedrockResolvers resolvers;
    protected final Map<Long, Map<ChunkerVillagerMemory, ChunkerGlobalPosition>> villagerMemories = new ConcurrentHashMap<>();
    protected final Set<Long> playerIds = ConcurrentHashMap.newKeySet();
    protected final Map<Dimension, Set<ChunkCoordPair>> entitiesOnlyColumns = new ConcurrentHashMap<>();
    protected DB database;
    /**
     * The level being read, so items read with it (e.g. in player inventories) can find its maps.
     */
    @Nullable
    protected volatile ChunkerLevel readLevel;

    /**
     * Create a new level reader.
     *
     * @param inputDirectory the input world directory.
     * @param inputVersion   the version being read.
     * @param converter      the converter instance.
     */
    public BedrockLevelReader(File inputDirectory, Version inputVersion, Converter converter) {
        this.inputDirectory = inputDirectory;
        this.inputVersion = inputVersion;
        this.converter = converter;
        resolvers = buildResolvers(converter).build();
    }

    @Override
    public Set<ChunkerBiome.ChunkerVanillaBiome> getSupportedBiomes() {
        return resolvers.biomeIdResolver().getSupportedBiomes();
    }

    /**
     * Open the LevelDB database.
     *
     * @throws IOException if it fails to open the database.
     */
    protected void openDatabase() throws IOException {
        File databaseDirectory = new File(inputDirectory, "db");

        // Delete LOCK file (as it may have been left behind by a bad abort / corrupted DB)
        new File(databaseDirectory, "LOCK").delete();

        // LevelDB Options
        Options options = new Options();
        options.compressionType(CompressionType.ZLIB_RAW);
        options.blockSize(160 * 1024); // 160KB
        options.filterPolicy(new BloomFilterPolicy(10));
        options.createIfMissing(true);

        // Create the factory and open the database
        DBFactory factory = new Iq80DBFactory();
        database = factory.open(databaseDirectory, options);
    }

    @Override
    public void free() throws Exception {
        // Called on exception / done
        if (database != null) {
            try {
                database.close();
            } finally {
                database = null;
            }
        }
    }

    @Override
    public void readLevel(LevelConversionHandler levelConversionHandler) throws IOException {
        // Open database
        openDatabase();

        // Read custom dimensions
        readCustomDimensions();

        // Collect level data
        FutureTask<WorldConversionHandler> levelDataCollection = Task.asyncUnwrap("Collecting Level Data", TaskWeight.MEDIUM, this::collectLevelData, levelConversionHandler);

        Task<Map<Dimension, Map<RegionCoordPair, Set<ChunkCoordPair>>>> usedRegions = Task.async("Collecting Used Regions", TaskWeight.MEDIUM, () -> {
            // Create a lookup for each dimension and region present
            Map<Dimension, Map<RegionCoordPair, Set<ChunkCoordPair>>> dimensionLookup = new HashMap<>();
            // Columns with a list of entities (digp), which may have no terrain
            Map<Dimension, Set<ChunkCoordPair>> entityColumns = new HashMap<>();
            // Columns with block entities or entities stored in the column, and columns with a version
            Map<Dimension, Set<ChunkCoordPair>> objectColumns = new HashMap<>();
            Map<Dimension, Set<ChunkCoordPair>> versionColumns = new HashMap<>();
            boolean collectEntityColumns = converter.shouldProcessEntities();
            // Scan the database for valid chunks
            DimensionRegistry dimensionRegistry = converter.getDimensionRegistry();
            try (DBIterator iterator = database.iterator(new ReadOptions().fillCache(false))) {
                while (iterator.hasNext()) {
                    Map.Entry<byte[], byte[]> entry = iterator.next();
                    byte[] key = entry.getKey();
                    if (collectEntityColumns && LevelDBKey.isEntityListKey(key) && entry.getValue().length > 0) {
                        Dimension dimension = key.length == 16 ? dimensionRegistry.fromBedrock(LevelDBKey.readLittleEndianInt(key, 12), null) : Dimension.OVERWORLD;
                        if (dimension != null) {
                            entityColumns.computeIfAbsent(dimension, (ignored) -> new ObjectOpenHashSet<>())
                                    .add(new ChunkCoordPair(LevelDBKey.readLittleEndianInt(key, 4), LevelDBKey.readLittleEndianInt(key, 8)));
                        }
                        continue;
                    }
                    boolean columnKey = LevelDBKey.isColumnKey(key);
                    if (!columnKey && !LevelDBKey.isColumnVersionKey(key)) continue;

                    int x = LevelDBKey.readLittleEndianInt(key, 0);
                    int z = LevelDBKey.readLittleEndianInt(key, 4);

                    // Read dimension
                    Dimension dimension = Dimension.OVERWORLD;
                    if (key.length >= 13) {
                        int dimensionID = LevelDBKey.readLittleEndianInt(key, 8);
                        dimension = dimensionRegistry.fromBedrock(dimensionID, null);

                        // Non-chunk string keys can share a chunk-key length. Unknown dimensions are not usable here.
                        if (dimension == null) {
                            continue;
                        }
                    }

                    // Block entities, entities and versions alone don't make a column with terrain
                    if (!columnKey || LevelDBKey.isColumnObjectKey(key)) {
                        (columnKey ? objectColumns : versionColumns).computeIfAbsent(dimension, (ignored) -> new ObjectOpenHashSet<>())
                                .add(new ChunkCoordPair(x, z));
                        continue;
                    }

                    // Create the pairs used for adding to the lookup
                    RegionCoordPair regionCoordPair = new RegionCoordPair(x >> 5, z >> 5);

                    // Add to lookup
                    Map<RegionCoordPair, Set<ChunkCoordPair>> regionLookup = dimensionLookup.computeIfAbsent(dimension, (ignored) -> new Object2ObjectOpenHashMap<>());
                    BedrockChunkCoordinateSet columns = (BedrockChunkCoordinateSet) regionLookup.computeIfAbsent(regionCoordPair, BedrockChunkCoordinateSet::new);
                    columns.add(x, z);
                }
            }

            // Bedrock keeps block entity records (e.g. the trial spawners and vaults of a trial chamber) for columns it
            // has never generated, which have no terrain and no version. Such a column isn't written as terrain, as
            // an empty column would be a hole in the output; the output generates it instead. Its entities are read.
            for (Map.Entry<Dimension, Set<ChunkCoordPair>> entry : objectColumns.entrySet()) {
                Map<RegionCoordPair, Set<ChunkCoordPair>> regionLookup = dimensionLookup.computeIfAbsent(entry.getKey(), (ignored) -> new Object2ObjectOpenHashMap<>());
                Set<ChunkCoordPair> versions = versionColumns.getOrDefault(entry.getKey(), Collections.emptySet());
                for (ChunkCoordPair position : entry.getValue()) {
                    Set<ChunkCoordPair> columns = regionLookup.get(position.getRegion());
                    if (columns != null && columns.contains(position)) continue;

                    if (versions.contains(position)) {
                        regionLookup.computeIfAbsent(position.getRegion(), BedrockChunkCoordinateSet::new).add(position);
                    } else if (collectEntityColumns) {
                        entityColumns.computeIfAbsent(entry.getKey(), (ignored) -> new ObjectOpenHashSet<>()).add(position);
                    }
                }
            }

            // Columns with entities but no terrain are read for their entities only
            for (Map.Entry<Dimension, Set<ChunkCoordPair>> entry : entityColumns.entrySet()) {
                Map<RegionCoordPair, Set<ChunkCoordPair>> regionLookup = dimensionLookup.computeIfAbsent(entry.getKey(), (ignored) -> new Object2ObjectOpenHashMap<>());
                Set<ChunkCoordPair> entitiesOnly = new ObjectOpenHashSet<>();
                for (ChunkCoordPair position : entry.getValue()) {
                    Set<ChunkCoordPair> columns = regionLookup.get(position.getRegion());
                    if (columns != null && columns.contains(position)) continue;

                    regionLookup.computeIfAbsent(position.getRegion(), BedrockChunkCoordinateSet::new).add(position);
                    entitiesOnly.add(position);
                }
                if (!entitiesOnly.isEmpty()) {
                    entitiesOnlyColumns.put(entry.getKey(), entitiesOnly);
                }
            }

            return dimensionLookup;
        });

        // When we've collected the level data and present chunks, go through each world and call the reading process
        ProgressiveTask<Void> worldReading = levelDataCollection.thenConsume("Reading Worlds", TaskWeight.HIGHEST, (worldConversionHandler) -> {
            if (worldConversionHandler == null) return; // This can be null if the worlds aren't handled by the reader

            // Read the worlds
            usedRegions.thenConsume("Reading Worlds", TaskWeight.HIGHEST, (dimensionLookup) -> {
                List<Task<Void>> worlds = new ArrayList<>(dimensionLookup.size());
                for (Map.Entry<Dimension, Map<RegionCoordPair, Set<ChunkCoordPair>>> entry : dimensionLookup.entrySet()) {
                    // Create a world reader if it should be processed
                    if (converter.shouldProcessDimension(entry.getKey())) {
                        BedrockWorldReader reader = createWorldReader(entry.getValue(), entry.getKey());
                        reader.setEntitiesOnlyColumns(entitiesOnlyColumns.getOrDefault(entry.getKey(), Collections.emptySet()));

                        // Read the world
                        worlds.add(Task.asyncConsume("Reading World", TaskWeight.HIGHER, reader::readWorld, worldConversionHandler));
                    }
                }

                Task.join(worlds).then("Flushing Worlds", TaskWeight.MEDIUM, worldConversionHandler::flushWorlds);
            });
        });

        // Flush the level after all the worlds have been read
        worldReading.then("Flushing Level", TaskWeight.MEDIUM, levelConversionHandler::flushLevel);
    }

    /**
     * Collect all the data required for ChunkerLevel and submit it.
     *
     * @param levelConversionHandler the handler for submitting the level data to.
     * @return the task which submits the ChunkerLevel data.
     */
    protected Task<WorldConversionHandler> collectLevelData(LevelConversionHandler levelConversionHandler) {
        ChunkerLevel output = new ChunkerLevel();
        readLevel = output;
        ProgressiveTask<Void> parseLevelSettings = Task.asyncConsume("Parsing Level Settings", TaskWeight.NORMAL, this::parseLevelSettings, output);
        ProgressiveTask<Void> parseMaps = Task.asyncConsume("Parsing Saved Maps", TaskWeight.NORMAL, this::parseMaps, output);

        // Players are read after the maps, so the maps in their inventories can be found
        ProgressiveTask<Void> parsePlayers = parseMaps.thenConsume("Parsing Players", TaskWeight.NORMAL, this::parsePlayers, output);
        ProgressiveTask<Void> parsePortals = Task.asyncConsume("Parsing Portals", TaskWeight.NORMAL, this::parsePortals, output);
        ProgressiveTask<Void> parseStructures = Task.asyncConsume("Parsing Structures", TaskWeight.NORMAL, this::parseStructures, output);
        ProgressiveTask<Void> parseCustomBiomes = Task.asyncConsume("Parsing Biome List", TaskWeight.NORMAL, this::parseBiomeList, output);
        ProgressiveTask<Void> parseVillages = Task.asyncConsume("Parsing Villages", TaskWeight.NORMAL, this::parseVillages, output);

        // When those tasks are done we can convert the level (entities are read after this)
        return Task.join(parseLevelSettings, parsePlayers, parseMaps, parsePortals, parseStructures, parseCustomBiomes, parseVillages)
                .thenUnwrap("Converting Level", TaskWeight.LOW, levelConversionHandler::convertLevel, output);
    }

    /**
     * Parse the level.dat of the world.
     *
     * @param output the output to set the settings and original data.
     * @throws Exception if it failed to parse the level.dat
     */
    protected void parseLevelSettings(ChunkerLevel output) throws Exception {
        CompoundTag level = Tag.readBedrockNBT(new File(inputDirectory, "level.dat"));
        output.setSettings(ChunkerLevelSettings.fromNBT(Objects.requireNonNull(level), this, converter));
        output.setOriginalLevelData(level);
    }

    /**
     * Register all the custom dimensions that the world contains to the DimensionRegistry.
     */
    protected void readCustomDimensions() {
        try {
            byte[] value = database.get(LevelDBKey.DIMENSION_NAME_ID_TABLE);
            if (value == null) return;

            CompoundTag root = Tag.readBedrockNBT(value);
            if (root == null) return;

            CompoundTag entries = root.getCompound("entries");
            if (entries == null) return;

            for (Map.Entry<String, Tag<?>> entry : entries) {
                // Only register custom dimensions
                if (entry.getValue() instanceof IntTag id && id.getValue() >= DimensionRegistry.BEDROCK_CUSTOM_DIMENSION_ID_START) {
                    converter.getDimensionRegistry().registerFromWorld(entry.getKey(), OptionalInt.of(id.getValue()));
                }
            }
        } catch (Exception e) {
            converter.logNonFatalException(new Exception("Failed to read custom dimensions", e));
        }
    }

    @Override
    public @Nullable Object readCustomLevelSetting(@NotNull CompoundTag root, @NotNull ChunkerLevelSettings chunkerLevelSettings, @NotNull String targetName, @NotNull Class<?> type) {
        // Check for WinterDrop2026 support
        if (targetName.equals("WinterDrop2026")) {
            return false;
        }

        // Check for AutumnDrop2026 support
        if (targetName.equals("AutumnDrop2026")) {
            return false;
        }

        // Check for SummerDrop2026 support
        if (targetName.equals("SummerDrop2026")) {
            return false;
        }

        // Check for AutumnDrop2025 support
        if (targetName.equals("AutumnDrop2025")) {
            return false;
        }

        // Check for SummerDrop2025 support
        if (targetName.equals("SummerDrop2025")) {
            return false;
        }

        // Check for WinterDrop2024 support
        if (targetName.equals("WinterDrop2024")) {
            return false;
        }

        // Check for R21 support
        if (targetName.equals("R21Support")) {
            return false;
        }

        // Check for R20 support
        if (targetName.equals("R20Support")) {
            return false;
        }

        // Check for caves and cliffs
        if (targetName.equals("CavesAndCliffs")) {
            return false;
        }

        // Read flat world version
        if (targetName.equals("FlatWorldVersion")) {
            return root.getInt("WorldVersion", 0);
        }

        // Read the default permissions given to players
        if (targetName.equals("playerPermissionsLevel")) {
            // Before 1.19.10 the permissions were held inside the abilities compound
            CompoundTag source = root.contains(targetName) ? root : root.getCompound("abilities");
            if (source == null) return null;

            // Use a number as the field isn't always written as an int
            return source.getOptionalValue(targetName, Number.class).map(Number::intValue).orElse(null);
        }

        // Default implementation for seed
        if (targetName.equals("RandomSeed")) {
            // After StorageVersion 8, seed became based on a long
            if (root.getInt("StorageVersion", 0) > 8) {
                return root.contains(targetName) ? String.valueOf(root.getLong(targetName)) : null;
            } else {
                // Integer is used as this is what is displayed in Bedrock settings on older versions
                return root.contains(targetName) ? String.valueOf((int) root.getLong(targetName)) : null;
            }
        }

        if (type == ChunkerGeneratorType.class) {
            int generator = root.getInt("Generator", 1); // Default to normal

            return switch (generator) {
                case 0 -> ChunkerGeneratorType.CUSTOM; // Legacy
                case 1 -> ChunkerGeneratorType.NORMAL; // Normal generation
                case 2 -> {
                    // Flat generator
                    String generatorOptions = root.getString("generatorOptions", null);
                    if (generatorOptions != null && !generatorOptions.isEmpty()) {
                        if (VOID_WORD_STRING.equals(root.getString("FlatWorldLayers", null))) {
                            // VOID
                            yield ChunkerGeneratorType.VOID;
                        }

                        yield ChunkerGeneratorType.CUSTOM;
                    }

                    yield ChunkerGeneratorType.FLAT;
                }
                default -> ChunkerGeneratorType.CUSTOM; // Unknown
            };
        }

        // Throw an error, so we know to handle something if we missed it
        throw new IllegalArgumentException("Type " + targetName + " isn't implemented for setting parsing");
    }

    /**
     * Parse the players of the level: the local player (singleplayer worlds) and every player record of a server
     * world (player_server_ keys). The records of linked accounts (player_ keys) are never read.
     *
     * @param output the output to set the players.
     */
    protected void parsePlayers(ChunkerLevel output) {
        List<ChunkerLevelPlayer> players = new ArrayList<>();
        ChunkerLevelPlayer localPlayer = parseLocalPlayer(output);
        if (localPlayer != null) {
            players.add(localPlayer);
        }

        try (DBIterator iterator = database.iterator(new ReadOptions().fillCache(false))) {
            iterator.seek(LevelDBKey.SERVER_PLAYER_PREFIX);
            while (iterator.hasNext()) {
                Map.Entry<byte[], byte[]> entry = iterator.next();
                if (!LevelDBKey.startsWith(entry.getKey(), LevelDBKey.SERVER_PLAYER_PREFIX)) break;
                try {
                    CompoundTag player = Objects.requireNonNull(Tag.readBedrockNBT(entry.getValue()));
                    player.getOptionalValue("UniqueID", Long.class).ifPresent(playerIds::add);
                    players.add(BedrockPlayerReader.read(resolvers, converter, player));
                } catch (Exception e) {
                    converter.logNonFatalException(new Exception("Failed to parse a player record", e));
                }
            }
        } catch (Exception e) {
            converter.logNonFatalException(e);
        }
        output.setPlayers(players);
    }

    /**
     * Parse the local player data.
     *
     * @param output the output to set the player data.
     * @return the local player or null if there isn't one.
     */
    @Nullable
    protected ChunkerLevelPlayer parseLocalPlayer(ChunkerLevel output) {
        byte[] bytes = database.get(LevelDBKey.LOCAL_PLAYER);
        if (bytes == null) return null; // No local player
        try {
            // Parse the NBT
            CompoundTag player = Objects.requireNonNull(Tag.readBedrockNBT(bytes));
            player.getOptionalValue("UniqueID", Long.class).ifPresent(playerIds::add);
            ChunkerLevelPlayer localPlayer = BedrockPlayerReader.read(resolvers, converter, player);
            output.setPlayer(localPlayer);
            return localPlayer;
        } catch (Exception e) {
            converter.logNonFatalException(e);
            return null;
        }
    }

    @Override
    public Version getVersion() {
        return inputVersion;
    }

    /**
     * Parse all the in-game maps inside the level.
     *
     * @param output the output to add the maps to.
     */
    protected void parseMaps(ChunkerLevel output) {
        List<ChunkerMap> maps = new ArrayList<>();
        if (converter.shouldProcessMaps()) {
            List<Task<ChunkerMap>> tasks = new ArrayList<>();

            // Iterate through the database for maps
            try (DBIterator iterator = database.iterator(new ReadOptions().fillCache(false))) {
                iterator.seek(LevelDBKey.MAP_PREFIX); // Skip to the key

                while (iterator.hasNext()) {
                    Map.Entry<byte[], byte[]> entry = iterator.next();

                    // Ensure it starts with the map key
                    if (!LevelDBKey.startsWith(entry.getKey(), LevelDBKey.MAP_PREFIX)) break;

                    // Attempt to parse the mapID from the key
                    try {
                        String suffix = LevelDBKey.extractSuffix(entry.getKey(), LevelDBKey.MAP_PREFIX);
                        long mapID = Long.parseLong(suffix);

                        tasks.add(Task.async("Parsing map", TaskWeight.NORMAL, () -> parseMap(mapID, entry.getValue())));
                    } catch (Exception e) {
                        converter.logNonFatalException(e);
                    }
                }
            }

            // Write maps back to our array when they're done
            if (!tasks.isEmpty()) {
                Task.join(tasks).thenConsume("Parsing map sync", TaskWeight.LOW, (mapArray) -> {
                    for (ChunkerMap map : mapArray) {
                        if (map == null) continue;
                        maps.add(map);
                    }

                    // Sort the list
                    maps.sort(ChunkerMap.BY_ID_COMPARATOR);
                });
            }
        }
        output.setMaps(maps);
    }

    /**
     * Parse a map from the ID and NBT encoded data.
     *
     * @param id   the ID of a map.
     * @param data the NBT as a byte array.
     * @return a parsed map otherwise null if it failed to parse.
     */
    @Nullable
    protected ChunkerMap parseMap(long id, byte[] data) {
        try {
            // Read the data
            CompoundTag mapCompound = Objects.requireNonNull(Tag.readBedrockNBT(data));

            DimensionRegistry dimensionRegistry = converter.getDimensionRegistry();
            Dimension overworld = Dimension.OVERWORLD;
            // Create a nice ChunkerMap with all the properties we need (and defaults)
            return new ChunkerMap(
                    id,
                    id,
                    mapCompound.getShort("height", (short) 128),
                    mapCompound.getShort("width", (short) 128),
                    mapCompound.getByte("scale", (byte) 0),
                    dimensionRegistry.fromBedrockNBT(mapCompound.get("dimension"), overworld),
                    mapCompound.getInt("xCenter", 0),
                    mapCompound.getInt("zCenter", 0),
                    mapCompound.getByte("unlimitedTracking", (byte) 0) != 0,
                    mapCompound.getByte("mapLocked", (byte) 0) != 0,
                    mapCompound.getByteArray("colors", null),
                    resolvers.converter().shouldAllowNBTCopying() ? mapCompound : null
            );

        } catch (Exception e) {
            converter.logNonFatalException(e);
            return null;
        }
    }

    /**
     * Parse all the portal data in the world.
     *
     * @param output the output to add the portal data to.
     */
    protected void parsePortals(ChunkerLevel output) {
        output.setPortals(new ArrayList<>());
        try {
            byte[] value = database.get(LevelDBKey.PORTALS);
            if (value == null) return;

            DimensionRegistry dimensionRegistry = converter.getDimensionRegistry();
            Dimension overworld = Dimension.OVERWORLD;

            // Read the data
            CompoundTag wrappedData = Objects.requireNonNull(Tag.readBedrockNBT(value));
            CompoundTag data = wrappedData.getCompound("data");
            if (data == null) return;

            // Grab the portals
            ListTag<CompoundTag, Map<String, Tag<?>>> portalRecords = data.getList("PortalRecords", CompoundTag.class, null);
            if (portalRecords == null) return;
            for (CompoundTag portalRecord : portalRecords) {
                output.getPortals().add(new ChunkerPortal(
                        dimensionRegistry.fromBedrock(portalRecord.getInt("DimId", 0), overworld),
                        portalRecord.getInt("TpX", 0),
                        portalRecord.getInt("TpY", 0),
                        portalRecord.getInt("TpZ", 0),
                        portalRecord.getByte("Span", (byte) 0),
                        portalRecord.getByte("Xa", (byte) 0),
                        portalRecord.getByte("Za", (byte) 0)
                ));
            }
        } catch (Exception e) {
            converter.logNonFatalException(e);
        }
    }

    /**
     * Parse all the structure data in the world (stored per chunk) and the chunk columns which are written near it.
     *
     * @param output the output to add the structure data to.
     */
    protected void parseStructures(ChunkerLevel output) {
        List<ChunkerStructureVolume> volumes = new ArrayList<>();
        Map<Dimension, LongSet> structureColumns = new HashMap<>();
        output.setStructureVolumes(volumes);
        output.setStructureColumns(structureColumns);

        try {
            DimensionRegistry dimensionRegistry = converter.getDimensionRegistry();
            Map<Dimension, LongSet> presentColumns = new HashMap<>();
            List<ChunkerStructureVolume> candidates = new ArrayList<>();
            Set<String> reportedIds = new HashSet<>();

            // Scan the whole database once
            try (DBIterator iterator = database.iterator(new ReadOptions().fillCache(false))) {
                iterator.seekToFirst();
                while (iterator.hasNext()) {
                    Map.Entry<byte[], byte[]> entry = iterator.next();
                    byte[] key = entry.getKey();
                    try {
                        // Only chunk based keys are of interest
                        if (key.length < 9) continue;

                        // Resolve the dimension
                        Dimension dimension = Dimension.OVERWORLD;
                        if (key.length >= 13) {
                            dimension = dimensionRegistry.fromBedrock(LevelDBKey.readLittleEndianInt(key, 8), null);
                            if (dimension == null) continue; // Non-chunk keys can share the length
                        }

                        // Columns which are present
                        if (LevelDBKey.isColumnKey(key)) {
                            presentColumns.computeIfAbsent(dimension, (ignored) -> new LongOpenHashSet()).add(ChunkerStructureVolume.packChunk(
                                    LevelDBKey.readLittleEndianInt(key, 0),
                                    LevelDBKey.readLittleEndianInt(key, 4)
                            ));
                            continue;
                        }

                        // Structure data
                        if (key.length != 9 && key.length != 13) continue;
                        byte type = key[key.length - 1];
                        if (type != LevelDBChunkType.AABB_VOLUMES.getId() && type != LevelDBChunkType.HARDCODED_SPAWNERS.getId()) continue;
                        if (LevelDBKey.startsWith(key, LevelDBKey.MAP_PREFIX)
                                || LevelDBKey.startsWith(key, LevelDBKey.ACTOR_PREFIX)
                                || LevelDBKey.startsWith(key, LevelDBKey.DIGP_PREFIX)) {
                            continue;
                        }

                        ChunkCoordPair chunk = new ChunkCoordPair(LevelDBKey.readLittleEndianInt(key, 0), LevelDBKey.readLittleEndianInt(key, 4));
                        List<ChunkerStructureVolume> parsed;
                        if (type == LevelDBChunkType.AABB_VOLUMES.getId()) {
                            parsed = BedrockStructureParser.parseAABBVolumes(entry.getValue(), dimension, chunk, converter::logNonFatalException);
                        } else {
                            parsed = BedrockStructureParser.parseHardcodedSpawners(entry.getValue(), dimension, chunk, converter::logNonFatalException);
                        }

                        // Only keep the structures which can be converted
                        for (ChunkerStructureVolume volume : parsed) {
                            String javaId = BedrockStructureParser.toJavaStructureId(volume.getStructureId());
                            if (javaId == null) {
                                if (reportedIds.add(volume.getStructureId())) {
                                    converter.logNonFatalException(new Exception("Structure " + volume.getStructureId() + " cannot be converted and will be skipped"));
                                }
                                continue;
                            }
                            candidates.add(volume);
                        }
                    } catch (Exception e) {
                        converter.logNonFatalException(e);
                    }
                }
            }

            // Only keep the volumes of columns which are written
            Map<Dimension, Boolean> dimensionProcessed = new HashMap<>();
            for (ChunkerStructureVolume volume : candidates) {
                Dimension dimension = volume.getDimension();
                LongSet present = presentColumns.get(dimension);
                if (present == null || !dimensionProcessed.computeIfAbsent(dimension, converter::shouldProcessDimension)) continue;

                ChunkCoordPair chunk = volume.getSourceChunk();
                if (!isWrittenColumn(dimension, chunk.chunkX(), chunk.chunkZ(), present)) continue;
                volumes.add(volume);

                // Written columns near the structure (they can reference structures that overlap into them)
                LongSet near = structureColumns.computeIfAbsent(dimension, (ignored) -> new LongOpenHashSet());
                for (int x = chunk.chunkX() - 1; x <= chunk.chunkX() + 1; x++) {
                    for (int z = chunk.chunkZ() - 1; z <= chunk.chunkZ() + 1; z++) {
                        long packed = ChunkerStructureVolume.packChunk(x, z);
                        if (near.contains(packed)) continue;
                        if (isWrittenColumn(dimension, x, z, present)) near.add(packed);
                    }
                }
            }
        } catch (Exception e) {
            converter.logNonFatalException(e);
        }
    }

    /**
     * Parse the points of interest which villages recorded for their villagers, these become villager memories when
     * the villagers are read.
     *
     * @param output the level (unused, the memories are kept by the reader).
     */
    protected void parseVillages(ChunkerLevel output) {
        if (!converter.shouldProcessEntities()) return;
        byte[] prefix = BedrockVillageParser.KEY_PREFIX.getBytes(StandardCharsets.US_ASCII);
        try (DBIterator iterator = database.iterator(new ReadOptions().fillCache(false))) {
            iterator.seek(prefix);
            while (iterator.hasNext()) {
                Map.Entry<byte[], byte[]> entry = iterator.next();
                if (!LevelDBKey.startsWith(entry.getKey(), prefix)) break;
                String key = new String(entry.getKey(), StandardCharsets.US_ASCII);
                Dimension dimension = BedrockVillageParser.getDimension(key);
                if (dimension == null) continue;
                try {
                    BedrockVillageParser.parsePOI(entry.getValue(), dimension, villagerMemories);
                } catch (Exception e) {
                    converter.logNonFatalException(new Exception("Failed to parse village record " + key, e));
                }
            }
        } catch (Exception e) {
            converter.logNonFatalException(e);
        }
    }

    @Override
    public Optional<Integer> findMapIndex(long mapId) {
        ChunkerLevel level = readLevel;
        return level == null ? Optional.empty() : level.findMapIndexByOriginalID(mapId);
    }

    @Override
    public @Nullable Map<ChunkerVillagerMemory, ChunkerGlobalPosition> getVillagerMemories(long villagerId) {
        return villagerMemories.get(villagerId);
    }

    @Override
    public Collection<Long> getPlayerIds() {
        return playerIds;
    }

    /**
     * Check whether a column will be written by the converter.
     *
     * @param dimension the dimension of the column.
     * @param chunkX    the X of the chunk.
     * @param chunkZ    the Z of the chunk.
     * @param present   the columns which are present in the database.
     * @return true if the column exists and is processed.
     */
    private boolean isWrittenColumn(Dimension dimension, int chunkX, int chunkZ, LongSet present) {
        if (!present.contains(ChunkerStructureVolume.packChunk(chunkX, chunkZ))) return false;
        return converter.shouldProcessRegion(dimension, new RegionCoordPair(chunkX >> 5, chunkZ >> 5))
                && converter.shouldProcessColumn(dimension, new ChunkCoordPair(chunkX, chunkZ));
    }

    /**
     * Parse all the biome data in the world.
     *
     * @param output the output to add the biome data to.
     */
    protected void parseBiomeList(ChunkerLevel output) {
        // Support for custom biomes was added in 1.21.110
    }

    @Override
    public int getOrCreateLodestoneData(ChunkerLodestoneData lodestoneData) {
        return -1; // By default, return -1 as it's not supported on old versions
    }

    @Override
    public @Nullable ChunkerLodestoneData getLodestoneData(int index) {
        return null; // Return null as it's not supported on old versions
    }

    /**
     * Create a new reader for worlds.
     *
     * @param presentRegions the regions present in the dimension.
     * @param dimension      the dimension type.
     * @return the newly created world reader.
     */
    public BedrockWorldReader createWorldReader(Map<RegionCoordPair, Set<ChunkCoordPair>> presentRegions, Dimension dimension) {
        return new BedrockWorldReader(resolvers, converter, database, presentRegions, dimension);
    }
}
