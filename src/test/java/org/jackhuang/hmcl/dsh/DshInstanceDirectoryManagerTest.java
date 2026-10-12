/*
 * HDSL
 * Copyright (C) 2026 HDSL contributors
 */
package org.jackhuang.hmcl.dsh;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DshInstanceDirectoryManagerTest {
    @Test
    void initializeCreatesInstanceOwnedDirectories() throws Exception {
        var root = Files.createTempDirectory("hdsl-instance-test");
        var instanceRoot = root.resolve("instance");
        var instance = new DshInstance(
                "test-instance",
                "test-version",
                "web",
                root.resolve("workspace").toString(),
                null,
                DshHomeMode.ISOLATED,
                null,
                java.util.List.of(),
                java.util.Map.of(),
                null,
                null,
                null,
                0,
                System.currentTimeMillis()
        );

        DshInstanceDirectoryManager.initialize(instance);

        assertEquals(DshInstanceDirectoryHealth.OK,
                DshInstanceDirectoryManager.check(instance));
        assertTrue(Files.isDirectory(instance.paths().logs()));
        assertTrue(Files.isDirectory(instance.paths().state()));
        assertTrue(Files.isDirectory(instance.paths().home()));

        // Cleanup is intentionally local to the temporary test directory.
        assertTrue(Files.exists(instanceRoot) || Files.exists(instance.paths().root()));
    }
}
