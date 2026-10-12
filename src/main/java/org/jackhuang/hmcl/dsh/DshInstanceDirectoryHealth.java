/*
 * HDSL
 * Copyright (C) 2026 HDSL contributors
 */
package org.jackhuang.hmcl.dsh;

/**
 * Result of checking an instance filesystem layout.
 */
public enum DshInstanceDirectoryHealth {
    OK,
    MISSING_ROOT,
    INCOMPLETE
}
