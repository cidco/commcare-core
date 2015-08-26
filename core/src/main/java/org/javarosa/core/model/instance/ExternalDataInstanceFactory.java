package org.javarosa.core.model.instance;

import org.javarosa.core.services.Logger;

import java.util.Hashtable;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class ExternalDataInstanceFactory {
    private static final Hashtable<String, DataInstanceBuilder> instanceIdToBuilder = new Hashtable<String, DataInstanceBuilder>();
    private static final Object lock = new Object();
    private static final ExternalDataInstance dummyDefaultInstance = new ExternalDataInstance();

    public static ExternalDataInstance getDataInstance(String instanceId, String reference) {
        if (instanceIdToBuilder.containsKey(instanceId)) {
            DataInstanceBuilder builder = instanceIdToBuilder.get(instanceId);
            return builder.buildDataInstance(reference, instanceId);
        } else {
            return dummyDefaultInstance.buildDataInstance(reference, instanceId);
        }
    }

    public static void registerInstanceBuilder(String instanceId, DataInstanceBuilder builder) {
        synchronized (lock) {
            if (instanceIdToBuilder.contains(instanceId)){
                Logger.log("Warning", "registering an existing external data instance");
            }
            instanceIdToBuilder.put(instanceId, builder);
        }
    }
}
