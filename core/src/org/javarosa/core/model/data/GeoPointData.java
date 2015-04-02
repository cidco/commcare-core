/*
 * Copyright (C) 2009 JavaRosa
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.core.model.data;

import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.math.BigDecimal;


/**
 * A response to a question requesting an GeoPoint Value.
 *
 * @author Yaw Anokwa
 */
public class GeoPointData implements IAnswerData {

    // latitude, longitude, and potentially altitude and accuracy data
    private double[] gp = new double[4];
    private int len = 2;

    // accuracy and altitude data points stored will contain this many decimal
    // points:
    private final int MAX_DECIMAL_ACCURACY = 1;


    /**
     * Empty Constructor, necessary for dynamic construction during
     * deserialization. Shouldn't be used otherwise.
     */
    public GeoPointData() {

    }


    public GeoPointData(double[] gp) {
        this.fillArray(gp);
    }


    /**
     * Copy data in argument array into local geopoint array.
     * @param gp double array of max size 4 representing geopoints
     */
    private void fillArray(double[] gp) {
        len = gp.length;
        for (int i = 0; i < len; i++) {
            if (i < 2) {
                // don't truncate lat & lng decimal values
                this.gp[i] = gp[i];
            } else {
                // accuracy & altitude should have their decimal values truncated
                this.gp[i] = formatDouble(gp[i], MAX_DECIMAL_ACCURACY);
            }
        }
    }


    public IAnswerData clone() {
        return new GeoPointData(gp);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.javarosa.core.model.data.IAnswerData#getDisplayText()
     */
    public String getDisplayText() {
        String s = "";
        for (int i = 0; i < len; i++) {
            s += gp[i] + " ";
        }
        return s.trim();

    }


    /*
     * (non-Javadoc)
     * 
     * @see org.javarosa.core.model.data.IAnswerData#getValue()
     */
    public double[] getValue() {
        return gp;
    }


    public void setValue(Object o) {
        if (o == null) {
            throw new NullPointerException("Attempt to set an IAnswerData class to null.");
        }
        this.fillArray((double[])o);
    }


    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException,
            DeserializationException {
        len = (int)ExtUtil.readNumeric(in);
        for (int i = 0; i < len; i++) {
            gp[i] = ExtUtil.readDecimal(in);
        }
    }


    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, len);
        for (int i = 0; i < len; i++) {
            ExtUtil.writeDecimal(out, gp[i]);
        }
    }


    public UncastData uncast() {
        return new UncastData(getDisplayText());
    }

    public GeoPointData cast(UncastData data) throws IllegalArgumentException {
        double[] ret = new double[4];

        Vector<String> choices = DateUtils.split(data.value, " ", true);
        int i = 0;
        for (String s : choices) {
            double d = Double.parseDouble(s);
            ret[i] = d;
            ++i;
        }
        return new GeoPointData(ret);
    }

    /**
     * Truncate double to have the given number of decimals.
     *
     * @param x double to be truncated
     * @param numberofDecimals number of decimals that should present in result
     */
    private static double truncateDecimal(double x, int numberofDecimals) {
        // via:
        // https://stackoverflow.com/questions/7747469/how-can-i-truncate-a-double-to-only-two-decimal-places-in-java/21468258#21468258
        BigDecimal decimal;
        if (x > 0) {
            decimal = new BigDecimal(String.valueOf(x)).setScale(numberofDecimals, BigDecimal.ROUND_FLOOR);
        } else {
            decimal = new BigDecimal(String.valueOf(x)).setScale(numberofDecimals, BigDecimal.ROUND_CEILING);
        }
        return decimal.doubleValue();
    }

    /**
     * Jenky J2ME-compatible decimal truncate for doubles.
     *
     * @param x double to be truncated
     * @param numberOfDecimals number of decimals that should present in result
     */
    private static double formatDouble(double x, int numberOfDecimals) {
        int index;
        String doubleStr = "" + x;

        // find the period (or comma)
        if (doubleStr.indexOf(".") != -1) {
            index = doubleStr.indexOf(".");
        } else {
            index = doubleStr.indexOf(",");
        }

        // Number doesn't have a decimal point, just return it.
        if (index == -1) {
            return x;
        }

        // We want to truncate all decimals
        if (numberOfDecimals == 0) {
            return Double.parseDouble(doubleStr.substring(0, index));
        }

        int len = index + numberOfDecimals + 1;
        if (len >= doubleStr.length()) {
            len = doubleStr.length();
        }

        return Double.parseDouble(doubleStr.substring(0, len));
    }
}
