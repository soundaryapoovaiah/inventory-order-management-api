package microservices.postgresql.repository;

import microservices.postgresql.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByCustomerCustomerIdOrderByCreatedAtDesc(Long customerId);
}