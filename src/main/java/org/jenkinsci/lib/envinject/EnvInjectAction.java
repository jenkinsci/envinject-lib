package org.jenkinsci.lib.envinject;

import com.google.common.collect.Maps;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import org.apache.commons.collections.map.UnmodifiableMap;
import org.jenkinsci.lib.envinject.service.EnvInjectSavable;
import org.kohsuke.stapler.StaplerProxy;

import java.io.File;
import java.io.ObjectStreamException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectAction implements Action, StaplerProxy {

    public static final String URL_NAME = "injectedEnvVars";

    protected transient Map<String, String> envMap;

    private AbstractBuild build;

    /**
     * Backward compatibility
     */
    private transient Map<String, String> resultVariables;
    private transient File rootDir;
    private transient Set<String> sensibleVariables;

    public EnvInjectAction(AbstractBuild build, Map<String, String> envMap) {
        this.build = build;
        this.envMap = envMap;
    }

    public void overrideAll(Map<String, String> all) {
        overrideAll(Collections.<String>emptySet(), all);
    }

    public void overrideAll(final Set<String> sensibleVariables, Map<String, String> all) {
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
                                return sensibleVariables.contains(key) ? "********" : value;
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


    private Map<String, String> getEnvironment(AbstractBuild build) throws EnvInjectException {

        if (build == null) {
            return null;
        }

        AbstractProject project = build.getProject();
        if (project == null) {
            return null;
        }

        EnvInjectSavable dao = new EnvInjectSavable();
        return dao.getEnvironment(build.getRootDir());
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

    public Set<String> getSensibleVariables() {
        return sensibleVariables;
    }
}
