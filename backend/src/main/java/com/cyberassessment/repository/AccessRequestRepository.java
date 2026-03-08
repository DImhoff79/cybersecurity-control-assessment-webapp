package com.cyberassessment.repository;

import com.cyberassessment.entity.AccessRequest;
import com.cyberassessment.entity.AccessRequestStatus;
import com.cyberassessment.entity.IdentityProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
    Optional<AccessRequest> findByProviderAndProviderSubject(IdentityProvider provider, String providerSubject);
    List<AccessRequest> findByStatusOrderByRequestedAtDesc(AccessRequestStatus status);
}
