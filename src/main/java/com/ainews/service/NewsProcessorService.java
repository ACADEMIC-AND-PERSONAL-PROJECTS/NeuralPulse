package com.ainews.service;

import com.ainews.model.NewsItem;

import java.util.List;

public interface NewsProcessorService {

    String processWithLLM(List<NewsItem> newsItems);
}
