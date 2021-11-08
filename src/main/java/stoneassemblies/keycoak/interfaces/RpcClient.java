package stoneassemblies.keycoak.interfaces;

import org.json.JSONObject;

public interface RpcClient {
    JSONObject call(String queueName, String requestExchange, String responseExchange, JSONObject requestMessageObject);
}
