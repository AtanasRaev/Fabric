package bg.tshirt.database.dto.clothes;

import bg.tshirt.database.entity.enums.Category;

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
