package org.opencron.server.vo;

import java.io.Serializable;

/**
 * Created by benjobs on 2016/12/13.
 */
public class Cropper implements Serializable {

    private Double x;
    private Double y;
    private Double width;
    private Double height;
    private int rotate;

    public Integer getX() {
        return x==null?null:x.intValue();
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Integer getY() {
        return y==null?null:y.intValue();
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Integer getWidth() {
        return width==null?null:width.intValue();
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height==null?null:height.intValue();
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }
}
