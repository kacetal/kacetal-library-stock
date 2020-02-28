package com.kacetal.library.stock.service;

import com.kacetal.library.stock.domain.Stock;
import com.kacetal.library.stock.domain.enumeration.BookStockStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service Interface for managing {@link Stock}.
 */
public interface StockService {

    /**
     * Save a stock.
     *
     * @param stock the entity to save.
     * @return the persisted entity.
     */
    Stock save(Stock stock);

    /**
     * Get all the stocks.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<Stock> findAll(Pageable pageable);

    /**
     * Get the "id" stock.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<Stock> findOne(Long id);

    /**
     * Delete the "id" stock.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    /**
     * Search for the stock corresponding to the query.
     *
     * @param query    the query of the search.
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<Stock> search(String query, Pageable pageable);

    /**
     * Borrow Book from {@link Stock} with specific id.
     *
     * @param id the id of stock.
     * @return the Status of Borrow.
     */
    Optional<BookStockStatus> borrowBook(Long id);

    /**
     * Return Book to {@link Stock} with specific id.
     *
     * @param id the id of stock.
     * @return the Status of Borrow.
     */
    Optional<BookStockStatus> returnBook(Long id);
}
