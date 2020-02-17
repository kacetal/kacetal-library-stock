package com.kacetal.library.stock.web.rest;

import com.kacetal.library.stock.KacetalLibraryStockApp;
import com.kacetal.library.stock.domain.Stock;
import com.kacetal.library.stock.domain.enumeration.BookStockStatus;
import com.kacetal.library.stock.repository.StockRepository;
import com.kacetal.library.stock.repository.search.StockSearchRepository;
import com.kacetal.library.stock.service.StockService;
import com.kacetal.library.stock.web.rest.errors.ExceptionTranslator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.kacetal.library.stock.domain.enumeration.BookStockStatus.AVAILABLE;
import static com.kacetal.library.stock.domain.enumeration.BookStockStatus.OUT_OF_BORROW;
import static com.kacetal.library.stock.domain.enumeration.BookStockStatus.OUT_OF_STOCK;
import static com.kacetal.library.stock.web.rest.TestUtil.APPLICATION_JSON_UTF8;
import static com.kacetal.library.stock.web.rest.TestUtil.createFormattingConversionService;
import static com.kacetal.library.stock.web.rest.errors.ErrorConstants.STOCK_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the {@link StockResource} REST controller.
 */
@SpringBootTest(classes = KacetalLibraryStockApp.class)
public class StockResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";

    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final Integer DEFAULT_QUANTITY = 2;

    private static final Integer UPDATED_QUANTITY = 3;

    private static final BookStockStatus DEFAULT_BOOK_STOCK_STATUS = AVAILABLE;

    private static final BookStockStatus UPDATED_BOOK_STOCK_STATUS = OUT_OF_STOCK;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockService stockService;

    /**
     * This repository is mocked in the com.kacetal.library.stock.repository.search test package.
     *
     * @see com.kacetal.library.stock.repository.search.StockSearchRepositoryMockConfiguration
     */
    @Autowired
    private StockSearchRepository mockStockSearchRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    private MockMvc restStockMockMvc;

    private Stock stock;

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Stock createEntity(EntityManager em) {
        Stock stock = new Stock();
        stock.setName(DEFAULT_NAME);
        stock.setQuantity(DEFAULT_QUANTITY);
        stock.setBookStockStatus(DEFAULT_BOOK_STOCK_STATUS);
        return stock;
    }

    /**
     * Create an updated entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Stock createUpdatedEntity(EntityManager em) {
        Stock stock = new Stock();
        stock.setName(UPDATED_NAME);
        stock.setQuantity(UPDATED_QUANTITY);
        stock.setBookStockStatus(UPDATED_BOOK_STOCK_STATUS);
        return stock;
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final StockResource stockResource = new StockResource(stockService);
        this.restStockMockMvc = MockMvcBuilders.standaloneSetup(stockResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    @BeforeEach
    public void initTest() {
        stock = createEntity(em);
    }

    @Test
    @Transactional
    public void createStock() throws Exception {
        int databaseSizeBeforeCreate = stockRepository.findAll().size();

        // Create the Stock
        restStockMockMvc.perform(post("/api/stocks")
            .contentType(APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(stock)))
            .andExpect(status().isCreated());

        // Validate the Stock in the database
        List<Stock> stockList = stockRepository.findAll();
        assertThat(stockList).hasSize(databaseSizeBeforeCreate + 1);
        Stock testStock = stockList.get(stockList.size() - 1);
        assertThat(testStock.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testStock.getQuantity()).isEqualTo(DEFAULT_QUANTITY);
        assertThat(testStock.getBookStockStatus()).isEqualTo(DEFAULT_BOOK_STOCK_STATUS);

        // Validate the Stock in Elasticsearch
        verify(mockStockSearchRepository, times(1)).save(testStock);
    }

    @Test
    @Transactional
    public void createStockWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = stockRepository.findAll().size();

        // Create the Stock with an existing ID
        stock.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restStockMockMvc.perform(post("/api/stocks")
            .contentType(APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(stock)))
            .andExpect(status().isBadRequest());

        // Validate the Stock in the database
        List<Stock> stockList = stockRepository.findAll();
        assertThat(stockList).hasSize(databaseSizeBeforeCreate);

        // Validate the Stock in Elasticsearch
        verify(mockStockSearchRepository, times(0)).save(stock);
    }


    @Test
    @Transactional
    public void checkQuantityIsRequired() throws Exception {
        int databaseSizeBeforeTest = stockRepository.findAll().size();
        // set the field null
        stock.setQuantity(null);

        // Create the Stock, which fails.

        restStockMockMvc.perform(post("/api/stocks")
            .contentType(APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(stock)))
            .andExpect(status().isBadRequest());

        List<Stock> stockList = stockRepository.findAll();
        assertThat(stockList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkBookStockStatusIsRequired() throws Exception {
        int databaseSizeBeforeTest = stockRepository.findAll().size();
        // set the field null
        stock.setBookStockStatus(null);

        // Create the Stock, which fails.

        restStockMockMvc.perform(post("/api/stocks")
            .contentType(APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(stock)))
            .andExpect(status().isBadRequest());

        List<Stock> stockList = stockRepository.findAll();
        assertThat(stockList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllStocks() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);

        // Get all the stockList
        restStockMockMvc.perform(get("/api/stocks?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(stock.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].quantity").value(hasItem(DEFAULT_QUANTITY)))
            .andExpect(jsonPath("$.[*].bookStockStatus").value(hasItem(DEFAULT_BOOK_STOCK_STATUS.toString())));
    }

    @Test
    @Transactional
    public void getStock() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);

        // Get the stock
        restStockMockMvc.perform(get("/api/stocks/{id}", stock.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(stock.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.quantity").value(DEFAULT_QUANTITY))
            .andExpect(jsonPath("$.bookStockStatus").value(DEFAULT_BOOK_STOCK_STATUS.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingStock() throws Exception {
        // Get the stock
        restStockMockMvc.perform(get("/api/stocks/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateStock() throws Exception {
        // Initialize the database
        stockService.save(stock);
        // As the test used the service layer, reset the Elasticsearch mock repository
        reset(mockStockSearchRepository);

        final int databaseSizeBeforeUpdate = stockRepository.findAll().size();

        // Update the stock
        Stock updatedStock = stockRepository.findById(stock.getId()).get();
        // Disconnect from session so that the updates on updatedStock are not directly saved in db
        em.detach(updatedStock);
        updatedStock.setName(UPDATED_NAME);
        updatedStock.setQuantity(UPDATED_QUANTITY);
        updatedStock.setBookStockStatus(UPDATED_BOOK_STOCK_STATUS);

        restStockMockMvc.perform(put("/api/stocks")
            .contentType(APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedStock)))
            .andExpect(status().isOk());

        // Validate the Stock in the database
        List<Stock> stockList = stockRepository.findAll();
        assertThat(stockList).hasSize(databaseSizeBeforeUpdate);
        Stock testStock = stockList.get(stockList.size() - 1);
        assertThat(testStock.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testStock.getQuantity()).isEqualTo(UPDATED_QUANTITY);
        assertThat(testStock.getBookStockStatus()).isEqualTo(UPDATED_BOOK_STOCK_STATUS);

        // Validate the Stock in Elasticsearch
        verify(mockStockSearchRepository, times(1)).save(testStock);
    }

    @Test
    @Transactional
    public void updateNonExistingStock() throws Exception {
        final int databaseSizeBeforeUpdate = stockRepository.findAll().size();

        // Create the Stock

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restStockMockMvc.perform(put("/api/stocks")
            .contentType(APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(stock)))
            .andExpect(status().isBadRequest());

        // Validate the Stock in the database
        List<Stock> stockList = stockRepository.findAll();
        assertThat(stockList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Stock in Elasticsearch
        verify(mockStockSearchRepository, times(0)).save(stock);
    }

    @Test
    @Transactional
    public void deleteStock() throws Exception {
        // Initialize the database
        stockService.save(stock);

        int databaseSizeBeforeDelete = stockRepository.findAll().size();

        // Delete the stock
        restStockMockMvc.perform(delete("/api/stocks/{id}", stock.getId())
            .accept(APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Stock> stockList = stockRepository.findAll();
        assertThat(stockList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the Stock in Elasticsearch
        verify(mockStockSearchRepository, times(1)).deleteById(stock.getId());
    }

    @Test
    @Transactional
    public void searchStock() throws Exception {
        // Initialize the database
        stockService.save(stock);
        when(mockStockSearchRepository.search(queryStringQuery("id:" + stock.getId()), PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(Collections.singletonList(stock), PageRequest.of(0, 1), 1));
        // Search the stock
        restStockMockMvc.perform(get("/api/_search/stocks?query=id:" + stock.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(stock.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].quantity").value(hasItem(DEFAULT_QUANTITY)))
            .andExpect(jsonPath("$.[*].bookStockStatus").value(hasItem(DEFAULT_BOOK_STOCK_STATUS.toString())));
    }

    @Test
    @Transactional
    public void borrowBook() throws Exception {
        // Initialize the database
        stockService.save(stock);
        // Borrow the book
        restStockMockMvc.perform(patch("/api/stocks/{id}/borrow", stock.getId())
            .accept(APPLICATION_JSON_UTF8))
            .andExpect(status().isAccepted())
            .andExpect(content().string(isEmptyOrNullString()));

        final Optional<Stock> optionalStock = stockService.findOne(stock.getId());
        assertThat(optionalStock).isPresent();

        final Stock actualStock = optionalStock.get();
        assertThat(actualStock.getQuantity()).isEqualTo(DEFAULT_QUANTITY - 1);
        assertThat(actualStock.getBookStockStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @Transactional
    public void borrowBookWhichNotExist() throws Exception {
        // Initialize the database
        stockService.save(stock);
        // Borrow the book
        restStockMockMvc.perform(patch("/api/stocks/{id}/borrow", stock.getId() + 5)
            .accept(APPLICATION_JSON_UTF8))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorKey").value(STOCK_NOT_FOUND))
            .andExpect(jsonPath("$.status").value(NOT_FOUND.value()));
    }

    @Test
    @Transactional
    public void borrowLastBook() throws Exception {
        // Initialize the database
        stock.setQuantity(1);
        stockService.save(stock);
        // Borrow the book
        restStockMockMvc.perform(patch("/api/stocks/{id}/borrow", stock.getId())
            .accept(APPLICATION_JSON_UTF8))
            .andExpect(status().isAccepted())
            .andExpect(content().string(isEmptyOrNullString()));

        final Optional<Stock> optionalStock = stockService.findOne(stock.getId());
        assertThat(optionalStock).isPresent();

        final Stock actualStock = optionalStock.get();
        assertThat(actualStock.getQuantity()).isEqualTo(0);
        assertThat(actualStock.getBookStockStatus()).isEqualTo(OUT_OF_STOCK);
    }

    @Test
    @Transactional
    public void borrowBookWhichOutOfStock() throws Exception {
        // Initialize the database
        stock.setQuantity(0);
        stock.setBookStockStatus(OUT_OF_STOCK);
        stockService.save(stock);
        // Borrow the book
        restStockMockMvc.perform(patch("/api/stocks/{id}/borrow", stock.getId())
            .accept(APPLICATION_JSON_UTF8))
            .andExpect(status().isNotAcceptable())
            .andExpect(content().contentType(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errorKey").value(OUT_OF_STOCK.errorKey()))
            .andExpect(jsonPath("$.status").value(NOT_ACCEPTABLE.value()));
    }

    @Test
    @Transactional
    public void borrowBookWhichOutOfBorrow() throws Exception {
        // Initialize the database
        stock.setBookStockStatus(OUT_OF_BORROW);
        stockService.save(stock);
        // Borrow the book
        restStockMockMvc.perform(patch("/api/stocks/{id}/borrow", stock.getId())
            .accept(APPLICATION_JSON_UTF8))
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errorKey").value(OUT_OF_BORROW.errorKey()))
            .andExpect(jsonPath("$.status").value(FORBIDDEN.value()));
    }

    @Test
    @Transactional
    public void returnBook() throws Exception {
        // Initialize the database
        stockService.save(stock);
        // Borrow the book
        restStockMockMvc.perform(patch("/api/stocks/{id}/return", stock.getId())
            .accept(APPLICATION_JSON_UTF8))
            .andExpect(status().isAccepted())
            .andExpect(content().string(isEmptyOrNullString()));

        final Optional<Stock> optionalStock = stockService.findOne(stock.getId());
        assertThat(optionalStock).isPresent();

        final Stock actualStock = optionalStock.get();
        assertThat(actualStock.getQuantity()).isEqualTo(DEFAULT_QUANTITY + 1);
        assertThat(actualStock.getBookStockStatus()).isEqualTo(AVAILABLE);
    }


    @Test
    @Transactional
    public void returnFirstBook() throws Exception {
        // Initialize the database
        stock.setQuantity(0);
        stock.setBookStockStatus(OUT_OF_STOCK);
        stockService.save(stock);
        // Borrow the book
        restStockMockMvc.perform(patch("/api/stocks/{id}/return", stock.getId())
            .accept(APPLICATION_JSON_UTF8))
            .andExpect(status().isAccepted())
            .andExpect(content().string(isEmptyOrNullString()));

        final Optional<Stock> optionalStock = stockService.findOne(stock.getId());
        assertThat(optionalStock).isPresent();

        final Stock actualStock = optionalStock.get();
        assertThat(actualStock.getQuantity()).isEqualTo(1);
        assertThat(actualStock.getBookStockStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @Transactional
    public void returnBookAfterOutOfStock() throws Exception {
        // Initialize the database
        stock.setQuantity(-1);
        stock.setBookStockStatus(OUT_OF_STOCK);
        stockService.save(stock);
        // Borrow the book
        restStockMockMvc.perform(patch("/api/stocks/{id}/return", stock.getId())
            .accept(APPLICATION_JSON_UTF8))
            .andExpect(status().isAccepted())
            .andExpect(content().string(isEmptyOrNullString()));

        final Optional<Stock> optionalStock = stockService.findOne(stock.getId());
        assertThat(optionalStock).isPresent();

        final Stock actualStock = optionalStock.get();
        assertThat(actualStock.getQuantity()).isEqualTo(0);
        assertThat(actualStock.getBookStockStatus()).isEqualTo(OUT_OF_STOCK);
    }

    @Test
    @Transactional
    public void returnBookWithOutOfBorrow() throws Exception {
        // Initialize the database
        stock.setBookStockStatus(OUT_OF_BORROW);
        stockService.save(stock);
        // Borrow the book
        restStockMockMvc.perform(patch("/api/stocks/{id}/return", stock.getId())
            .accept(APPLICATION_JSON_UTF8))
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errorKey").value(OUT_OF_BORROW.errorKey()))
            .andExpect(jsonPath("$.status").value(FORBIDDEN.value()));
    }
}
