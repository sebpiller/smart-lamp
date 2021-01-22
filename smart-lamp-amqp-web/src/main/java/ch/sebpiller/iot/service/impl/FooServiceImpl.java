package ch.sebpiller.iot.service.impl;

import ch.sebpiller.iot.service.FooService;
import ch.sebpiller.iot.domain.Foo;
import ch.sebpiller.iot.repository.FooRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Implementation for managing {@link Foo}.
 */
@Service
@Transactional
public class FooServiceImpl implements FooService {

    private final Logger log = LoggerFactory.getLogger(FooServiceImpl.class);

    private final FooRepository fooRepository;

    public FooServiceImpl(FooRepository fooRepository) {
        this.fooRepository = fooRepository;
    }

    @Override
    public Foo save(Foo foo) {
        log.debug("Request to save Foo : {}", foo);
        return fooRepository.save(foo);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Foo> findAll(Pageable pageable) {
        log.debug("Request to get all Foos");
        return fooRepository.findAll(pageable);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<Foo> findOne(Long id) {
        log.debug("Request to get Foo : {}", id);
        return fooRepository.findById(id);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete Foo : {}", id);
        fooRepository.deleteById(id);
    }
}
