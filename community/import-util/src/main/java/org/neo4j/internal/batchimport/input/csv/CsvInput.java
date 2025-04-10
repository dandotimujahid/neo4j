/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neo4j.internal.batchimport.input.csv;

import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.neo4j.batchimport.api.input.Collector.EMPTY;
import static org.neo4j.csv.reader.CharSeekers.charSeeker;
import static org.neo4j.internal.batchimport.input.InputEntityDecorators.NO_DECORATOR;
import static org.neo4j.internal.batchimport.input.csv.CsvInputIterator.extractHeader;
import static org.neo4j.io.ByteUnit.mebiBytes;
import static org.neo4j.io.pagecache.context.CursorContext.NULL_CONTEXT;
import static org.neo4j.util.Preconditions.checkState;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;
import org.apache.logging.log4j.util.TriConsumer;
import org.neo4j.batchimport.api.InputIterable;
import org.neo4j.batchimport.api.InputIterator;
import org.neo4j.batchimport.api.input.Collector;
import org.neo4j.batchimport.api.input.IdType;
import org.neo4j.batchimport.api.input.Input;
import org.neo4j.batchimport.api.input.PropertySizeCalculator;
import org.neo4j.batchimport.api.input.ReadableGroups;
import org.neo4j.csv.reader.CharReadable;
import org.neo4j.csv.reader.CharSeeker;
import org.neo4j.csv.reader.Configuration;
import org.neo4j.csv.reader.Extractor;
import org.neo4j.csv.reader.Extractors;
import org.neo4j.csv.reader.MultiReadable;
import org.neo4j.internal.batchimport.input.Groups;
import org.neo4j.internal.batchimport.input.InputEntity;
import org.neo4j.internal.batchimport.input.Inputs;
import org.neo4j.internal.schema.SchemaCommand;
import org.neo4j.internal.schema.SchemaDescriptor;
import org.neo4j.internal.schema.SchemaDescriptors;
import org.neo4j.io.ByteUnit;
import org.neo4j.memory.MemoryTracker;
import org.neo4j.token.TokenHolders;
import org.neo4j.token.api.TokenConstants;
import org.neo4j.util.Preconditions;

/**
 * Provides {@link Input} from data contained in tabular/csv form. Expects factories for instantiating
 * the {@link CharSeeker} objects seeking values in the csv data and header factories for how to
 * extract meta data about the values.
 */
public class CsvInput implements Input {
    private static final long ESTIMATE_SAMPLE_SIZE = mebiBytes(1);

    private final Iterable<DataFactory> nodeDataFactory;
    private final Header.Factory nodeHeaderFactory;
    private final Iterable<DataFactory> relationshipDataFactory;
    private final Header.Factory relationshipHeaderFactory;
    private final List<SchemaCommand> schemaCommands;
    private final IdType idType;
    private final Configuration config;
    private final Monitor monitor;
    private final Groups groups;
    private final boolean autoSkipHeaders;
    private final MemoryTracker memoryTracker;

    /**
     * @param nodeDataFactory multiple {@link DataFactory} instances providing data, each {@link DataFactory}
     * specifies an input group with its own header, extracted by the {@code nodeHeaderFactory}. From the outside
     * it looks like one stream of nodes.
     * @param nodeHeaderFactory factory for reading node headers.
     * @param relationshipDataFactory multiple {@link DataFactory} instances providing data, each {@link DataFactory}
     * specifies an input group with its own header, extracted by the {@code relationshipHeaderFactory}.
     * From the outside it looks like one stream of relationships.
     * @param relationshipHeaderFactory factory for reading relationship headers.
     * @param idType {@link IdType} to expect in id fields of node and relationship input.
     * @param config CSV configuration.
     * @param autoSkipHeaders  flag to skip headers
     * @param monitor {@link Monitor} for internal events.
     * @param memoryTracker the {@link MemoryTracker} to use
     */
    public CsvInput(
            Iterable<DataFactory> nodeDataFactory,
            Header.Factory nodeHeaderFactory,
            Iterable<DataFactory> relationshipDataFactory,
            Header.Factory relationshipHeaderFactory,
            IdType idType,
            Configuration config,
            boolean autoSkipHeaders,
            Monitor monitor,
            MemoryTracker memoryTracker) {
        this(
                nodeDataFactory,
                nodeHeaderFactory,
                relationshipDataFactory,
                relationshipHeaderFactory,
                List.of(),
                idType,
                config,
                autoSkipHeaders,
                monitor,
                new Groups(),
                memoryTracker);
    }

    /**
     * @param nodeDataFactory multiple {@link DataFactory} instances providing data, each {@link DataFactory}
     * specifies an input group with its own header, extracted by the {@code nodeHeaderFactory}. From the outside
     * it looks like one stream of nodes.
     * @param nodeHeaderFactory factory for reading node headers.
     * @param relationshipDataFactory multiple {@link DataFactory} instances providing data, each {@link DataFactory}
     * specifies an input group with its own header, extracted by the {@code relationshipHeaderFactory}.
     * From the outside it looks like one stream of relationships.
     * @param relationshipHeaderFactory factory for reading relationship headers.
     * @param idType {@link IdType} to expect in id fields of node and relationship input.
     * @param config CSV configuration.
     * @param autoSkipHeaders  flag to skip headers
     * @param monitor {@link Monitor} for internal events.
     * @param groups the ID groups to use
     * @param memoryTracker the {@link MemoryTracker} to use
     */
    public CsvInput(
            Iterable<DataFactory> nodeDataFactory,
            Header.Factory nodeHeaderFactory,
            Iterable<DataFactory> relationshipDataFactory,
            Header.Factory relationshipHeaderFactory,
            IdType idType,
            Configuration config,
            boolean autoSkipHeaders,
            Monitor monitor,
            Groups groups,
            MemoryTracker memoryTracker) {
        this(
                nodeDataFactory,
                nodeHeaderFactory,
                relationshipDataFactory,
                relationshipHeaderFactory,
                List.of(),
                idType,
                config,
                autoSkipHeaders,
                monitor,
                groups,
                memoryTracker);
    }

    /**
     * @param nodeDataFactory multiple {@link DataFactory} instances providing data, each {@link DataFactory}
     * specifies an input group with its own header, extracted by the {@code nodeHeaderFactory}. From the outside
     * it looks like one stream of nodes.
     * @param nodeHeaderFactory factory for reading node headers.
     * @param relationshipDataFactory multiple {@link DataFactory} instances providing data, each {@link DataFactory}
     * specifies an input group with its own header, extracted by the {@code relationshipHeaderFactory}.
     * From the outside it looks like one stream of relationships.
     * @param relationshipHeaderFactory factory for reading relationship headers.
     * @param schemaCommands the schema changes to apply to the database after the data is imported.
     * @param idType {@link IdType} to expect in id fields of node and relationship input.
     * @param config CSV configuration.
     * @param autoSkipHeaders  flag to skip headers
     * @param monitor {@link Monitor} for internal events.
     * @param groups the ID groups to use
     * @param memoryTracker the {@link MemoryTracker} to use
     *
     */
    public CsvInput(
            Iterable<DataFactory> nodeDataFactory,
            Header.Factory nodeHeaderFactory,
            Iterable<DataFactory> relationshipDataFactory,
            Header.Factory relationshipHeaderFactory,
            List<SchemaCommand> schemaCommands,
            IdType idType,
            Configuration config,
            boolean autoSkipHeaders,
            Monitor monitor,
            Groups groups,
            MemoryTracker memoryTracker) {
        this.autoSkipHeaders = autoSkipHeaders;
        this.memoryTracker = memoryTracker;
        assertSaneConfiguration(config);

        this.nodeDataFactory = nodeDataFactory;
        this.nodeHeaderFactory = nodeHeaderFactory;
        this.relationshipDataFactory = relationshipDataFactory;
        this.relationshipHeaderFactory = relationshipHeaderFactory;
        this.schemaCommands = schemaCommands;
        this.idType = idType;
        this.config = config;
        this.monitor = monitor;
        this.groups = groups;
    }

    private static void assertSaneConfiguration(Configuration config) {
        Map<Character, String> delimiters = new HashMap<>();
        delimiters.put(config.delimiter(), "delimiter");
        checkUniqueCharacter(delimiters, config.arrayDelimiter(), "array delimiter");
        checkUniqueCharacter(delimiters, config.quotationCharacter(), "quotation character");
    }

    private static void checkUniqueCharacter(
            Map<Character, String> characters, char character, String characterDescription) {
        String conflict = characters.put(character, characterDescription);
        if (conflict != null) {
            throw new IllegalArgumentException("Character '" + character + "' specified by " + characterDescription
                    + " is the same as specified by " + conflict);
        }
    }

    @Override
    public List<SchemaCommand> schemaCommands() {
        return schemaCommands;
    }

    @Override
    public InputIterable nodes(Collector badCollector) {
        return () -> stream(nodeDataFactory, nodeHeaderFactory, badCollector);
    }

    @Override
    public InputIterable relationships(Collector badCollector) {
        return () -> stream(relationshipDataFactory, relationshipHeaderFactory, badCollector);
    }

    private InputIterator stream(Iterable<DataFactory> data, Header.Factory headerFactory, Collector badCollector) {
        return new CsvGroupInputIterator(
                data.iterator(), headerFactory, idType, config, badCollector, groups, autoSkipHeaders, NO_MONITOR);
    }

    @Override
    public IdType idType() {
        return idType;
    }

    @Override
    public ReadableGroups groups() {
        return groups;
    }

    @Override
    public Estimates validateAndEstimate(PropertySizeCalculator valueSizeCalculator) throws IOException {
        final var seenSourceFiles = new HashSet<String>();
        // parse all node headers and remember all ID spaces
        final var nodeSample = validateAndEstimate(
                nodeDataFactory,
                nodeHeaderFactory,
                (header, source, noDecorator) -> {
                    if (Arrays.stream(header.entries()).noneMatch(entry -> entry.type() == Type.LABEL) && noDecorator) {
                        monitor.noNodeLabelsSpecified(source);
                    }

                    var numIdColumns = Arrays.stream(header.entries())
                            .filter(e -> e.type() == Type.ID)
                            .count();
                    if (numIdColumns > 1) {
                        Preconditions.checkState(
                                idType == IdType.STRING,
                                "Having multiple :ID columns requires idType:" + IdType.STRING);
                    }
                    var numIdColumnsGroups = Arrays.stream(header.entries())
                            .filter(e -> e.type() == Type.ID)
                            .map(Header.Entry::group)
                            .distinct()
                            .count();
                    Preconditions.checkState(
                            numIdColumnsGroups <= 1,
                            "There are multiple :ID columns, but they are referring to different groups");
                },
                valueSizeCalculator,
                node -> node.labels().length,
                seenSourceFiles);

        // parse all relationship headers and verify all ID spaces
        final var relationshipSample = validateAndEstimate(
                relationshipDataFactory,
                relationshipHeaderFactory,
                (header, source, noDecorator) -> {
                    // Merely parsing and constructing the header here will as a side effect verify that the id
                    // groups already exists (relationship header isn't allowed to create groups)
                    if (Arrays.stream(header.entries()).noneMatch(entry -> entry.type() == Type.TYPE) && noDecorator) {
                        monitor.noRelationshipTypeSpecified(source);
                    }
                },
                valueSizeCalculator,
                entity -> 0,
                seenSourceFiles);

        final var propPreAllocAdditional = propertyPreAllocateRounding(nodeSample[2] + relationshipSample[2]) / 2;
        return Input.knownEstimates(
                nodeSample[0],
                relationshipSample[0],
                nodeSample[1],
                relationshipSample[1],
                nodeSample[2] + propPreAllocAdditional,
                relationshipSample[2] + propPreAllocAdditional,
                nodeSample[3]);
    }

    private long[] validateAndEstimate(
            Iterable<DataFactory> dataFactories,
            Header.Factory headerFactory,
            TriConsumer<Header, String, Boolean> headerChecker,
            PropertySizeCalculator valueSizeCalculator,
            ToIntFunction<InputEntity> additionalCalculator,
            Set<String> seenSourceFiles)
            throws IOException {
        final var estimates = new long[4]; // [entity count, property count, property size, labels (for nodes only)]

        try (var chunk = new CsvInputChunkProxy()) {
            final var sampleConfig =
                    config.toBuilder().withReadIsForSampling(true).build();
            var groupId = 0;
            for (var dataFactory : dataFactories) {
                // one input group
                groupId++;
                Header header = null;
                final var data = dataFactory.create(sampleConfig);
                try (var decorator = data.decorator()) {
                    final var stream = data.stream();
                    while (stream.hasNext()) {
                        try (var source = stream.next()) {
                            final var sourceDescription = source.sourceDescription();
                            if (!seenSourceFiles.add(sourceDescription)) {
                                monitor.duplicateSourceFile(sourceDescription);
                            }

                            if (header == null) {
                                // Parsing and constructing this header will create this group
                                // Extract the header from the first file in this group
                                // This is the only place we monitor type normalization because it's before import and
                                // it touches all headers
                                header = extractHeader(source, headerFactory, idType, sampleConfig, groups, monitor);
                                headerChecker.accept(header, sourceDescription, decorator == NO_DECORATOR);
                            }

                            sample(
                                    chunk,
                                    sampleConfig,
                                    source,
                                    groupId,
                                    header,
                                    decorator,
                                    valueSizeCalculator,
                                    additionalCalculator,
                                    estimates);
                        }
                    }
                }
            }
        }

        return estimates;
    }

    private void sample(
            CsvInputChunkProxy chunk,
            Configuration sampleConfig,
            CharReadable source,
            int groupId,
            Header header,
            Decorator decorator,
            PropertySizeCalculator valueSizeCalculator,
            ToIntFunction<InputEntity> additionalCalculator,
            long[] estimates)
            throws IOException {
        try (var iterator = new CsvInputIterator(
                        source,
                        decorator,
                        header,
                        sampleConfig,
                        idType,
                        EMPTY,
                        CsvGroupInputIterator.extractors(sampleConfig),
                        groupId,
                        autoSkipHeaders);
                var entity = new InputEntity()) {
            var entities = 0;
            var properties = 0d;
            var propertySize = 0d;
            var additional = 0d;
            while (iterator.position() < ESTIMATE_SAMPLE_SIZE && iterator.next(chunk)) {
                for (; chunk.next(entity); entities++) {
                    properties += entity.propertyCount();
                    propertySize +=
                            Inputs.calculatePropertySize(entity, valueSizeCalculator, NULL_CONTEXT, memoryTracker);
                    additional += additionalCalculator.applyAsInt(entity);
                }
            }
            if (entities > 0) {
                final var position = iterator.position();
                final var actualFileSize = source.length() / iterator.compressionRatio();
                final var entityCountInSource = ((actualFileSize / position) * entities);
                estimates[0] += (long) entityCountInSource;
                estimates[1] += (long) ((properties / entities) * entityCountInSource);
                estimates[2] += (long) ((propertySize / entities) * entityCountInSource);
                estimates[3] += (long) ((additional / entities) * entityCountInSource);
            }
        }
    }

    @Override
    public Map<String, SchemaDescriptor> referencedNodeSchema(TokenHolders tokenHolders) {
        try {
            // parse all node headers and remember all ID spaces
            Map<String, SchemaDescriptor> result = new HashMap<>();
            for (DataFactory dataFactory : nodeDataFactory) {
                Data data = dataFactory.create(config);
                try (CharSeeker dataStream = charSeeker(new MultiReadable(data.stream()), config, true)) {
                    // Parsing and constructing this header will create this group,
                    // so no need to do something with the result of it right now
                    Header header =
                            DataFactories.defaultFormatNodeFileHeader().create(dataStream, config, idType, groups);
                    collectReferencedNodeSchemaFromHeader(header, tokenHolders, result);
                }
            }
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void collectReferencedNodeSchemaFromHeader(
            Header header, TokenHolders tokenHolders, Map<String, SchemaDescriptor> result) {
        Arrays.stream(header.entries())
                .filter(e -> e.type() == Type.ID)
                .findAny()
                .ifPresent(entry -> {
                    var options = entry.rawOptions();
                    var labelName = options.get("label");
                    checkState(labelName != null, "No label was specified for the node index in '%s'", entry);
                    var keyName = entry.name();
                    checkState(keyName != null, "No property key was specified for node index in '%s'", entry);
                    var label = tokenHolders.labelTokens().getIdByName(labelName);
                    var key = tokenHolders.propertyKeyTokens().getIdByName(keyName);
                    checkState(
                            label != TokenConstants.NO_TOKEN,
                            "Label '%s' for node index specified in '%s' does not exist",
                            labelName,
                            entry);
                    checkState(
                            key != TokenConstants.NO_TOKEN,
                            "Property key '%s' for node index specified in '%s' does not exist",
                            keyName,
                            entry);
                    var schemaDescriptor = SchemaDescriptors.forLabel(label, key);
                    var prev = result.put(entry.group().name(), schemaDescriptor);
                    checkState(
                            prev == null || prev.equals(schemaDescriptor),
                            "Multiple different indexes for group " + entry.group());
                });
    }

    private static long propertyPreAllocateRounding(long initialEstimatedPropertyStoreSize) {
        if (!IS_OS_LINUX) {
            // Only linux systems does pre-allocation of store files, so the pre-allocation rounding is zero for all
            // other systems.
            return 0;
        }
        // By default, the page cache will grow large store files in 32 MiB sized chunks.
        long preAllocSize = ByteUnit.mebiBytes(32);
        if (initialEstimatedPropertyStoreSize < preAllocSize) {
            return 0;
        }
        long chunks = 1 + initialEstimatedPropertyStoreSize / preAllocSize;
        long estimatedFinalPropertyStoreSize = chunks * preAllocSize;
        // Compute the difference from the initial estimate, to what we anticipate when we account for pre-allocation.
        return estimatedFinalPropertyStoreSize - initialEstimatedPropertyStoreSize;
    }

    public static Extractor<?> idExtractor(IdType idType, Extractors extractors) {
        return switch (idType) {
            case STRING -> extractors.string();
            case INTEGER, ACTUAL -> extractors.long_();
        };
    }

    public interface Monitor extends Header.Monitor {
        /**
         * Reports that a given source file has been specified more than one time.
         * @param sourceFile source file that is a duplicate.
         */
        void duplicateSourceFile(String sourceFile);

        /**
         * Reports that a given source file, in this case the first in its group, doesn't specify any node label header and
         * the group wasn't assigned any label from the command line specification.
         * @param sourceFile source file of the first file in the file group that is missing node labels.
         */
        void noNodeLabelsSpecified(String sourceFile);

        /**
         * Reports that a given source file, in this case the first in its group, doesn't specify any relationship type header and
         * the group wasn't assigned a relationship type from the command line specification.
         * @param sourceFile source file of the first file in the file group that is missing the relationship type..
         */
        void noRelationshipTypeSpecified(String sourceFile);
    }

    public static final Monitor NO_MONITOR = new Monitor() {
        @Override
        public void duplicateSourceFile(String sourceFile) { // no-op
        }

        @Override
        public void noNodeLabelsSpecified(String sourceFile) { // no-op
        }

        @Override
        public void noRelationshipTypeSpecified(String sourceFile) { // no-op
        }

        @Override
        public void typeNormalized(String sourceDescription, String header, String fromType, String toType) { // no-op
        }
    };

    public static class PrintingMonitor extends Header.PrintingMonitor implements Monitor {
        private final PrintStream out;

        public PrintingMonitor(PrintStream out) {
            super(out);
            this.out = out;
        }

        @Override
        public void duplicateSourceFile(String sourceFile) {
            out.printf(
                    "WARN: source file %s has been specified multiple times, this may result in unwanted duplicates%n",
                    sourceFile);
        }

        @Override
        public void noNodeLabelsSpecified(String sourceFile) {
            out.printf(
                    "WARN: file group with header file %s specifies no node labels, which could be a mistake%n",
                    sourceFile);
        }

        @Override
        public void noRelationshipTypeSpecified(String sourceFile) {
            out.printf(
                    "WARN: file group with header file %s specifies no relationship type, which could be a mistake%n",
                    sourceFile);
        }
    }
}
