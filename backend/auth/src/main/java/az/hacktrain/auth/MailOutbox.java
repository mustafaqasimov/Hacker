package az.hacktrain.auth;
/** Queue a message atomically with the caller's transaction. */
public interface MailOutbox {
    void enqueue(String recipient,String subject,String body);
}
