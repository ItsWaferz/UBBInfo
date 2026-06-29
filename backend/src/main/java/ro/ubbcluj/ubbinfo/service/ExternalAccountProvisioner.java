package ro.ubbcluj.ubbinfo.service;

/**
 * Extension point for provisioning an EXTERNAL account (e.g. a Microsoft
 * Exchange / stud.ubbcluj.ro mailbox via Microsoft Graph) alongside the in-app
 * academic account.
 *
 * <p>For now only the {@link NoopAccountProvisioner} is active ("academic
 * accounts only"). When the faculty grants Graph/Entra access, add a
 * {@code GraphAccountProvisioner} (MSAL4J + Microsoft Graph) gated by
 * {@code app.provisioning.exchange.enabled=true} — the import flow stays the same.</p>
 */
public interface ExternalAccountProvisioner {

    ProvisionResult provision(ProvisionRequest request);

    record ProvisionRequest(String email, String fullName, String cnp) {
    }

    record ProvisionResult(boolean done, String message) {
    }
}
