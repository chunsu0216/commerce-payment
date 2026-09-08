package com.commercepayment.adapter.out.persistence.repository;

import com.commercepayment.adapter.out.persistence.entity.CancelStatus;
import com.commercepayment.adapter.out.persistence.entity.PaymentCancel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * PaymentCancel 엔티티에 대한 Spring Data JPA 저장소
 */
public interface PaymentCancelJpaRepository extends JpaRepository<PaymentCancel, Long> {

    Optional<PaymentCancel> findByCancelId(String cancelId);

    List<PaymentCancel> findByCancelStatusInAndUpdatedAtBeforeAndRetryCountLessThan(
            Collection<CancelStatus> cancelStatuses, LocalDateTime updatedBefore, int retryCount, Pageable pageable
    );
}
