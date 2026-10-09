package az.hacktrain.ai;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
@Component
@ConditionalOnProperty(name="hacktrain.ai.provider",havingValue="groq")
class GroqProvider implements AiProvider {
    private final String key,model;
    private final ObjectMapper mapper;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    GroqProvider(@Value("${hacktrain.ai.groq-key:}") String key,@Value("${hacktrain.ai.groq-model:openai/gpt-oss-20b}") String model,ObjectMapper mapper) {
        this.key=key;this.model=model;this.mapper=mapper;
    }
    public String name() { return "groq"; }
    public String model() { return model; }
    public boolean configured() { return !key.isBlank()&&!model.isBlank(); }
    public Completion complete(String system,String context,String schema) throws Exception {
        if(!configured()) throw new ProviderUnavailable(ProviderUnavailable.Reason.AUTHENTICATION);
        var body=Map.of("model",model,"max_completion_tokens",2000,
            "messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content",context)),
            "response_format",Map.of("type","json_schema","json_schema",Map.of("name","hacktrain_tutor_v1","strict",true,"schema",mapper.readTree(schema))));
        var request=HttpRequest.newBuilder(URI.create("https://api.groq.com/openai/v1/chat/completions"))
            .timeout(Duration.ofSeconds(25)).header("Authorization","Bearer "+key).header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
        var pending=client.sendAsync(request,HttpResponse.BodyHandlers.ofByteArray());
        HttpResponse<byte[]> response;
        try { response=pending.get(25,TimeUnit.SECONDS); }
        finally { if(!pending.isDone()) pending.cancel(true); }
        if(response.statusCode()!=200) {
            var reason=switch(response.statusCode()) {
                case 401,403 -> ProviderUnavailable.Reason.AUTHENTICATION;
                case 429 -> ProviderUnavailable.Reason.RATE_LIMIT;
                default -> ProviderUnavailable.Reason.UNAVAILABLE;
            };
            throw new ProviderUnavailable(reason);
        }
        if(response.body().length>65536) throw new IllegalStateException("Groq response too large");
        var root=mapper.readTree(response.body());var choice=root.path("choices").path(0);
        if(!choice.path("finish_reason").asText().equals("stop")||!choice.path("message").path("refusal").isMissingNode()&&!choice.path("message").path("refusal").isNull()) throw new IllegalStateException("Groq response incomplete or refused");
        String json=choice.path("message").path("content").asText();
        if(json.isBlank()) throw new IllegalStateException("Groq returned no content");
        var usage=root.path("usage");
        return new Completion(json,usage.path("prompt_tokens").asInt(),usage.path("completion_tokens").asInt(),root.path("model").asText(model));
    }
}
