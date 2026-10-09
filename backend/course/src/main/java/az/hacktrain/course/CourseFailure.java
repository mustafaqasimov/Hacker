package az.hacktrain.course;
import org.springframework.http.HttpStatus;
final class CourseFailure extends RuntimeException {
    final HttpStatus status;
    CourseFailure(HttpStatus status,String message) { super(message); this.status=status; }
    static CourseFailure missing() { return new CourseFailure(HttpStatus.NOT_FOUND,"Kurs tapılmadı."); }
    static CourseFailure forbidden() { return new CourseFailure(HttpStatus.FORBIDDEN,"Bu əməliyyat üçün müəllim və ya administrator rolu lazımdır."); }
    static CourseFailure conflict(String message) { return new CourseFailure(HttpStatus.CONFLICT,message); }
}
