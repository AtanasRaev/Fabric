package bg.tshirt.init;

import bg.tshirt.service.PriceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class init implements CommandLineRunner {
    private final PriceService priceService;

    public init(PriceService priceService) {
        this.priceService = priceService;
    }

    @Override
    public void run(String... args) {
        if (this.priceService.isEmpty()) {
            this.priceService.saveDefaultPrices();
        }
    }
}
