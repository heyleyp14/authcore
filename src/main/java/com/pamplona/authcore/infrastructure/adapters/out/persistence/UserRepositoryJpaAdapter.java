package com.pamplona.authcore.infrastructure.adapters.out.persistence;

import com.pamplona.authcore.application.ports.out.Pagina;
import com.pamplona.authcore.application.ports.out.Paginacion;
import com.pamplona.authcore.application.ports.out.UserRepositoryPort;
import com.pamplona.authcore.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida: implementa el puerto UserRepositoryPort usando JPA.
 * Es el UNICO lugar donde se traduce entre User (dominio) y UserJpaEntity (infraestructura).
 */
@Component
public class UserRepositoryJpaAdapter implements UserRepositoryPort {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryJpaAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = new UserJpaEntity(
                user.getId(), user.getUsername(), user.getPasswordHash(), new HashSet<>(user.getRoles()),
                user.getImageUrl());
        UserJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Pagina<User> findAll(Paginacion paginacion) {
        Page<UserJpaEntity> page = jpaRepository.findAll(
                PageRequest.of(paginacion.pagina() - 1, paginacion.tamano()));
        return new Pagina<>(page.getContent().stream().map(this::toDomain).toList(), page.getTotalElements(),
                paginacion.pagina(), paginacion.tamano());
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getUsername(), entity.getPasswordHash(),
                new HashSet<>(entity.getRoles()), entity.getImageUrl());
    }
}
