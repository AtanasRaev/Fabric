package com.fabric.service.impl;

import com.fabric.database.dto.clothes.*;
import com.fabric.database.entity.Clothing;
import com.fabric.database.entity.Image;
import com.fabric.database.entity.OrderItem;
import com.fabric.database.entity.enums.Category;
import com.fabric.database.entity.enums.Type;
import com.fabric.database.repository.ClothingRepository;
import com.fabric.exceptions.ClothingAlreadyExistsException;
import com.fabric.exceptions.ImageUploadFailedException;
import com.fabric.exceptions.NotFoundException;
import com.fabric.service.ClothingService;
import com.fabric.service.ImageService;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Service
public class ClothingServiceImpl implements ClothingService {
    private final ClothingRepository clothingRepository;
    private final ImageService imageService;
    private final ModelMapper modelMapper;

    public ClothingServiceImpl(ClothingRepository clothingRepository,
                               ImageService imageService,
                               ModelMapper modelMapper) {
        this.clothingRepository = clothingRepository;
        this.imageService = imageService;
        this.modelMapper = modelMapper;
    }

    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "categories", allEntries = true),
            @CacheEvict(value = "clothingQuery", allEntries = true)
    })
    public CompletableFuture<Boolean> addClothing(ClothingValidationDTO clothingDTO) {
        Optional<Clothing> optional = this.clothingRepository.findByModelAndTypeAndCategory(clothingDTO.getModel(), clothingDTO.getType(), clothingDTO.getCategory());

        if (optional.isPresent() && optional.get().isSelected()) {
            return CompletableFuture.completedFuture(false);
        }

        Clothing clothing;

        if (optional.isPresent() && !optional.get().isSelected()) {
            clothing = optional.get();
            clothing.setSelected(true);

            List<String> publicIds = clothing.getImages()
                    .stream()
                    .map(Image::getPublicId)
                    .toList();
            this.imageService.deleteAll(publicIds);

            clothing.getImages().clear();
        } else {
            clothing = new Clothing(clothingDTO.getName(),
                    clothingDTO.getDescription(),
                    clothingDTO.getPrice(),
                    clothingDTO.getModel().substring(0, 3),
                    clothingDTO.getType(),
                    clothingDTO.getCategory());
        }


        Double discountPriceByType = this.clothingRepository.findDiscountPriceByType(clothingDTO.getType());
        if (discountPriceByType != null) {
            clothing.setDiscountPrice(discountPriceByType);
        }

        List<Image> images = new ArrayList<>();


        CompletableFuture<String> cloudinaryUpload = CompletableFuture.supplyAsync(() -> {
            addNewImagesToCloud(clothingDTO, clothing);
            return "Cloudinary";
        });

        CompletableFuture<String> hostUpload = CompletableFuture.supplyAsync(() -> {
            addNewImagesToHost(clothingDTO, clothing);
            return "Host";
        });


        return CompletableFuture.allOf(hostUpload, cloudinaryUpload)
                .thenApply(successSource -> {
                    images.addAll(List.of(
                            new Image(this.imageService.getUniqueImageId(clothing, "F"), clothing),
                            new Image(this.imageService.getUniqueImageId(clothing, "B"), clothing)
                    ));

                    clothing.setImages(images);
                    this.clothingRepository.save(clothing);
                    this.imageService.saveAll(images);
                    return true;
                }).exceptionally(ex -> {
                    throw new ImageUploadFailedException("Both image uploads failed: " + ex.getMessage(), ex);
                });
    }

    @Override
    @Cacheable(value = "clothing", key = "#id")
    public ClothingDetailsPageDTO findById(Long id, String selected) {
        Optional<Clothing> optional;
        switch (selected) {
            case "all" -> optional = this.clothingRepository.findById(id);
            case "selected" -> optional = this.clothingRepository.findByIdSelected(id, true);
            default -> optional = this.clothingRepository.findByIdSelected(id, false);
        }

        if (optional.isEmpty()) {
            return null;
        }

        Optional<ClothingDetailsPageDTO> optionalDTO = optional
                .map(clothing -> this.modelMapper.map(clothing, ClothingDetailsPageDTO.class));


        if (optionalDTO.isEmpty()) {
            return null;
        }

        ClothingDetailsPageDTO clothing = optionalDTO.get();

        if (clothing.getType() != Type.KIT) {
            return clothing;
        }

        addImagesToKit(clothing, clothing.getModel());

        return clothing;
    }

    @Override
    public Clothing getClothingEntityById(Long id) {
        return this.clothingRepository.findById(id)
                .orElse(null);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "clothing", key = "#id"),
            @CacheEvict(value = "categories", allEntries = true),
            @CacheEvict(value = "clothingQuery", allEntries = true)
    })
    public boolean editClothing(ClothingEditValidationDTO clothingDTO, Long id) {
        Clothing clothing = this.clothingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Clothing with id: %d is not found", id)));

        if (isInvalidUpdate(clothingDTO, clothing)) {
            return false;
        }

        Optional<Clothing> byModelAndType = this.clothingRepository.findByModelAndTypeAndCategory(clothingDTO.getModel(), clothingDTO.getType(), clothingDTO.getCategory());
        if (byModelAndType.isPresent()) {
            if (clothing.getId() != byModelAndType.get().getId()) {
                throw new ClothingAlreadyExistsException("Clothing with model " + clothingDTO.getModel() + " already exists.");
            }
        }

        setClothDetails(clothing, clothingDTO);

        List<Image> updatedImages = processImages(clothingDTO, clothing);

        clothing.setImages(updatedImages);
        this.clothingRepository.save(clothing);
        this.imageService.saveAll(updatedImages);

        return !updatedImages.isEmpty();
    }

    @Override
    @Cacheable(
            value = "clothingQuery",
            key = "'findByQuery_' + #query + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()"
    )
    public Page<ClothingPageDTO> findByQuery(Pageable pageable, String query) {
        return this.clothingRepository.findByQuery(pageable, "%" + query + "%")
                .map(clothing -> this.modelMapper.map(clothing, ClothingPageDTO.class));
    }

    @Override
    @Cacheable(
            value = "clothingQuery",
            key = "'findByQueryWithType_' + #query + '_' + #type + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()"
    )
    public Page<ClothingPageDTO> findByQuery(Pageable pageable, String query, List<String> type) {
        List<String> lowerTypes = type.stream().map(String::toLowerCase).collect(Collectors.toList());
        return this.clothingRepository.findByQueryAndType(pageable, "%" + query + "%", lowerTypes)
                .map(clothing -> this.modelMapper.map(clothing, ClothingPageDTO.class));
    }

    @Override
    @Cacheable(
            value = "clothingQuery",
            key = "'findByCategory_' + #category + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()"
    )
    public Page<ClothingPageDTO> findByCategory(Pageable pageable, String category) {
        return this.clothingRepository.findByCategory(pageable, category)
                .map(clothing -> this.modelMapper.map(clothing, ClothingPageDTO.class));
    }

    @Override
    @Cacheable(
            value = "clothingQuery",
            key = "'findByType_' + #type + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()"
    )
    public Page<ClothingPageDTO> findByType(Pageable pageable, String type) {
        return this.clothingRepository.findByType(pageable, type)
                .map(clothing -> this.modelMapper.map(clothing, ClothingPageDTO.class));
    }

    @Override
    @Cacheable(
            value = "clothingQuery",
            key = "'findByTypeAndCategory_' + #type + '_' + #category + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()"
    )
    public Page<ClothingPageDTO> findByTypeAndCategory(Pageable pageable, String type, String category) {
        return this.clothingRepository.findByTypeAndCategory(pageable, type, category)
                .map(clothing -> this.modelMapper.map(clothing, ClothingPageDTO.class));
    }

    @Override
    public void setTotalSales(List<OrderItem> items) {
        Map<Long, Integer> clothingQuantityMap = items.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getClothing().getId(),
                        Collectors.summingInt(OrderItem::getQuantity)
                ));

        List<Clothing> allById = this.clothingRepository.findAllById(clothingQuantityMap.keySet());

        allById.forEach(clothing -> {
            int totalQuantity = clothingQuantityMap.get(clothing.getId());
            for (int i = 0; i < totalQuantity; i++) {
                clothing.updateTotalSales();
            }
        });

        this.clothingRepository.saveAll(allById);
    }

    @Override
    @Cacheable(
            value = "clothingQuery",
            key = "'getAllPage_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()"
    )
    public Page<ClothingPageDTO> getAllPage(Pageable pageable) {
        return this.clothingRepository.findAllPage(pageable)
                .map(clothing -> this.modelMapper.map(clothing, ClothingPageDTO.class));
    }

    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "clothing", key = "#id"),
            @CacheEvict(value = "clothingQuery", allEntries = true),
            @CacheEvict(value = "categories", allEntries = true),
    })
    public boolean remove(Long id) {
        Optional<Clothing> optional = this.clothingRepository.findById(id);
        if (optional.isEmpty()) {
            return false;
        }
        Clothing clothing = optional.get();
        clothing.setSelected(false);

        this.clothingRepository.save(clothing);

        return true;
    }


    @Override
    @Cacheable(value = "categories", key = "#type")
    public List<Category> getCategoriesByType(String type) {
        return this.clothingRepository.getCategoriesByType(type);
    }

    @Override
    public Map<Type, Double> getPrices() {
        Map<Type, Double> defaultPrices = Map.of(
                Type.T_SHIRT, 40.99,
                Type.SWEATSHIRT, 54.00,
                Type.KIT, 59.00,
                Type.SHORTS, 30.00,
                Type.LONG_T_SHIRT, 48.99,
                Type.TOWELS, 24.00,
                Type.BANDANAS, 12.00
        );

        if (this.clothingRepository.count() == 0) {
            return defaultPrices;
        }

        Map<Type, Double> prices = clothingRepository.findPricesForTypes(List.of(Type.values()))
                .stream()
                .collect(Collectors.toMap(ClothingPriceDTO::getType, ClothingPriceDTO::getPrice));

        for (Type type : Type.values()) {
            prices.putIfAbsent(type, defaultPrices.get(type));
        }

        return prices;
    }

    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "clothing", allEntries = true),
            @CacheEvict(value = "clothingQuery", allEntries = true)
    })
    public int updatePrices(String type, ClothingPriceEditDTO clothingPriceEditDTO) {
        if (clothingPriceEditDTO.getPrice() == null) {
            return 0;
        }

        return this.clothingRepository.bulkUpdatePrices(type, clothingPriceEditDTO.getPrice(), clothingPriceEditDTO.getDiscountPrice() != null ? clothingPriceEditDTO.getDiscountPrice() : null);
    }

    @Override
    public Map<Type, Double> getDiscountPrices() {
        List<ClothingDiscountPriceDTO> dtoList = clothingRepository.findDiscountPricesForTypes(List.of(Type.values()));
        Map<Type, Double> discountPrices = new HashMap<>();
        for (ClothingDiscountPriceDTO dto : dtoList) {
            discountPrices.put(dto.getType(), dto.getDiscountPrice());
        }
        return discountPrices;
    }

    @Override
    @Cacheable(value = "categories")
    public Map<Type, List<Category>> getAllCategories() {
        List<Object[]> results = this.clothingRepository.findTypesAndCategories();
        Map<Type, List<Category>> typeCategoriesMap = new HashMap<>();

        for (Object[] row : results) {
            Type type = (Type) row[0];
            Category category = (Category) row[1];
            typeCategoriesMap.computeIfAbsent(type, k -> new ArrayList<>()).add(category);
        }

        return typeCategoriesMap;
    }

    protected void addImagesToKit(ClothingPageDTO clothing, String model) {
        List<Clothing> byModel = new ArrayList<>();

        this.clothingRepository.findFirstByModelAndTypeOrderByIdAsc(model, Type.T_SHIRT).ifPresent(byModel::add);
        this.clothingRepository.findFirstByModelAndTypeOrderByIdAsc(model, Type.SHORTS).ifPresent(byModel::add);

        if (byModel.isEmpty()) {
            return;
        }

        Map<Integer, ImagePageDTO> firstFront = new LinkedHashMap<>();
        Map<Integer, ImagePageDTO> firstBack = new LinkedHashMap<>();
        Map<Integer, ImagePageDTO> kFront = new LinkedHashMap<>();
        Map<Integer, ImagePageDTO> kBack = new LinkedHashMap<>();

        byModel.forEach(foundClothing ->
                foundClothing.getImages().stream()
                        .map(image -> this.modelMapper.map(image, ImagePageDTO.class))
                        .forEach(imageDTO -> {
                            String publicId = imageDTO.getPublicId();
                            String numberPart = publicId.replaceAll("[^0-9]", "");
                            int numericValue = numberPart.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(numberPart);

                            if (publicId.matches("^(\\d+_F|\\d+[A-Z]+__F)$")) {
                                firstFront.put(numericValue, imageDTO);
                            } else if (publicId.matches("^(\\d+_B|\\d+[A-Z]+__B)$")) {
                                firstBack.put(numericValue, imageDTO);
                            } else if (publicId.matches("^\\d+K_F$|^\\d+[A-Z]+_K_F$")) {
                                kFront.put(numericValue, imageDTO);
                            } else if (publicId.matches("^\\d+K_B$|^\\d+[A-Z]+_K_B$")) {
                                kBack.put(numericValue, imageDTO);
                            }
                        })
        );

        clothing.getImages().addAll(firstFront.values());
        clothing.getImages().addAll(firstBack.values());
        clothing.getImages().addAll(kFront.values());
        clothing.getImages().addAll(kBack.values());
    }

    private boolean isInvalidUpdate(ClothingEditValidationDTO clothDto, Clothing cloth) {
        boolean frontAndBackImagesEmpty = clothDto.getFrontImage() != null && clothDto.getFrontImage().isEmpty()
                && clothDto.getBackImage() != null && clothDto.getBackImage().isEmpty();
        boolean removingAllImages = clothDto.getRemovedImages().size() >= cloth.getImages().size();

        return frontAndBackImagesEmpty && removingAllImages;
    }

    private void setClothDetails(Clothing clothing, ClothingEditValidationDTO clothingEditDTO) {
        clothing.setName(clothingEditDTO.getName());
        clothing.setDescription(clothingEditDTO.getDescription());
        clothing.setPrice(clothingEditDTO.getPrice());
        clothing.setModel(clothingEditDTO.getModel().substring(0, 3));
        clothing.setType(clothingEditDTO.getType());
        clothing.setCategory(clothingEditDTO.getCategory());
    }

    private List<Image> processImages(ClothingEditValidationDTO clothingDto, Clothing clothing) {
        List<String> removedImagesPublicId = clothingDto.getRemovedImages();
        List<Image> imagesToSave = new ArrayList<>();

        if (!removedImagesPublicId.isEmpty()) {
            removeImages(removedImagesPublicId);
        }

        List<String> existingPaths = clothing.getImages()
                .stream()
                .map(Image::getPublicId)
                .filter(publicId -> !removedImagesPublicId.contains(publicId))
                .toList();

        existingPaths.forEach(path -> {
            Image image = this.imageService.findByPublicId(path);
            if (image != null) {
                imagesToSave.add(image);
            }
        });

        CompletableFuture<String> cloudinaryUpload = CompletableFuture.supplyAsync(() -> {
            addNewImagesToCloud(clothingDto, clothing);
            return "Cloudinary";
        });

        CompletableFuture<String> hostUpload = CompletableFuture.supplyAsync(() -> {
            addNewImagesToHost(clothingDto, clothing, imagesToSave);
            return "Host";
        });

        CompletableFuture.allOf(hostUpload, cloudinaryUpload).join();

        return imagesToSave;
    }

    private void removeImages(List<String> removedImagesPaths) {
        removedImagesPaths.forEach(path -> {
            Image image = this.imageService.findByPublicId(path);
            if (image != null) {
                this.imageService.deleteImage(image);
            }
        });
    }

    private void addNewImagesToCloud(ClothingValidationDTO clothingValidationDTO, Clothing clothing) {
        if (clothingValidationDTO.getFrontImage() != null && !clothingValidationDTO.getFrontImage().isEmpty()) {
            this.imageService.saveImageInCloud(clothingValidationDTO.getFrontImage(), clothing, "F");
        }

        if (clothingValidationDTO.getBackImage() != null && !clothingValidationDTO.getBackImage().isEmpty()) {
            this.imageService.saveImageInCloud(clothingValidationDTO.getBackImage(), clothing, "B");
        }
    }

    private void addNewImagesToHost(ClothingValidationDTO clothingValidationDTO, Clothing clothing) {
        if (clothingValidationDTO.getFrontImage() != null && !clothingValidationDTO.getFrontImage().isEmpty()) {
            this.imageService.saveImageInHost(clothingValidationDTO.getFrontImage(), clothing, "F");
        }

        if (clothingValidationDTO.getBackImage() != null && !clothingValidationDTO.getBackImage().isEmpty()) {
            this.imageService.saveImageInHost(clothingValidationDTO.getBackImage(), clothing, "B");
        }
    }

    private void addNewImagesToHost(ClothingValidationDTO clothingValidationDTO, Clothing clothing, List<Image> imagesToSave) {
        if (clothingValidationDTO.getFrontImage() != null && !clothingValidationDTO.getFrontImage().isEmpty()) {
            this.imageService.saveImageInHost(clothingValidationDTO.getFrontImage(), clothing, "F");
            imagesToSave.add(new Image(this.imageService.getUniqueImageId(clothing, "F"), clothing));
        }

        if (clothingValidationDTO.getBackImage() != null && !clothingValidationDTO.getBackImage().isEmpty()) {
            this.imageService.saveImageInHost(clothingValidationDTO.getBackImage(), clothing, "B");
            imagesToSave.add(new Image(this.imageService.getUniqueImageId(clothing, "B"), clothing));
        }
    }
}
