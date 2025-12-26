package com.paseto.repository;

import com.paseto.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActive(Boolean active);

    List<Product> findByNameContainingIgnoreCase(String name);
}
