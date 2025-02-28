package bg.tshirt.database.repository;

import bg.tshirt.database.entity.Price;
import bg.tshirt.database.entity.enums.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PriceRepository extends JpaRepository<Price, Long> {
    Optional<Price> findByType(Type type);
}
