package app.nebula.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class NetworkNode implements AutoCloseable {

    private static final int PROTOCOL_VERSION = 1;
    private static final int CONNECTION_TIMEOUT_MILLIS = 5_000;

    private final String nodeId = UUID.randomUUID().toString();
    private final int requestedPort;
    private final Consumer<String> eventListener;
    private final Consumer<String> messageHandler;

    private final Map<String, PeerConnection> peers = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private volatile ServerSocket serverSocket;
    private volatile boolean running;

    public NetworkNode(int port, Consumer<String> eventListener, Consumer<String> messageHandler) {
        if (port < 0 || port > 65_535)
            throw new IllegalArgumentException("Port must be between 0 and 65535.");

        this.requestedPort = port;
        this.eventListener = eventListener != null ? eventListener : message -> {};
        this.messageHandler = messageHandler != null ? messageHandler : message -> {};
    }

    public void start() throws IOException {
        if (running)
            throw new IllegalArgumentException("Node is already running.");

        ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(requestedPort));

        serverSocket = socket;
        running = true;

        executor.execute(this::acceptConnections);
        log("Node started. ID: " + nodeId);
        log("Listening on port " + getPort());
    }

    public void connectTo(String host, int port) throws IOException {
        if (!running)
            throw new IllegalStateException("Node was not started.");

        Socket socket = new Socket();

        try {
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MILLIS);
            socket.setKeepAlive(true);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
            );
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            String hello = reader.readLine();
            String remoteNodeId = parseHello(hello);

            if (remoteNodeId == null)
                throw new IOException("Invalid handshake from " + host + ":" + port);

            writer.println(createHello());

            PeerConnection peer = new PeerConnection(remoteNodeId, socket, reader, writer);
            registerPeer(peer);
            socket = null;

            executor.execute(() -> readMessages(peer));
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException _) {
                }
            }
        }
    }

    public void pingPeers() {
        for (var peer : peers.values()) {
            String nonce = UUID.randomUUID().toString();

            if (peer.send("PING|" + nonce))
                log("PING sent to " + peer.nodeId);
        }
    }

    public Set<String> getConnectedPeers() {
        return Set.copyOf(peers.keySet());
    }

    public String getNodeId() {
        return nodeId;
    }

    public int getPort() {
        ServerSocket socket = serverSocket;

        if (socket == null)
            return requestedPort;

        return socket.getLocalPort();
    }

    public void broadcast(String message) {
        if (message == null || message.isBlank() || message.indexOf('\n') >= 0 || message.indexOf('\r') >= 0)
            throw new IllegalArgumentException("Invalid network message.");

        for (var peer : peers.values()) {
            if (!peer.send(message)) {
                log("Failed to send message to " + peer.nodeId);
            }
        }
    }

    private void acceptConnections() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setKeepAlive(true);
                executor.execute(() -> handleIncomingConnection(socket));
            } catch (IOException ex) {
                if (running)
                    log("Accept failed: " + ex.getMessage());
            }
        }
    }

    private void handleIncomingConnection(Socket socket) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
            );
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            writer.println(createHello());
            String hello = reader.readLine();
            String remoteNodeId = parseHello(hello);

            if (remoteNodeId == null) {
                log("Rejected connection with invalid handshake.");
                socket.close();
                return;
            }

            PeerConnection peer = new PeerConnection(remoteNodeId, socket, reader, writer);
            registerPeer(peer);
            readMessages(peer);
        } catch (IOException ex) {
            log("Incoming connection failed: " + ex.getMessage());

            try {
                socket.close();
            } catch (IOException _) {
            }
        }
    }

    private String createHello() {
        return "HELLO|" +
                PROTOCOL_VERSION + "|" +
                nodeId + "|" +
                getPort();
    }

    private String parseHello(String message) {
        if (message == null)
            return null;

        String[] parts = message.split("\\|", -1);

        if (parts.length != 4)
            return null;

        if (!"HELLO".equals(parts[0]) ||
                !String.valueOf(PROTOCOL_VERSION).equals(parts[1]) ||
                parts[2].isBlank() ||
                parts[2].equals(nodeId)
        ) return null;

        try {
            int remotePort = Integer.parseInt(parts[3]);

            if (remotePort < 1 || remotePort > 65_535) return null;
        } catch (NumberFormatException _) {
            return null;
        }

        return parts[2];
    }

    private void registerPeer(PeerConnection peer) {
        var existing = peers.putIfAbsent(peer.nodeId, peer);

        if (existing != null) {
            log("Already connected to peer " + peer.nodeId);
            peer.close();
            return;
        }

        log("Peer connected: " + peer.nodeId);
    }

    private void readMessages(PeerConnection peer) {
        try {
            String message;

            while (running &&
                    !peer.socket.isClosed() &&
                    (message = peer.reader.readLine()) != null
            ) {
                handleMessage(peer, message);
            }
        } catch (IOException ex) {
            if (running) log("Connection to " + peer.nodeId + " ended: " + ex.getMessage());
        } finally {
            peers.remove(peer.nodeId, peer);
            peer.close();
            log("Peer disconnected: " + peer.nodeId);
        }
    }

    private void handleMessage(PeerConnection peer, String message) {
        String[] parts = message.split("\\|", -1);

        switch (parts[0]) {
            case "PING":
                if (parts.length == 2) {
                    peer.send("PONG|" + parts[1]);
                    log("PING received from " + peer.nodeId);
                }
                break;
            case "PONG":
                if (parts.length == 2) {
                    log("PONG received from " + peer.nodeId);
                }
                break;
            case "TX":
                if (parts.length == 2 && !parts[1].isBlank())
                    messageHandler.accept(message);
                else
                    log("Malformed transaction message from " + peer.nodeId);
                break;
            default:
                log("Unknown message received from " + peer.nodeId);
        }
    }

    private void log(String message) {
        eventListener.accept(message);
    }

    @Override
    public void close() {
        running = false;

        ServerSocket socket = serverSocket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException _) {
            }
        }

        peers.values().forEach(PeerConnection::close);
        peers.clear();

        executor.shutdown();
        log("Node stopped: " + nodeId);
    }

    private record PeerConnection(
            String nodeId,
            Socket socket,
            BufferedReader reader,
            PrintWriter writer
    ) {

        private synchronized boolean send(String message) {
            if (socket.isClosed())
                return false;

            writer.println(message);
            return !writer.checkError();
        }

        private void close() {
            try {
                socket.close();
            } catch (IOException _) {
            }
        }
    }
}
