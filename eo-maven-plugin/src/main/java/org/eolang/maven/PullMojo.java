/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2023 Objectionary.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.eolang.maven;

import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eolang.maven.hash.ChCached;
import org.eolang.maven.hash.ChCompound;
import org.eolang.maven.hash.ChNarrow;
import org.eolang.maven.hash.CommitHash;
import org.eolang.maven.objectionary.Objectionary;
import org.eolang.maven.objectionary.OyCaching;
import org.eolang.maven.objectionary.OyFallbackSwap;
import org.eolang.maven.objectionary.OyHome;
import org.eolang.maven.objectionary.OyIndexed;
import org.eolang.maven.objectionary.OyRemote;
import org.eolang.maven.tojos.ForeignTojo;
import org.eolang.maven.util.Home;
import org.eolang.maven.util.Rel;

/**
 * Pull EO files from Objectionary.
 *
 * @since 0.1
 */
@Mojo(
    name = "pull",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    threadSafe = true
)
public final class PullMojo extends SafeMojo {
    /**
     * The directory where to process to.
     */
    public static final String DIR = "3-pull";

    /**
     * The Git hash to pull objects from, in objectionary.
     *
     * @since 0.21.0
     * @checkstyle VisibilityModifierCheck (6 lines)
     */
    @SuppressWarnings("PMD.ImmutableField")
    @Parameter(property = "eo.tag", required = true, defaultValue = "master")
    private String tag = "master";

    /**
     * Read hashes from local file.
     *
     * @checkstyle MemberNameCheck (7 lines)
     * @checkstyle VisibilityModifierCheck (6 lines)
     */
    @Parameter(property = "offlineHashFile")
    private Path offlineHashFile;

    /**
     * Return hash by pattern.
     * -DofflineHash=0.*.*:abc2sd3
     * -DofflineHash=0.2.7:abc2sd3,0.2.8:s4se2fe
     *
     * @checkstyle MemberNameCheck (7 lines)
     * @checkstyle VisibilityModifierCheck (6 lines)
     */
    @Parameter(property = "offlineHash")
    private String offlineHash;

    /**
     * The objectionary.
     *
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    @SuppressWarnings("PMD.ImmutableField")
    private Objectionary objectionary;

    /**
     * Hash-Objectionary map.
     * @todo #1602:30min Use objectionaries to pull objects with different
     *  versions. Objects with different versions are stored in different
     *  storages (objectionaries). Every objectionary has its own hash.
     *  To pull versioned object from objectionary firstly we need to get
     *  right objectionary by object's version and then get object from that
     *  objectionary by name.
     * @checkstyle MemberNameCheck (5 lines)
     */
    private final Map<String, Objectionary> objectionaries = new HashMap<>();

    /**
     * Pull again even if the .eo file is already present?
     *
     * @checkstyle MemberNameCheck (7 lines)
     * @since 0.10.0
     */
    @Parameter(property = "eo.overWrite", required = true, defaultValue = "false")
    private boolean overWrite;

    @Override
    public void exec() throws IOException {
        final CommitHash hash = new ChCached(
            new ChCompound(
                this.offlineHashFile, this.offlineHash, this.tag
            )
        );
        if (this.objectionary == null) {
            this.objectionary = this.objectionaryBy(hash);
        }
        final Collection<ForeignTojo> tojos = this.scopedTojos().withoutSources();
        for (final ForeignTojo tojo : tojos) {
            tojo.withSource(this.pull(tojo.identifier()).toAbsolutePath())
                .withHash(new ChNarrow(hash));
        }
        Logger.info(
            this, "%d program(s) pulled from %s",
            tojos.size(), this.objectionary
        );
    }

    /**
     * Get objectionary from the map by given hash.
     * @param hash Hash.
     * @return Objectionary by given hash.
     */
    private Objectionary objectionaryBy(final CommitHash hash) {
        final String value = hash.value();
        final CommitHash narrow = new ChCached(new ChNarrow(hash));
        if (!this.objectionaries.containsKey(value)) {
            this.objectionaries.put(
                value,
                new OyFallbackSwap(
                    new OyHome(
                        narrow,
                        this.cache
                    ),
                    new OyCaching(
                        narrow,
                        this.cache,
                        new OyIndexed(
                            new OyRemote(hash)
                        )
                    ),
                    this.session.getRequest().isUpdateSnapshots()
                )
            );
        }
        return this.objectionaries.get(value);
    }

    /**
     * Pull one object.
     *
     * @param name Name of the object, e.g. "org.eolang.io.stdout"
     * @return The path of .eo file
     * @throws IOException If fails
     */
    private Path pull(final String name) throws IOException {
        final Path dir = this.targetDir.toPath().resolve(PullMojo.DIR);
        final Path src = new Place(name).make(
            dir, "eo"
        );
        if (src.toFile().exists() && !this.overWrite) {
            Logger.debug(
                this, "The object '%s' already pulled to %s (and 'overWrite' is false)",
                name, new Rel(src)
            );
        } else {
            new Home(dir).save(
                this.objectionary.get(name),
                dir.relativize(src)
            );
            Logger.debug(
                this, "The sources of the object '%s' pulled to %s",
                name, new Rel(src)
            );
        }
        return src;
    }
}
