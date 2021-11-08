package stoneassemblies.keycoak;

import stoneassemblies.keycoak.interfaces.RpcClient;

import java.util.Hashtable;

public class RpcClientFactory {
    private static Hashtable<String, RpcClient> clients = new Hashtable<>();

    public static synchronized RpcClient create(String host, int port, String virtualHost, String username, String password, long timeout){
        String key = host + ":" + "port" + "/" + virtualHost + "/" + username + "@" + password + ":" + timeout;
        return clients.computeIfAbsent(key, s -> new MassTransitRpcClient(host, port, virtualHost, username, password, timeout));
    }
}
