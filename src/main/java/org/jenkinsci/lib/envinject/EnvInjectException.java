package org.jenkinsci.lib.envinject;

import javax.annotation.Nonnull;

/**
 * Exception type for the EnvInject logic.
 * 
 * @author Gregory Boissinot
 */
public class EnvInjectException extends Exception {

    public EnvInjectException(@Nonnull String s) {
        super(s);
    }

    public EnvInjectException(@Nonnull Throwable throwable) {
        super(throwable);
    }

    public EnvInjectException(@Nonnull String s, @Nonnull Throwable throwable) {
        super(s, throwable);
    }
}
