package com.commercepayment.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_id",
                        columnNames = "payment_id"
                ),
                @UniqueConstraint(
                        name = "uk_payment_auth_id",
                        columnNames = "auth_id"
                ),
                @UniqueConstraint(
                        name = "uk_payment_pg_transaction",
                        columnNames = {"pg_provider", "pg_transaction_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_payment_order_id",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_payment_member_id",
                        columnList = "member_id"
                ),
                @Index(
                        name = "idx_payment_status_updated_at",
                        columnList = "payment_status, updated_at"
                )
        }
)

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;

    @Column(name = "payment_id", nullable = false, length = 36)

    private String paymentId;

    @Column(name = "auth_id", length = 36)

    private String authId;

    @Column(name = "order_id", nullable = false, length = 36)

    private String orderId;

    @Column(name = "member_id", nullable = false)

    private Long memberId;

    @Enumerated(EnumType.STRING)

    @Column(name = "payment_method", nullable = false, length = 30)

    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)

    @Column(name = "payment_status", nullable = false, length = 30)

    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)

    @Column(name = "pg_provider", nullable = false, length = 30)

    private PgProvider pgProvider;

    @Column(name = "pg_transaction_id", length = 100)

    private String pgTransactionId;

    @Column(name = "payment_amount", nullable = false)

    private Long paymentAmount;

    @Column(name = "approved_amount", nullable = false)

    private Long approvedAmount;

    @Column(name = "cancelled_amount", nullable = false)

    private Long cancelledAmount;

    @Column(name = "pg_result_code", length = 50)

    private String pgResultCode;

    @Column(name = "pg_result_message", length = 500)

    private String pgResultMessage;

    @Column(name = "approved_at")

   private LocalDateTime approvedAt;

    @Column(name = "failed_at")

    private LocalDateTime failedAt;

    public Payment(

            String paymentId,

            String authId,

            String orderId,

            Long memberId,

            PaymentMethod paymentMethod,

            PgProvider pgProvider,

            Long paymentAmount

    ) {

        this.paymentId = paymentId;

        this.authId = authId;

        this.orderId = orderId;

        this.memberId = memberId;

        this.paymentMethod = paymentMethod;

        this.pgProvider = pgProvider;

        this.paymentAmount = paymentAmount;

        this.paymentStatus = PaymentStatus.PROCESSING;

        this.approvedAmount = 0L;

        this.cancelledAmount = 0L;

    }

    public void success(

            String pgTransactionId,

            Long approvedAmount,

            String pgResultCode,

            String pgResultMessage

    ) {

        this.paymentStatus = PaymentStatus.SUCCESS;

        this.pgTransactionId = pgTransactionId;

        this.approvedAmount = approvedAmount;

        this.pgResultCode = pgResultCode;

        this.pgResultMessage = pgResultMessage;

        this.approvedAt = LocalDateTime.now();

    }

    public void fail(

            String pgResultCode,

            String pgResultMessage

    ) {

        this.paymentStatus = PaymentStatus.FAILED;

        this.pgResultCode = pgResultCode;

        this.pgResultMessage = pgResultMessage;

        this.failedAt = LocalDateTime.now();

    }

    public void unknown(

            String pgResultCode,

            String pgResultMessage

    ) {

        this.paymentStatus = PaymentStatus.UNKNOWN;

        this.pgResultCode = pgResultCode;

        this.pgResultMessage = pgResultMessage;

    }

    public void cancel(Long cancelAmount) {

        this.cancelledAmount += cancelAmount;
        if (this.cancelledAmount.equals(this.approvedAmount)) {

            this.paymentStatus = PaymentStatus.CANCELLED;
            return;

        }
        this.paymentStatus = PaymentStatus.PARTIAL_CANCELLED;
    }

}
