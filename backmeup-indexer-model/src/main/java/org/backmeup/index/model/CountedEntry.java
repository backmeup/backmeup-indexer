package org.backmeup.index.model;

public class CountedEntry {

    private String title;
    private int count;

    public CountedEntry() {
    }

    public CountedEntry(String title, int count) {
        if (title.contains(";")) {
            this.title = title.substring(0, title.indexOf(";"));
        } else {
            this.title = title;
        }
        this.count = count;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}