package ch.sebpiller.iot.service;

import ch.sebpiller.iot.domain.Foo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service Interface for managing {@link Foo}.
 */
public interface FooService {

    /**
     * Save a foo.
     *
     * @param foo the entity to save.
     * @return the persisted entity.
     */
    Foo save(Foo foo);

    /**
     * Get all the foos.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<Foo> findAll(Pageable pageable);


    /**
     * Get the "id" foo.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<Foo> findOne(Long id);

    /**
     * Delete the "id" foo.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
