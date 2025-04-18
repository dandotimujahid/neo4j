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
package org.neo4j.procedure.builtin;

import static org.neo4j.cypher.operations.CypherFunctions.CYPHER_TYPE_NAME_VALUE_MAPPER;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.neo4j.cypher.internal.util.symbols.CypherType;
import org.neo4j.internal.kernel.api.CursorFactory;
import org.neo4j.internal.kernel.api.NodeCursor;
import org.neo4j.internal.kernel.api.PropertyCursor;
import org.neo4j.internal.kernel.api.Read;
import org.neo4j.internal.kernel.api.RelationshipScanCursor;
import org.neo4j.internal.kernel.api.TokenRead;
import org.neo4j.io.pagecache.context.CursorContext;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.memory.MemoryTracker;
import org.neo4j.token.api.NamedToken;
import org.neo4j.values.storable.Value;

public class SchemaCalculator {
    private final Map<Integer, String> propertyIdToPropertyNameMapping;

    private final MutableIntSet emptyPropertyIdSet = IntSets.mutable.empty();

    private final Read dataRead;
    private final TokenRead tokenRead;
    private final CursorFactory cursors;
    private final CursorContext cursorContext;
    private final MemoryTracker memoryTracker;
    private boolean useCypherTypes;

    SchemaCalculator(KernelTransaction ktx, boolean useCypherTypes) {
        this.dataRead = ktx.dataRead();
        this.tokenRead = ktx.tokenRead();
        this.cursors = ktx.cursors();
        this.cursorContext = ktx.cursorContext();
        this.memoryTracker = ktx.memoryTracker();
        this.useCypherTypes = useCypherTypes;

        // the only one that is common for both nodes and rels so thats why we can do it here
        propertyIdToPropertyNameMapping = new HashMap<>(tokenRead.propertyKeyCount());
        addNamesToCollection(tokenRead.propertyKeyGetAllTokens(), propertyIdToPropertyNameMapping);
    }

    private NodeMappings initializeMappingsForNodes() {
        int labelCount = tokenRead.labelCount();
        return new NodeMappings(labelCount);
    }

    private RelationshipMappings initializeMappingsForRels() {
        int relationshipTypeCount = tokenRead.relationshipTypeCount();
        return new RelationshipMappings(relationshipTypeCount);
    }

    // If we would have this schema information in the count store (or somewhere), this could be super fast
    public Stream<NodePropertySchemaInfoResult> calculateTabularResultStreamForNodes() {
        NodeMappings nodeMappings = initializeMappingsForNodes();
        scanEverythingBelongingToNodes(nodeMappings, cursorContext, memoryTracker);

        // go through all labels to get actual names
        addNamesToCollection(tokenRead.labelsGetAllTokens(), nodeMappings.labelIdToLabelName);

        return produceResultsForNodes(nodeMappings).stream();
    }

    public Stream<RelationshipPropertySchemaInfoResult> calculateTabularResultStreamForRels() {
        RelationshipMappings relMappings = initializeMappingsForRels();
        scanEverythingBelongingToRelationships(relMappings, cursorContext, memoryTracker);

        // go through all relationshipTypes to get actual names
        addNamesToCollection(
                tokenRead.relationshipTypesGetAllTokens(), relMappings.relationshipTypIdToRelationshipName);

        return produceResultsForRelationships(relMappings).stream();
    }

    private List<RelationshipPropertySchemaInfoResult> produceResultsForRelationships(
            RelationshipMappings relMappings) {
        List<RelationshipPropertySchemaInfoResult> results = new ArrayList<>();
        for (Integer typeId : relMappings.relationshipTypeIdToPropertyKeys.keySet()) {
            // lookup typ name
            String name = relMappings.relationshipTypIdToRelationshipName.get(typeId);
            name = ":`" + name + "`"; // escaping

            // lookup property value types
            MutableIntSet propertyIds = relMappings.relationshipTypeIdToPropertyKeys.get(typeId);
            if (propertyIds.size() == 0) {
                results.add(new RelationshipPropertySchemaInfoResult(name, null, null, false));
            } else {
                String finalName = name;
                propertyIds.forEach(propId -> {
                    // lookup propId name and valueGroup
                    String propName = propertyIdToPropertyNameMapping.get(propId);
                    ValueTypeListHelper valueTypeListHelper =
                            relMappings.relationshipTypeIdANDPropertyTypeIdToValueType.get(
                                    new RelationshipTypePropertyKey(typeId, propId));
                    if (relMappings.nullableRelationshipTypes.contains(typeId)) {
                        results.add(new RelationshipPropertySchemaInfoResult(
                                finalName, propName, valueTypeListHelper.getCypherTypesList(), false));
                    } else {
                        results.add(new RelationshipPropertySchemaInfoResult(
                                finalName,
                                propName,
                                valueTypeListHelper.getCypherTypesList(),
                                valueTypeListHelper.isMandatory()));
                    }
                });
            }
        }
        return results;
    }

    private List<NodePropertySchemaInfoResult> produceResultsForNodes(NodeMappings nodeMappings) {
        List<NodePropertySchemaInfoResult> results = new ArrayList<>();
        for (SortedLabels labelSet : nodeMappings.labelSetToPropertyKeys.keySet()) {
            // lookup label names and produce list of names and produce String out of them
            List<String> labelNames = new ArrayList<>();
            for (int i = 0; i < labelSet.numberOfLabels(); i++) {
                String name = nodeMappings.labelIdToLabelName.get(labelSet.label(i));
                labelNames.add(name);
            }
            Collections.sort(labelNames); // this is optional but waaaaay nicer
            StringBuilder labelsConcatenator = new StringBuilder();
            for (String item : labelNames) {
                labelsConcatenator.append(":`").append(item).append('`');
            }
            String labels = labelsConcatenator.toString();

            // lookup property value types
            MutableIntSet propertyIds = nodeMappings.labelSetToPropertyKeys.get(labelSet);
            if (propertyIds.size() == 0) {
                results.add(new NodePropertySchemaInfoResult(labels, labelNames, null, null, false));
            } else {
                propertyIds.forEach(propId -> {
                    // lookup propId name and valueGroup
                    String propName = propertyIdToPropertyNameMapping.get(propId);
                    ValueTypeListHelper valueTypeListHelper = nodeMappings.labelSetANDNodePropertyKeyIdToValueType.get(
                            new LabelSetPropertyKey(labelSet, propId));
                    if (nodeMappings.nullableLabelSets.contains(labelSet)) {
                        results.add(new NodePropertySchemaInfoResult(
                                labels, labelNames, propName, valueTypeListHelper.getCypherTypesList(), false));
                    } else {
                        results.add(new NodePropertySchemaInfoResult(
                                labels,
                                labelNames,
                                propName,
                                valueTypeListHelper.getCypherTypesList(),
                                valueTypeListHelper.isMandatory()));
                    }
                });
            }
        }
        return results;
    }

    private void scanEverythingBelongingToRelationships(
            RelationshipMappings relMappings, CursorContext cursorContext, MemoryTracker memoryTracker) {
        try (RelationshipScanCursor relationshipScanCursor =
                        cursors.allocateRelationshipScanCursor(cursorContext, memoryTracker);
                PropertyCursor propertyCursor = cursors.allocatePropertyCursor(cursorContext, memoryTracker)) {
            dataRead.allRelationshipsScan(relationshipScanCursor);
            while (relationshipScanCursor.next()) {
                int typeId = relationshipScanCursor.type();
                relationshipScanCursor.properties(propertyCursor);
                MutableIntSet propertyIds = IntSets.mutable.empty();

                while (propertyCursor.next()) {
                    int propertyKey = propertyCursor.propertyKey();

                    Value currentValue = propertyCursor.propertyValue();
                    var key = new RelationshipTypePropertyKey(typeId, propertyKey);
                    updateValueTypeInMapping(
                            currentValue,
                            key,
                            relMappings.relationshipTypeIdANDPropertyTypeIdToValueType,
                            this.useCypherTypes);

                    propertyIds.add(propertyKey);
                }
                propertyCursor.close();

                MutableIntSet oldPropertyKeySet =
                        relMappings.relationshipTypeIdToPropertyKeys.getOrDefault(typeId, emptyPropertyIdSet);

                // find out which old properties we did not visited and mark them as nullable
                if (oldPropertyKeySet == emptyPropertyIdSet) {
                    if (propertyIds.size() == 0) {
                        // Even if we find property key on other rels with this type, set all of them nullable
                        relMappings.nullableRelationshipTypes.add(typeId);
                    }

                    propertyIds.addAll(oldPropertyKeySet);
                } else {
                    MutableIntSet currentPropertyIdsHelperSet = new IntHashSet(propertyIds.size());
                    currentPropertyIdsHelperSet.addAll(propertyIds);
                    propertyIds.removeAll(oldPropertyKeySet); // only the brand new ones in propIds now
                    oldPropertyKeySet.removeAll(
                            currentPropertyIdsHelperSet); // only the old ones that are not on the new rel

                    propertyIds.addAll(oldPropertyKeySet);
                    propertyIds.forEach(id -> {
                        var key = new RelationshipTypePropertyKey(typeId, id);
                        relMappings
                                .relationshipTypeIdANDPropertyTypeIdToValueType
                                .get(key)
                                .setNullable();
                    });

                    propertyIds.addAll(currentPropertyIdsHelperSet);
                }

                relMappings.relationshipTypeIdToPropertyKeys.put(typeId, propertyIds);
            }
        }
    }

    private void scanEverythingBelongingToNodes(
            NodeMappings nodeMappings, CursorContext cursorContext, MemoryTracker memoryTracker) {
        try (NodeCursor nodeCursor = cursors.allocateNodeCursor(cursorContext, memoryTracker);
                PropertyCursor propertyCursor = cursors.allocatePropertyCursor(cursorContext, memoryTracker)) {
            dataRead.allNodesScan(nodeCursor);
            while (nodeCursor.next()) {
                // each node
                SortedLabels labels = SortedLabels.from(nodeCursor.labels());
                nodeCursor.properties(propertyCursor);
                MutableIntSet propertyIds = IntSets.mutable.empty();

                while (propertyCursor.next()) {
                    Value currentValue = propertyCursor.propertyValue();
                    int propertyKeyId = propertyCursor.propertyKey();
                    var key = new LabelSetPropertyKey(labels, propertyKeyId);
                    updateValueTypeInMapping(
                            currentValue,
                            key,
                            nodeMappings.labelSetANDNodePropertyKeyIdToValueType,
                            this.useCypherTypes);

                    propertyIds.add(propertyKeyId);
                }
                propertyCursor.close();

                MutableIntSet oldPropertyKeySet =
                        nodeMappings.labelSetToPropertyKeys.getOrDefault(labels, emptyPropertyIdSet);

                // find out which old properties we did not visited and mark them as nullable
                if (oldPropertyKeySet == emptyPropertyIdSet) {
                    if (propertyIds.size() == 0) {
                        // Even if we find property key on other nodes with those labels, set all of them nullable
                        nodeMappings.nullableLabelSets.add(labels);
                    }

                    propertyIds.addAll(oldPropertyKeySet);
                } else {
                    MutableIntSet currentPropertyIdsHelperSet = new IntHashSet(propertyIds.size());
                    currentPropertyIdsHelperSet.addAll(propertyIds);
                    propertyIds.removeAll(oldPropertyKeySet); // only the brand new ones in propIds now
                    oldPropertyKeySet.removeAll(
                            currentPropertyIdsHelperSet); // only the old ones that are not on the new node

                    propertyIds.addAll(oldPropertyKeySet);
                    propertyIds.forEach(id -> {
                        var key = new LabelSetPropertyKey(labels, id);
                        nodeMappings
                                .labelSetANDNodePropertyKeyIdToValueType
                                .get(key)
                                .setNullable();
                    });

                    propertyIds.addAll(currentPropertyIdsHelperSet);
                }

                nodeMappings.labelSetToPropertyKeys.put(labels, propertyIds);
            }
        }
    }

    private static <T> void updateValueTypeInMapping(
            Value currentValue, T key, Map<T, ValueTypeListHelper> mappingToUpdate, boolean useCypherTypes) {
        ValueTypeListHelper helper = mappingToUpdate.get(key);
        if (helper == null) {
            helper = new ValueTypeListHelper(currentValue, useCypherTypes);
            mappingToUpdate.put(key, helper);
        } else {
            helper.updateValueTypesWith(currentValue);
        }
    }

    private static void addNamesToCollection(Iterator<NamedToken> labelIterator, Map<Integer, String> collection) {
        while (labelIterator.hasNext()) {
            NamedToken label = labelIterator.next();
            collection.put(label.id(), label.name());
        }
    }

    private static class ValueTypeListHelper {
        private final Set<String> seenValueTypes;
        private boolean isMandatory = true;
        private boolean useCypherTypes;

        ValueTypeListHelper(Value v, boolean useCypherTypes) {
            seenValueTypes = new HashSet<>();
            this.useCypherTypes = useCypherTypes;
            updateValueTypesWith(v);
        }

        private void setNullable() {
            isMandatory = false;
        }

        public boolean isMandatory() {
            return isMandatory;
        }

        List<String> getCypherTypesList() {
            return new ArrayList<>(seenValueTypes);
        }

        void updateValueTypesWith(Value newValue) {
            if (newValue == null) {
                throw new IllegalArgumentException();
            }

            seenValueTypes.add(
                    useCypherTypes
                            ? CypherType.normalizeTypes(newValue.map(CYPHER_TYPE_NAME_VALUE_MAPPER))
                                    .description()
                            : newValue.getTypeName());
        }
    }

    /*
     All mappings needed to describe Nodes except for property infos
    */
    private static class NodeMappings {
        final Map<SortedLabels, MutableIntSet> labelSetToPropertyKeys;
        final Map<LabelSetPropertyKey, ValueTypeListHelper> labelSetANDNodePropertyKeyIdToValueType;
        final Set<SortedLabels>
                nullableLabelSets; // used for label combinations without properties -> all properties are viewed as
        // nullable
        final Map<Integer, String> labelIdToLabelName;

        NodeMappings(int labelCount) {
            labelSetToPropertyKeys = new HashMap<>(labelCount);
            labelIdToLabelName = new HashMap<>(labelCount);
            labelSetANDNodePropertyKeyIdToValueType = new HashMap<>();
            nullableLabelSets = new HashSet<>();
        }
    }

    private record LabelSetPropertyKey(SortedLabels sortedLabels, int proprtyId) {}

    /*
     All mappings needed to describe Rels except for property infos
    */
    private static class RelationshipMappings {
        final Map<Integer, String> relationshipTypIdToRelationshipName;
        final Map<Integer, MutableIntSet> relationshipTypeIdToPropertyKeys;
        final Map<RelationshipTypePropertyKey, ValueTypeListHelper> relationshipTypeIdANDPropertyTypeIdToValueType;
        final Set<Integer>
                nullableRelationshipTypes; // used for types without properties -> all properties are viewed as nullable

        RelationshipMappings(int relationshipTypeCount) {
            relationshipTypIdToRelationshipName = new HashMap<>(relationshipTypeCount);
            relationshipTypeIdToPropertyKeys = new HashMap<>(relationshipTypeCount);
            relationshipTypeIdANDPropertyTypeIdToValueType = new HashMap<>();
            nullableRelationshipTypes = new HashSet<>();
        }
    }

    private record RelationshipTypePropertyKey(int relationshipType, int propertyId) {}
}
