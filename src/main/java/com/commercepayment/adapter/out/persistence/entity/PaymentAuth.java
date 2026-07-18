package com.commercepayment.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "payment_auth",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_auth_id",
                        columnNames = "auth_id"
                ),
                @UniqueConstraint(
                        name = "uk_payment_auth_pg",
                        columnNames = {"pg_provider", "pg_auth_key"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_payment_auth_order_id",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_payment_auth_member_id",
                        columnList = "member_id"
                )
        }
)

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAuth extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth_id", nullable = false, length = 36)
    private String authId;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pg_provider", nullable = false, length = 30)
    private PgProvider pgProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)

    private PaymentMethod paymentMethod;

    @Column(name = "pg_auth_key", nullable = false, length = 100)
    private String pgAuthKey;

    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_status", nullable = false, length = 30)
    private AuthStatus authStatus;

    @Column(name = "request_amount", nullable = false)
    private Long requestAmount;

    @Column(name = "pg_result_code", length = 50)
    private String pgResultCode;

    @Column(name = "pg_result_message", length = 500)
    private String pgResultMessage;

    @Column(name = "authenticated_at")
    private LocalDateTime authenticatedAt;

    public PaymentAuth(

            String authId,

            String orderId,

            Long memberId,

            PgProvider pgProvider,

            PaymentMethod paymentMethod,

            String pgAuthKey,

            String pgTransactionId,

            AuthStatus authStatus,

            Long requestAmount,

            String pgResultCode,

            String pgResultMessage,

            LocalDateTime authenticatedAt

    ) {

        this.authId = authId;

        this.orderId = orderId;

        this.memberId = memberId;

        this.pgProvider = pgProvider;

        this.paymentMethod = paymentMethod;

        this.pgAuthKey = pgAuthKey;

        this.pgTransactionId = pgTransactionId;

        this.authStatus = authStatus;

        this.requestAmount = requestAmount;

        this.pgResultCode = pgResultCode;

        this.pgResultMessage = pgResultMessage;

        this.authenticatedAt = authenticatedAt;

    }

}
