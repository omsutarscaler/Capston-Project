package com.scaler.ecommerce.dao;

import com.scaler.ecommerce.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Product p where p.id = :id")
    java.util.Optional<Product> lockById(long id);

    Page<Product> findByTitle(String title, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            select p from Product p left join p.category c
            where (:category is null or c.id = :category)
            and (:search = '' or locate(lower(:search), lower(p.title)) > 0
                 or locate(lower(:search), lower(c.name)) > 0)
            """)
    Page<Product> browse(Long category, String search, Pageable pageable);
}
