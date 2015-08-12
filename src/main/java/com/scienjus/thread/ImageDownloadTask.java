package com.scienjus.thread;

import com.scienjus.bean.Image;
import com.scienjus.config.PixivCrawlerConfig;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 负责下载图片的线程
 */
public class ImageDownloadTask implements Runnable {
    /**
     * 日志
     */
    private static final Logger logger = Logger.getLogger(ImageDownloadTask.class);

    /**
     * 重试次数
     */
    private int failure;

    /**
     * 请求客户端
     * @param client
     * @param id
     * @param url
     * @param referer
     */
    private CloseableHttpClient client;

    /**
     * 图片对象
     */
    private Image image;


    public ImageDownloadTask(CloseableHttpClient client, Image image) {
        this.client = client;
        this.image = image;
        this.failure = 0;
    }

    @Override
    public void run() {
        String id = image.getChildId() == null ? image.getId() : image.getId() + "-" + image.getChildId();
        logger.info("开始下载图片[" + id + "]...");

        File file = new File(image.getPath());
        if (file.exists()) {
            return;
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        CloseableHttpResponse response = null;
        OutputStream out = null;
        HttpGet get;
        try {
            get = new HttpGet(image.getUrl());
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(PixivCrawlerConfig.SOCKET_TIMEOUT).setConnectTimeout(PixivCrawlerConfig.CONNECT_TIMEOUT).build();
            get.setConfig(requestConfig);
            get.setHeader("Referer", image.getReferer());
            response = client.execute(get);
            out = new FileOutputStream(file);
            byte[] buffer = new byte[1024 * 1024];
            int bytesRead;
            while((bytesRead = response.getEntity().getContent().read(buffer)) != -1){
                out.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            if (failure++ < PixivCrawlerConfig.MAX_FAILURE_TIME) {
                try {
                    Thread.sleep(PixivCrawlerConfig.SLEEP_TIME);
                } catch (InterruptedException e1) {}
                logger.error("图片[" + id + "]下载失败...正在重试第" + failure +"次...");
                if (file.exists()) {
                    file.delete();
                }
                run();
            } else {
                logger.error("图片[" + id + "]无法下载...");
            }
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }
}