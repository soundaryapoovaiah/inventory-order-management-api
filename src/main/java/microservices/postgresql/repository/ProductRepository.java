package microservices.postgresql.repository;

import microservices.postgresql.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryIgnoreCase(String category);

    List<Product> findByStockQuantityLessThan(Integer threshold);

    List<Product> findByNameContainingIgnoreCase(String name);
}