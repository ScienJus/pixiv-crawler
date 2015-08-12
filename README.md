# Pixiv Crawler

##介绍

批量抓取和下载Pixiv上的图片。

通过解析www.pixiv.net完成图片的爬取和下载。

如果你想了解如何解析并模拟请求，可以参考我博客的文章：[Pixiv爬图教程][1]。

##操作说明

###如何运行

在`com/scienjus/main/Launch.java`中编写你的任务。

###创建和关闭

使用`PixivCrawlerClient`的`create`方法或`createDefault`方法创建一个新的客户端实例，前者可以指定图片存放的位置，后者则默认存放在`E:/pixiv/`文件夹中。

创建实例后通过`setUsername`和`setPassword`方法设置你的用户名和密码，然后调用`login`方法进行登录，如果登录成功会返回`true`（所有下载任务都需要在登陆后进行）。

当图片下载任务全部完成后需要通过`PixivCrawlerClient`的`close`方法关闭客户端，否则即使图片全部下载完成程序也不会停止运行。

示例：
```java
//创建一个客户端实例
PixivCrawlerClient client = PixivCrawlerClient.create("path");
//设置用户名和对应的密码
client.setUsername("username");
client.setPassword("password");
//登陆
if (client.login()) {
    //进行下载图片任务..
}
//关闭客户端
client.close();
```

###关键词搜索

使用`PixivCrawlerClient`中的`searchByKeyword`方法进行关键词搜索并下载，参数有：
 - `String keyWord`：指定搜索的关键词（请尽量使用准确的日文关键词，否则只能依靠Pixiv的联想匹配）
 - `boolean isR18`：是否只选择R18图片，如果设为`true`则只下载R18图片
 - `int baseStar`：用户收藏数筛选，如果不需要筛选请设置为`-1`（不推荐，图片数量会非常多并且质量参差不齐）
 
示例：
```java
//下载kancolle(舰队Collection)的收藏数大于1000的所有图片
client.searchByKeyword("kancolle", false, 1000);
//下载ラブライブ!（love live!）的收藏数大于2000的R18图片
client.searchByKeyword("ラブライブ!", true, 2000);
```

###排行榜

使用`PixivCrawlerClient`中排行榜相关的方法可以完成排行榜图片下载，当前提供的方法有：
 - `downloadNowRank`：下载当天的排行榜（当天的排行榜生成日期一般为前一日）
 - `downloadAllRank`：下载所有的排行榜（自2007年9月13日到当天的所有图片，慎用）
 - `downloadRankOn`：下载历史中某一天的排行榜
 - `downloadRankAfter`：下载自某一天开始至今日的排行榜
 - `downloadRankBetween`：下载从某一天到另一天之间的排行榜
 
这些方法共有的参数为：
 - `String date/start/end`：指定日期，通用格式为`yyyyMMdd`
 - `RankingMode mode`：排行榜的类型（全部/原创/新人/男性向/女性向）
 - `boolean isR18`：是否只选择R18图片，如果设为`true`则只下载R18图片
 
示例：
```java
//下载当天排行榜
client.downloadNowRank(RankingMode.all, false);
//下载全部排行榜
client.downloadAllRank(RankingMode.all, false);
//下载2015年4月29日的排行榜
client.downloadRankOn("20150429", RankingMode.all, false);
//下载自2015年4月29日至今日的全部排行榜
client.downloadRankAfter("20150429", RankingMode.all, false);
//下载自2014年4月29日至2015年4月29日的全部排行榜
client.downloadRankBetween("20140429", "20150429", RankingMode.all, false);
```

###作者索引
使用`PixivCrawlerClient`的`searchByAuthor`方法可以下载某位作者的全部作品，作者的id需要自行去作者主页查找，方法为进入作者个人资料页面后，如果链接为`http://www.pixiv.net/member.php?id=一串数字`的格式，一串数字就是作者的id。

示例：
```java
//下载作者id为111111的全部作品
client.searchByAuthor("111111");
```

##帮助

在使用中如果您遇到了问题或是有新的想法，请给我提Issues。或是通过以下方式联系我：
 - 博客：[我的博客][2]
 - 邮箱：xie_enlong@foxmail.com

[1]:http://www.scienjus.com/pixiv-parser/
[2]:http://www.scienjus.com