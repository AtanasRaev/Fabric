package bg.tshirt.service;

import bg.tshirt.database.dto.clothes.*;
import bg.tshirt.database.entity.OrderItem;
import bg.tshirt.database.entity.enums.Category;
import bg.tshirt.database.entity.enums.Type;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ClothingService {
    boolean addClothing(ClothingDTO clothAddDTO);

    ClothingDetailsPageDTO findById(Long id);

    boolean editClothing(ClothingEditDTO clothAddDTO, Long id);

    Page<ClothingPageDTO> findByQuery(Pageable pageable, String query);

    Page<ClothingPageDTO> findByQuery(Pageable pageable, String query, List<String> type);

    Page<ClothingPageDTO> findByCategory(Pageable pageable, List<String> category);

    Page<ClothingPageDTO> findByType(Pageable pageable, String type);

    Page<ClothingPageDTO> findByTypeAndCategory(Pageable pageable, String type, List<String> category);

    void setTotalSales(List<OrderItem> items);

    Page<ClothingPageDTO> getAllPage(Pageable pageable);

    boolean delete(Long id);

    Map<Category, Long> getClothingCountByCategories(String type);

    Map<Type, Double> getPrices();

    int updatePrices(String type, ClothingPriceEditDTO clothingPriceEditDTO);

    Map<Type, Double> getDiscountPrices();
}
