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
package org.neo4j.kernel.api.impl.index.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.store.Directory;
import org.neo4j.internal.helpers.collection.NumberAwareStringComparator;
import org.neo4j.io.IOUtils;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.fs.FileSystemUtils;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.kernel.api.impl.index.storage.layout.FolderLayout;
import org.neo4j.kernel.api.impl.index.storage.layout.IndexFolderLayout;

/**
 * Utility class that manages directory structure for a partitioned lucene index.
 * It is aware of the {@link FileSystemAbstraction file system} structure of all index related folders, lucene
 * {@link Directory directories} and {@link FailureStorage failure storage}.
 */
public class PartitionedIndexStorage {
    private static final Comparator<Path> FILE_COMPARATOR = (o1, o2) -> NumberAwareStringComparator.INSTANCE.compare(
            o1.getFileName().toString(), o2.getFileName().toString());

    private final DirectoryFactory directoryFactory;
    private final FileSystemAbstraction fileSystem;
    private final FolderLayout folderLayout;
    private final FailureStorage failureStorage;

    public PartitionedIndexStorage(
            DirectoryFactory directoryFactory, FileSystemAbstraction fileSystem, Path rootFolder) {
        this.fileSystem = fileSystem;
        this.folderLayout = new IndexFolderLayout(rootFolder);
        this.directoryFactory = directoryFactory;
        this.failureStorage = new FailureStorage(fileSystem, folderLayout);
    }

    /**
     * Opens a {@link Directory lucene directory} for the given folder.
     *
     * @param folder the folder that denotes a lucene directory.
     * @return the lucene directory denoted by the given folder.
     * @throws IOException if directory can't be opened.
     */
    public Directory openDirectory(Path folder) throws IOException {
        return directoryFactory.open(folder);
    }

    /**
     * Resolves a folder for the partition with the given index.
     *
     * @param partition the partition index.
     * @return the folder where partition's lucene directory should be located.
     */
    public Path getPartitionFolder(int partition) {
        return folderLayout.getPartitionFolder(partition);
    }

    /**
     * Resolves root folder for the given index.
     *
     * @return the folder containing index partition folders.
     */
    public Path getIndexFolder() {
        return folderLayout.getIndexFolder();
    }

    /**
     * Create a failure storage in the {@link #getIndexFolder() index folder}.
     *
     * @throws IOException if failure storage creation fails.
     * @see FailureStorage#reserveForIndex()
     */
    public void reserveIndexFailureStorage() throws IOException {
        failureStorage.reserveForIndex();
    }

    /**
     * Writes index failure into the failure storage.
     *
     * @param failure the cause of the index failure.
     * @throws IOException if writing to the failure storage file failed.
     * @see FailureStorage#storeIndexFailure(String)
     */
    public void storeIndexFailure(String failure) throws IOException {
        failureStorage.storeIndexFailure(failure);
    }

    /**
     * Retrieves stored index failure.
     *
     * @return index failure as string or {@code null} if there is no failure.
     * @see FailureStorage#loadIndexFailure()
     */
    public String getStoredIndexFailure() {
        return failureStorage.loadIndexFailure();
    }

    public Path getIndexFailureFile() {
        return failureStorage.failureFile();
    }

    /**
     * For the given {@link Path folder} removes all nested folders from both {@link FileSystemAbstraction file system}
     * and {@link Directory lucene directories}.
     *
     * @param folder the folder to clean up.
     * @throws IOException if some removal operation fails.
     */
    public void prepareFolder(Path folder) throws IOException {
        cleanupFolder(folder);
        fileSystem.mkdirs(folder);
    }

    /**
     * For the given {@link Path folder} removes the folder itself and all nested folders from both
     * {@link FileSystemAbstraction file system} and {@link Directory lucene directories}.
     *
     * @param folder the folder to remove.
     * @throws IOException if some removal operation fails.
     */
    public void cleanupFolder(Path folder) throws IOException {
        if (!fileSystem.fileExists(folder)) {
            return;
        }
        List<Path> partitionFolders = listFolders(folder);
        if (!partitionFolders.isEmpty()) {
            for (Path partitionFolder : partitionFolders) {
                cleanupLuceneDirectory(partitionFolder);
            }
        }
        fileSystem.deleteRecursively(folder);
    }

    /**
     * Opens all {@link Directory lucene directories} contained in the {@link #getIndexFolder() index folder}.
     *
     * @return the map from file system  {@link Path directory} to the corresponding {@link Directory lucene directory}.
     * @throws IOException if opening of some lucene directory (via {@link DirectoryFactory#open(Path)}) fails.
     */
    public Map<Path, Directory> openIndexDirectories() throws IOException {
        Map<Path, Directory> directories = new LinkedHashMap<>();
        try {
            for (Path dir : listFolders()) {
                directories.put(dir, directoryFactory.open(dir));
            }
        } catch (IOException oe) {
            try {
                IOUtils.closeAll(directories.values());
            } catch (Exception ce) {
                oe.addSuppressed(ce);
            }
            throw oe;
        }
        return directories;
    }

    /**
     * List all folders in the {@link #getIndexFolder() index folder}.
     *
     * @return the list of index partition folders or {@link Collections#emptyList() empty list} if index folder is
     * empty.
     */
    public List<Path> listFolders() throws IOException {
        return listFolders(getIndexFolder());
    }

    private List<Path> listFolders(Path rootFolder) throws IOException {
        if (!fileSystem.fileExists(rootFolder)) {
            return Collections.emptyList();
        }
        return Stream.of(fileSystem.listFiles(rootFolder))
                .filter(f -> fileSystem.isDirectory(f)
                        && StringUtils.isNumeric(f.getFileName().toString()))
                .sorted(FILE_COMPARATOR)
                .toList();
    }

    /**
     * Removes content of the lucene directory denoted by the given {@link Path file}. This might seem unnecessary
     * since we cleanup the folder using {@link FileSystemAbstraction file system} but in fact for testing we often use
     * in-memory directories whose content can't be removed via the file system.
     * <p>
     * Uses {@link FileUtils#windowsSafeIOOperation(FileUtils.Operation)} underneath.
     *
     * @param folder the path to the directory to cleanup.
     * @throws IOException if removal operation fails.
     */
    private void cleanupLuceneDirectory(Path folder) throws IOException {
        try (Directory dir = directoryFactory.open(folder)) {
            String[] indexFiles = dir.listAll();
            for (String indexFile : indexFiles) {
                FileUtils.windowsSafeIOOperation(() -> dir.deleteFile(indexFile));
            }
        }
    }

    public long sizeInBytes() {
        return FileSystemUtils.size(fileSystem, folderLayout.getIndexFolder());
    }
}
