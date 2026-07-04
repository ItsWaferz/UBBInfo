package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.TuitionPayment;

import java.util.List;
import java.util.UUID;

public interface TuitionPaymentRepository extends JpaRepository<TuitionPayment, UUID> {
    List<TuitionPayment> findByStudentId(UUID studentId);
    boolean existsByStudentIdAndChargeKey(UUID studentId, String chargeKey);
}
