package bg.tshirt.service.impl;

import bg.tshirt.database.entity.Price;
import bg.tshirt.database.entity.enums.Type;
import bg.tshirt.database.repository.PriceRepository;
import bg.tshirt.exceptions.NotFoundException;
import bg.tshirt.service.PriceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PriceServiceImpl implements PriceService {
    private final PriceRepository priceRepository;

    public PriceServiceImpl(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    @Override
    public boolean isEmpty() {
        return this.priceRepository.count() == 0;
    }

    @Override
    public void saveDefaultPrices() {
        List<Price> prices = List.of(
                new Price(Type.T_SHIRT, 29.00),
                new Price(Type.SWEATSHIRT, 54.00),
                new Price(Type.KIT, 59.00),
                new Price(Type.SHORTS, 30.00),
                new Price(Type.LONG_T_SHIRT, 37.00)
        );

        this.priceRepository.saveAll(prices);
    }

    @Override
    public Double getPriceByType(Type type) {
        return this.priceRepository.findByType(type)
                .map(Price::getPrice)
                .orElseGet(() -> this.priceRepository.findByType(type)
                        .map(Price::getPrice)
                        .orElseThrow(() -> new NotFoundException("Price not found for type: " + type)));
    }
}
