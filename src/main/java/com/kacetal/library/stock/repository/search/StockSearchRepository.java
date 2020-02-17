package com.kacetal.library.stock.repository.search;

import com.kacetal.library.stock.domain.Stock;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for the {@link Stock} entity.
 */
public interface StockSearchRepository extends ElasticsearchRepository<Stock, Long> {

}
