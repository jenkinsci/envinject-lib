package org.jenkinsci.lib.envinject.service;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jenkinsci.lib.envinject.EnvInjectException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @author Gregory Boissinot
 * @deprecated The actual version of this API class is located in EnvInject API Plugin
 */
@Deprecated
@Restricted(NoExternalUse.class)
public class EnvInjectSavable {

    private static final String ENVINJECT_TXT_FILENAME = "injectedEnvVars.txt";
    private static final String TOKEN = "=";

    @SuppressFBWarnings(value = "DM_DEFAULT_ENCODING", justification = "Deprecated class")
    public Map<String, String> getEnvironment(File envInjectBaseDir) throws EnvInjectException {

        if (envInjectBaseDir == null) {
            throw new NullPointerException("A base directory of the envinject file must be set.");
        }

        File f = new File(envInjectBaseDir, ENVINJECT_TXT_FILENAME);
        if (!f.exists()) {
            return null;
        }

        try(FileReader fileReader = new FileReader(f)) {
            final Map<String, String> result = new HashMap<>();
            fromTxt(fileReader, result);
            return result;
        } catch (IOException fne) {
            throw new EnvInjectException(fne);
        }
    }

    @SuppressFBWarnings(value = "DM_DEFAULT_ENCODING", justification = "Deprecated class")
    public void saveEnvironment(@NonNull File rootDir,@NonNull Map<String, String> envMap) throws EnvInjectException {

        File f = new File(rootDir, ENVINJECT_TXT_FILENAME);
        try(FileWriter fileWriter = new FileWriter(f)) {
            Map<String, String> map2Write = new TreeMap<>();
            map2Write.putAll(envMap);
            toTxt(map2Write, fileWriter);
        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        }
    }

    private void fromTxt(FileReader fileReader, Map<String, String> result) throws EnvInjectException {
        String line;
        try(BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            while ((line = bufferedReader.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line, TOKEN);
                int tokens = tokenizer.countTokens();
                if (tokens == 2) {
                    result.put(String.valueOf(tokenizer.nextElement()), String.valueOf(tokenizer.nextElement()));
                }
            }
        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        }
    }

    private void toTxt(Map<String, String> envMap, FileWriter fw) throws IOException {
        for (Map.Entry<String, String> entry : envMap.entrySet()) {
            fw.write(entry.getKey());
            fw.write(TOKEN);
            fw.write(entry.getValue());
            fw.write(System.lineSeparator());
        }
    }

}
