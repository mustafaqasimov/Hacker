package az.hacktrain.course;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
public final class CourseDtos {
    private CourseDtos() {}
    public enum Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }
    public record Create(@NotBlank @Size(max=160) String title,@NotNull @Size(max=2000) String description,@NotNull Difficulty difficulty) {}
    public record ObjectiveInput(@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{1,79}") String skillCode,@NotBlank @Size(max=600) String description) {}
    public record TaskInput(@NotBlank @Size(max=160) String title,@NotBlank @Size(max=6000) String instructions,@NotNull Difficulty difficulty,@NotNull @Size(max=10) List<@NotNull @Valid ObjectiveInput> objectives) {}
    public record TopicInput(@NotBlank @Size(max=160) String title,@NotNull @Size(max=20) List<@NotNull @Valid TaskInput> tasks) {}
    public record Update(@Min(0) long version,@NotBlank @Size(max=160) String title,@NotNull @Size(max=2000) String description,@NotNull Difficulty difficulty,@NotNull @Size(max=20) List<@NotNull @Valid TopicInput> topics) {}
    public record Version(@Min(0) long version) {}
    public record ObjectiveView(UUID id,String skillCode,String description,int position) {}
    public record TaskView(UUID id,String title,String instructions,String difficulty,int position,List<ObjectiveView> objectives) {}
    public record TopicView(UUID id,String title,int position,List<TaskView> tasks) {}
    public record Summary(UUID id,String title,String description,String difficulty,String status,long version,Instant createdAt) {}
    public record View(UUID id,String title,String description,String difficulty,String status,long version,Instant createdAt,List<TopicView> topics) {}
    public record Assignment(UUID id,UUID groupId) {}
    public record PageResult<T>(List<T> items,int page,int size,long total) {}
}
