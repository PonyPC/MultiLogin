package moe.caa.multilogin.core.impl;

/**
 * 公共命令执行者对象
 */
public interface ISender {

    /**
     * 这个命令执行者是不是一位玩家
     *
     * @return 是不是玩家
     */
    boolean isPlayer();

    /**
     * 这个命令执行者是否具有某权限
     *
     * @return 是否具有某权限
     */
    boolean hasPermission(String permission);

    /**
     * 给执行者发送特定的字符串消息
     *
     * @param message 发送的字符串消息
     */
    void sendMessage(String message);

    /**
     * 获得这个命令执行者的名称
     *
     * @return 命令执行者的名称
     */
    String getName();

    /**
     * 获取对应的玩家对象
     *
     * @return 对应的玩家对象
     */
    IPlayer getAsPlayer();
}
