package ch.sebpiller.iot.web.rest;

import ch.sebpiller.iot.domain.Foo;
import ch.sebpiller.iot.service.FooService;
import ch.sebpiller.iot.web.rest.errors.BadRequestAlertException;
import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing {@link ch.sebpiller.iot.domain.Foo}.
 */
@RestController
@RequestMapping("/api")
public class FooResource {

    private final Logger log = LoggerFactory.getLogger(FooResource.class);

    private static final String ENTITY_NAME = "foo";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final FooService fooService;

    public FooResource(FooService fooService) {
        this.fooService = fooService;
    }

    /**
     * {@code POST  /foos} : Create a new foo.
     *
     * @param foo the foo to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new foo, or with status {@code 400 (Bad Request)} if the foo has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/foos")
    public ResponseEntity<Foo> createFoo(@RequestBody Foo foo) throws URISyntaxException {
        log.debug("REST request to save Foo : {}", foo);
        if (foo.getId() != null) {
            throw new BadRequestAlertException("A new foo cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Foo result = fooService.save(foo);
        return ResponseEntity.created(new URI("/api/foos/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /foos} : Updates an existing foo.
     *
     * @param foo the foo to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated foo,
     * or with status {@code 400 (Bad Request)} if the foo is not valid,
     * or with status {@code 500 (Internal Server Error)} if the foo couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/foos")
    public ResponseEntity<Foo> updateFoo(@RequestBody Foo foo) throws URISyntaxException {
        log.debug("REST request to update Foo : {}", foo);
        if (foo.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        Foo result = fooService.save(foo);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, foo.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /foos} : get all the foos.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of foos in body.
     */
    @GetMapping("/foos")
    public ResponseEntity<List<Foo>> getAllFoos(Pageable pageable) {
        log.debug("REST request to get a page of Foos");
        Page<Foo> page = fooService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /foos/:id} : get the "id" foo.
     *
     * @param id the id of the foo to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the foo, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/foos/{id}")
    public ResponseEntity<Foo> getFoo(@PathVariable Long id) {
        log.debug("REST request to get Foo : {}", id);
        Optional<Foo> foo = fooService.findOne(id);
        return ResponseUtil.wrapOrNotFound(foo);
    }

    /**
     * {@code DELETE  /foos/:id} : delete the "id" foo.
     *
     * @param id the id of the foo to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/foos/{id}")
    public ResponseEntity<Void> deleteFoo(@PathVariable Long id) {
        log.debug("REST request to delete Foo : {}", id);
        fooService.delete(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build();
    }
}
