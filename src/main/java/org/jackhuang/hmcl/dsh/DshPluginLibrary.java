/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
 *
 * This program is free software: you can redistribute it and/or modify
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
package org.jackhuang.hmcl.dsh;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/// A shared source archive library for locally installed plugins.
///
/// The library retains an immutable, content-addressed copy of each package. It does not
/// share node_modules, profile patches or live plugin state: DshLocalPlugins.install still
/// copies the selected archive into the destination instance before invoking DSH.
@NotNullByDefault
public final class DshPluginLibrary {
    private DshPluginLibrary() {
    }

    /// The directory exposed to the file chooser.
    public static Path directory() {
        return DshPaths.PLUGIN_LIBRARY;
    }

    /// Retains a packed plugin so another instance can install it later.
    ///
    /// The caller may delete or move its original after this returns. Validation and hashing
    /// are done against the same temporary copy, avoiding source-file races. Identical
    /// archives use the same path; differing contents cannot silently overwrite one another.
    public static Path retain(Path source) throws DshException {
        return retain(source, directory());
    }

    /// Allows tests to exercise the same behavior under a temporary directory.
    static Path retain(Path source, Path root) throws DshException {
        Path temporary = null;
        try {
            Files.createDirectories(root);
            temporary = Files.createTempFile(root, ".import-", ".tgz");
            Files.copy(source, temporary, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            DshLocalPlugins.Package pkg = DshLocalPlugins.inspect(temporary);
            String sha256 = digest(temporary);
            // A package controls both metadata strings. Bound the generated file name for
            // Windows and Linux path limits; the full hash still distinguishes archives.
            String name = DshLocalPlugins.safeName(pkg.name());
            String version = DshLocalPlugins.safeName(pkg.version());
            if (name.isBlank() || version.isBlank()) {
                throw new DshException("Plugin package has no usable name or version");
            }
            name = name.substring(0, Math.min(name.length(), 60));
            version = version.substring(0, Math.min(version.length(), 32));
            Path destination = root.resolve(name + "-" + version + "-" + sha256 + ".tgz");
            try {
                Files.move(temporary, destination);
                temporary = null;
            } catch (FileAlreadyExistsException e) {
                // A concurrent import of the very same content is fine. An existing symlink
                // or damaged file is not: never overwrite a path the library did not create.
                if (!Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)
                        || !sha256.equals(digest(destination))) {
                    throw new DshException("Shared plugin archive is damaged: " + destination, e);
                }
            }
            return destination;
        } catch (IOException e) {
            throw new DshException("Failed to save a plugin in the shared library: " + source, e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Keep the original error; never touch an existing library entry.
                }
            }
        }
    }

    private static String digest(Path archive) throws IOException {
        MessageDigest hasher;
        try {
            hasher = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is required by Java", e);
        }
        try (InputStream input = Files.newInputStream(archive)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                hasher.update(buffer, 0, count);
            }
        }
        return HexFormat.of().formatHex(hasher.digest());
    }
}
