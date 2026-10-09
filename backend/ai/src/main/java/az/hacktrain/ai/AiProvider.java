package az.hacktrain.ai;
/** Adapter boundary; credentials never cross this interface. */
public interface AiProvider {
    record Completion(String json,int inputTokens,int outputTokens,String model) {}
    String name();
    String model();
    boolean configured();
    Completion complete(String system,String context,String schema) throws Exception;
}
