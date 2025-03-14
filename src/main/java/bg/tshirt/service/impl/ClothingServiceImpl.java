package bg.tshirt.service.impl;

import bg.tshirt.database.dto.clothes.*;
import bg.tshirt.database.entity.Clothing;
import bg.tshirt.database.entity.Image;
import bg.tshirt.database.entity.OrderItem;
import bg.tshirt.database.entity.enums.Category;
import bg.tshirt.database.entity.enums.Type;
import bg.tshirt.database.repository.ClothingRepository;
import bg.tshirt.exceptions.ClothingAlreadyExistsException;
import bg.tshirt.exceptions.NotFoundException;
import bg.tshirt.service.ClothingService;
import bg.tshirt.service.ImageService;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class ClothingServiceImpl implements ClothingService {
    private final ClothingRepository clothingRepository;
    private final ImageService imageService;
    private final ModelMapper modelMapper;

    public ClothingServiceImpl(ClothingRepository clothRepository,
                               ImageService imageService,
                               ModelMapper modelMapper) {
        this.clothingRepository = clothRepository;
        this.imageService = imageService;
        this.modelMapper = modelMapper;
    }

    @Override
    public boolean addClothing(ClothingValidationDTO clothingDTO) {
        Optional<Clothing> optional = this.clothingRepository.findByModelAndType(clothingDTO.getModel(), clothingDTO.getType());

        if (optional.isPresent()) {
            return false;
        }

        Clothing clothing = new Clothing(clothingDTO.getName(),
                clothingDTO.getDescription(),
                clothingDTO.getPrice(),
                clothingDTO.getModel().substring(0, 3),
                clothingDTO.getType(),
                clothingDTO.getCategory());

        List<Image> images = new ArrayList<>();
        addNewImages(clothingDTO, clothing, images);
        clothing.setImages(images);

        this.clothingRepository.save(clothing);
        this.imageService.saveAll(images);
        return true;
    }

    @Override
    public ClothingDetailsPageDTO findById(Long id) {
        Optional<ClothingDetailsPageDTO> optional = this.clothingRepository.findById(id)
                .map(clothing -> this.modelMapper.map(clothing, ClothingDetailsPageDTO.class));

        if (optional.isEmpty()) {
            return null;
        }

        ClothingDetailsPageDTO clothing = optional.get();

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
    public boolean editClothing(ClothingEditValidationDTO clothingDTO, Long id) {
        Clothing clothing = this.clothingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Clothing with id: %d is not found", id)));

        if (isInvalidUpdate(clothingDTO, clothing)) {
            return false;
        }

        Optional<Clothing> byModelAndType = this.clothingRepository.findByModelAndType(clothingDTO.getModel(), clothingDTO.getType());
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
    public Page<ClothingPageDTO> findByQuery(Pageable pageable, String query) {
        return this.clothingRepository.findByQuery(pageable, "%" + query + "%")
                .map(clothing -> this.modelMapper.map(clothing, ClothingPageDTO.class));
    }

    @Override
    public Page<ClothingPageDTO> findByQuery(Pageable pageable, String query, List<String> type) {
        return this.clothingRepository.findByQueryAndType(pageable, "%" + query + "%", type.stream().map(String::toLowerCase).toList())
                .map(clothing -> this.modelMapper.map(clothing, ClothingPageDTO.class));
    }

    @Override
    public Page<ClothingPageDTO> findByCategory(Pageable pageable, List<String> category) {
        return this.clothingRepository.findByCategory(pageable, category.stream().map(String::toLowerCase).toList())
                .map(clothing -> this.modelMapper.map(clothing, ClothingPageDTO.class));
    }

    @Override
    public Page<ClothingPageDTO> findByType(Pageable pageable, String type) {
        return this.clothingRepository.findByType(pageable, type)
                .map(clothing -> this.modelMapper.map(clothing, ClothingPageDTO.class));
    }

    @Override
    public Page<ClothingPageDTO> findByTypeAndCategory(Pageable pageable, String type, List<String> category) {
        return this.clothingRepository.findByTypeAndCategory(pageable, type, category.stream().map(String::toLowerCase).toList())
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
    public Page<ClothingPageDTO> getAllPage(Pageable pageable) {
        return this.clothingRepository.findAllPage(pageable)
                .map(clothing -> this.modelMapper.map(clothing, ClothingPageDTO.class));
    }

    @Transactional
    @Override
    public boolean delete(Long id) {
        Optional<Clothing> optional = this.clothingRepository.findById(id);
        if (optional.isEmpty()) {
            return false;
        }
        Clothing clothing = optional.get();

        List<String> publicIds = clothing.getImages()
                .stream()
                .map(Image::getPublicId)
                .toList();
        this.imageService.deleteAll(publicIds);

        clothing.getImages().clear();

        this.clothingRepository.delete(clothing);
        return true;
    }


    @Override
    public List<Category> getClothingCountByCategories(String type) {
        return this.clothingRepository.countClothingByCategory(type);
    }

    @Override
    public Map<Type, Double> getPrices() {
        Map<Type, Double> defaultPrices = Map.of(
                Type.T_SHIRT, 29.00,
                Type.SWEATSHIRT, 54.00,
                Type.KIT, 59.00,
                Type.SHORTS, 30.00,
                Type.LONG_T_SHIRT, 37.00,
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

    @Override
    @Transactional
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

    private void addImagesToKit(ClothingPageDTO clothing, String model) {
        List<Clothing> byModel = this.clothingRepository.findByModel(model);
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

                            if (publicId.matches("\\d+_F")) {
                                firstFront.put(numericValue, imageDTO);
                            } else if (publicId.matches("\\d+_B")) {
                                firstBack.put(numericValue, imageDTO);
                            } else if (publicId.matches("\\d+K_F") && !publicId.matches(".*KT.*")) {
                                kFront.put(numericValue, imageDTO);
                            } else if (publicId.matches("\\d+K_B") && !publicId.matches(".*KT.*")) {
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

    private List<Image> processImages(ClothingEditValidationDTO clothDto, Clothing cloth) {
        List<String> removedImagesPaths = clothDto.getRemovedImages();
        List<Image> imagesToSave = new ArrayList<>();

        if (!removedImagesPaths.isEmpty()) {
            removeImages(removedImagesPaths);
        }

        List<String> existingPaths = cloth.getImages()
                .stream()
                .map(Image::getPath)
                .filter(path -> !removedImagesPaths.contains(path))
                .toList();

        existingPaths.forEach(path -> {
            Image image = this.imageService.findByPath(path);
            if (image != null) {
                imagesToSave.add(image);
            }
        });

        addNewImages(clothDto, cloth, imagesToSave);

        return imagesToSave;
    }

    private void removeImages(List<String> removedImagesPaths) {
        removedImagesPaths.forEach(path -> {
            Image image = this.imageService.findByPath(path);
            if (image != null) {
                this.imageService.deleteImage(image);
            }
        });
    }

    private void addNewImages(ClothingValidationDTO clothDto, Clothing cloth, List<Image> imagesToSave) {
        if (clothDto.getFrontImage() != null && !clothDto.getFrontImage().isEmpty()) {
            Image frontImage = this.imageService.saveImageInCloud(clothDto.getFrontImage(), cloth, "F");
            imagesToSave.add(frontImage);
        }

        if (clothDto.getBackImage() != null && !clothDto.getBackImage().isEmpty()) {
            Image backImage = this.imageService.saveImageInCloud(clothDto.getBackImage(), cloth, "B");
            imagesToSave.add(backImage);
        }
    }
}
