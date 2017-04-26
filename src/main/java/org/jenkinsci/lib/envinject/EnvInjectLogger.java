package org.jenkinsci.lib.envinject;

import hudson.model.TaskListener;

import java.io.Serializable;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @author Gregory Boissinot
 * @deprecated The actual version of this API class is located in EnvInject API Plugin
 */
@Deprecated
@Restricted(NoExternalUse.class)
public class EnvInjectLogger implements Serializable {

    private TaskListener listener;

    public EnvInjectLogger(TaskListener listener) {
        this.listener = listener;
    }

    public TaskListener getListener() {
        return listener;
    }

    public void info(String message) {
        listener.getLogger().println("[EnvInject] - " + message);
    }

    public void error(String message) {
        listener.getLogger().println("[EnvInject] - [ERROR] - " + message);
    }
}

