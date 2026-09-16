package com.ainews.service;

import com.ainews.model.NewsItem;

import java.util.List;

public interface NewsFetcherService {

    List<NewsItem> fetchAllSources();
}
