package az.hacktrain.ai;
import jakarta.validation.constraints.*;
import java.util.*;
public final class AiDtos {
    private AiDtos() {}
    public record Ask(@NotBlank @Size(max=3000) String explanation) {}
    public record Hint(String difficulty,List<UUID> evidenceAttemptIds,boolean needsClarification,String clarifyingQuestion,int hintLevel,String hintText,double confidence) {}
    public record Answer(UUID requestId,Hint hint,boolean fallback,String source,String provider,String model,String promptVersion,String message) {}
    public record Availability(boolean configured,String provider,String model,int allowedHintLevel,String mode) {}
}
