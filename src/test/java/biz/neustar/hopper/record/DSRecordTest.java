// -*- Java -*-
//
// Copyright (c) 2005, Matthew J. Rutherford <rutherfo@cs.colorado.edu>
// Copyright (c) 2005, University of Colorado at Boulder
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
// 
// * Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// * Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// * Neither the name of the University of Colorado at Boulder nor the
//   names of its contributors may be used to endorse or promote
//   products derived from this software without specific prior written
//   permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
package biz.neustar.hopper.record;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.DNSInput;
import biz.neustar.hopper.message.DNSOutput;
import biz.neustar.hopper.message.DNSSEC;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.util.Hex;
import biz.neustar.hopper.util.Tokenizer;

/**
 * Test cases for DS Records
 * 
 * @author Brian Wellington bwelling@xbill.org
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class DSRecordTest extends TestCase {
    public void test_ctor_0arg() {
        DSRecord dr = new DSRecord();
        assertNull(dr.getName());
        assertEquals(0, dr.getType());
        assertNull(dr.getDClass());
        assertEquals(0, dr.getTTL());
        assertNull(dr.getAlgorithm());
        assertEquals(0, dr.getDigestID());
        assertNull(dr.getDigest());
        assertEquals(0, dr.getFootprint());
    }

    public void test_getObject() {
        DSRecord dr = new DSRecord();
        Record r = dr.getObject();
        assertTrue(r instanceof DSRecord);
    }

    public static class Test_Ctor_7arg extends TestCase {
        private Name m_n;
        private long m_ttl;
        private int m_footprint;
        private DNSSEC.Algorithm m_algorithm;
        private int m_digestid;
        private byte[] m_digest;

        protected void setUp() throws TextParseException {
            m_n = Name.fromString("The.Name.");
            m_ttl = 0xABCDL;
            m_footprint = 0xEF01;
            m_algorithm = DNSSEC.Algorithm.valueOf(0x23);
            m_digestid = 0x45;
            m_digest = new byte[] { (byte) 0x67, (byte) 0x89, (byte) 0xAB,
                    (byte) 0xCD, (byte) 0xEF };
        }

        public void test_basic() throws TextParseException {
            DSRecord dr = new DSRecord(m_n, DClass.IN, m_ttl, m_footprint,
                    m_algorithm, m_digestid, m_digest);
            assertEquals(m_n, dr.getName());
            assertEquals(DClass.IN, dr.getDClass());
            assertEquals(Type.DS, dr.getType());
            assertEquals(m_ttl, dr.getTTL());
            assertEquals(m_footprint, dr.getFootprint());
            assertEquals(m_algorithm, dr.getAlgorithm());
            assertEquals(m_digestid, dr.getDigestID());
            assertTrue(Arrays.equals(m_digest, dr.getDigest()));
        }

        public void test_toosmall_footprint() throws TextParseException {
            try {
                new DSRecord(m_n, DClass.IN, m_ttl, -1, m_algorithm,
                        m_digestid, m_digest);
                fail("IllegalArgumentException not thrown");
            } catch (IllegalArgumentException e) {
            }
        }

        public void test_toobig_footprint() throws TextParseException {
            try {
                new DSRecord(m_n, DClass.IN, m_ttl, 0x10000, m_algorithm,
                        m_digestid, m_digest);
                fail("IllegalArgumentException not thrown");
            } catch (IllegalArgumentException e) {
            }
        }

        public void test_toosmall_digestid() throws TextParseException {
            try {
                new DSRecord(m_n, DClass.IN, m_ttl, m_footprint, m_algorithm,
                        -1, m_digest);
                fail("IllegalArgumentException not thrown");
            } catch (IllegalArgumentException e) {
            }
        }

        public void test_toobig_digestid() throws TextParseException {
            try {
                new DSRecord(m_n, DClass.IN, m_ttl, m_footprint, m_algorithm,
                        0x10000, m_digest);
                fail("IllegalArgumentException not thrown");
            } catch (IllegalArgumentException e) {
            }
        }

        public void test_null_digest() {
            DSRecord dr = new DSRecord(m_n, DClass.IN, m_ttl, m_footprint,
                    m_algorithm, m_digestid, null);
            assertEquals(m_n, dr.getName());
            assertEquals(DClass.IN, dr.getDClass());
            assertEquals(Type.DS, dr.getType());
            assertEquals(m_ttl, dr.getTTL());
            assertEquals(m_footprint, dr.getFootprint());
            assertEquals(m_algorithm, dr.getAlgorithm());
            assertEquals(m_digestid, dr.getDigestID());
            assertNull(dr.getDigest());
        }
    }

    public void test_rrFromWire() throws IOException {
        byte[] raw = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF,
                (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x89 };
        DNSInput in = new DNSInput(raw);

        DSRecord dr = new DSRecord();
        dr.rrFromWire(in);
        assertEquals(0xABCD, dr.getFootprint());
        assertEquals(0xEF, dr.getAlgorithm().getValue());
        assertEquals(0x01, dr.getDigestID());
        assertTrue(Arrays.equals(new byte[] { (byte) 0x23, (byte) 0x45,
                (byte) 0x67, (byte) 0x89 }, dr.getDigest()));
    }

    public void test_rdataFromString() throws IOException {
        Tokenizer t = new Tokenizer(0xABCD + " " + 0xEF + " " + 0x01
                + " 23456789AB");

        DSRecord dr = new DSRecord();
        dr.rdataFromString(t, null);
        assertEquals(0xABCD, dr.getFootprint());
        assertEquals(0xEF, dr.getAlgorithm().getValue());
        assertEquals(0x01, dr.getDigestID());
        assertTrue(Arrays.equals(new byte[] { (byte) 0x23, (byte) 0x45,
                (byte) 0x67, (byte) 0x89, (byte) 0xAB }, dr.getDigest()));
    }

    public void test_rrToString() throws TextParseException {
        String exp = 0xABCD + " " + 0xEF + " " + 0x01 + " 23456789AB";

        DSRecord dr = new DSRecord(Name.fromString("The.Name."), DClass.IN,
                0x123, 0xABCD, DNSSEC.Algorithm.valueOf(0xEF), 0x01, new byte[] { (byte) 0x23,
                        (byte) 0x45, (byte) 0x67, (byte) 0x89, (byte) 0xAB });
        assertEquals(exp, dr.rrToString());
    }

    public void test_rrToWire() throws TextParseException {
        DSRecord dr = new DSRecord(Name.fromString("The.Name."), DClass.IN,
                0x123, 0xABCD, DNSSEC.Algorithm.valueOf(0xEF), 0x01, new byte[] { (byte) 0x23,
                        (byte) 0x45, (byte) 0x67, (byte) 0x89, (byte) 0xAB });

        byte[] exp = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF,
                (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67,
                (byte) 0x89, (byte) 0xAB };

        DNSOutput out = new DNSOutput();
        dr.rrToWire(out, null, true);

        assertTrue(Arrays.equals(exp, out.toByteArray()));
    }

    public void testDigest() throws TextParseException, IOException {

        // the following expected results are from a dnssec-signzone run
        // (Version:
        // 9.7.1-P2)
        String ksk = "signzone.biz. 86400 IN DNSKEY 257 3 8 AwEAAceDrbuyohhRgE//F5zcnCyQrI/zB2Ve2SG2aeenNUP3husgP31bPz8KNmfnpbBTwU08r3pUnamXjV36VltEjgPzqo3xjrDeGugT4jLuP07m/pLEzsn/vvuztTCh15p6Z4sFq+P1J/WPpR3hODAA5ywJBVNX8QOvmZpxUw8GlcXn";
        String ds1 = "signzone.biz.           IN DS 10045 8 1 CB2623B9580376827F15ED348CFBF3DF87321855";
        String ds2 = "signzone.biz.           IN DS 10045 8 2 C4C8E23D9DA3878EA86113A60F8A01F5A5FADDBC03DC0FD3950BDC4A D9326AFA";

        // pick off the key RRData
        String kskRRData = ksk.substring(30);
        // pick off the digest values
        String expectedDigestSHA1 = ds1.substring(40);
        String expectedDigestSHA256 = ds2.substring(40).replaceAll(" ", "");

        // Construct a DNSKEY
        DNSKEYRecord kskRecord = (DNSKEYRecord) Record.fromString(new Name(
                "signzone.biz."), Type.DNSKEY, DClass.IN, 86400, kskRRData,
                null);

        // Check DS Record for SHA1 digest
        DSRecord dsRecordSHA1 = new DSRecord(new Name("signzone.biz."),
                DClass.IN, 86400, DSRecord.Digest.SHA1, kskRecord);
        byte[] actualDigestSHA1 = dsRecordSHA1.getDigest();
        assertTrue(Arrays.equals(
                Hex.decode(expectedDigestSHA1),
                actualDigestSHA1));

        // Check DS Record for SHA256 digest
        DSRecord dsRecordSHA256 = new DSRecord(new Name("signzone.biz."),
                DClass.IN, 86400, DSRecord.Digest.SHA256, kskRecord);
        byte[] actualDigestSHA256 = dsRecordSHA256.getDigest();
        assertTrue(Arrays.equals(
                Hex.decode(expectedDigestSHA256),
                actualDigestSHA256));
    }

    public static Test suite() {
        TestSuite s = new TestSuite();
        s.addTestSuite(Test_Ctor_7arg.class);
        s.addTestSuite(DSRecordTest.class);
        return s;
    }
}
