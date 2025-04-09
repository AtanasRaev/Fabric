package com.fabric.database.dto.clothes;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImagePageDTO {
    private String publicId;

    public String getSide() {
        char side = publicId.charAt(publicId.length() - 1);
        if (side == 'F') {
            return "front";
        }
        return "back";
    }

    @JsonProperty("path")
    public String getPath() {
        return "/" + this.publicId;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }
}
