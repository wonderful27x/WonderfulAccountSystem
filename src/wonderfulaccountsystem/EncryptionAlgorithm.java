/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wonderfulaccountsystem;

/**
 * @Author wonderful
 * @Description 加密算法接口
 * @Date 2019-09-28
 */
public interface EncryptionAlgorithm {
    
    public String encrypt(String password,String encryptKey);
    public String decrypt(String encryptPass,String encryptKey);
    
}
