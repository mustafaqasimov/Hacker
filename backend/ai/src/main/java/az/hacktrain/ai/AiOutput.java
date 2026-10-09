package az.hacktrain.ai;
import com.fasterxml.jackson.databind.*;
import com.networknt.schema.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Component
class AiOutput {
    final String schema,system; private final JsonSchema validator; private final ObjectMapper mapper; private final List<String> secrets;
    AiOutput(ObjectMapper mapper,@Value("${hacktrain.ai.openai-key:}") String key,@Value("${hacktrain.ai.anthropic-key:}") String anthropicKey,@Value("${hacktrain.ai.groq-key:}") String groqKey,@Value("${hacktrain.auth.jwt-secret}") String jwt) throws java.io.IOException {
        this.mapper=mapper;
        try(var input=new ClassPathResource("ai/tutor-response-v1.schema.json").getInputStream()) { schema=new String(input.readAllBytes(),StandardCharsets.UTF_8); }
        try(var input=new ClassPathResource("ai/tutor-system-v1.txt").getInputStream()) { system=new String(input.readAllBytes(),StandardCharsets.UTF_8); }
        validator=JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schema);
        secrets=List.of(key,anthropicKey,groqKey,jwt).stream().filter(s->!s.isBlank()).toList();
    }
    AiDtos.Hint validate(String json) throws Exception {
        var tree=mapper.readTree(json);
        if(!validator.validate(tree).isEmpty()) throw new IllegalArgumentException("AI schema mismatch");
        var hint=mapper.treeToValue(tree,AiDtos.Hint.class);
        if(hint.needsClarification()!=(hint.clarifyingQuestion()!=null&&!hint.clarifyingQuestion().isBlank())||!hint.evidenceAttemptIds().isEmpty()||hint.hintLevel()!=1) throw new IllegalArgumentException("AI policy mismatch");
        String output=hint.difficulty()+" "+hint.hintText()+" "+hint.clarifyingQuestion();
        if(secrets.stream().anyMatch(output::contains)||java.util.regex.Pattern.compile("(?i)(?:flag|ctf|hacktrain)\\{[^}]+}|(?:sk-|gsk_)[a-zA-Z0-9_-]{12,}").matcher(output).find()) throw new IllegalArgumentException("AI output contains a protected pattern");
        return hint;
    }
}
