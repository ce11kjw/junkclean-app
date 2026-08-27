package com.ce11kjw.junkclean;

import java.util.ArrayList;
import java.util.List;

/** 垃圾分类 */
public class JunkCategory {
    public String id;
    public String name;
    public String desc;
    public String icon;
    public boolean careful;      // 谨慎项，默认不勾
    public boolean needRoot;     // 需要 root 才能扫
    public List<JunkItem> items = new ArrayList<JunkItem>();

    public JunkCategory(String id, String name, String desc, String icon,
                        boolean careful, boolean needRoot) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.icon = icon;
        this.careful = careful;
        this.needRoot = needRoot;
    }

    public long total() {
        long t = 0;
        for (JunkItem it : items) t += it.size;
        return t;
    }

    public long checkedTotal() {
        long t = 0;
        for (JunkItem it : items) if (it.checked) t += it.size;
        return t;
    }
}
