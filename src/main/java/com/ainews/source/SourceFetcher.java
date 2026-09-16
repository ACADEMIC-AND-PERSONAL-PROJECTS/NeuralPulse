package com.ainews.source;

import com.ainews.model.NewsItem;

import java.util.List;

public interface SourceFetcher {

    String getSourceName();

    List<NewsItem> fetchLastWeek();
}
