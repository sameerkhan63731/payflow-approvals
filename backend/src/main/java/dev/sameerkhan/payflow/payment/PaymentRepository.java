package dev.sameerkhan.payflow.payment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Fetch approvals in the same query to avoid an N+1 when rendering the list.
    @EntityGraph(attributePaths = "approvals")
    List<Payment> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "approvals")
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    @EntityGraph(attributePaths = "approvals")
    Optional<Payment> findWithApprovalsById(Long id);

    Optional<Payment> findByCreatedByAndIdempotencyKey(String createdBy, String idempotencyKey);

    @Query("select p.status, count(p) from Payment p group by p.status")
    List<Object[]> countByStatus();
}
