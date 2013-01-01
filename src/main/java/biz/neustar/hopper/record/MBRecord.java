// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.record.impl.SingleNameBase;

/**
 * Mailbox Record - specifies a host containing a mailbox.
 * 
 * @author Brian Wellington
 */

public class MBRecord extends SingleNameBase {

    private static final long serialVersionUID = 532349543479150419L;

    public MBRecord() {
    }

    protected Record getObject() {
        return new MBRecord();
    }

    /**
     * Creates a new MB Record with the given data
     * 
     * @param mailbox
     *            The host containing the mailbox for the domain.
     */
    public MBRecord(Name name, DClass in, long ttl, Name mailbox) {
        super(name, Type.MB, in, ttl, mailbox, "mailbox");
    }

    /** Gets the mailbox for the domain */
    public Name getMailbox() {
        return getSingleName();
    }

    public Name getAdditionalName() {
        return getSingleName();
    }

}
