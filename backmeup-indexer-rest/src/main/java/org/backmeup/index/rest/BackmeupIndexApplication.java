package org.backmeup.index.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.backmeup.index.rest.provider.JacksonJsonConfiguration;
import org.backmeup.index.rest.resources.Index;

public class BackmeupIndexApplication extends Application {
    private final Set<Class<?>> set = new HashSet<>();
    private final Set<Object> singletons = new HashSet<>();

    public BackmeupIndexApplication() {
        // The default life-cycle for resource class instances is per-request. 
        set.add(Index.class);

        // The default life-cycle for providers (registered directly or via a feature) is singleton.
        set.add(JacksonJsonConfiguration.class); // provider
    }

    @Override
    public Set<Class<?>> getClasses() {
        return set;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
