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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DshVendorTest {
    @Test
    void offeredProvidersTakePrecedenceOverTheBundledCatalogue() {
        // A few catalogue entries use unsupported protocols (or have no protocol at all).
        // When the launcher offers a compatible route, that route must win discovery.
        for (DshVendor offered : DshVendor.offered()) {
            if (offered.hasBaseUrl()) {
                assertSame(offered, DshVendor.byBaseUrl(offered.baseUrl()), offered.id());
            }
        }
        assertEquals("openai-completions",
                DshVendor.byBaseUrl("https://openrouter.ai/api/v1").api());
        assertEquals("anthropic-messages",
                DshVendor.byBaseUrl("https://api.z.ai/api/coding/paas/v4").api());
    }

    @Test
    void theCatalogueStillResolvesProvidersNotOfferedInTheUi() {
        DshVendor supplier = DshVendor.byBaseUrl("https://api.cerebras.ai/v1");
        assertEquals("cerebras", supplier.id());
        assertEquals("openai-completions", supplier.api());
    }

    @Test
    void unknownAndMalformedHostsAreNotMatched() {
        assertNull(DshVendor.byBaseUrl(null));
        assertNull(DshVendor.byBaseUrl("not a url"));
        assertNull(DshVendor.byBaseUrl("https://unknown.example/v1"));
    }
}
