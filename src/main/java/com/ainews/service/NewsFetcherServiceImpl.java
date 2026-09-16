package com.ainews.service;

import com.ainews.model.NewsItem;
import com.ainews.source.SourceFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class NewsFetcherServiceImpl implements NewsFetcherService {

    private static final Logger log = LoggerFactory.getLogger(NewsFetcherServiceImpl.class);

    private final List<SourceFetcher> sources;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public NewsFetcherServiceImpl(List<SourceFetcher> sources) {
        this.sources = sources;
    }

    @Override
    public List<NewsItem> fetchAllSources() {
        log.info("Fetching news from {} sources", sources.size());

        List<CompletableFuture<List<NewsItem>>> futures = sources.stream()
                .map(source -> CompletableFuture.supplyAsync(() -> {
                    try {
                        List<NewsItem> items = source.fetchLastWeek();
                        log.info("Fetched {} items from {}", items.size(), source.getSourceName());
                        return items;
                    } catch (Exception e) {
                        log.error("Error fetching from {}: {}", source.getSourceName(), e.getMessage());
                        return List.<NewsItem>of();
                    }
                }, executor))
                .collect(Collectors.toList());

        List<NewsItem> allItems = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        log.info("Total items fetched: {}", allItems.size());
        return allItems;
    }
}
