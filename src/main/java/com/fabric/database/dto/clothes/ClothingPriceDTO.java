package com.fabric.database.dto.clothes;

import com.fabric.database.entity.enums.Type;

public class ClothingPriceDTO {
    private Type type;
    private Double price;

    public ClothingPriceDTO(Type type, double price) {
        this.type = type;
        this.price = price;
    }

    public Type getType() {
        return type;
    }

    public double getPrice() {
        return price;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}
