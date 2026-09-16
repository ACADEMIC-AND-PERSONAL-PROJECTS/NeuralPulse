package com.ainews.service;

import com.ainews.model.NewsItem;

import java.util.List;

public interface DeduplicationService {

    List<NewsItem> deduplicate(List<NewsItem> items);
}
