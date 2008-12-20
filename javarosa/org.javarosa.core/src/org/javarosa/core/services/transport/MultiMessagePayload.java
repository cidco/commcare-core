/**
 * 
 */
package org.javarosa.core.services.transport;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

import org.javarosa.core.api.Constants;
import org.javarosa.core.util.MultiInputStream;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author Clayton Sims
 * @date Dec 18, 2008 
 *
 */
public class MultiMessagePayload implements IDataPayload {
	/** IDataPayload **/
	Vector payloads;
	
	/**
	 * Note: Only useful for serialization.
	 */
	public MultiMessagePayload() {
		//ONLY FOR SERIALIZATION
	}
	
	/**
	 * Adds a payload that should be sent as part of this
	 * payload.
	 * @param payload A payload that will be transmitted
	 * after all previously added payloads.
	 */
	public void addPayload(IDataPayload payload) {
		payloads.addElement(payload);
	}
	
	/**
	 *  @return A vector object containing each IDataPayload in this payload.
	 */
	public Vector getPayloads() {
		return payloads;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.services.transport.IDataPayload#getPayloadStream()
	 */
	public InputStream getPayloadStream() {
		MultiInputStream bigStream = new MultiInputStream();
		Enumeration en = payloads.elements();
		while(en.hasMoreElements()) {
			IDataPayload payload = (IDataPayload)en.nextElement();
			bigStream.addStream(payload.getPayloadStream());
		}
		bigStream.prepare();
		return bigStream;
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		payloads = (Vector)ExtUtil.read(in, new ExtWrapList(new ExtWrapTagged()), pf);
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.write(out, new ExtWrapList(payloads));
	}
	
	public Object accept(IDataPayloadVisitor visitor) {
		return visitor.visit(this);
	}

	public String getPayloadId() {
		return null;
	}

	public int getPayloadType() {
		return Constants.PAYLOAD_TYPE_MULTI;
	}
}
