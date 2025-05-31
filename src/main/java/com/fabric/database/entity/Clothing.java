package com.fabric.database.entity;

import com.fabric.database.entity.enums.Category;
import com.fabric.database.entity.enums.Type;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clothes")
public class Clothing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private double price;

    @Column
    private Double discountPrice;

    @Column(nullable = false)
    private String model;

    @Column
    @Enumerated(EnumType.STRING)
    private Type type;

    @Column
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(nullable = false)
    private int saleCount = 0;

    @Column(nullable = true)
    private boolean selected;

    @OneToMany(mappedBy = "cloth",
            fetch = FetchType.EAGER)
    private List<Image> images;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "clothing_tags",
            joinColumns = @JoinColumn(name = "clothing_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags = new ArrayList<>();

    public Clothing(String name, String description, double price, String model, Type type, Category category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.model = model;
        this.type = type;
        this.images = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.category = category;
        this.selected = true;
    }

    public Clothing() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Double getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(Double discountPrice) {
        this.discountPrice = discountPrice;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public int getSaleCount() {
        return saleCount;
    }

    public void setSaleCount(int saleCount) {
        this.saleCount = saleCount;
    }

    public void updateTotalSales() {
        this.saleCount += 1;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
        tag.getClothes().add(this);
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
        tag.getClothes().remove(this);
    }
}
