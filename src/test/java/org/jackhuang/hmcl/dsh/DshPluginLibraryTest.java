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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DshPluginLibraryTest {
    @Test
    void anArchiveSurvivesRemovalOfItsSource(@TempDir Path root) throws Exception {
        Path source = root.resolve("download.tgz");
        Files.write(source, packed("one"));
        Path library = root.resolve("library");

        Path saved = DshPluginLibrary.retain(source, library);
        Files.delete(source);

        assertTrue(Files.isRegularFile(saved));
        assertEquals("example-plugin", DshLocalPlugins.inspect(saved).name());
        assertEquals("1.0.0", DshLocalPlugins.inspect(saved).version());
        assertTrue(saved.getParent().equals(library));
    }

    @Test
    void importingTheSameBytesTwiceReusesOneArchive(@TempDir Path root) throws Exception {
        Path first = root.resolve("first.tgz");
        Path second = root.resolve("second.tgz");
        Files.write(first, packed("same"));
        Files.copy(first, second);
        Path library = root.resolve("library");

        assertEquals(DshPluginLibrary.retain(first, library), DshPluginLibrary.retain(second, library));
        try (var files = Files.list(library)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void DifferentArchivesWithTheSameNameAndVersionDoNotOverwrite(@TempDir Path root) throws Exception {
        Path first = root.resolve("first.tgz");
        Path second = root.resolve("second.tgz");
        Files.write(first, packed("first"));
        Files.write(second, packed("second"));
        Path library = root.resolve("library");

        Path retainedFirst = DshPluginLibrary.retain(first, library);
        Path retainedSecond = DshPluginLibrary.retain(second, library);

        assertNotEquals(retainedFirst, retainedSecond);
        assertEquals(-1, Files.mismatch(first, retainedFirst));
        assertEquals(-1, Files.mismatch(second, retainedSecond));
    }

    @Test
    void invalidPackagesNeverEnterTheLibrary(@TempDir Path root) throws Exception {
        Path source = root.resolve("invalid.tgz");
        Files.writeString(source, "this is not a tgz");
        Path library = root.resolve("library");

        assertThrows(DshException.class, () -> DshPluginLibrary.retain(source, library));
        try (var files = Files.list(library)) {
            assertEquals(0, files.count(), "temporary imports must be cleaned up");
        }
    }

    private static byte[] packed(String payload) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(entry("package/package.json",
                    "{\"name\":\"example-plugin\",\"version\":\"1.0.0\",\"dsh\":{\"bundle\":{\"patch\":\"patch.yml\"}}}"
                            .getBytes(StandardCharsets.UTF_8)));
            gzip.write(entry("package/content.txt", payload.getBytes(StandardCharsets.UTF_8)));
            gzip.write(new byte[1024]);
        }
        return out.toByteArray();
    }

    private static byte[] entry(String name, byte[] body) {
        byte[] header = new byte[512];
        byte[] fileName = name.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(fileName, 0, header, 0, Math.min(fileName.length, 100));
        byte[] octalSize = Long.toOctalString(body.length).getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(octalSize, 0, header, 124, octalSize.length);
        header[156] = '0';
        byte[] result = new byte[512 + body.length + (512 - body.length % 512) % 512];
        System.arraycopy(header, 0, result, 0, 512);
        System.arraycopy(body, 0, result, 512, body.length);
        return result;
    }
}
