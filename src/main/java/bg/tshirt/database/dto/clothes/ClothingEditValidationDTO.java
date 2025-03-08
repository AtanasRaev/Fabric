package bg.tshirt.database.dto.clothes;

import java.util.ArrayList;
import java.util.List;

public class ClothingEditValidationDTO extends ClothingValidationDTO {
    private List<String> removedImages;

    public ClothingEditValidationDTO() {
        this.removedImages = new ArrayList<>();
    }

    public List<String> getRemovedImages() {
        return removedImages;
    }

    public void setRemovedImages(List<String> removedImages) {
        this.removedImages = removedImages;
    }
}
