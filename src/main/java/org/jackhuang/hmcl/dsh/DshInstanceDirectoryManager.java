/*
 * HDSL
 * Copyright (C) 2026 HDSL contributors
 */
package org.jackhuang.hmcl.dsh;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages the filesystem lifecycle of a DSH instance.
 *
 * <p>This class only owns instance directories. It does not manage runtime
 * downloads, plugins, accounts, or launching.</p>
 */
@NotNullByDefault
public final class DshInstanceDirectoryManager {
    private DshInstanceDirectoryManager() {
    }

    /**
     * Creates the directory layout owned by an instance.
     *
     * @param instance target instance
     * @throws DshException when the layout cannot be created
     */
    public static void initialize(DshInstance instance) throws DshException {
        try {
            DshInstancePaths paths = instance.paths();
            Files.createDirectories(paths.root());
            Files.createDirectories(paths.logs());
            Files.createDirectories(paths.state());
            Files.createDirectories(paths.home());
        } catch (IOException e) {
            throw new DshException("Failed to initialize instance directory for " + instance.id(), e);
        }
    }

    /**
     * Checks whether the basic instance layout exists.
     *
     * @param instance target instance
     * @return health state
     */
    public static DshInstanceDirectoryHealth check(DshInstance instance) {
        DshInstancePaths paths = instance.paths();
        if (!Files.isDirectory(paths.root())) {
            return DshInstanceDirectoryHealth.MISSING_ROOT;
        }
        if (!Files.isDirectory(paths.logs()) || !Files.isDirectory(paths.state())) {
            return DshInstanceDirectoryHealth.INCOMPLETE;
        }
        return DshInstanceDirectoryHealth.OK;
    }

    /**
     * Safely removes the instance-owned directory.
     *
     * @param instance target instance
     * @throws DshException when removal fails
     */
    public static void delete(DshInstance instance) throws DshException {
        Path root = instance.paths().root();
        if (!root.startsWith(DshPaths.INSTANCES)) {
            throw new DshException("Refusing to delete directory outside instance storage: " + root);
        }
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new DshException("Failed to delete instance directory " + root, e);
        }
    }
}
