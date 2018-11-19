package io.nuls.test;

import io.nuls.rpc.server.WsServer;
import org.junit.Test;

public class WebSocketServer {
    @Test
    public void test() throws Exception{
        int port = 8887;
        WsServer s = new WsServer(port);
        s.init("kernel", null, "io.nuls.rpc.cmd.kernel");
        s.startAndSyncKernel("ws://127.0.0.1:8887");
        Thread.sleep(Integer.MAX_VALUE);
    }
}
