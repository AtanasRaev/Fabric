package bg.tshirt.database.dto.clothes;

import bg.tshirt.database.entity.enums.Type;

public class ClothingDiscountPriceDTO {
    private Type type;

    private Double discountPrice;

    public ClothingDiscountPriceDTO(Type type, Double price) {
        this.type = type;
        this.discountPrice = price;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Double getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(Double discountPrice) {
        this.discountPrice = discountPrice;
    }
}
