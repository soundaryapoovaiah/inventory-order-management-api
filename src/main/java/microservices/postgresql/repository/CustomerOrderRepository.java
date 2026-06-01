package microservices.postgresql.repository;

import microservices.postgresql.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByCustomerCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<CustomerOrder> findByIdempotencyKey(String idempotencyKey);
}