package com.scaler.ecommerce.dao;

import com.scaler.ecommerce.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Long> {
    Category findByName(String name);

    @Query("select c.id as id, c.name as name, count(p) as productCount from Category c join c.products p group by c.id, c.name order by c.name")
    List<CatalogCategory> catalog();

    interface CatalogCategory {
        Long getId();
        String getName();
        long getProductCount();
    }

}
