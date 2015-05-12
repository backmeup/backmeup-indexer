package org.backmeup.index.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.backmeup.index.rest.provider.JacksonJsonConfiguration;
import org.backmeup.index.rest.resources.Collections;
import org.backmeup.index.rest.resources.Config;
import org.backmeup.index.rest.resources.Index;
import org.backmeup.index.rest.resources.IndexDocumentUpload;
import org.backmeup.index.rest.resources.Sharing;

public class BackmeupIndexApplication extends Application {
    private final Set<Class<?>> set = new HashSet<>();
    private final Set<Object> singletons = new HashSet<>();

    public BackmeupIndexApplication() {
        // The default life-cycle for resource class instances is per-request. 
        this.set.add(Index.class);
        this.set.add(Config.class);
        this.set.add(Sharing.class);
        this.set.add(IndexDocumentUpload.class);
        this.set.add(Collections.class);

        // The default life-cycle for providers (registered directly or via a feature) is singleton.
        this.set.add(JacksonJsonConfiguration.class); // provider
    }

    @Override
    public Set<Class<?>> getClasses() {
        return this.set;
    }

    @Override
    public Set<Object> getSingletons() {
        return this.singletons;
    }
}
