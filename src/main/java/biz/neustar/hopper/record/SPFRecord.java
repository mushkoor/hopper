// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import java.util.List;

import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.record.impl.TXTBase;

/**
 * Sender Policy Framework (RFC 4408, experimental)
 * 
 * @author Brian Wellington
 */

public class SPFRecord extends TXTBase {

    private static final long serialVersionUID = -2100754352801658722L;

    public SPFRecord() {
    }

    protected Record getObject() {
        return new SPFRecord();
    }

    /**
     * Creates a SPF Record from the given data
     * 
     * @param strings
     *            The text strings
     * @throws IllegalArgumentException
     *             One of the strings has invalid escapes
     */
    public SPFRecord(Name name, DClass dclass, long ttl, List<String> strings) {
        super(name, Type.SPF, dclass, ttl, strings);
    }

    /**
     * Creates a SPF Record from the given data
     * 
     * @param string
     *            One text string
     * @throws IllegalArgumentException
     *             The string has invalid escapes
     */
    public SPFRecord(Name name, DClass dclass, long ttl, String string) {
        super(name, Type.SPF, dclass, ttl, string);
    }

}
