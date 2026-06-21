package com.wut.shortlink.project.service.impl;

import com.wut.shortlink.project.service.UrlTitleService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * URL 标题接口实现层
 */
@Service
@Slf4j
public class UrlTitleServiceImpl implements UrlTitleService {

    @Override
    public String getTitleByUrl(String url) {
        try {
            URL targetUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(1500);
            connection.setReadTimeout(1500);
            connection.setRequestMethod("GET");
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                String redirectUrl = connection.getHeaderField("Location");
                if (redirectUrl != null) {
                    targetUrl = new URL(redirectUrl);
                    connection = (HttpURLConnection) targetUrl.openConnection();
                    connection.setConnectTimeout(1500);
                    connection.setReadTimeout(1500);
                    connection.setRequestMethod("GET");
                    connection.connect();
                    responseCode = connection.getResponseCode();
                }
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Document document = Jsoup.connect(targetUrl.toString())
                        .timeout(1500)
                        .get();
                return document.title();
            }
        } catch (Throwable ex) {
            log.warn("Fetch title failed, url: {}", url, ex);
        }
        return "Unknown title";
    }
}
