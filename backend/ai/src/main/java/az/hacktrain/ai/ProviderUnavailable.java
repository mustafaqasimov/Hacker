package az.hacktrain.ai;
/** Safe operational reason; never contains provider bodies, credentials or learner data. */
final class ProviderUnavailable extends RuntimeException {
    enum Reason { CREDIT_BALANCE, AUTHENTICATION, RATE_LIMIT, UNAVAILABLE }
    final Reason reason;
    ProviderUnavailable(Reason reason) { super(reason.name());this.reason=reason; }
}
