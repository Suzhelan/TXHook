package moe.ore.tars.support;

import java.util.List;

public class TarsStructInfo {
    private List<TarsStrutPropertyInfo> propertyList;
    private String comment;

    public List<TarsStrutPropertyInfo> getPropertyList() {
        return propertyList;
    }

    public void setPropertyList(List<TarsStrutPropertyInfo> propertyList) {
        this.propertyList = propertyList;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
