/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wonderfulaccountsystem;

/**
 * @Author wonderful
 * @Description 异或加密算法
 * @Date 2019-09-28
 */
public class EncryptionAlgorithmXOR implements EncryptionAlgorithm{

    @Override
    public String encrypt(String original, String encryptKey) {
        if(original == null || encryptKey == null){
            return null;
        }
        int lengthMax = original.length()>encryptKey.length()?original.length():encryptKey.length();

        char[] textChar = original.toCharArray();
        char[] keyChar = encryptKey.toCharArray();
        int[] textInt = new int[lengthMax];
        int[] keyInt = new int[lengthMax];
        
        for(int i=0; i<lengthMax; i++){
            textInt[i] = 15;
            keyInt[i] = 15;
        }

        StringBuilder result = new StringBuilder();

        String binaryString = null;
        char c;

        for(int i=0; i<textChar.length; i++){
            binaryString = Integer.toBinaryString(textChar[i]);
            textInt[i] = Integer.parseInt(binaryString,2);
        }

        for(int i=0; i<keyChar.length; i++){
            binaryString = Integer.toBinaryString(keyChar[i]);
            keyInt[i] = Integer.parseInt(binaryString,2);
        }

        for(int i=0; i<lengthMax; i++) {
            c = (char)(textInt[i]^keyInt[i]);
            result.append(c);
        }

        return result.toString();
    }

    @Override
    public String decrypt(String ciphertext, String encryptKey) {
        if(ciphertext == null || encryptKey == null){
            return null;
        }
        int lengthMax = ciphertext.length()>encryptKey.length()?ciphertext.length():encryptKey.length();

        char[] textChar = ciphertext.toCharArray();
        char[] keyChar = encryptKey.toCharArray();
        int[] textInt = new int[lengthMax];
        int[] keyInt = new int[lengthMax];
        
        for(int i=0; i<lengthMax; i++){
            textInt[i] = 15;
            keyInt[i] = 15;
        }

        StringBuilder result = new StringBuilder();

        String binaryString = null;
        char c;

        for(int i=0; i<textChar.length; i++){
            binaryString = Integer.toBinaryString(textChar[i]);
            textInt[i] = Integer.parseInt(binaryString,2);
        }

        for(int i=0; i<keyChar.length; i++){
            binaryString = Integer.toBinaryString(keyChar[i]);
            keyInt[i] = Integer.parseInt(binaryString,2);
        }

        for(int i=0; i<lengthMax; i++) {
            c = (char)(textInt[i]^keyInt[i]);
            result.append(c);
        }

        return result.toString();
    }

}
