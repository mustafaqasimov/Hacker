package az.hacktrain.ai;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.*;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
@Service
public class AiService {
    private static final String PROMPT="course-tutor-v1";
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(AiService.class);
    private final AiProvider provider;private final AiContext context;private final AiQuota quota;private final AiOutput output;private final ObjectMapper mapper;
    private final Semaphore concurrency=new Semaphore(12);
    private final CircuitBreaker breaker=CircuitBreaker.of("course-tutor",CircuitBreakerConfig.custom().failureRateThreshold(50).minimumNumberOfCalls(5).slidingWindowSize(10).waitDurationInOpenState(Duration.ofSeconds(30)).build());
    AiService(AiProvider provider,AiContext context,AiQuota quota,AiOutput output,ObjectMapper mapper) { this.provider=provider;this.context=context;this.quota=quota;this.output=output;this.mapper=mapper; }
    public AiDtos.Availability availability() { return new AiDtos.Availability(provider.configured(),provider.name(),provider.model(),1,"COURSE_TUTOR"); }
    public AiDtos.Answer hint(UUID org,UUID actor,UUID course,UUID task,AiDtos.Ask request) {
        var data=context.load(org,actor,course,task,request.explanation());quota.consume(org,actor);
        var id=UUID.randomUUID();
        if(!provider.configured()) return fallback(id,"AI bağlantısı hələ konfiqurasiya edilməyib. Aşağıdakı sual sabit təlim sualıdır.");
        if(!concurrency.tryAcquire()) return fallback(id,"AI hazırda məşğuldur. Sabit təlim sualı göstərilir.");
        try {
            String serialized=mapper.writeValueAsString(Map.of("untrustedLearningData",data));
            // One repair attempt for invalid output; transport errors go straight to the fallback.
            for(int attempt=0;attempt<2;attempt++) {
                long started=System.nanoTime();
                var completion=breaker.executeCallable(()->provider.complete(output.system,serialized,output.schema));
                LOG.info("ai_call requestId={} provider={} model={} promptVersion={} inputTokens={} outputTokens={} latencyMs={}",id,provider.name(),completion.model(),PROMPT,completion.inputTokens(),completion.outputTokens(),(System.nanoTime()-started)/1_000_000);
                try { var hint=output.validate(completion.json());return new AiDtos.Answer(id,hint,false,"AI",provider.name(),completion.model(),PROMPT,"AI rəyi texniki uğur yoxlaması deyil."); }
                catch(Exception invalid) { LOG.warn("ai_invalid_output requestId={} attempt={}",id,attempt+1); }
            }
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
        catch(ProviderUnavailable unavailable) {
            LOG.warn("ai_unavailable requestId={} provider={} reason={}",id,provider.name(),unavailable.reason);
            if(unavailable.reason==ProviderUnavailable.Reason.CREDIT_BALANCE) return fallback(id,"Claude API balansı kifayət deyil. Administrator Anthropic hesabına kredit əlavə etməlidir. Hazırda sabit təlim sualı göstərilir.");
        }
        catch(Exception unavailable) { LOG.warn("ai_unavailable requestId={} provider={}",id,provider.name()); }
        finally { concurrency.release(); }
        return fallback(id,"AI cavabı alınmadı. Sabit təlim sualı göstərilir.");
    }
    private AiDtos.Answer fallback(UUID id,String message) {
        return new AiDtos.Answer(id,new AiDtos.Hint("Konkret çətinliyi müəyyən etmək üçün daha çox izah lazımdır.",List.of(),true,"Hansı addımı sınadınız və hansı nəticəni gözləyirdiniz?",1,"Öyrənmə məqsədini kiçik addımlara bölün. İlk addımda hansı müşahidə sizə irəlilədiyinizi göstərə bilər?",0),true,"STATIC",provider.name(),provider.model(),PROMPT,message);
    }
}
