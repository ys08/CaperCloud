/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

/**
 *
 * @author shuai
 */
public class Account {
    private String nickname;
    private String accessKey;
    private String privateKey;

    public Account(String nickname, String accessKey, String privateKey) {
        this.nickname = nickname;
        this.accessKey = accessKey;
        this.privateKey = privateKey;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
    
    
}
