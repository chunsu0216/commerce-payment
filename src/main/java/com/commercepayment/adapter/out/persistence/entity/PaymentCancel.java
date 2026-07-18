package com.commercepayment.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "payment_cancel",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_cancel_id",
                        columnNames = "cancel_id"
                ),
                @UniqueConstraint(
                        name = "uk_payment_cancel_request_id",
                        columnNames = "cancel_request_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_payment_cancel_payment_id",
                        columnList = "payment_id"
                ),
                @Index(
                        name = "idx_payment_cancel_status_updated_at",
                        columnList = "cancel_status, updated_at"
                )

        }

)

@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class PaymentCancel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cancel_id", nullable = false, length = 36)

    private String cancelId;

    @Column(name = "payment_id", nullable = false, length = 36)

    private String paymentId;

    @Column(name = "cancel_request_id", nullable = false, length = 100)

    private String cancelRequestId;

    @Enumerated(EnumType.STRING)

    @Column(name = "cancel_type", nullable = false, length = 30)

    private CancelType cancelType;

    @Enumerated(EnumType.STRING)

    @Column(name = "cancel_status", nullable = false, length = 30)

    private CancelStatus cancelStatus;

    @Column(name = "cancel_amount", nullable = false)

    private Long cancelAmount;

    @Column(name = "cancel_reason", length = 500)

    private String cancelReason;

    @Column(name = "pg_cancel_id", length = 100)

    private String pgCancelId;

    @Column(name = "pg_result_code", length = 50)

    private String pgResultCode;

    @Column(name = "pg_result_message", length = 500)

    private String pgResultMessage;

    @Column(name = "requested_at")

    private LocalDateTime requestedAt;

    @Column(name = "cancelled_at")

    private LocalDateTime cancelledAt;

    public PaymentCancel(

            String cancelId,

            String paymentId,

            String cancelRequestId,

            CancelType cancelType,

            Long cancelAmount,

            String cancelReason

    ) {

        this.cancelId = cancelId;

        this.paymentId = paymentId;

        this.cancelRequestId = cancelRequestId;

        this.cancelType = cancelType;

        this.cancelAmount = cancelAmount;

        this.cancelReason = cancelReason;

        this.cancelStatus = CancelStatus.PROCESSING;

        this.requestedAt = LocalDateTime.now();

    }

    public void success(

            String pgCancelId,

            String pgResultCode,

            String pgResultMessage

    ) {

        this.cancelStatus = CancelStatus.SUCCESS;

        this.pgCancelId = pgCancelId;

        this.pgResultCode = pgResultCode;

        this.pgResultMessage = pgResultMessage;

        this.cancelledAt = LocalDateTime.now();

    }

    public void fail(

            String pgResultCode,

            String pgResultMessage

    ) {

        this.cancelStatus = CancelStatus.FAILED;

        this.pgResultCode = pgResultCode;

        this.pgResultMessage = pgResultMessage;

    }

    public void unknown(

            String pgResultCode,

            String pgResultMessage

    ) {

        this.cancelStatus = CancelStatus.UNKNOWN;

        this.pgResultCode = pgResultCode;

        this.pgResultMessage = pgResultMessage;

    }

}
