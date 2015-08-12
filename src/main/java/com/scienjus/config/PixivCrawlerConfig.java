package com.scienjus.config;

import java.nio.charset.Charset;

/**
 * @author XieEnlong
 * @date 2015/3/21.
 */
public class PixivCrawlerConfig {

    /**
     * 字符编码
     */
    public static final Charset ENCODING = Charset.forName("UTF-8");

    /**
     * 默认的图片存放位置
     */
    public static final String DEFAULT_PATH = "E:/pixiv/";

    /**
     * 登陆请求地址
     */
    public static final String LOGIN_URL = "https://www.secure.pixiv.net/login.php";

    /**
     * 搜索请求地址
     */
    public static final String SEARCH_URL = "http://www.pixiv.net/search.php";

    /**
     * 排行榜请求地址
     */
    public static final String RANK_URL = "http://www.pixiv.net/ranking.php";

    /**
     * 图片/作者详情请求地址
     */
    public static final String DETAIL_URL = "http://www.pixiv.net/member_illust.php";

    /**
     * 最大发送时间
     */
    public static final int SOCKET_TIMEOUT = 2000;

    /**
     * 最大连接时间
     */
    public static final int CONNECT_TIMEOUT = 2000;

    /**
     * 下载图片间隔时间
     */
    public static final long SLEEP_TIME = 200;

    /**
     * 最大重试次数
     */
    public static final int MAX_FAILURE_TIME = 5;

    /**
     * 线程池数量
     */
    public static final int POOL_SIZE = 50;

    /**
     * 起始页数
     */
    public static final int START_PAGE = 1;

    /**
     * 排行榜中当天的字符串常量
     */
    public static final String TODAY = "";
}
