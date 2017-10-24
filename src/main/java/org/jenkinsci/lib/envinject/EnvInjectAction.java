package org.jenkinsci.lib.envinject;

import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.AbstractBuild;
import hudson.model.Job;
import hudson.model.Run;
import org.apache.commons.collections.map.UnmodifiableMap;
import org.jenkinsci.lib.envinject.service.EnvInjectSavable;
import org.kohsuke.stapler.StaplerProxy;

import java.io.File;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.RunAction2;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectAction implements RunAction2, StaplerProxy {

    public static final String URL_NAME = "injectedEnvVars";

    /**
     * Local cache of environment variables.
     * This cache may be null if the loading has never been performed.
     * Use {@link #getEnvMap()} in external API
     */
    @Restricted(NoExternalUse.class)
    protected transient @CheckForNull Map<String, String> envMap;
 
    private transient @CheckForNull Run<?, ?> build;

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

    /**
     * Constructs action for the specified environment variables.
     * @param build Build
     * @param envMap Environment Map 
     * @deprecated The action implements {@link RunAction2} now, hence passing build is not required anymore.
     *             Use {@link #EnvInjectAction(java.util.Map)}.
     */
    @Deprecated
    public EnvInjectAction(@Nonnull AbstractBuild build, 
            @CheckForNull Map<String, String> envMap) {
        this.build = build;
        this.envMap = envMap;
    }
    
    /**
     * Constructs action for the specified environment variables.
     * @param envMap Environment Map 
     * @since 0.25
     */
    public EnvInjectAction(@CheckForNull Map<String, String> envMap) {
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
    @CheckForNull
    public Map<String, String> getEnvMap() {
        if (envMap == null) {
            //Try to fill the envMap from the build injected environment
            //file (injectedEnvVars.txt by default).
            try {
                Map<String, String> result = getEnvironment(build);
                if (build != null) {
                    // Cache the result so we don't keep loading the environment from disk.
                    envMap = result == null ? new HashMap<String, String>(0) : result;
                }
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

            Map<String, String> toWrite = getEnvMap();
            if (toWrite == null) {
                toWrite = Collections.<String, String>emptyMap();
            }

            if (build == null && rootDir == null) {
                throw new InvalidObjectException("Cannot save the environment file. Action " + this + " has no associated run instance. Target root dir is unknown");
            }

            if (rootDir == null) { // New logic
                dao.saveEnvironment(build.getRootDir(), Maps.transformEntries(toWrite,
                        new Maps.EntryTransformer<String, String, String>() {
                            public String transformEntry(String key, String value) {
                                return (sensibleVariables != null && sensibleVariables.contains(key)) 
                                        ? "********" : value;
                            }
                        }));
            } else { // Fall-back to the legacy logic
                dao.saveEnvironment(rootDir, toWrite);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return this;
    }

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
            justification = "JENKINS-47574 - parent may be null during readResolve()")
    private static boolean runHasNoParent(@Nonnull Run<?,?> run) {
        return run.getParent() == null;
    }

    @CheckForNull
    private Map<String, String> getEnvironment(@CheckForNull Run<?, ?> build) throws EnvInjectException {

        if (build == null) {
            return null;
        }

        // It happens in the case of usage of this logic for a not-fully loaded build
        if (runHasNoParent(build)) {
            return null;
        }

        EnvInjectSavable dao = new EnvInjectSavable();
        return dao.getEnvironment(build.getRootDir());
    }

    /**
     * Retrieves an owner {@link Run} of this action.
     * @return {@link Run}, which contains the action.
     *         May be {@code null} if and only if the action is not attached to the run.
     * @since TODO
     */
    @CheckForNull
    public Run<?,?> getOwner() {
        return build;
    }
    
    @SuppressWarnings("unused")
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
            justification = "Data migration")
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
