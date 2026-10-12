/*
 * HDSL
 * Copyright (C) 2026 HDSL contributors
 */
package org.jackhuang.hmcl.dsh;

import java.nio.file.Path;

/**
 * Resolved filesystem locations owned by a DSH instance.
 *
 * <p>This class intentionally only describes instance boundaries. It does not
 * create directories, download runtimes, or manage plugins. Those responsibilities
 * stay in their respective managers.</p>
 *
 * @param root instance root directory
 * @param home DSH_HOME used by this instance
 * @param logs instance log directory
 * @param state instance runtime state directory
 */
public record DshInstancePaths(
        Path root,
        Path home,
        Path logs,
        Path state) {

    public DshInstancePaths {
        root = root.toAbsolutePath().normalize();
        home = home.toAbsolutePath().normalize();
        logs = logs.toAbsolutePath().normalize();
        state = state.toAbsolutePath().normalize();
    }

    /**
     * Resolves the standard layout under an instance directory.
     */
    public static DshInstancePaths of(Path root) {
        Path normalized = root.toAbsolutePath().normalize();
        return new DshInstancePaths(
                normalized,
                normalized.resolve("home"),
                normalized.resolve("logs"),
                normalized.resolve("state"));
    }
}
