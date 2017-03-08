package org.commcare.core.graph.model;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Contains all of the fully-evaluated data to draw a graph: a type, set of series, set of text annotations, and key-value map of configuration.
 *
 * @author jschweers
 */
public class GraphData implements ConfigurableData {
    private String mType;
    private final Vector<SeriesData> mSeries;
    private final Hashtable<String, String> mConfiguration;
    private final Vector<org.commcare.core.graph.model.AnnotationData> mAnnotations;

    public GraphData() {
        mSeries = new Vector<>();
        mConfiguration = new Hashtable<>();
        mAnnotations = new Vector<>();
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }

    public Vector<SeriesData> getSeries() {
        return mSeries;
    }

    public void addSeries(SeriesData s) {
        mSeries.addElement(s);
    }

    public void addAnnotation(org.commcare.core.graph.model.AnnotationData a) {
        mAnnotations.addElement(a);
    }

    public Vector<org.commcare.core.graph.model.AnnotationData> getAnnotations() {
        return mAnnotations;
    }

    @Override
    public void setConfiguration(String key, String value) {
        mConfiguration.put(key, value);
    }

    @Override
    public String getConfiguration(String key) {
        return mConfiguration.get(key);
    }

    @Override
    public String getConfiguration(String key, String defaultValue) {
        String value = getConfiguration(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

}
