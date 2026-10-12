/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
 */
package org.jackhuang.hmcl.dsh;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies that an instance owns a predictable filesystem boundary.
class DshInstancePathsTest {
    @Test
    void createsTheInstanceOwnedDirectories() {
        Path root = Path.of("instances", "research-agent");

        DshInstancePaths paths = DshInstancePaths.of(root);

        assertEquals(root.resolve("home"), paths.home());
        assertEquals(root.resolve("logs"), paths.logs());
        assertEquals(root.resolve("state"), paths.state());
    }
}
