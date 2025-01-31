package ch.chiodonig.fotomap.repository;

import ch.chiodonig.fotomap.model.Role;
import ch.chiodonig.fotomap.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
