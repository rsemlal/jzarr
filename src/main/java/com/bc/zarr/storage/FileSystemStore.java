/*
 *
 * Copyright (C) 2020 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package com.bc.zarr.storage;

import com.bc.zarr.ZarrConstants;
import com.bc.zarr.ZarrUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class FileSystemStore implements Store {

    private Path root;

    public FileSystemStore(String path, FileSystem fileSystem) {
        if (fileSystem == null) {
            root = Paths.get(path);
        } else {
            root = fileSystem.getPath(path);
        }
    }

    public FileSystemStore(Path rootPath) {
        root = rootPath;
    }

    @Override
    public InputStream getInputStream(String key) throws IOException {
        final Path path = root.resolve(key);
        if (Files.isReadable(path)) {
            return Files.newInputStream(path);
        } else {
            return null;
        }
    }

    @Override
    public OutputStream getOutputStream(String key) throws IOException {
        final Path filePath = root.resolve(key);
        final Path dir = filePath.getParent();
        Files.createDirectories(dir);
        return Files.newOutputStream(filePath);
    }

    @Override
    public void delete(String key) throws IOException {
        final Path toBeDeleted = root.resolve(key);
        if (Files.isDirectory(toBeDeleted)) {
            ZarrUtils.deleteDirectoryTreeRecursively(toBeDeleted);
        }
        if (Files.exists(toBeDeleted)){
            Files.delete(toBeDeleted);
        }
        if (Files.exists(toBeDeleted)|| Files.isDirectory(toBeDeleted)) {
            throw new IOException("Unable to initialize " + toBeDeleted.toAbsolutePath().toString());
        }
    }

    @Override
    public TreeSet<String> getArrayKeys() throws IOException {
        return getKeysFor(ZarrConstants.FILENAME_DOT_ZARRAY);
    }

    @Override
    public TreeSet<String> getGroupKeys() throws IOException {
        return getKeysFor(ZarrConstants.FILENAME_DOT_ZGROUP);
    }

    private TreeSet<String> getKeysFor(String suffix) throws IOException {
        return  Files.walk(root)
                .filter(path -> path.getFileName().toString().endsWith(suffix))
                .map(path -> root.relativize(path.getParent()).toString())
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
