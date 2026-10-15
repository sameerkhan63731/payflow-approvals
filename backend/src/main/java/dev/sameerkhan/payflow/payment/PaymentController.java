package dev.sameerkhan.payflow.payment;

import dev.sameerkhan.payflow.payment.dto.CreatePaymentRequest;
import dev.sameerkhan.payflow.payment.dto.DecisionRequest;
import dev.sameerkhan.payflow.payment.dto.PaymentResponse;
import dev.sameerkhan.payflow.payment.dto.StatusSummary;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('INITIATOR')")
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication auth) {
        PaymentService.CreateResult result = service.create(request, auth.getName(), idempotencyKey);
        if (!result.created()) {
            return ResponseEntity.ok(result.payment());
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(result.payment().id()).toUri();
        return ResponseEntity.created(location).body(result.payment());
    }

    @GetMapping
    public List<PaymentResponse> list(@RequestParam(required = false) PaymentStatus status, Authentication auth) {
        return service.list(status, auth.getName(), isApprover(auth));
    }

    @GetMapping("/summary")
    public StatusSummary summary() {
        return service.summary();
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable Long id, Authentication auth) {
        return service.get(id, auth.getName(), isApprover(auth));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('INITIATOR')")
    public PaymentResponse submit(@PathVariable Long id, Authentication auth) {
        return service.submit(id, auth.getName());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('APPROVER')")
    public PaymentResponse approve(@PathVariable Long id,
                                   @Valid @RequestBody(required = false) DecisionRequest body,
                                   Authentication auth) {
        return service.decide(id, auth.getName(), Decision.APPROVED, body == null ? null : body.note());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('APPROVER')")
    public PaymentResponse reject(@PathVariable Long id,
                                  @Valid @RequestBody DecisionRequest body,
                                  Authentication auth) {
        return service.decide(id, auth.getName(), Decision.REJECTED, body.note());
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse release(@PathVariable Long id, Authentication auth) {
        return service.release(id, auth.getName());
    }

    private static boolean isApprover(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_APPROVER".equals(a.getAuthority()));
    }
}
