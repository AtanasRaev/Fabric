package bg.tshirt.web;

import java.util.List;
import java.util.Map;

import bg.tshirt.database.dto.clothes.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import bg.tshirt.database.dto.user.UserDTO;
import bg.tshirt.exceptions.NotFoundException;
import bg.tshirt.service.ClothingService;
import bg.tshirt.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/clothes")
public class ClothingController {
    private final UserService userService;
    private final ClothingService clothingService;

    public ClothingController(UserService userService,
                              ClothingService clothService) {
        this.userService = userService;
        this.clothingService = clothService;
    }

    @GetMapping("/prices")
    public ResponseEntity<?> getPrices() {
        return ResponseEntity.ok(this.clothingService.getPrices());
    }

    @GetMapping("/prices-discount")
    public ResponseEntity<?> getDiscountPrices() {
        return ResponseEntity.ok(this.clothingService.getDiscountPrices());
    }

    @PutMapping("/prices")
    public ResponseEntity<?> updatePrices(@RequestParam String type,
                                          @RequestBody @Valid ClothingPriceEditDTO clothingPriceEditDTO,
                                          HttpServletRequest request) {
        UserDTO admin = this.userService.validateAdmin(request);

        int updatedRows = this.clothingService.updatePrices(type, clothingPriceEditDTO);

        if (updatedRows == 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "No matching clothes found to update."));
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Clothes price updated successfully!",
                "added_by", admin.getEmail(),
                "updated_count", updatedRows
        ));
    }


    @PostMapping("/add")
    public ResponseEntity<?> addCloth(@ModelAttribute @Valid ClothingValidationDTO clothDTO,
                                      HttpServletRequest request) {
        UserDTO admin = this.userService.validateAdmin(request);

        if (!this.clothingService.addClothing(clothDTO)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Clothing with this model and type already exists."));
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Clothing added successfully!",
                "clothing_name", clothDTO.getName(),
                "added_by", admin.getEmail()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getClothById(@PathVariable("id") Long id) {
        if (id == null || id < 1) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Id must be a positive number"
            ));
        }
        ClothingDetailsPageDTO dto = this.clothingService.findById(id);

        if (dto == null) {
            throw new NotFoundException("Clothing not found in the system.");
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "clothing", dto
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editClothById(@PathVariable("id") Long id,
                                           @ModelAttribute @Valid ClothingEditValidationDTO clothDto,
                                           HttpServletRequest request) {
        if (id == null || id < 1) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Id must be a positive number"
            ));
        }

        UserDTO admin = this.userService.validateAdmin(request);

        if (!this.clothingService.editClothing(clothDto, id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", String.format("Clothing with id: %d has no images", id)));
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Clothing edited successfully!",
                "clothing_name", clothDto.getName(),
                "edited_by", admin.getEmail()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClothById(@PathVariable("id") Long id,
                                             HttpServletRequest request) {
        if (id == null || id < 1) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Id must be a positive number"
            ));
        }

        UserDTO admin = this.userService.validateAdmin(request);

        if (!this.clothingService.delete(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", String.format("Clothing with id: %d was not found", id)));
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Clothing with id: " + id + " was deleted successfully!",
                "deleted_by", admin.getEmail()
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCloth(@RequestParam(name = "name") String name,
                                         @RequestParam(name = "type", required = false) List<String> type,
                                         @RequestParam(name = "sort", required = false) String sort,
                                         @RequestParam(defaultValue = "10") @Min(4) @Max(100) int size,
                                         @RequestParam(defaultValue = "1") @Min(1) int page) {

        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Query parameter 'name' cannot be empty"
            ));
        }

        Pageable pageable = getPageableSearch(page, size, sort);
        Page<ClothingPageDTO> clothesPage = getSearchPage(pageable, name, type);

        return buildPagedResponse(clothesPage);
    }

    @GetMapping("/catalog")
    public ResponseEntity<?> getCatalog(@RequestParam(defaultValue = "most-sold") String sort,
                                        @RequestParam(required = false) String type,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(defaultValue = "10") @Min(4) @Max(100) int size,
                                        @RequestParam(defaultValue = "1") @Min(1) int page) {

        Pageable pageable = getPageable(page, size, sort);
        Page<ClothingPageDTO> clothesPage = getClothesPage(pageable, type, category);

        return buildPagedResponse(clothesPage);
    }

    @GetMapping("/category")
    public ResponseEntity<?> getCategories(@RequestParam String type) {
        return ResponseEntity.ok(this.clothingService.getClothingCountByCategories(type));
    }

    private Pageable getPageableSearch(int page, int size, String sort) {
        return StringUtils.hasText(sort) ? getPageable(page, size, sort) : PageRequest.of(page - 1, size);
    }

    private Page<ClothingPageDTO> getSearchPage(Pageable pageable, String name, List<String> type) {
        if (type != null && !type.isEmpty() && StringUtils.hasText(name)) {
            return this.clothingService.findByQuery(pageable, name, type);
        } else {
            return this.clothingService.findByQuery(pageable, name);
        }
    }

    private Pageable getPageable(int page, int size, String sort) {
        Sort.Direction direction = Sort.Direction.DESC;
        String sortBy;

        switch (sort) {
            case "most-sold" -> sortBy = "saleCount";
            case "new" -> sortBy = "id";
            case "price_asc" -> {
                sortBy = "price";
                direction = Sort.Direction.ASC;
            }
            case "price_desc" -> sortBy = "price";
            case "name_asc" -> {
                sortBy = "name";
                direction = Sort.Direction.ASC;
            }
            default -> sortBy = "name";
        }

        return PageRequest.of(page - 1, size, Sort.by(direction, sortBy));
    }

    private Page<ClothingPageDTO> getClothesPage(Pageable pageable, String type, String category) {
        if (StringUtils.hasText(type) && category != null && !category.isEmpty()) {
            return this.clothingService.findByTypeAndCategory(pageable, type, category);
        } else if (StringUtils.hasText(type)) {
            return this.clothingService.findByType(pageable, type);
        } else if (category != null && !category.isEmpty()) {
            return this.clothingService.findByCategory(pageable, category);
        } else {
            return this.clothingService.getAllPage(pageable);
        }
    }

    private ResponseEntity<?> buildPagedResponse(Page<?> clothPage) {
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "items_on_page", clothPage.getNumberOfElements(),
                "total_items", clothPage.getTotalElements(),
                "total_pages", clothPage.getTotalPages(),
                "current_page", clothPage.getNumber() + 1,
                "clothes", clothPage.getContent()
        ));
    }
}
