package com.fabric.database.dto.clothes;

import com.fabric.database.entity.enums.Category;

public class ClothingDetailsPageDTO extends ClothingPageDTO {
    private Category category;

    public ClothingDetailsPageDTO() {
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

}
