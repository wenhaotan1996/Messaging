package com.wenhao.cmpe277project;

import java.util.Objects;

public class Message implements Comparable<Message> {
    private String id;
    private String type;
    private String data;
    private String from;

    public Message(String id, String type, String data, String from) {
        this.id = id;
        this.type = type;
        this.data = data;
        this.from = from;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    @Override
    public int compareTo(Message o) {

        if (Integer.parseInt(this.id) < Integer.parseInt(o.id)) return -1;
        else if (Integer.parseInt(this.id) > Integer.parseInt(o.id)) return 1;
        else return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, data, from);
    }
}
