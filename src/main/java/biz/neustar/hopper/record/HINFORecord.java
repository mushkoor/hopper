// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import java.io.IOException;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.Compression;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.util.Tokenizer;

/**
 * Host Information - describes the CPU and OS of a host
 * 
 * @author Brian Wellington
 */

public class HINFORecord extends Record {

    private static final long serialVersionUID = -4732870630947452112L;

    private byte[] cpu, os;

    public HINFORecord() {
    }

    protected Record getObject() {
        return new HINFORecord();
    }

    /**
     * Creates an HINFO Record from the given data
     * 
     * @param cpu
     *            A string describing the host's CPU
     * @param os
     *            A string describing the host's OS
     * @throws IllegalArgumentException
     *             One of the strings has invalid escapes
     */
    public HINFORecord(Name name, DClass dclass, long ttl, String cpu, String os) {
        super(name, Type.HINFO, dclass, ttl);
        try {
            this.cpu = byteArrayFromString(cpu);
            this.os = byteArrayFromString(os);
        } catch (TextParseException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    protected void rrFromWire(DNSInput in) throws IOException {
        cpu = in.readCountedString();
        os = in.readCountedString();
    }

    protected void rdataFromString(Tokenizer st, Name origin) throws IOException {
        try {
            cpu = byteArrayFromString(st.getString());
            os = byteArrayFromString(st.getString());
        } catch (TextParseException e) {
            throw st.exception(e.getMessage());
        }
    }

    /**
     * Returns the host's CPU
     */
    public String getCPU() {
        return byteArrayToString(cpu, false);
    }

    /**
     * Returns the host's OS
     */
    public String getOS() {
        return byteArrayToString(os, false);
    }

    public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
        out.writeCountedString(cpu);
        out.writeCountedString(os);
    }

    /**
     * Converts to a string
     */
    public String rrToString() {
        StringBuffer sb = new StringBuffer();
        sb.append(byteArrayToString(cpu, true));
        sb.append(" ");
        sb.append(byteArrayToString(os, true));
        return sb.toString();
    }

}
