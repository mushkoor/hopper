/*
 * *
 *  * Copyright 2000-2011 NeuStar, Inc. All rights reserved.
 *  * NeuStar, the Neustar logo and related names and logos are registered
 *  * trademarks, service marks or tradenames of NeuStar, Inc. All other
 *  * product names, company names, marks, logos and symbols may be trademarks
 *  * of their respective owners.
 *
 */
package biz.neustar.hopper;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import junit.framework.TestCase;
import biz.neustar.hopper.config.Options;
import biz.neustar.hopper.exception.ZoneTransferException;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.ZoneTransferIn;
import biz.neustar.hopper.message.ZoneTransferResult;
import biz.neustar.hopper.resolver.TCPClient;

/**
 * @author Marty Kube marty@beavercreekconsulting.com
 */
public class ZoneTransferInTest extends TestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Options.clear();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Options.clear();
    }

    public void test_connecttimeout() throws IOException, UnknownHostException,
            ZoneTransferException {
        String zoneName = "test.zone.";
        // Lets hope nobody turns on port 53 on this host ever, so it will be
        // unreachable.
        String unreachableHost = "8.99.99.99";
        int connectTimeout = 10;
        int timeout = 900;

        ZoneTransferIn zoneTransferIn = ZoneTransferIn.newAXFR(new Name(
                zoneName), unreachableHost, null);
        zoneTransferIn.setConnectTimeout(connectTimeout);
        zoneTransferIn.setTimeout(timeout);

        long startTime = System.currentTimeMillis();
        long endTime, timeTakenInSecs;
        try {
            zoneTransferIn.run();
            fail("Zone transfer did not throw any exception");
        } catch (SocketTimeoutException e) {
            endTime = System.currentTimeMillis();
            timeTakenInSecs = (endTime - startTime) / 1000;
            assertTrue(timeTakenInSecs <= timeout);
        }
    }

    /**
     * In the spirit of being lenient in what you accept... We have seen IXFR
     * responses for up-to-date zones that look like a AXFR style IXFR response:
     * 
     * <pre>
     * for:
     * dig example.com ixfr=2
     * 
     * the response is:
     * SOA serial 2
     * RR
     * RR
     * ..
     * SOA serial 2
     * </pre>
     * 
     * Which is wrong (either respond correctly per IXFR with a single SOA
     * record, or, respond that you don't support IXFR). But, once we see the
     * first SOA we know the zone is up to date. Instead of throwing an
     * "extra data" exception, just accept that the zone is up-to-date - If
     * configured to do so.
     */
    public void test_extraDataInIXFRResponse() throws Exception {

        // fail by default
        ZoneTransferIn newIXFR = ZoneTransferIn
                .newIXFR(new Name("example.biz."), 2008021850l, false,
                        "localhost", null);
        newIXFR.setClient(new StringTCPClient(ixfrResponseExtraDataHex));
        try {
            newIXFR.run();
            fail("Should have failed for extra data");
        } catch (ZoneTransferException zte) {
            // pass
        }

        // Configure to be lenient
        Options.set("ignoreextradata");
        newIXFR = ZoneTransferIn.newIXFR(new Name("example.biz."), 2008021850l,
                false, "localhost", null);
        newIXFR.setClient(new StringTCPClient(ixfrResponseExtraDataHex));
        ZoneTransferResult run = newIXFR.run();
        assertTrue(run.isUpToDate());
    }

    /**
     * When the response question type QTYPE is wrong, ignore the problem if
     * asked to.
     */
    public void test_incorrectQuestionTypeInIXFRResponse() throws Exception {

        // fail by default
        ZoneTransferIn newIXFR = ZoneTransferIn
                .newIXFR(new Name("example.biz."), 2011071501l, false,
                        "localhost", null);
        newIXFR.setClient(new StringTCPClient(ixfrResponseWrongQuestionHex));
        try {
            newIXFR.run();
            fail("Should have failed for invalid question section");
        } catch (ZoneTransferException zte) {
            // pass
        }

        // Configure to be lenient - the test data has both wrong QTYPE and
        // extra data
        Options.set("ignoreresponsequestiontype");
        newIXFR = ZoneTransferIn.newIXFR(new Name("example.biz."), 2011071501l,
                false, "localhost", null);
        newIXFR.setClient(new StringTCPClient(ixfrResponseWrongQuestionHex));
        try {
            newIXFR.run();
            fail("Should have failed for extra data");
        } catch (ZoneTransferException zte) {
            // pass
        }

        // Now ignore all problems
        Options.set("ignoreresponsequestiontype");
        Options.set("ignoreextradata");
        newIXFR = ZoneTransferIn.newIXFR(new Name("example.biz."), 2011071501l,
                false, "localhost", null);
        newIXFR.setClient(new StringTCPClient(ixfrResponseWrongQuestionHex));
        ZoneTransferResult run = newIXFR.run();
        assertTrue(run.isUpToDate());
    }

    /**
     * A TCPClient for testing that returns a fixed wire message
     */
    public static class StringTCPClient implements TCPClient {

        String wireMessage;

        /**
         * Create a client which returns a fixed DNS message
         * 
         * @param wireMessage
         *            A base64 encoded DNS wire format message
         */
        StringTCPClient(String wireMessage) {
            this.wireMessage = wireMessage;
        }

        @Override
        public void bind(SocketAddress addr) throws IOException {
        }

        @Override
        public void connect(SocketAddress addr) throws IOException {
        }

        @Override
        public void send(byte[] data) throws IOException {
        }

        @Override
        public byte[] recv() throws IOException {
            return (new HexBinaryAdapter()).unmarshal(wireMessage);
        }

        @Override
        public void cleanup() throws IOException {
        }
    }

    String ixfrResponseExtraDataHex = "a27f8400000100750000000003623262096d616371756172696503636f6d0000fb0001c00c0006000"
            + "10000012c00310b6973647379647372763133c0100c697473627367756e78737570c01077affb5a0"
            + "000012c0000000f00093a800000012cc00c000200010000012c00160550444e533108554c5452414"
            + "44e5302434f02554b00c00c000200010000012c00080550444e5332c072c00c000200010000012c0"
            + "0080550444e5333c072c00c000200010000012c00080550444e5334c072c00c000200010000012c0"
            + "0080550444e5335c072c00c000200010000012c00080550444e5336c072056168303170c00c00050"
            + "0010000012c000f0c61683031702d72686f646573c00c0961683031702d67706fc00c00010001000"
            + "151800004ca2b851ec0f800010001000151800004ca2b841705616f303170c00c000500010000012"
            + "c000f0c616f3031702d72686f646573c00c09616f3031702d67706fc00c00010001000151800004c"
            + "a2b850fc14300010001000151800004ca2b840f0961703031702d67706fc00c00010001000151800"
            + "004ca2b85020f61703031702d6d6967726174696f6ec00c000500010000012c000f0c61703031702"
            + "d72686f646573c00c0f61703031702d706f73747365727631c00c000100010000012c0004cb12d12"
            + "ec1b200010001000151800004ca2b840205617a303170c00c000500010000012c000f0c617a30317"
            + "02d72686f646573c00c09617a3031702d67706fc00c00010001000151800004ca2b851ac20300010"
            + "001000151800004ca2b8413056261303170c00c000500010000012c000f0c62613031702d72686f6"
            + "46573c00c0962613031702d67706fc00c00010001000151800004ca2b851cc24e000100010001518"
            + "00004ca2b841505636c303170c00c000500010000012c00110e636f6e646972702d72686f646573c"
            + "00c05636d303170c00c000500010000012c000f0c636d3031702d72686f646573c00c09636d30317"
            + "02d67706fc00c00010001000151800004ca2b8512c2bc00010001000151800004ca2b841d0b636f6"
            + "e646972702d67706fc00c00010001000151800004ca2b8506c29900010001000151800004ca2b840"
            + "6056373303170c00c000500010000012c000f0c63753031702d72686f646573c00c0f63733031702"
            + "d706f73747365727631c00c000100010000012c0004cb1c5e15056373303270c00c0005000100000"
            + "12c000f0c63733032702d72686f646573c00c0963733032702d67706fc00c0001000100015180000"
            + "4ca2b8514c37400010001000151800004ca2b840e0963753031702d67706fc00c000100010001518"
            + "00004ca2b8503c33300010001000151800004ca2b8403056664303170c00c000500010000012c000"
            + "f0c66643031702d72686f646573c00c0966643031702d67706fc00c00010001000151800004ca2b8"
            + "520c3e900010001000151800004ca2b8419056673303170c00c000500010000012c0002c29909667"
            + "33031702d67706fc00c00010001000151800004ca2b85060c66733031702d72686f646573c00c000"
            + "10001000151800004ca2b8406056678303170c00c000500010000012c000f0c66783031702d72686"
            + "f646573c00c0966783031702d67706fc00c00010001000151800004ca2b8519c47f0001000100015"
            + "1800004ca2b841205676d303170c00c000500010000012c000f0c676d3031702d72686f646573c00"
            + "c09676d3031702d67706fc00c00010001000151800004ca2b851bc4ca00010001000151800004ca2"
            + "b84140968703031702d67706fc00c00010001000151800004ca2b85040f68703031702d6d6967726"
            + "174696f6ec00c000500010000012c000f0c68703031702d72686f646573c00c0f68703031702d706"
            + "f73747365727631c00c000100010000012c0004cb12d12ec53900010001000151800004ca2b84040"
            + "56c65303170c00c000500010000012c000f0c6c653031702d72686f646573c00c096c653031702d6"
            + "7706fc00c00010001000151800004ca2b8521c58a00010001000151800004ca2b841a056c6d30317"
            + "0c00c000500010000012c0002c333066d616366636cc00c000500010000012c00100d6d616366636"
            + "c2d72686f646573c00c0a6d616366636c2d67706fc00c00010001000151800004ca2b8528c5ea000"
            + "10001000151800004ca2b840a056d68303170c00c000500010000012c000f0c6d683031702d72686"
            + "f646573c00c096d683031702d67706fc00c00010001000151800004ca2b851dc6370001000100015"
            + "1800004ca2b8416056d73303170c00c000500010000012c000f0c6d733031702d72686f646573c00"
            + "c096d733031702d67706fc00c00010001000151800004ca2b8511c68200010001000151800004ca2"
            + "b841c056d79303170c00c000500010000012c000f0c6d793031702d72686f646573c00c096d79303"
            + "1702d67706fc00c00010001000151800004ca2b85050f6d793031702d6d6967726174696f6ec00c0"
            + "00500010000012c0002c6cd0f6d793031702d706f73747365727631c00c000100010000012c0004c"
            + "b12d183c6cd00010001000151800004ca2b8405056e61303170c00c000500010000012c0002c2990"
            + "f6e613031702d706f73747365727631c00c000100010000012c0004cb12d183056e7a303170c00c0"
            + "00500010000012c0002c299096e7a3031702d67706fc00c00010001000151800004ca2b85060c6e7"
            + "a3031702d72686f646573c00c00010001000151800004ca2b8406057063303170c00c00050001000"
            + "0012c000f0c70633031702d72686f646573c00c0970633031702d67706fc00c00010001000151800"
            + "004ca2b8523c7d500010001000151800004ca2b842305706c303170c00c000500010000012c000f0"
            + "c706c3031702d72686f646573c00c09706c3031702d67706fc00c00010001000151800004ca2b851"
            + "fc82000010001000151800004ca2b8418057267303170c00c000500010000012c000f0c726730317"
            + "02d72686f646573c00c0972673031702d67706fc00c00010001000151800004ca2b8526c86b00010"
            + "001000151800004ca2b841f057368303170c00c000500010000012c000f0c73683031702d72686f6"
            + "46573c00c0973683031702d67706fc00c00010001000151800004ca2b8522c8b6000100010001518"
            + "00004ca2b841b057370303170c00c000500010000012c000f0c73703031702d72686f646573c00c0"
            + "973703031702d67706fc00c00010001000151800004ca2b85070f73703031702d706f73747365727"
            + "631c00c000100010000012c0004cb1c5e15c90100010001000151800004ca2b84070474657374c00"
            + "c000100010000012c0004cb17c80a057473303170c00c000500010000012c000f0c74733031702d7"
            + "2686f646573c00c0974733031702d67706fc00c00010001000151800004ca2b8524c981000100010"
            + "00151800004ca2b8424057561303170c00c000500010000012c000f0c75613031702d72686f64657"
            + "3c00c0975613031702d67706fc00c00010001000151800004ca2b85080f75613031702d6d6967726"
            + "174696f6ec00c000500010000012c0002c9cc0f75613031702d706f73747365727631c00c0001000"
            + "10000012c0004cb12d183c9cc00010001000151800004ca2b8408057570303170c00c00050001000"
            + "0012c000f0c75703031702d72686f646573c00c0975703031702d67706fc00c00010001000151800"
            + "004ca2b8522ca5500010001000151800004ca2b8422057668303170c00c000500010000012c000f0"
            + "c76683031702d72686f646573c00c05766c303170c00c000500010000012c000f0c766c3031702d7"
            + "2686f646573c00c09766c3031702d67706fc00c00010001000151800004ca2b8525cac1000100010"
            + "00151800004ca2b8425057670303170c00c000500010000012c000f0c76703031702d72686f64657"
            + "3c00c0976703031702d67706fc00c00010001000151800004ca2b8510cb0c0001000100015180000"
            + "4ca2b8410057762303170c00c000500010000012c000f0c77623031702d72686f646573c00c09776"
            + "23031702d67706fc00c00010001000151800004ca2b8518cb5700010001000151800004ca2b84110"
            + "57770303170c00c000500010000012c000f0c77703031702d72686f646573c00c0977703031702d6"
            + "7706fc00c00010001000151800004ca2b850ecba200010001000151800004ca2b840cc00c0006000"
            + "10000012c0018c02fc03d77affb5a0000012c0000000f00093a800000012c";

    String ixfrResponseWrongQuestionHex = "58078400000100070000000017657870657274666f6f7462616c6c6368616c6c656e676503636f6"
            + "d0000fc0001c00c0006000100001c20002e036e733306756e69626574c0240a686f73746d617374"
            + "6572c00c77de840d00002a3000001c20001baf8000001c20c00c0002000100001c2000140575646"
            + "e733108756c747261646e73036e657400c00c0002000100001c200002c039c00c0002000100001c"
            + "2000080575646e7332c079c00c0001000100001c20000457fd981f03777777c00c0001000100001"
            + "c20000457fd981fc00c0006000100001c200018c039c04677de840d00002a3000001c20001baf80"
            + "00001c20";

}
