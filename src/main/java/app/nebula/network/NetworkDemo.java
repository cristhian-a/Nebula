package app.nebula.network;

final class NetworkDemo {

    static void main() throws Exception {
        try(
                NetworkNode nodeA = new NetworkNode(8_001, message -> IO.println("[Node A] " + message), _ -> {});
                NetworkNode nodeB = new NetworkNode(8_002, message -> IO.println("[Node B] " + message), _ -> {})
        ) {
            nodeA.start();
            nodeB.start();

            nodeA.connectTo("127.0.0.1", nodeB.getPort());
            Thread.sleep(500);

            IO.println();
            IO.println("Node A peers: " + nodeA.getConnectedPeers());
            IO.println("Node B peers: " + nodeB.getConnectedPeers());

            IO.println();
            IO.println("Sending ping...");
            nodeA.pingPeers();

            Thread.sleep(500);
        }
    }
}
