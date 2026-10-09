package az.hacktrain.auth;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
/** Replay only a bounded JSON body; Tomcat form limits do not cover JSON. */
final class BoundedRequest extends HttpServletRequestWrapper {
    private final byte[] body;
    BoundedRequest(HttpServletRequest request, byte[] body) { super(request); this.body=body; }
    @Override public int getContentLength() { return body.length; }
    @Override public long getContentLengthLong() { return body.length; }
    @Override public ServletInputStream getInputStream() {
        var input=new ByteArrayInputStream(body);
        return new ServletInputStream() {
            public int read() { return input.read(); }
            public int read(byte[] bytes,int offset,int length) { return input.read(bytes,offset,length); }
            public boolean isFinished() { return input.available()==0; }
            public boolean isReady() { return true; }
            public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException("Synchronous auth request"); }
        };
    }
    @Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8)); }
}
