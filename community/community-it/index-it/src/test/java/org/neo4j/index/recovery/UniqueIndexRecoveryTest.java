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
package org.neo4j.index.recovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;
import static org.neo4j.graphdb.Label.label;
import static org.neo4j.io.pagecache.context.CursorContext.NULL_CONTEXT;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.neo4j.common.DependencyResolver;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.io.pagecache.tracing.DatabaseFlushEvent;
import org.neo4j.kernel.impl.transaction.log.LogAppendEvent;
import org.neo4j.kernel.impl.transaction.log.checkpoint.CheckPointer;
import org.neo4j.kernel.impl.transaction.log.checkpoint.CheckPointerImpl;
import org.neo4j.kernel.impl.transaction.log.checkpoint.SimpleTriggerInfo;
import org.neo4j.kernel.impl.transaction.log.files.LogFiles;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;
import org.neo4j.test.extension.Inject;
import org.neo4j.test.extension.testdirectory.TestDirectoryExtension;
import org.neo4j.test.utils.TestDirectory;

@TestDirectoryExtension
public class UniqueIndexRecoveryTest {
    @Inject
    private TestDirectory storeDir;

    private static final String PROPERTY_KEY = "key";
    private static final String PROPERTY_VALUE = "value";
    private static final Label LABEL = label("label");

    private GraphDatabaseAPI db;
    private DatabaseManagementService managementService;

    private void setupDatabase() {
        db = (GraphDatabaseAPI) newDb();
    }

    @AfterEach
    void after() {
        managementService.shutdown();
    }

    @Test
    void shouldRecoverCreationOfUniquenessConstraintFollowedByDeletionOfThatSameConstraint() throws Exception {
        setupDatabase();

        // given
        createUniqueConstraint();
        dropConstraints();

        // when - perform recovery
        restart(snapshot(storeDir.absolutePath()));

        // then - just make sure the constraint is gone
        try (Transaction tx = db.beginTx()) {
            assertFalse(tx.schema().getConstraints(LABEL).iterator().hasNext());
            tx.commit();
        }
    }

    @Test
    void shouldRecoverWhenCommandsTemporarilyViolateConstraints() throws Exception {
        setupDatabase();

        // GIVEN
        Node unLabeledNode = createUnLabeledNodeWithProperty();
        Node labeledNode = createLabeledNode();
        createUniqueConstraint();
        rotateLogAndCheckPoint(); // snapshot
        setPropertyOnLabeledNode(labeledNode);
        deletePropertyOnLabeledNode(labeledNode);
        addLabelToUnLabeledNode(unLabeledNode);
        flushAll(); // persist - recovery will do everything since last log rotate

        // WHEN recovery is triggered
        restart(snapshot(storeDir.absolutePath()));

        // THEN
        // it should just not blow up!
        try (Transaction tx = db.beginTx()) {
            assertThat(tx.findNode(LABEL, PROPERTY_KEY, PROPERTY_VALUE)).isEqualTo(unLabeledNode);
            tx.commit();
        }
    }

    private void restart(Path newStore) {
        managementService.shutdown();
        managementService = new TestDatabaseManagementServiceBuilder(newStore).build();
        db = (GraphDatabaseAPI) managementService.database(DEFAULT_DATABASE_NAME);
    }

    private GraphDatabaseService newDb() {
        managementService = new TestDatabaseManagementServiceBuilder(storeDir.absolutePath()).build();
        return managementService.database(DEFAULT_DATABASE_NAME);
    }

    private static Path snapshot(final Path path) throws IOException {
        Path snapshotDir = path.resolve("snapshot-" + new Random().nextInt());
        FileUtils.copyDirectory(path, snapshotDir, pathName -> {
            String subPath = pathName.toAbsolutePath()
                    .toString()
                    .substring(path.toString().length() + 1);
            // since the db is running, exclude the lock files
            return !"store_lock".equals(subPath) && !subPath.endsWith("write.lock");
        });
        return snapshotDir;
    }

    private void addLabelToUnLabeledNode(Node unLabeledNode) {
        try (Transaction tx = db.beginTx()) {
            tx.getNodeById(unLabeledNode.getId()).addLabel(LABEL);
            tx.commit();
        }
    }

    private void setPropertyOnLabeledNode(Node labeledNode) {
        try (Transaction tx = db.beginTx()) {
            tx.getNodeById(labeledNode.getId()).setProperty(PROPERTY_KEY, PROPERTY_VALUE);
            tx.commit();
        }
    }

    private void deletePropertyOnLabeledNode(Node labeledNode) {
        try (Transaction tx = db.beginTx()) {
            tx.getNodeById(labeledNode.getId()).removeProperty(PROPERTY_KEY);
            tx.commit();
        }
    }

    private void createUniqueConstraint() {
        try (Transaction tx = db.beginTx()) {
            tx.schema()
                    .constraintFor(LABEL)
                    .assertPropertyIsUnique(PROPERTY_KEY)
                    .create();
            tx.commit();
        }
    }

    private Node createLabeledNode() {
        try (Transaction tx = db.beginTx()) {
            Node node = tx.createNode(LABEL);
            tx.commit();
            return node;
        }
    }

    private Node createUnLabeledNodeWithProperty() {
        try (Transaction tx = db.beginTx()) {
            Node node = tx.createNode();
            node.setProperty(PROPERTY_KEY, PROPERTY_VALUE);
            tx.commit();
            return node;
        }
    }

    private void dropConstraints() {
        try (Transaction tx = db.beginTx()) {
            for (ConstraintDefinition constraint : tx.schema().getConstraints(LABEL)) {
                constraint.drop();
            }
            tx.commit();
        }
    }

    private void rotateLogAndCheckPoint() throws IOException {
        DependencyResolver resolver = db.getDependencyResolver();
        resolver.resolveDependency(LogFiles.class).getLogFile().getLogRotation().rotateLogFile(LogAppendEvent.NULL);
        resolver.resolveDependency(CheckPointer.class).forceCheckPoint(new SimpleTriggerInfo("test"));
    }

    private void flushAll() throws IOException {
        db.getDependencyResolver()
                .resolveDependency(CheckPointerImpl.ForceOperation.class)
                .flushAndForce(DatabaseFlushEvent.NULL, NULL_CONTEXT);
    }
}
