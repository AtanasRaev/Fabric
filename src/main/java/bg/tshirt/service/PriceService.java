package bg.tshirt.service;

import bg.tshirt.database.entity.enums.Type;

public interface PriceService {
    boolean isEmpty();

    void saveDefaultPrices();

    Double getPriceByType(Type type);
}
