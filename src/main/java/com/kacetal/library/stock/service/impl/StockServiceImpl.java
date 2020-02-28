package com.kacetal.library.stock.service.impl;

import com.kacetal.library.stock.domain.Stock;
import com.kacetal.library.stock.domain.enumeration.BookStockStatus;
import com.kacetal.library.stock.repository.StockRepository;
import com.kacetal.library.stock.repository.search.StockSearchRepository;
import com.kacetal.library.stock.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.kacetal.library.stock.domain.enumeration.BookStockStatus.AVAILABLE;
import static com.kacetal.library.stock.domain.enumeration.BookStockStatus.OUT_OF_BORROW;
import static com.kacetal.library.stock.domain.enumeration.BookStockStatus.OUT_OF_STOCK;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

/**
 * Service Implementation for managing {@link Stock}.
 */
@Service
@Transactional
public class StockServiceImpl implements StockService {

    private final Logger log = LoggerFactory.getLogger(StockServiceImpl.class);

    private final StockRepository stockRepository;

    private final StockSearchRepository stockSearchRepository;

    public StockServiceImpl(StockRepository stockRepository, StockSearchRepository stockSearchRepository) {
        this.stockRepository = stockRepository;
        this.stockSearchRepository = stockSearchRepository;
    }

    /**
     * Save a stock.
     *
     * @param stock the entity to save.
     * @return the persisted entity.
     */
    @Override
    public Stock save(Stock stock) {
        log.debug("Request to save Stock : {}", stock);
        Stock result = stockRepository.save(stock);
        stockSearchRepository.save(result);
        return result;
    }

    /**
     * Get all the stocks.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Stock> findAll(Pageable pageable) {
        log.debug("Request to get all Stocks");
        return stockRepository.findAll(pageable);
    }

    /**
     * Get one stock by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Stock> findOne(Long id) {
        log.debug("Request to get Stock : {}", id);
        return stockRepository.findById(id);
    }

    /**
     * Delete the stock by id.
     *
     * @param id the id of the entity.
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete Stock : {}", id);
        stockRepository.deleteById(id);
        stockSearchRepository.deleteById(id);
    }

    /**
     * Search for the stock corresponding to the query.
     *
     * @param query    the query of the search.
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Stock> search(String query, Pageable pageable) {
        log.debug("Request to search for a page of Stocks for query {}", query);
        return stockSearchRepository.search(queryStringQuery(query), pageable);
    }

    @Override
    @Transactional
    public Optional<BookStockStatus> borrowBook(Long id) {
        final Optional<Stock> optionalStock = this.findOne(id);
        if (optionalStock.isEmpty()) {
            return Optional.empty();
        }

        final Stock stock = optionalStock.get();
        final BookStockStatus bookStockStatus = stock.getBookStockStatus();
        if (bookStockStatus == OUT_OF_BORROW) {
            return Optional.of(OUT_OF_BORROW);
        } else if (bookStockStatus == OUT_OF_STOCK) {
            return Optional.of(OUT_OF_STOCK);
        }
        final int updatedQuantity = stock.getQuantity() - 1;
        if (updatedQuantity < 1) {
            stock.setBookStockStatus(OUT_OF_STOCK);
        }
        stock.setQuantity(updatedQuantity);

        this.save(stock);
        return Optional.of(AVAILABLE);
    }

    @Override
    public Optional<BookStockStatus> returnBook(Long id) {
        final Optional<Stock> optionalStock = this.findOne(id);
        if (optionalStock.isEmpty()) {
            return Optional.empty();
        }

        final Stock stock = optionalStock.get();
        final BookStockStatus bookStockStatus = stock.getBookStockStatus();
        if (bookStockStatus == OUT_OF_BORROW) {
            return Optional.of(OUT_OF_BORROW);
        }
        final int updatedQuantity = stock.getQuantity() + 1;
        if (updatedQuantity > 0) {
            stock.setBookStockStatus(AVAILABLE);
        }
        stock.setQuantity(updatedQuantity);
        this.save(stock);
        return Optional.of(AVAILABLE);
    }
}
