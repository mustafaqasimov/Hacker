package az.hacktrain.organization;
import org.springframework.http.HttpStatus;
final class OrganizationFailure extends RuntimeException {
    final HttpStatus status;
    final String code;
    OrganizationFailure(HttpStatus status,String code,String message) { super(message); this.status=status; this.code=code; }
    static OrganizationFailure missing() { return new OrganizationFailure(HttpStatus.NOT_FOUND,"not_found","Resurs tapılmadı."); }
    static OrganizationFailure forbidden() { return new OrganizationFailure(HttpStatus.FORBIDDEN,"forbidden","Bu əməliyyat üçün icazə yoxdur."); }
    static OrganizationFailure conflict(String message) { return new OrganizationFailure(HttpStatus.CONFLICT,"conflict",message); }
}
