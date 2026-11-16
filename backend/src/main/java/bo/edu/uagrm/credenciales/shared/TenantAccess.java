package bo.edu.uagrm.credenciales.shared;

import bo.edu.uagrm.credenciales.consent.ConsentRepository;
import bo.edu.uagrm.credenciales.identity.*;
import bo.edu.uagrm.credenciales.platform.*;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.tenant.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class TenantAccess {
    private final StudentAccountRepository accounts;
    private final ConsentRepository consents;
    private final TenantRepository tenants;
    private final GovernanceRepository governance;

    public TenantAccess(
            StudentAccountRepository a,
            ConsentRepository c,
            TenantRepository t,
            GovernanceRepository g) {
        accounts = a;
        consents = c;
        tenants = t;
        governance = g;
    }

    public UUID require(UserPrincipal p, String permission) {
        if (p.tenantId() == null
                || !p.enabled()
                || p.onboarding()
                || !p.permissions().contains(permission))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        active(p.tenantId());
        return p.tenantId();
    }

    public Tenant active(UUID tenant) {
        var t =
                tenants.findById(tenant)
                        .filter(Tenant::isActive)
                        .orElseThrow(TenantAccess::missing);
        if (governance
                .findById(tenant)
                .map(g -> g.getStatus() != OrganizationStatus.ACTIVE)
                .orElse(false)) throw missing();
        return t;
    }

    public StudentAccount own(UUID student, boolean lock) {
        return (lock ? accounts.findLockedById(student) : accounts.findById(student))
                .filter(StudentAccount::isActive)
                .orElseThrow(TenantAccess::missing);
    }

    public StudentAccount linked(UUID student, UUID tenant, boolean lock) {
        var a = own(student, lock);
        active(tenant);
        boolean permitted =
                lock
                        ? !consents.lockActive(student, tenant).isEmpty()
                        : consents.existsByStudentIdAndTenantIdAndUsedAtIsNotNullAndRevokedAtIsNull(
                                student, tenant);
        if (!permitted) throw missing();
        return a;
    }

    public static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    public static ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT);
    }
}
