package com.ce11kjw.junkclean;

/** 单条垃圾项 */
public class JunkItem {
    public String path;
    public String name;
    public long size;
    public boolean checked = true;

    public JunkItem(String path, String name, long size) {
        this.path = path;
        this.name = name;
        this.size = size;
    }
}
