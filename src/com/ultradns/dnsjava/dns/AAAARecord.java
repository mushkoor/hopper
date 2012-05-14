// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package com.ultradns.dnsjava.dns;

import java.io.*;
import java.net.*;

/**
 * IPv6 Address Record - maps a domain name to an IPv6 address
 *
 * @author Brian Wellington
 */

public class AAAARecord extends Record {

	private static final long serialVersionUID = -7749806497566704077L;

	/**
     * Has to be an IPV6 Address, see http://www.ietf.org/rfc/rfc3596.txt
     */
    private Inet6Address address;

    AAAARecord() {}

    Record getObject() {
        return new AAAARecord();
    }

    /**
     * Creates an AAAA Record from the given data
     * @param address The address suffix
     */
    public AAAARecord(Name name, int dclass, long ttl, InetAddress address) {
        super(name, Type.AAAA, dclass, ttl);
        if (Address.familyOf(address) != Address.IPv6) {
            throw new IllegalArgumentException("invalid IPv6 address");
        }
        if(!(address instanceof Inet6Address)) {
            throw new IllegalArgumentException("invalid IPv6 address");
        }
        this.address = (Inet6Address) address;
    }

    void rrFromWire(DNSInput in) throws IOException {
        address = Inet6Address.getByAddress(null, in.readByteArray(16), null);
	}

    void rdataFromString(Tokenizer st, Name origin) throws IOException {
    	InetAddress shouldBeIPV6 = st.getAddress(Address.IPv6);
        if(!(shouldBeIPV6 instanceof Inet6Address)) {
            throw new IllegalArgumentException("invalid IPv6 address");
        }
        address = (Inet6Address) shouldBeIPV6;
    }

    /** Converts rdata to a String */
    String rrToString() {
        return address.getHostAddress();
    }

    /** Returns the address */
    public InetAddress getAddress() {
        return address;
    }

    void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        out.writeByteArray(address.getAddress());
    }

}
