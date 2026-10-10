package com.hivemc.chunker.conversion.encoding.java.v1_21.writer;

import com.hivemc.chunker.conversion.encoding.base.Converter;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.encoding.java.base.writer.JavaChunkWriter;
import com.hivemc.chunker.conversion.encoding.java.base.writer.JavaWorldWriter;
import com.hivemc.chunker.conversion.handlers.structure.StructureAssembler;
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerStructureVolume;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.nbt.TagType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.TagWithName;
import com.hivemc.chunker.nbt.tags.array.IntArrayTag;
import com.hivemc.chunker.nbt.tags.array.LongArrayTag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;

public class ColumnWriter extends com.hivemc.chunker.conversion.encoding.java.v1_20_5.writer.ColumnWriter {
    public ColumnWriter(JavaWorldWriter parent, Converter converter, JavaResolvers resolvers, Dimension dimension) {
        super(parent, converter, resolvers, dimension);
    }

    @Override
    public JavaChunkWriter createChunkWriter(ChunkerColumn column) {
        return new ChunkWriter(converter, resolvers, dimension, column);
    }

    @Override
    @Nullable
    protected TagWithName<?> writeStructures(ChunkerColumn column) {
        StructureAssembler.DimensionStructures structures = parent.getStructures(dimension);
        if (structures == null) return null;

        long position = ChunkerStructureVolume.packChunk(column.getPosition().chunkX(), column.getPosition().chunkZ());
        Map<String, StructureAssembler.AssembledStart> starts = structures.startsByChunk().get(position);
        Map<String, LongSet> references = structures.referencesByChunk().get(position);
        if (starts == null && references == null) return null;

        // Write the starts
        CompoundTag startsTag = new CompoundTag();
        if (starts != null) {
            for (StructureAssembler.AssembledStart start : starts.values()) {
                ListTag<CompoundTag, Map<String, Tag<?>>> children = new ListTag<>(TagType.COMPOUND, start.pieces().size());
                for (StructureAssembler.AssembledPiece piece : start.pieces()) {
                    CompoundTag pieceTag = new CompoundTag();
                    pieceTag.put("id", piece.id());
                    pieceTag.put("BB", new IntArrayTag(piece.box().toArray()));
                    for (Map.Entry<String, Tag<?>> field : piece.extra()) {
                        pieceTag.put(field.getKey(), field.getValue().clone());
                    }
                    children.add(pieceTag);
                }

                CompoundTag startTag = new CompoundTag();
                startTag.put("id", start.id());
                startTag.put("ChunkX", start.chunkX());
                startTag.put("ChunkZ", start.chunkZ());
                startTag.put("references", 0);
                startTag.put("Children", children);
                startsTag.put(start.id(), startTag);
            }
        }

        // Write the references
        CompoundTag referencesTag = new CompoundTag();
        if (references != null) {
            for (Map.Entry<String, LongSet> reference : references.entrySet()) {
                long[] values = reference.getValue().toLongArray();
                Arrays.sort(values);
                referencesTag.put(reference.getKey(), new LongArrayTag(values));
            }
        }

        CompoundTag structuresTag = new CompoundTag();
        structuresTag.put("starts", startsTag);
        structuresTag.put("References", referencesTag);
        return new TagWithName<>("structures", structuresTag);
    }
}
