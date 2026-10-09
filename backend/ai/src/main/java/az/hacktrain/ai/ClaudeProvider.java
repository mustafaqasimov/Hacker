package az.hacktrain.ai;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
@Component
@ConditionalOnProperty(name="hacktrain.ai.provider",havingValue="claude")
class ClaudeProvider implements AiProvider {
    private final String key,model;
    private final ObjectMapper mapper;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    ClaudeProvider(@Value("${hacktrain.ai.anthropic-key:}") String key,@Value("${hacktrain.ai.claude-model:claude-sonnet-4-6}") String model,ObjectMapper mapper) {
        this.key=key;this.model=model;this.mapper=mapper;
    }
    @Override public String name() { return "claude"; }
    @Override public String model() { return model; }
    @Override public boolean configured() { return !key.isBlank()&&!model.isBlank(); }
    @Override public Completion complete(String system,String context,String schema) throws Exception {
        if(!configured()) throw new IllegalStateException("Claude is not configured");
        var providerSchema=mapper.readTree(schema);
        adaptSchema(providerSchema);
        var body=Map.of("model",model,"max_tokens",1200,"system",system,
            "messages",List.of(Map.of("role","user","content",context)),
            "output_config",Map.of("format",Map.of("type","json_schema","schema",providerSchema)));
        var request=HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
            .timeout(Duration.ofSeconds(25)).header("x-api-key",key).header("anthropic-version","2023-06-01")
            .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
        var pending=client.sendAsync(request,HttpResponse.BodyHandlers.ofByteArray());
        HttpResponse<byte[]> response;
        try { response=pending.get(25,TimeUnit.SECONDS); }
        finally { if(!pending.isDone()) pending.cancel(true); }
        if(response.statusCode()!=200) {
            var reason=ProviderUnavailable.Reason.UNAVAILABLE;
            if(response.statusCode()==401||response.statusCode()==403) reason=ProviderUnavailable.Reason.AUTHENTICATION;
            else if(response.statusCode()==429) reason=ProviderUnavailable.Reason.RATE_LIMIT;
            else if(response.statusCode()==400 && response.body().length<=65536) {
                var error=mapper.readTree(response.body()).path("error");
                if(error.path("message").asText().contains("credit balance is too low")) reason=ProviderUnavailable.Reason.CREDIT_BALANCE;
            }
            throw new ProviderUnavailable(reason);
        }
        if(response.body().length>65536) throw new IllegalStateException("Claude response too large");
        var root=mapper.readTree(response.body());
        if(!root.path("stop_reason").asText().equals("end_turn")) throw new IllegalStateException("Claude response incomplete or refused");
        var text=new StringBuilder();
        for(var block:root.path("content")) if(block.path("type").asText().equals("text")) text.append(block.path("text").asText());
        if(text.isEmpty()) throw new IllegalStateException("Claude returned no text");
        var usage=root.path("usage");
        return new Completion(text.toString(),usage.path("input_tokens").asInt(),usage.path("output_tokens").asInt(),root.path("model").asText(model));
    }
    /** Claude's wire schema omits unsupported bounds; AiOutput validates the original schema. */
    private static void adaptSchema(JsonNode node) {
        if(node instanceof ObjectNode object) {
            var descriptions=new ArrayList<String>();
            for(String constraint:List.of("minimum","maximum","minLength","maxLength","maxItems","uniqueItems")) {
                var value=object.remove(constraint);if(value!=null) descriptions.add(constraint+"="+value);
            }
            if(!descriptions.isEmpty()) object.put("description",object.path("description").asText("")+" Constraints: "+String.join(", ",descriptions));
        }
        if(node.isContainerNode()) node.forEach(ClaudeProvider::adaptSchema);
    }
}
