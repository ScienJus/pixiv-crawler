package com.scienjus.bean;

/**
 * 表示图片对象的类
 */
public class Image {

    /**
     * 图片id
     */
    private String id;

    /**
     * 图片地址
     */
    private String url;

    /**
     * 多张图片时的子id（单张图片为0）
     */
    private String childId;

    /**
     * 图片的来源地址
     */
    private String referer;

    /**
     * 图片的存放文件夹
     */
    private String dir;

    /**
     * 图片地址
     */
    private String path;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getPath() {
        String ext = url.substring(url.lastIndexOf("."));
        if (ext.indexOf("?") > -1) {
            ext = ext.substring(0, ext.indexOf("?"));
        }
        if (childId  == null || Integer.parseInt(childId) == 0) {
            return dir + id + ext;
        } else {
            return dir + id + "/" + childId + ext;
        }
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}
