package com.kacetal.library.stock.web.rest;

import com.kacetal.library.stock.domain.Stock;
import com.kacetal.library.stock.domain.enumeration.BookStockStatus;
import com.kacetal.library.stock.service.StockService;
import com.kacetal.library.stock.web.rest.errors.BadRequestAlertException;
import com.kacetal.library.stock.web.rest.errors.BookOutOfBorrowException;
import com.kacetal.library.stock.web.rest.errors.BookOutOfStockException;
import com.kacetal.library.stock.web.rest.errors.StockNotFoundException;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import static com.kacetal.library.stock.domain.enumeration.BookStockStatus.OUT_OF_BORROW;
import static com.kacetal.library.stock.domain.enumeration.BookStockStatus.OUT_OF_STOCK;
import static com.kacetal.library.stock.web.rest.errors.ErrorConstants.ID_NULL;
import static com.kacetal.library.stock.web.rest.errors.ErrorConstants.STOCK_NOT_FOUND;

/**
 * REST controller for managing {@link com.kacetal.library.stock.domain.Stock}.
 */
@RestController
@RequestMapping("/api")
public class StockResource {

    private static final String ENTITY_NAME = "kacetalLibraryStockStock";

    private final Logger log = LoggerFactory.getLogger(StockResource.class);

    private final StockService stockService;

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public StockResource(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * {@code POST  /stocks} : Create a new stock.
     *
     * @param stock the stock to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new stock, or with status {@code 400 (Bad Request)} if the stock has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/stocks")
    public ResponseEntity<Stock> createStock(@Valid @RequestBody Stock stock) throws URISyntaxException {
        log.debug("REST request to save Stock : {}", stock);
        if (stock.getId() != null) {
            throw new BadRequestAlertException("A new stock cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Stock result = stockService.save(stock);
        return ResponseEntity.created(new URI("/api/stocks/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /stocks} : Updates an existing stock.
     *
     * @param stock the stock to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated stock,
     * or with status {@code 400 (Bad Request)} if the stock is not valid,
     * or with status {@code 500 (Internal Server Error)} if the stock couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/stocks")
    public ResponseEntity<Stock> updateStock(@Valid @RequestBody Stock stock) throws URISyntaxException {
        log.debug("REST request to update Stock : {}", stock);
        if (stock.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, ID_NULL);
        }
        Stock result = stockService.save(stock);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, stock.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /stocks} : Borrow the book from an existing stock.
     *
     * @param id the book to borrow from stock with same id.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with empty body,
     * or with status {@code 400 (Bad Request)} if the stock is not valid,
     * or with status {@code 500 (Internal Server Error)} if the book couldn't be borrowed.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping("/stocks/{id}/borrow")
    public ResponseEntity<Void> borrowBook(@PathVariable Long id) throws URISyntaxException {
        log.debug("REST request to borrow Book from Stock with ID : {}", id);
        if (id == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, ID_NULL);
        }

        final Optional<BookStockStatus> bookStockStatus = stockService.borrowBook(id);
        if (bookStockStatus.isEmpty()) {
            throw new StockNotFoundException("Stock for the book not found for this id", ENTITY_NAME, STOCK_NOT_FOUND);
        }
        switch (bookStockStatus.get()) {
            case AVAILABLE:
                return ResponseEntity.accepted()
                    .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
                    .build();
            case OUT_OF_STOCK:
                throw new BookOutOfStockException("Book is out of stock", ENTITY_NAME, OUT_OF_STOCK.errorKey());
            case OUT_OF_BORROW:
            default:
                throw new BookOutOfBorrowException("Book is out of borrow", ENTITY_NAME, OUT_OF_BORROW.errorKey());
        }
    }

    /**
     * {@code PATCH  /stocks} : Return the book to an existing stock.
     *
     * @param id the book to return to stock with same id.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with empty body,
     * or with status {@code 400 (Bad Request)} if the stock is not valid,
     * or with status {@code 500 (Internal Server Error)} if the book couldn't be returned.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping("/stocks/{id}/return")
    public ResponseEntity<Void> returnBook(@PathVariable Long id) throws URISyntaxException {
        log.debug("REST request to return Book to Stock with ID : {}", id);
        if (id == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, ID_NULL);
        }

        final Optional<BookStockStatus> bookStockStatus = stockService.returnBook(id);
        if (bookStockStatus.isEmpty()) {
            throw new StockNotFoundException("Invalid id", ENTITY_NAME, STOCK_NOT_FOUND);
        }

        switch (bookStockStatus.get()) {
            case AVAILABLE:
            case OUT_OF_STOCK:
                return ResponseEntity.accepted()
                    .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
                    .build();
            case OUT_OF_BORROW:
            default:
                throw new BookOutOfBorrowException("Book is out of borrow", ENTITY_NAME, OUT_OF_BORROW.errorKey());
        }

    }

    /**
     * {@code GET  /stocks} : get all the stocks.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of stocks in body.
     */
    @GetMapping("/stocks")
    public ResponseEntity<List<Stock>> getAllStocks(Pageable pageable) {
        log.debug("REST request to get a page of Stocks");
        Page<Stock> page = stockService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /stocks/:id} : get the "id" stock.
     *
     * @param id the id of the stock to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the stock, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/stocks/{id}")
    public ResponseEntity<Stock> getStock(@PathVariable Long id) {
        log.debug("REST request to get Stock : {}", id);
        Optional<Stock> stock = stockService.findOne(id);
        return ResponseUtil.wrapOrNotFound(stock);
    }

    /**
     * {@code DELETE  /stocks/:id} : delete the "id" stock.
     *
     * @param id the id of the stock to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/stocks/{id}")
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        log.debug("REST request to delete Stock : {}", id);
        stockService.delete(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build();
    }

    /**
     * {@code SEARCH  /_search/stocks?query=:query} : search for the stock corresponding
     * to the query.
     *
     * @param query    the query of the stock search.
     * @param pageable the pagination information.
     * @return the result of the search.
     */
    @GetMapping("/_search/stocks")
    public ResponseEntity<List<Stock>> searchStocks(@RequestParam String query, Pageable pageable) {
        log.debug("REST request to search for a page of Stocks for query {}", query);
        Page<Stock> page = stockService.search(query, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
