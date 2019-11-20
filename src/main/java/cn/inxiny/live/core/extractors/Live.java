package cn.inxiny.live.core.extractors;

import cn.inxiny.live.utils.JsonUtils;

import java.util.Date;

/**
 * Created by huxins on 2019/11/20 17:27
 */
public class Live {
    private Platform platform;      // 平台

    private String roomId;          // 房号
    private String roomName;        // 房名
    private String roomInfo;        // 公告
    private String roomImg;         // 预览
    private boolean online;         // 是否在线
    private Date lastTime;          // 上次在线

    private String ownerName;       // 昵称
    private String ownerAvatar;     // 头像
    private String privateHost;     // 私有房号

    private String link;            // 默认


    public Live() {
    }

    public Live(Platform platform) {
        this.platform = platform;
    }


    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomInfo() {
        return roomInfo;
    }

    public void setRoomInfo(String roomInfo) {
        this.roomInfo = roomInfo;
    }

    public String getRoomImg() {
        return roomImg;
    }

    public void setRoomImg(String roomImg) {
        this.roomImg = roomImg;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerAvatar() {
        return ownerAvatar;
    }

    public void setOwnerAvatar(String ownerAvatar) {
        this.ownerAvatar = ownerAvatar;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Date getLastTime() {
        return lastTime;
    }

    public String getPrivateHost() {
        return privateHost;
    }

    public void setPrivateHost(String privateHost) {
        this.privateHost = privateHost;
    }

    public void setLastTime(Date lastTime) {
        this.lastTime = lastTime;
    }

}
