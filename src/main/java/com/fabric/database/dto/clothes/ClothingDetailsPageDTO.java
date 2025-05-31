package com.fabric.database.dto.clothes;

import com.fabric.database.entity.enums.Category;

import java.util.List;

public class ClothingDetailsPageDTO extends ClothingPageDTO {
    private Category category;

    private List<String> tags;

    public ClothingDetailsPageDTO() {
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
