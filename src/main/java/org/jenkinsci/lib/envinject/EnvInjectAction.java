package org.jenkinsci.lib.envinject;

import com.google.common.collect.Maps;
import hudson.model.Job;
import hudson.model.Run;
import org.apache.commons.collections.map.UnmodifiableMap;
import org.jenkinsci.lib.envinject.service.EnvInjectSavable;
import org.kohsuke.stapler.StaplerProxy;

import java.io.File;
import java.io.ObjectStreamException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.RunAction2;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectAction implements RunAction2, StaplerProxy {

    public static final String URL_NAME = "injectedEnvVars";

    protected transient @CheckForNull Map<String, String> envMap;
 
    private transient @Nonnull Run<?, ?> build;

    @Override
    public void onAttached(Run<?, ?> run) {
        build = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        build = run;
    }
 
    /**
     * Backward compatibility
     */
    private transient Map<String, String> resultVariables;
    private transient File rootDir;
    private transient @CheckForNull Set<String> sensibleVariables;

    public EnvInjectAction(@Nonnull Run<?,?> build, 
            @CheckForNull Map<String, String> envMap) {
        this.build = build;
        this.envMap = envMap;
    }

    public void overrideAll(Map<String, String> all) {
        overrideAll(Collections.<String>emptySet(), all);
    }

    public void overrideAll(
            final @CheckForNull Set<String> sensibleVariables, 
            @CheckForNull Map<String, String> all) {
        if (envMap == null) {
            return;
        }

        if (all == null) {
            return;
        }

        this.sensibleVariables = sensibleVariables;
        envMap.putAll(all);
    }

    @SuppressWarnings({"unused", "unchecked"})
    public Map<String, String> getEnvMap() {
        if (envMap == null) {
            //Try to fill the envMap from the build injected environment
            //file (injectedEnvVars.txt by default).
            try {
                Map<String, String> result = getEnvironment(build);
                return result == null ? null : UnmodifiableMap.decorate(result);
            } catch (EnvInjectException e) {
                return null;
            }
        }

        return UnmodifiableMap.decorate(envMap);
    }

    public String getIconFileName() {
        return "document-properties.gif";
    }

    public String getDisplayName() {
        return "Environment Variables";
    }

    public String getUrlName() {
        return URL_NAME;
    }

    @SuppressWarnings("unused")
    private Object writeReplace() throws ObjectStreamException {
        try {
            EnvInjectSavable dao = new EnvInjectSavable();

            if (rootDir == null) {
                dao.saveEnvironment(build.getRootDir(), Maps.transformEntries(envMap,
                        new Maps.EntryTransformer<String, String, String>() {
                            public String transformEntry(String key, String value) {
                                return (sensibleVariables != null && sensibleVariables.contains(key)) 
                                        ? "********" : value;
                            }
                        }));
                return this;
            }

            dao.saveEnvironment(rootDir, envMap);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return this;
    }


    private Map<String, String> getEnvironment(@CheckForNull Run<?, ?> build) throws EnvInjectException {

        if (build == null) {
            return null;
        }

        Job<?, ?> project = build.getParent();
        if (project == null) {
            return null;
        }

        EnvInjectSavable dao = new EnvInjectSavable();
        return dao.getEnvironment(build.getRootDir());
    }

    /**
     * Retrieves an owner {@link Run} of this action.
     * @return {@link Run}, which contains the action
     * @since TODO
     */
    public Run<?,?> getOwner() {
        return build;
    }
    
    @SuppressWarnings("unused")
    private Object readResolve() throws ObjectStreamException {

        if (resultVariables != null) {
            envMap = resultVariables;
            return this;
        }

        Map<String, String> resultMap = null;
        try {
            if (build != null) {
                resultMap = getEnvironment(build);
            } else if (rootDir != null) {
                EnvInjectSavable dao = new EnvInjectSavable();
                resultMap = dao.getEnvironment(rootDir);
            }

            if (resultMap != null) {
                envMap = resultMap;
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }

        return this;
    }


    public Object getTarget() {
        throw new UnsupportedOperationException();
    }

    public @CheckForNull Set<String> getSensibleVariables() {
        return sensibleVariables;
    }
}
