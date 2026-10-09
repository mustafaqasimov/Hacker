package az.hacktrain.auth;
import org.springframework.http.HttpStatus;
public final class AuthFailure extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public AuthFailure(HttpStatus status, String code, String message) {
        super(message); this.status=status; this.code=code;
    }
    public HttpStatus status() { return status; }
    public String code() { return code; }
    static AuthFailure invalid() { return new AuthFailure(HttpStatus.UNAUTHORIZED,"invalid_credentials","Etibarsız və ya vaxtı bitmiş giriş məlumatı."); }
}
