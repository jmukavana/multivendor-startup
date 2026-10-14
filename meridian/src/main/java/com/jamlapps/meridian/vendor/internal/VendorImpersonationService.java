package com.jamlapps.meridian.vendor.internal;

import com.jamlapps.meridian.iam.ImpersonationRecord;
import com.jamlapps.meridian.iam.ImpersonationSessions;
import com.jamlapps.meridian.iam.IssuedImpersonationSession;
import com.jamlapps.meridian.vendor.internal.VendorMembershipService.Relationship;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-ADM-08: the "who may be impersonated" half of impersonation — {@code iam.
 * ImpersonationSessions} owns the session mechanics itself (see that interface's javadoc for the
 * full split) and knows nothing about vendors; this class is the one place that resolves a vendor
 * id to the account id actually impersonated (its owner, never a staff account — BR-ADM-08 says
 * "vendor account", and a vendor's staff are a per-vendor relationship, not a second principal
 * worth a support admin's session) and decides who may read the resulting history back.
 */
@Service
class VendorImpersonationService {

    private final VendorRepository vendors;
    private final VendorMembershipService membership;
    private final ImpersonationSessions impersonationSessions;

    VendorImpersonationService(VendorRepository vendors, VendorMembershipService membership, ImpersonationSessions impersonationSessions) {
        this.vendors = vendors;
        this.membership = membership;
        this.impersonationSessions = impersonationSessions;
    }

    @Transactional
    IssuedImpersonationSession impersonate(UUID vendorId, UUID adminAccountId, String reason) {
        VendorEntity vendor = vendors.findById(vendorId).orElseThrow(() -> ApiException.notFound("Vendor", vendorId));
        return impersonationSessions.issue(vendor.ownerAccountId(), adminAccountId, reason);
    }

    /**
     * BR-ADM-08's "visible to the vendor" — owner only, the same restriction {@code
     * PayoutDetailController.AddStaffRequest} already gives owner-level actions, not staff:
     * whether this account has been impersonated is the vendor principal's own security posture
     * to see, not a delegated staff permission.
     */
    @Transactional(readOnly = true)
    List<ImpersonationRecord> history(UUID vendorId, UUID callerAccountId) {
        VendorEntity vendor = vendors.findById(vendorId).orElseThrow(() -> ApiException.notFound("Vendor", vendorId));

        if (membership.relationshipOf(vendorId, callerAccountId) != Relationship.OWNER) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only the vendor owner may view impersonation history.");
        }

        return impersonationSessions.historyFor(vendor.ownerAccountId());
    }
}
