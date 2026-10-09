package az.hacktrain.auth;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
final class ProblemWriter {
    private static final ObjectMapper JSON=new ObjectMapper();
    private ProblemWriter() {}
    static void write(HttpServletResponse response, int status, String code, String detail) throws IOException {
        response.setStatus(status); response.setContentType("application/problem+json"); response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control","no-store");
        JSON.writeValue(response.getWriter(),Map.of("type","urn:hacktrain:problem:"+code,"title",code,"status",status,"detail",detail));
    }
}
