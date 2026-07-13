package net.pcal.fwportals.common.attunement;

import net.minecraft.core.HolderLookup;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class AttunementRegistry {

    private final Logger logger;
    private final AtomicReference<AttunementLookup> publishedLookup = new AtomicReference<>(AttunementLookup.empty());

    public AttunementRegistry(Logger logger) {
        this.logger = Objects.requireNonNull(logger);
    }

    public AttunementLookup currentLookup() {
        return publishedLookup.get();
    }

    public net.minecraft.server.packs.resources.PreparableReloadListener createReloadListener(HolderLookup.Provider registryLookup) {
        return new ReloadListener(registryLookup);
    }

    private final class ReloadListener extends SimplePreparableReloadListener<AttunementLookup> {

        private final HolderLookup.Provider registryLookup;

        private ReloadListener(HolderLookup.Provider registryLookup) {
            this.registryLookup = registryLookup;
        }

        @Override
        protected AttunementLookup prepare(ResourceManager manager, ProfilerFiller profiler) {
            return AttunementLoader.load(manager, registryLookup);
        }

        @Override
        protected void apply(AttunementLookup preparations, ResourceManager manager, ProfilerFiller profiler) {
            publishedLookup.set(preparations);
            logger.info("[fwportals] Loaded {} attunement definitions", preparations.size());
        }
    }
}
