package dev.sameerkhan.payflow.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "payment_approvals")
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(nullable = false, length = 64)
    private String approver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Decision decision;

    @Column(length = 280)
    private String note;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    protected Approval() {
        // for JPA
    }

    Approval(Payment payment, String approver, Decision decision, String note, Instant decidedAt) {
        this.payment = payment;
        this.approver = approver;
        this.decision = decision;
        this.note = note;
        this.decidedAt = decidedAt;
    }

    public Long getId() {
        return id;
    }

    public String getApprover() {
        return approver;
    }

    public Decision getDecision() {
        return decision;
    }

    public String getNote() {
        return note;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
