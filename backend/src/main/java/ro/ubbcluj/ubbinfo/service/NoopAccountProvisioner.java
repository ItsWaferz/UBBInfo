package ro.ubbcluj.ubbinfo.service;

import org.springframework.stereotype.Component;

/**
 * Active provisioner for now: creates NO external mailbox — only the in-app
 * academic account is provisioned. Replace/augment with a Graph-based one when
 * Exchange access is available.
 */
@Component
public class NoopAccountProvisioner implements ExternalAccountProvisioner {

    @Override
    public ProvisionResult provision(ProvisionRequest request) {
        return new ProvisionResult(false, "Cont academic creat (mailbox Exchange dezactivat momentan)");
    }
}
