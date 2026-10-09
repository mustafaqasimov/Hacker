package az.hacktrain.ai;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="hacktrain.ai.provider",havingValue="openai",matchIfMissing=true)
class OpenAiProvider implements AiProvider {
    private final String key,model; private final ObjectMapper mapper;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    OpenAiProvider(@Value("${hacktrain.ai.openai-key:}") String key,@Value("${hacktrain.ai.model:}") String model,ObjectMapper mapper) { this.key=key;this.model=model;this.mapper=mapper; }
    @Override public String name() { return "openai"; }
    @Override public String model() { return model; }
    @Override public boolean configured() { return !key.isBlank()&&!model.isBlank(); }
    @Override public Completion complete(String system,String context,String schema) throws Exception {
        if(!configured()) throw new IllegalStateException("AI provider is not configured");
        var body=Map.of("model",model,"store",false,"max_output_tokens",1200,
            "instructions",system,"input",List.of(Map.of("role","user","content",context)),
            "text",Map.of("format",Map.of("type","json_schema","name","hacktrain_tutor_v1","strict",true,"schema",mapper.readTree(schema))));
        var request=HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
            .timeout(Duration.ofSeconds(25)).header("Authorization","Bearer "+key).header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
        var response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());
        byte[] bytes;
        try(var stream=response.body()) { bytes=stream.readNBytes(65537); }
        if(response.statusCode()!=200||bytes.length>65536) throw new IllegalStateException("AI provider returned an unsuccessful response");
        JsonNode root=mapper.readTree(bytes);
        if(!root.path("status").asText().equals("completed")) throw new IllegalStateException("AI response is incomplete");
        var text=new StringBuilder();
        for(var output:root.path("output")) for(var content:output.path("content")) {
            if(content.path("type").asText().equals("refusal")) throw new IllegalStateException("AI response declined");
            if(content.path("type").asText().equals("output_text")) text.append(content.path("text").asText());
        }
        var usage=root.path("usage");
        return new Completion(text.toString(),usage.path("input_tokens").asInt(),usage.path("output_tokens").asInt(),model);
    }
}
