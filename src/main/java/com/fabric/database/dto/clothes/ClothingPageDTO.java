package com.fabric.database.dto.clothes;

import com.fabric.database.entity.enums.Type;

import java.util.List;

public class ClothingPageDTO extends ClothingBaseDTO {
    private String description;

    private Double discountPrice;

    private Type type;

   private List<ImagePageDTO> images;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(Double discountPrice) {
        this.discountPrice = discountPrice;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<ImagePageDTO> getImages() {
        return images;
    }

    public void setImages(List<ImagePageDTO> images) {
        this.images = images;
    }
}
