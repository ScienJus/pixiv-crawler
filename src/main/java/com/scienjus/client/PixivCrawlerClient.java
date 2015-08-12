package com.scienjus.client;

import com.scienjus.bean.Image;
import com.scienjus.bean.RankingMode;
import com.scienjus.config.PixivCrawlerConfig;
import com.scienjus.parser.PageParser;
import com.scienjus.thread.ImageDownloadTask;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 登陆/搜索并下载符合要求的图片/排行榜（暂未实现）
 */
public class PixivCrawlerClient {

    /**'
     * 日志文件
     */
    private static final Logger LOGGER = Logger.getLogger(PixivCrawlerClient.class);

    /**
     * 日期格式化
     */
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyyMMdd");

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 图片存放位置
     */
    private String path;

    /**
     * 请求的上下文（存储登陆信息）
     */
    private HttpClientContext context;

    /**
     * html文本解析器
     */
    private PageParser parser;

    /**
     * http请求发送端
     */
    private CloseableHttpClient client;

    /**
     * 下载图片的线程池
     */
    private ExecutorService pool;

    /**
     * 设置用户名
     * @param username  你的pixiv账号
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 设置密码
     * @param password  对应的pixiv密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 创建一个默认的下载器，它会把图片存放在默认位置
     * @return
     */
    public static PixivCrawlerClient createDefault() {
        return create(PixivCrawlerConfig.DEFAULT_PATH);
    }

    /**
     * 创建一个指定存放位置的下载器
     * @param path
     * @return
     */
    public static PixivCrawlerClient create(String path) {
        if (path == null || "".equals(path.trim())) {
            LOGGER.error("请输入地址！");
            return null;
        }
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!dir.isDirectory()) {
            LOGGER.error("选择的路径并不是一个文件夹！");
            return null;
        }
        return new PixivCrawlerClient(path);
    }

    /**
     * 创建实例
     * @param path
     */
    private PixivCrawlerClient(String path) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        if (pool == null) {
            pool = Executors.newFixedThreadPool(PixivCrawlerConfig.POOL_SIZE);
        }
        this.path = path;
        parser = new PageParser();
        client = HttpClients.createDefault();
    }

    /**
     * 默认Http配置
     * @param request
     */
    private static final void defaultHttpConfig(HttpRequestBase request) {
        RequestConfig requestConfig = RequestConfig.custom().
                setSocketTimeout(PixivCrawlerConfig.SOCKET_TIMEOUT).
                setConnectTimeout(PixivCrawlerConfig.CONNECT_TIMEOUT).
                build();
        request.setConfig(requestConfig);
    }

    /**
     * 一个登陆表单的构成
     * @return
     */
    private UrlEncodedFormEntity buildLoginForm() {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("mode", "login"));
        params.add(new BasicNameValuePair("pixiv_id", username));
        params.add(new BasicNameValuePair("pass", password));
        params.add(new BasicNameValuePair("return_to", "/"));
        params.add(new BasicNameValuePair("skip", "1"));
        return new UrlEncodedFormEntity(params, PixivCrawlerConfig.ENCODING);
    }

    /**
     * 登陆
     * @return 登陆结果
     */
    public boolean login() {
        if (username == null || password == null) {
            LOGGER.error("用户名或密码为空！");
            return false;
        }
        LOGGER.info("当前登录的用户为：" + username);
        context = HttpClientContext.create();
        HttpPost post = new HttpPost(PixivCrawlerConfig.LOGIN_URL);
        defaultHttpConfig(post);
        UrlEncodedFormEntity entity = buildLoginForm();
        post.setEntity(entity);
        try (CloseableHttpResponse response = client.execute(post, context)) {
            if (response.getStatusLine().getStatusCode() == 302) {
                LOGGER.info("登陆成功！");
                return true;
            } else {
                LOGGER.error("登陆失败！请检查用户名或密码是否正确");
                return false;
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return false;
        }
    }

    /**
     * 通过关键词搜索
     * @param keyWord  关键词
     * @param isR18 是否只需要r18
     * @param baseStar  收藏数过滤条件
     */
    public void searchByKeyword(String keyWord, boolean isR18, int baseStar) {
        if (keyWord == null || "".equals(keyWord.trim())) {
            LOGGER.error("请输入关键词！");
            return;
        }
        LOGGER.info("开始下载收藏数大于" + baseStar + "的\"" + keyWord + "\"所有图片");
        downloadFromPage(buildSearchUrl(keyWord, isR18), baseStar);
    }

    /**
     * 通过作者搜索
     * @param id
     */
    public void searchByAuthor(String id) {
        if (id == null || "".equals(id)) {
            LOGGER.error("请输入作者id！");
            return;
        }
        LOGGER.info("开始下载作者id[" + id + "]的所有图片");
        downloadFromPage(buildAuthorUrl(id), -1);
    }

    /**
     * 重连机制获取页面
     * @param url
     * @return
     */
    private String getPageWithReconnection(String url) {
        int fail = 0;
        String pageHtml = null;
        while (pageHtml == null) {
            pageHtml = getPage(url);
            if (fail++ > PixivCrawlerConfig.MAX_FAILURE_TIME) {
                return null;
            }
            try {
                Thread.sleep(PixivCrawlerConfig.SLEEP_TIME);
            } catch (InterruptedException e) {}
        }
        return pageHtml;
    }

    /**
     * 请求并获得页面
     * @param url
     * @return
     */
    private String getPage(String url) {
        url = decodeUrl(url);
        CloseableHttpResponse response = null;
        HttpGet get = null;
        client = HttpClients.createDefault();
        try {
            get = new HttpGet(url);
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(PixivCrawlerConfig.SOCKET_TIMEOUT).setConnectTimeout(PixivCrawlerConfig.CONNECT_TIMEOUT).build();
            get.setConfig(requestConfig);
            response = client.execute(get, context);
            BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), PixivCrawlerConfig.ENCODING));
            StringBuilder pageHTML = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                pageHTML.append(line);
                pageHTML.append("\r\n");
            }
            return pageHTML.toString();
        } catch (IOException e) {
            LOGGER.error("获取网页失败：" + url);
            LOGGER.error(e.getMessage());
            return null;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 请求地址并下载
     * @param url   请求的地址
     * @param baseStar    收藏数过滤条件
     */
    private void downloadFromPage(String url, int baseStar) {
        String pageHtml = getPageWithReconnection(url);
        if (pageHtml == null) {
            return;
        }
        List<String> ids = parser.parseList(pageHtml, baseStar);
        if (ids != null) {
            for (String id : ids) {
                downloadImage(id);
            }
        }
        String next = parser.parseNextPage(pageHtml);
        if (next != null) {
            if (url.startsWith(PixivCrawlerConfig.SEARCH_URL)) {
                downloadFromPage(PixivCrawlerConfig.SEARCH_URL + next, baseStar);
            } else if (url.startsWith(PixivCrawlerConfig.DETAIL_URL)) {
                downloadFromPage(PixivCrawlerConfig.DETAIL_URL + next, baseStar);
            }
        }
    }

    /**
     * url转译
     * @param url
     * @return
     */
    private String decodeUrl(String url) {
        return URLDecoder.decode(url).replace("&amp;", "&").replace(" ", "%20");
    }

    /**
     * 线程睡眠
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    /**
     * 下载该页面的图片（可能有多张）
     * @param id    作品id
     */
    private void downloadImage(String id) {
        String url = buildDetailUrl(id);
        String pageHtml = getPageWithReconnection(url);
        if (pageHtml == null) {
            return;
        }
        if (parser.isManga(pageHtml)) {
            url = url.replace("medium", "manga");
            pageHtml = getPageWithReconnection(url);
            if (pageHtml == null) {
                return;
            }
            List<String> images = parser.parseManga(pageHtml);
            if (images != null) {
                int i = 1;
                for (String imgUrl : images) {
                    Image image = new Image();
                    image.setDir(path);
                    image.setId(id);
                    image.setReferer(url);
                    image.setChildId(String.valueOf(i++));
                    image.setUrl(imgUrl);
                    pool.execute(new ImageDownloadTask(client, image));
                    sleep(PixivCrawlerConfig.SLEEP_TIME);
                }
            }
        } else {
            String imgUrl = parser.parseMedium(pageHtml);
            if (imgUrl != null) {
                Image image = new Image();
                image.setDir(path);
                image.setId(id);
                image.setReferer(url);
                image.setUrl(imgUrl);
                pool.execute(new ImageDownloadTask(client, image));
                sleep(PixivCrawlerConfig.SLEEP_TIME);
            }
        }
    }

    /**
     * 检查日期字符串是否符合格式
     * @param dateStr
     * @return
     */
    private static final boolean checkDate(String dateStr) {
        if (PixivCrawlerConfig.TODAY.equals(dateStr)) {
            return true;
        }
        try {
            Date date = FORMAT.parse(dateStr);
            return date.getTime() < System.currentTimeMillis();
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * 判断下载排行榜递归是否结束
     * @param nowDateStr
     * @param startDateStr
     * @return
     */
    private static final boolean isFinished(String nowDateStr, String startDateStr) {
        if (PixivCrawlerConfig.TODAY.equals(nowDateStr)) {
            return false;
        }
        if (PixivCrawlerConfig.TODAY.equals(startDateStr)) {
            return true;
        }
        try {
            Date nowDate = FORMAT.parse(nowDateStr);
            Date startDate = FORMAT.parse(startDateStr);
            return nowDate.compareTo(startDate) == -1;
        } catch (ParseException e) {
            return true;
        }
    }

    /**
     * 下载所有排行榜
     * @param mode  排名方式
     * @param isR18 是否R18
     */
    public void downloadAllRank(RankingMode mode, boolean isR18) {
        downloadRankAfter("20070913", mode,  isR18);
    }

    /**
     * 下载当天排行榜
     * @param mode
     * @param isR18
     */
    public void downloadNowRank(RankingMode mode, boolean isR18) {
        downloadRankOn(PixivCrawlerConfig.TODAY, mode, isR18);
    }

    /**
     * 下载历史排行榜
     * @param start
     * @param mode
     * @param isR18
     */
    public void downloadRankAfter(String start, RankingMode mode, boolean isR18) {
        downloadRankBetween(start, PixivCrawlerConfig.TODAY, mode, isR18);
    }

    /**
     * 下载历史排行榜
     * @param start
     * @param mode
     * @param isR18
     */
    public void downloadRankBetween(String start, String end, RankingMode mode, boolean isR18) {
        if (start.equals(end)) {
            downloadRankOn(end, mode, isR18);
            return;
        } else {
            String now = end;
            while ((now = downloadRankOn(now, mode, isR18)) != null) {
                if (isFinished(now, start)) {
                    return;
                }
            }
        }
    }

    /**
     * 下载历史排行榜
     * @param date
     * @param mode
     * @param isR18
     */
    public String downloadRankOn(String date, RankingMode mode, boolean isR18) {
        if (mode == null) {
            LOGGER.error("请选择排行榜类型！");
            return null;
        }
        if (!checkDate(date)) {
            LOGGER.error("日期格式错误！");
            return null;
        }
        int page = PixivCrawlerConfig.START_PAGE;
        while (true) {
            String pageJson = getPageWithReconnection(buildRankUrl(date, page, mode, isR18));
            if (pageJson == null) {
                return null;
            }
            JSONObject json = (JSONObject)JSONValue.parse(pageJson);
            List<String> ids = parser.parseRank(json);
            for (String id : ids) {
                downloadImage(id);
            }
            int nextPage = parser.parseRankNextPage(json);
            if (nextPage != -1 && nextPage != page) {
                page = nextPage;
                continue;
            }
            return parser.parseRankPreDate(json);
        }
    }

    /**
     * 合成一个作者主页的连接
     * @return
     */
    private static final String buildAuthorUrl(String id) {
        return PixivCrawlerConfig.DETAIL_URL + "?id=" + id;
    }

    /**
     * 合成一个作品详情页的链接
     * @param id 作品id
     * @return
     */
    private static final String buildDetailUrl(String id) {
        return PixivCrawlerConfig.DETAIL_URL + "?mode=medium&illust_id=" + id;
    }

    /**
     * 合成一个排行榜的链接
     * @param date 哪一天
     * @param page 第几页
     * @param mode  排名方式
     * @param isR18 是否R18
     * @return
     */
    private static final String buildRankUrl(String date, int page, RankingMode mode, boolean isR18) {
        String param = mode.name();
        if (isR18) {
            param += "_r18";
        }
        return PixivCrawlerConfig.RANK_URL + "?format=json&content=illust&date=" + date + "&p=" + page + "&mode=" + param;
    }

    /**
     * 合成一个搜索的链接
     * @param keyWord  关键词
     * @param isR18 是否R18
     * @return
     */
    private static final String buildSearchUrl(String keyWord, boolean isR18) {
        return PixivCrawlerConfig.SEARCH_URL + "?word=" + keyWord + "&r18=" + (isR18 ? "1" : "0");
    }

    /**
     * 关闭PixivClient端
     */
    public void close() {
        try {
            pool.shutdown();
            while (!pool.isTerminated()) {
                sleep(PixivCrawlerConfig.SLEEP_TIME);
            }
            client.close();
        } catch (IOException e) {
            LOGGER.error("关闭客户端失败：" + e.getMessage());
        }
    }
}
