package com.commercepayment.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "payment_outbox",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_outbox_event_id",
                        columnNames = "event_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_payment_outbox_status_created_at",
                        columnList = "status, created_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class PaymentOutbox extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;

    @Column(name = "event_id", nullable = false, length = 36)

    private String eventId;

    @Column(name = "aggregate_id", nullable = false, length = 36)

    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)

    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "json")

    private String payload;

    @Enumerated(EnumType.STRING)

    @Column(name = "status", nullable = false, length = 30)

    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)

    private Integer retryCount;

    @Column(name = "published_at")

    private LocalDateTime publishedAt;

    public PaymentOutbox(

            String eventId,

            String aggregateId,

            String eventType,

            String payload

    ) {

        this.eventId = eventId;

        this.aggregateId = aggregateId;

        this.eventType = eventType;

        this.payload = payload;

        this.status = OutboxStatus.PENDING;

        this.retryCount = 0;

    }

    public void published() {

        this.status = OutboxStatus.PUBLISHED;

        this.publishedAt = LocalDateTime.now();

    }

    public void fail() {

        this.status = OutboxStatus.FAILED;

        this.retryCount++;

    }

}
