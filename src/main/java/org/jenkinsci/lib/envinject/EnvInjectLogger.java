package org.jenkinsci.lib.envinject;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.TaskListener;

import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
@SuppressFBWarnings(value = "SE_NO_SERIALVERSIONID", justification = "Legacy code")
public class EnvInjectLogger implements Serializable {

    @NonNull
    private final TaskListener listener;

    public EnvInjectLogger(@NonNull TaskListener listener) {
        this.listener = listener;
    }

    @NonNull
    public TaskListener getListener() {
        return listener;
    }

    public void info(@NonNull String message) {
        listener.getLogger().println("[EnvInject] - " + message);
    }

    public void error(@NonNull String message) {
        listener.getLogger().println("[EnvInject] - [ERROR] - " + message);
    }
}

