package bg.tshirt.database.dto.clothes;

import java.util.ArrayList;
import java.util.List;

public class ClothingEditDTO extends ClothingDTO {
    private List<String> removedImages;

    public ClothingEditDTO() {
        this.removedImages = new ArrayList<>();
    }

    public List<String> getRemovedImages() {
        return removedImages;
    }

    public void setRemovedImages(List<String> removedImages) {
        this.removedImages = removedImages;
    }
}
