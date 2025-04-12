package com.fabric.database.repository;

import com.fabric.database.dto.clothes.ClothingDiscountPriceDTO;
import com.fabric.database.dto.clothes.ClothingPriceDTO;
import com.fabric.database.entity.Clothing;
import com.fabric.database.entity.enums.Category;
import com.fabric.database.entity.enums.Type;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClothingRepository extends JpaRepository<Clothing, Long> {
    @Query("SELECT c FROM Clothing c WHERE c.id = :id AND c.selected = :selected ")
    Optional<Clothing> findByIdSelected(@Param("id") Long id, @Param("selected") boolean selected);

    Optional<Clothing> findByModelAndTypeAndCategory(String model, Type type, Category category);

    @Query("SELECT c FROM Clothing c " +
            "WHERE c.selected = TRUE AND c.id IN (" +
            "   SELECT MIN(c2.id) FROM Clothing c2 " +
            "   WHERE LOWER(c2.name) LIKE LOWER(:query) OR LOWER(c2.model) LIKE LOWER(:query) AND c2.selected = TRUE" +
            "   GROUP BY c2.model, c2.type" +
            ")")
    Page<Clothing> findByQuery(Pageable pageable, @Param("query") String query);

    @Query("SELECT c FROM Clothing c " +
            "WHERE c.selected = true " +
            "AND c.id IN (" +
            "    SELECT MIN(c2.id) FROM Clothing c2 " +
            "    WHERE (LOWER(c2.name) LIKE LOWER(:query) OR LOWER(c2.model) LIKE LOWER(:query)) " +
            "      AND LOWER(c2.type) IN (:type) " +
            "      AND c2.selected = true " +
            "    GROUP BY c2.model, c2.type" +
            ")")
    Page<Clothing> findByQueryAndType(Pageable pageable,
                                      @Param("query") String query,
                                      @Param("type") List<String> type);


    @Query("SELECT c FROM Clothing c WHERE c.selected = TRUE AND LOWER(c.category) LIKE LOWER(:category)")
    Page<Clothing> findByCategory(Pageable pageable, @Param("category") String category);

    @Query("SELECT c FROM Clothing c WHERE c.selected = TRUE AND LOWER(c.type) LIKE LOWER(:type)")
    Page<Clothing> findByType(Pageable pageable, @Param("type") String type);

    @Query("SELECT c FROM Clothing c WHERE c.selected = true AND LOWER(c.type) LIKE LOWER(:type) AND LOWER(c.category) LIKE LOWER(:category)")
    Page<Clothing> findByTypeAndCategory(Pageable pageable, @Param("type") String type, @Param("category") String category);

    @Query("SELECT c FROM Clothing c WHERE c.selected = true")
    Page<Clothing> findAllPage(Pageable pageable);

    List<Clothing> findByModel(String model);

    @Query("SELECT c.category FROM Clothing c WHERE c.selected = TRUE AND LOWER(c.type) LIKE LOWER(:type) GROUP BY c.category")
    List<Category> getCategoriesByType(String type);

    @Query("SELECT new com.fabric.database.dto.clothes.ClothingPriceDTO(c.type, c.price) " +
            "FROM Clothing c " +
            "WHERE c.id = (SELECT MIN(c2.id) FROM Clothing c2 WHERE c2.type = c.type) " +
            "AND c.type IN :types " +
            "ORDER BY c.type")
    List<ClothingPriceDTO> findPricesForTypes(@Param("types") List<Type> types);

    @Query("SELECT new com.fabric.database.dto.clothes.ClothingDiscountPriceDTO(c.type, c.discountPrice) " +
            "FROM Clothing c " +
            "WHERE c.id = (SELECT MIN(c2.id) FROM Clothing c2 WHERE c2.type = c.type) " +
            "AND c.type IN :types " +
            "ORDER BY c.type")
    List<ClothingDiscountPriceDTO> findDiscountPricesForTypes(@Param("types") List<Type> types);

    @Modifying
    @Query("UPDATE Clothing c SET c.price = :price, c.discountPrice = :discountPrice WHERE LOWER(c.type) LIKE LOWER(:type)")
    int bulkUpdatePrices(@Param("type") String type, @Param("price") double price, @Param("discountPrice") Double discountPrice);

    @Query("SELECT MIN(c.discountPrice) FROM Clothing c WHERE c.type = :type")
    Double findDiscountPriceByType(Type type);

    @Query("SELECT c.type, c.category FROM Clothing c WHERE c.selected = TRUE GROUP BY c.type, c.category")
    List<Object[]> findTypesAndCategories();
}
