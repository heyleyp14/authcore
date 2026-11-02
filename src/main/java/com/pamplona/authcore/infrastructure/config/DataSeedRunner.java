package com.pamplona.authcore.infrastructure.config;

import com.pamplona.authcore.application.ports.out.PasswordHasherPort;
import com.pamplona.authcore.application.ports.out.UserRepositoryPort;
import com.pamplona.authcore.domain.Role;
import com.pamplona.authcore.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seed de desarrollo: crea 10 usuarios de ejemplo SOLO si la tabla de usuarios
 * esta vacia. Corre antes de AdminBootstrapRunner para que el admin del seed
 * sea el mismo que ese runner espera y no se duplique.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "authcore.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeedRunner.class);

    private static final String CLOUDINARY_BASE =
            "https://res.cloudinary.com/ferreteria-murano3d/image/upload/v1/users/";

    /** Retrato real del admin (subido a la cuenta de Cloudinary del proyecto); los slugs no existen como archivos. */
    private static final String ADMIN_IMAGE_URL =
            "https://res.cloudinary.com/rln6jzqz/image/upload/v1793580170/kg7bkkni46siyo835blb.jpg";

    private static final List<SeedUser> DEMO_USERS = List.of(
            new SeedUser("admin", "admin123", Set.of(Role.ADMIN, Role.USER), "admin"),
            new SeedUser("maria.gomez", "Demo1234", Set.of(Role.USER), "maria-gomez"),
            new SeedUser("juan.perez", "Demo1234", Set.of(Role.USER), "juan-perez"),
            new SeedUser("ana.rodriguez", "Demo1234", Set.of(Role.USER), "ana-rodriguez"),
            new SeedUser("carlos.mendoza", "Demo1234", Set.of(Role.USER), "carlos-mendoza"),
            new SeedUser("lucia.fernandez", "Demo1234", Set.of(Role.USER), "lucia-fernandez"),
            new SeedUser("diego.ramirez", "Demo1234", Set.of(Role.USER), "diego-ramirez"),
            new SeedUser("sofia.torres", "Demo1234", Set.of(Role.USER), "sofia-torres"),
            new SeedUser("miguel.vargas", "Demo1234", Set.of(Role.USER), "miguel-vargas"),
            new SeedUser("valentina.rojas", "Demo1234", Set.of(Role.USER), "valentina-rojas")
    );

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;

    public DataSeedRunner(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.findAll().isEmpty()) {
            log.info("Seed omitido: ya existen usuarios en la base de datos.");
            return;
        }

        for (SeedUser seed : DEMO_USERS) {
            String imageUrl = "admin".equals(seed.username())
                    ? ADMIN_IMAGE_URL
                    : CLOUDINARY_BASE + seed.imageSlug() + ".png";
            User user = new User(null, seed.username(), passwordHasher.hash(seed.password()),
                    new HashSet<>(seed.roles()), imageUrl);
            userRepository.save(user);
        }

        log.warn("=================================================================");
        log.warn(" Seed de desarrollo creado: {} usuarios de ejemplo.", DEMO_USERS.size());
        log.warn("   admin / admin123  (ADMIN y USER)");
        log.warn("   resto de usuarios / Demo1234  (USER)");
        log.warn(" Desactiva el seed con authcore.seed.enabled=false en produccion.");
        log.warn("=================================================================");
    }

    private record SeedUser(String username, String password, Set<Role> roles, String imageSlug) {
    }
}
