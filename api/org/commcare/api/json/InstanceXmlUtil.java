package org.commcare.api.json;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import org.commcare.api.xml.XmlUtil;
import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.ledger.instance.LedgerInstanceTreeElement;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.model.xform.DataModelSerializer;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by willpride on 11/24/15.
 */
public class InstanceXmlUtil {

    public static String getFormInstanceXml(FormInstance instance){
        XFormSerializingVisitor visitor = new XFormSerializingVisitor();
        try {
            byte[] data = visitor.serializeInstance(instance);
            return getPrettyXml(data);
        } catch (IOException e) {
            e.printStackTrace();
            return("Error Serializing XForm Data! " + e.getMessage());
        }
    }

    public static String getCaseXml(InstanceInitializationFactory iif){
        byte[] bytes = serializeCaseInstanceFromSandbox(iif);
        return getPrettyXml(bytes);
    }

    public static String getLedgerXml(InstanceInitializationFactory iif){
        byte[] bytes = serializeLedgerInstanceFromSandbox(iif);
        return getPrettyXml(bytes);
    }

    public static String getSessionXml(InstanceInitializationFactory iif){
        byte[] bytes = serializeSessionInstanceFromSandbox(iif);
        return getPrettyXml(bytes);
    }

    public static String getFixtureXml(InstanceInitializationFactory iif, String name){
        byte[] bytes = serializeFixtureInstanceFromSandbox(iif, name);
        return getPrettyXml(bytes);
    }

    public static String getInstanceXml(InstanceInitializationFactory iif, String name, String root){
        byte[] bytes = serializeInstanceFromSandbox(iif, name, root);
        return getPrettyXml(bytes);
    }

    public static byte[] serializeInstance(InstanceInitializationFactory iif, String jrPath, String modelName){
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataModelSerializer s = new DataModelSerializer(bos, iif);
            s.serialize(new ExternalDataInstance(jrPath, modelName), null);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static byte[] serializeInstanceFromSandbox(InstanceInitializationFactory iif, String path, String root) {
        System.out.println("serializing instance: " + path + " root " + root);
        return serializeInstance(iif, path, root);
    }

    private static byte[] serializeSessionInstanceFromSandbox(InstanceInitializationFactory iif) {
        return serializeInstance(iif, "jr://instance/session", "session");
    }

    private static byte[] serializeLedgerInstanceFromSandbox(InstanceInitializationFactory iif) {
        return serializeInstance(iif, "jr://instance/ledgerdb", LedgerInstanceTreeElement.MODEL_NAME);
    }

    public static byte[] serializeCaseInstanceFromSandbox(InstanceInitializationFactory iif) {
        return serializeInstance(iif, "jr://instance/casedb", CaseInstanceTreeElement.MODEL_NAME);
    }


    public static byte[] serializeFixtureInstanceFromSandbox(InstanceInitializationFactory iif, String name) {
        return serializeInstance(iif, "jr://instance/fixture/" + name, null);
    }

    //fromXML util
    public static String getPrettyXml(byte[] xml) {
        try {
            String unformattedXml = new String(xml);
            final Document document = parseXmlFile(unformattedXml);

            OutputFormat format = new OutputFormat(document);
            format.setLineWidth(65);
            format.setIndenting(true);
            format.setIndent(2);
            Writer out = new StringWriter();
            XMLSerializer serializer = new XMLSerializer(out, format);
            serializer.serialize(document);

            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Document parseXmlFile(String in) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(in));
            return db.parse(is);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getSubmissionXML(FormDef form){
        return getFormInstanceXml(form.getInstance());
    }
}
