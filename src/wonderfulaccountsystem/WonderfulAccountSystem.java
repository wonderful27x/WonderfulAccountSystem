/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wonderfulaccountsystem;

import java.awt.BorderLayout;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 *
 * @author Acer
 */
public class WonderfulAccountSystem {

    /**
     * @param args the command line arguments
     */
    
    private JFrame frame;
    private JScrollPane scrollPane;
    private JPanel panel;
    private JTextArea text;
    private TextField edit;
    private String input;
    private boolean encryption;
    private EncryptionAlgorithm encryptionAlgorithm; //加密算法

//    private static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver"; 
//    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/secretdocument";
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String MYSQL_URL ="jdbc:mysql://localhost:3306/" + "secretdocument" + "?useSSL=false&serverTimezone=UTC";
    private static final String MYSQL_USER = "root";
    
    private static Connection connection;
    private static Statement statement;   
    public static void main(String[] args) throws IOException, SQLException, InterruptedException {
        // TODO code application logic here

        WonderfulAccountSystem system = new WonderfulAccountSystem();
        system.encryptionAlgorithm = new EncryptionAlgorithmXOR();
                
        try{
            system.initInterface();
            system.initListener();

            boolean initSuccess;
            do{
                int returnType = system.mysqlInit();
                if(returnType == 0){
                    initSuccess = false;
                }else if(returnType == 1){
                    initSuccess = true;
                }else{
                    return;
                }
            }while(!initSuccess);

            reStart:
            while(true){
                system.printMessage("请选择操作模式：加密 or 解密");
                String operationType = system.getInput();
                String command;
                keepwork:while(true){
                    if(operationType.equals("加密")){
                        system.printMessage("正在加密...");
                        system.encrypt();
                    }else if(operationType.equals("解密")){
                        system.printMessage("正在解密...");
                        system.decrypt();
                    }else if(operationType.equals("exit") || operationType.equals("EXIT")){
                        break reStart;
                    }else{
                        continue reStart;   
                    }
                    command = system.getInput();
                    if(command.equals("exit") || operationType.equals("EXIT")){
                        break reStart;
                    }else if(command.equals("")){
                        continue keepwork;
                    }else{
                        continue reStart;
                    }
                }
            }
        }finally{
            if(statement != null){
                statement.close();
            }
            if(connection != null){
                connection.close();
            }
            system.systemClose();
        }
    }
    
    private String getInput(){
        input = null;
        while(input == null){
           System.out.println("");
        }
        return input;
    }
    
    private int mysqlInit() throws IOException, InterruptedException{
        printMessage("数据库初始化...");
        printMessage("请输入密码:");
        edit.requestFocus();
        
        encryption = true;
        changeEditType(true);
        edit.enableInputMethods(false);//禁掉输入法
        String password = getInput();
        
        try {
            Class.forName(MYSQL_DRIVER);
            connection = DriverManager.getConnection(MYSQL_URL,MYSQL_USER,password);
            connection.setAutoCommit(false);
            statement = connection.createStatement();
        } catch (ClassNotFoundException ex) {
            if(password.equals("exit") || password.equals("EXIT")){
                return -1;
            }
            printMessage("初始化异常！");
            Thread.sleep(1000);
            return 0;
        } catch (SQLException ex) {
            if(password.equals("exit") || password.equals("EXIT")){
                return -1;
            }
            printMessage("初始化异常！");
            Thread.sleep(1000);
            return 0;
        }
        
        printMessage("欢迎来到@德芙账号管理系统！");
        return 1;
    }
    
    private boolean encrypt() throws IOException, SQLException{
        String accountType;
        String account;
        String password;
        String phonenum;
        String remark;
        String encryptKey;
        String ciphertext;
        
        printMessage("请输入类型:");
        accountType = getInput();
        printMessage("请输入账号:");
        account = getInput();
        printMessage("请输入密码:");
        encryption = true;
        changeEditType(true);
        edit.enableInputMethods(false);//禁掉输入法
        password = getInput();
        printMessage("请输入绑定手机号:");
        phonenum = getInput();
        printMessage("请输入备注:");
        remark = getInput();
        printMessage("请输入密钥:");
        encryption = true;
        changeEditType(true);
        encryptKey = getInput();
        
        if(password.length()<7){
            printMessage("密码太短，密钥容易被破解,数据将被忽略！");
            connection.rollback();
            return false;
        }
        if(encryptKey.length()<7){
            printMessage("密钥太短，数据将被忽略！");
            connection.rollback();
            return false;
        }
        
        ciphertext = encryptionAlgorithm.encrypt(password,encryptKey);
        
        String insertConent = "insert into secret values (null,'"+accountType+"','"+account+"','"+ciphertext+"','"+phonenum+"','"+remark+"')";
        insertIntoMysql(insertConent);
        
        String executeCommand = getInput();
        if(executeCommand.equals("execute")){
            try {
                connection.commit();
            } catch (SQLException ex) {
                printMessage("执行失败！");
                Logger.getLogger(WonderfulAccountSystem.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            printMessage("错误代码");
            return false;
        }
        
        String queryPass = queryFromMysql(accountType,account);
        queryPass = encryptionAlgorithm.decrypt(queryPass,encryptKey);
        queryPass = transformToRealText(password.length(),queryPass);
        
        if(queryPass == null || !queryPass.equals(password)){
            String sql = "delete from secret where type='"+accountType+"'and account='"+account+"'";
            deleteFromMysql(sql);
            printMessage("密码将无法还原，插入失效！");
            return false;
        }
        
        printMessage("ok");
        return true;
       
    }
    
    private String decrypt() throws IOException, SQLException{
        String accountType;
        String account;
        String encryptKey;
        String originalText = null;
        
        printMessage("请输入类型:");
        accountType = getInput();
        printMessage("请输入账号:");
        account = getInput();
        printMessage("请输入密钥:");
        encryption = true;
        changeEditType(true);
        encryptKey = getInput();
        
        String ciphertext = queryFromMysql(accountType,account);

        originalText = encryptionAlgorithm.decrypt(ciphertext,encryptKey);
        
        printMessage("密码:" + originalText);
        
        return originalText;
    }
    
    private String queryFromMysql(String type,String account) throws SQLException{
        ResultSet result = null;
        String ciphertext = null;
        String sql = "select password from secret where type='"+type+"'and account='"+account+"'";
        try {
             result = statement.executeQuery(sql);
             if(result.next()){
                 ciphertext = result.getString("password");
            } 
        } catch (SQLException ex) {
            printMessage("查询失败！");
            Logger.getLogger(WonderfulAccountSystem.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            if(result != null){
                result.close();
            }
        } 
        
        return ciphertext;
    }
    
    private boolean insertIntoMysql(String insertSql){
        try {
            connection.rollback();
            statement.executeUpdate(insertSql);
        } catch (SQLException ex) {
            printMessage("插入数据失败！");
            Logger.getLogger(WonderfulAccountSystem.class.getName()).log(Level.SEVERE, null, ex);
        } 
        return true;
    }
    
    private boolean deleteFromMysql(String insertSql){
        try {
            connection.rollback();
            statement.executeUpdate(insertSql);
            connection.commit();
        } catch (SQLException ex) {
            printMessage("删除数据失败！");
            Logger.getLogger(WonderfulAccountSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }
    
    private String transformToRealText(int originalLength,String queryText){
        String realText = null;
        realText = queryText.substring(0,originalLength);
        return realText;
    }
    
    private void printMessage(String message) {
        if(message == null || message.length() == 0) {
            return;
        }else{
            setVisibleOfText(true);
        }
        String content;
        if(text.getText().length() > 0) {
            content = text.getText() + "\n" + message;
        }else {
            content = message;
        }

        text.setText(content);
        edit.setText("");
       
    }
    
    private void changeEditType(boolean encryption){
        if(encryption){
            edit.setEchoChar('*');
        }else{
            edit.setEchoChar((char)0);
        }
    }
    
    private void initInterface() {
        frame = new JFrame("wonderful account system");
        scrollPane = new JScrollPane();
        panel = new JPanel();
        text = new JTextArea();
        edit = new TextField();
        
//        Font x = new Font("宋体",Font.PLAIN,14);
//        text.setFont(x);
//        edit.setFont(x);
                
        JTextArea positionHold = new JTextArea();
        positionHold.setEnabled(false);
       
        panel.setLayout(new BorderLayout());
        panel.add(text,BorderLayout.NORTH);
        panel.add(edit,BorderLayout.CENTER);
        panel.add(positionHold,BorderLayout.SOUTH);
        
        scrollPane.setViewportView(panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); //设置水平滚动条自动出现 
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);     //设置垂直滚动条自动出现 
//        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);         //滚动条总是滚动到最后,但滚动条会消失
        
        frame.add(scrollPane);
        frame.setSize(400,300);
        frame.setLocation(400, 300);			              
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	//设置虚拟机和界面一同关闭
        frame.setVisible(true);
        text.setEditable(false);
        text.setBorder(null);
        edit.setSize(600, 500);
        setVisibleOfText(false);
    }
    
    private void systemClose(){
        printMessage("system is closing...");
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(WonderfulAccountSystem.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            frame.dispose();
        }
    }
    
    private void setVisibleOfText(boolean visible) {
        if(visible) {
            if(!text.isVisible()) {
                text.setVisible(true);
            }
        }else {
            if(text.isVisible()) {
                text.setVisible(false);
            }
        }
    }
    
    private void initListener() {
        Listener listener = new Listener();
        edit.addKeyListener(listener);
    }
    
    class Listener implements KeyListener{

        @Override
        public void keyPressed(KeyEvent arg0) {
            // TODO Auto-generated method stub
            if(arg0.getKeyCode() == KeyEvent.VK_CONTROL) {
                if(encryption){
                    changeEditType(false);
                }
            }else if(arg0.getKeyCode() == KeyEvent.VK_ENTER) {
                String inputString = edit.getText();
                if(encryption){
                    printMessage(encryptString(inputString));
                }else{
                    printMessage(inputString);
                }
                input = inputString;
                
                reset();
            }
        }

        @Override
        public void keyReleased(KeyEvent arg0) {
            // TODO Auto-generated method stub
            if(arg0.getKeyCode() == KeyEvent.VK_CONTROL) {
                if(encryption){
                    changeEditType(true);
                }
            }else if(arg0.getKeyCode() == KeyEvent.VK_ENTER) {
                scrollToBottom();
            }
        }

        @Override
        public void keyTyped(KeyEvent arg0) {
            // TODO Auto-generated method stub
        }

    }
    
    private void scrollToBottom(){
        JScrollBar scrollBar = scrollPane.getVerticalScrollBar(); //得到JScrollPane中的JScrollBar 
        scrollBar.setValue(scrollBar.getMaximum());               //设置JScrollBar的位置到最后 
    }
    
    private void reset(){
        encryption = false;
        changeEditType(false);
        edit.enableInputMethods(true);//开启输入法
    }
    
    private String encryptString(String text) {
        StringBuilder builder = new StringBuilder();
        for(int i=0; i<text.length(); i++) {
            builder.append("*");
        }
        return builder.toString();
    }
    
}






//package wonderfulaccountsystem;
//
//import java.awt.BorderLayout;
//import java.awt.event.KeyEvent;
//import java.awt.event.KeyListener;
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.UnsupportedEncodingException;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.swing.JFrame;
//import static javax.swing.JFrame.EXIT_ON_CLOSE;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//import javax.swing.JScrollBar;
//import javax.swing.JScrollPane;
//import javax.swing.JTextArea;
//import javax.swing.KeyStroke;
//import javax.swing.SwingUtilities;
//import javax.swing.WindowConstants;
//import javax.swing.event.DocumentEvent;
//import javax.swing.event.DocumentListener;
//import javax.swing.text.BadLocationException;
//import javax.swing.text.Document;
//
///**
// *
// * @author Acer
// */
//public class WonderfulAccountSystem {
//
//    /**
//     * @param args the command line arguments
//     */
//    
//    private JFrame frame;
//    private JScrollPane scrollPane;
//    private JPanel panel;
//    private JTextArea text;
//    private JTextArea edit;
//    private int encryption = -1; //0:密码；1：密钥
//    private StringBuilder stringBuilder = new StringBuilder();
//    private String input = null;
//    private int type;
//
//    private static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";
//    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/account";
//    private static final String MYSQL_USER = "root";
//    
//    private static Connection connection;
//    private static Statement statement;   
//    public static void main(String[] args) throws IOException, SQLException, InterruptedException {
//        // TODO code application logic here
//
//        WonderfulAccountSystem system = new WonderfulAccountSystem();
//        try{
//            system.initInterface();
//            system.initListener();
//
//            boolean initSuccess;
//            do{
//                int returnType = system.mysqlInit();
//                if(returnType == 0){
//                    initSuccess = false;
//                }else if(returnType == 1){
//                    initSuccess = true;
//                }else{
//                    return;
//                }
//            }while(!initSuccess);
//
//            reStart:
//            while(true){
//                system.printMessage("请选择操作模式：加密 or 解密");
//                String operationType = system.getInput(MessageType.OPERATION_TYPE.getCode());
//                String command;
//                keepwork:while(true){
//                    if(operationType.equals("加密")){
//                        system.printMessage("正在加密...");
//                        system.encrypt();
//                    }else if(operationType.equals("解密")){
//                        system.printMessage("正在解密...");
//                        system.decrypt();
//                    }else if(operationType.equals("exit")){
//                        break reStart;
//                    }else{
//                        continue reStart;   
//                    }
//                    command = system.getInput(MessageType.COMMAND.getCode());
//                    if(command.equals("exit")){
//                        break reStart;
//                    }else if(command.equals("")){
//                        continue keepwork;
//                    }else{
//                        System.out.println(command);
//                        continue reStart;
//                    }
//                }
//            }
//        }finally{
//            if(statement != null){
//                statement.close();
//            }
//            if(connection != null){
//                connection.close();
//            }
//            system.systemClose();
//        }
//    }
//    
//    private String encryptDecrypt(String text ,String key){
//        if(text == null || key == null){
//            return null;
//        }
//        int lengthMax = text.length()>key.length()?text.length():key.length();
//
//        char[] textChar = text.toCharArray();
//        char[] keyChar = key.toCharArray();
//        int[] textInt = new int[lengthMax];
//        int[] keyInt = new int[lengthMax];
//        
//        for(int i=0; i<lengthMax; i++){
//            textInt[i] = 15;
//            keyInt[i] = 15;
//        }
//
//        StringBuilder result = new StringBuilder();
//
//        String binaryString = null;
//        char c;
//
//        for(int i=0; i<textChar.length; i++){
//            binaryString = Integer.toBinaryString(textChar[i]);
//            textInt[i] = Integer.parseInt(binaryString,2);
//        }
//
//        for(int i=0; i<keyChar.length; i++){
//            binaryString = Integer.toBinaryString(keyChar[i]);
//            keyInt[i] = Integer.parseInt(binaryString,2);
//        }
//
//        for(int i=0; i<lengthMax; i++) {
//            c = (char)(textInt[i]^keyInt[i]);
//            result.append(c);
//        }
//
//        return result.toString();
//    }
//    
//    private String getInput(int type){
//        this.type = type;
//        input = null;
//        while(input == null){
//           System.out.println("");
//        }
//        return input;
//    }
//    
//    private int mysqlInit() throws IOException, InterruptedException{
//        printMessage("数据库初始化...");
//        printMessage("请输入密码:");
//        edit.requestFocus();
//        
//        encryption = 0;
//        edit.enableInputMethods(false);//禁掉输入法
//        String password = getInput(MessageType.DATABASE_PASS.getCode());
//        System.out.println(password);
//        System.out.println(password.equals("wonderful123456"));
//        try {
//            Class.forName(MYSQL_DRIVER);
//            connection = DriverManager.getConnection(MYSQL_URL,MYSQL_USER,password);
//            connection.setAutoCommit(false);
//            statement = connection.createStatement();
//        } catch (ClassNotFoundException ex) {
//            if(password.equals("exit")){
//                return -1;
//            }
//            printMessage("初始化异常！");
//            Thread.sleep(1000);
//            return 0;
//        } catch (SQLException ex) {
//            if(password.equals("exit")){
//                return -1;
//            }
//            printMessage("初始化异常！");
//            Thread.sleep(1000);
//            return 0;
//        }
//        
//        printMessage("欢迎来到@德芙账号管理系统！");
//        return 1;
//    }
//    
//    private boolean encrypt() throws IOException, SQLException{
//        String accountType;
//        String account;
//        String password;
//        String phonenum;
//        String remark;
//        String encryptKey;
//        String ciphertext;
//        
//        printMessage("请输入类型:");
//        accountType = getInput(MessageType.ACCOUNT_TYPE.getCode());
//        printMessage("请输入账号:");
//        account = getInput(MessageType.ACCOUNT.getCode());
//        printMessage("请输入密码:");
//        encryption = 0;
//        edit.enableInputMethods(false);//禁掉输入法
//        password = getInput(MessageType.PASSWORD.getCode());
//        printMessage("请输入绑定手机号:");
//        phonenum = getInput(MessageType.PHONE.getCode());
//        printMessage("请输入备注:");
//        remark = getInput(MessageType.REMARK.getCode());
//        printMessage("请输入密钥:");
//        encryption = 1;
//        encryptKey = getInput(MessageType.ENCRYPT_KEY.getCode());
//        
//        if(password.length()<7){
//            printMessage("密码太短，密钥容易被破解,数据将被忽略！");
//            connection.rollback();
//            return false;
//        }
//        if(encryptKey.length()<7){
//            printMessage("密钥太短，数据将被忽略！");
//            connection.rollback();
//            return false;
//        }
//        
//        ciphertext = encryptDecrypt(password,encryptKey);
//        
//        String insertConent = "insert into secret values (null,'"+accountType+"','"+account+"','"+ciphertext+"','"+phonenum+"','"+remark+"')";
//        insertIntoMysql(insertConent);
//        
//        String executeCommand = getInput(MessageType.EXECUTE.getCode());
//        if(executeCommand.equals("execute")){
//            try {
//                connection.commit();
//            } catch (SQLException ex) {
//                printMessage("执行失败！");
//                Logger.getLogger(WonderfulAccountSystem.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }else{
//            printMessage("错误代码");
//            return false;
//        }
//        
//        String queryPass = queryFromMysql(accountType,account);
//        queryPass = encryptDecrypt(queryPass,encryptKey);
//        queryPass = transformToRealText(password.length(),queryPass);
//        
//        if(queryPass == null || !queryPass.equals(password)){
//            String sql = "delete from secret where type='"+accountType+"'and account='"+account+"'";
//            deleteFromMysql(sql);
//            printMessage("密码将无法还原，插入失效！");
//            return false;
//        }
//        
//        printMessage("ok");
//        return true;
//       
//    }
//    
//    private String decrypt() throws IOException, SQLException{
//        String accountType;
//        String account;
//        String encryptKey;
//        String originalText = null;
//        
//        printMessage("请输入类型:");
//        accountType = getInput(MessageType.ACCOUNT_TYPE.getCode());
//        printMessage("请输入账号:");
//        account = getInput(MessageType.ACCOUNT.getCode());
//        printMessage("请输入密钥:");
//        encryption = 1;
//        encryptKey = getInput(MessageType.ENCRYPT_KEY.getCode());
//        
//        String ciphertext = queryFromMysql(accountType,account);
//
//        originalText = encryptDecrypt(ciphertext,encryptKey);
//        
//        printMessage("密码:" + originalText);
//        
//        return originalText;
//    }
//    
//    private String queryFromMysql(String type,String account) throws SQLException{
//        ResultSet result = null;
//        String ciphertext = null;
//        String sql = "select password from secret where type='"+type+"'and account='"+account+"'";
//        try {
//             result = statement.executeQuery(sql);
//             if(result.next()){
//                 ciphertext = result.getString("password");
//            } 
//        } catch (SQLException ex) {
//            printMessage("查询失败！");
//            Logger.getLogger(WonderfulAccountSystem.class.getName()).log(Level.SEVERE, null, ex);
//        }finally{
//            if(result != null){
//                result.close();
//            }
//        } 
//        
//        return ciphertext;
//    }
//    
//    private boolean insertIntoMysql(String insertSql){
//        try {
//            connection.rollback();
//            statement.executeUpdate(insertSql);
//        } catch (SQLException ex) {
//            printMessage("插入数据失败！");
//            Logger.getLogger(WonderfulAccountSystem.class.getName()).log(Level.SEVERE, null, ex);
//        } 
//        return true;
//    }
//    
//    private boolean deleteFromMysql(String insertSql){
//        try {
//            connection.rollback();
//            statement.executeUpdate(insertSql);
//            connection.commit();
//        } catch (SQLException ex) {
//            printMessage("删除数据失败！");
//            Logger.getLogger(WonderfulAccountSystem.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return true;
//    }
//    
//    private String transformToRealText(int originalLength,String queryText){
//        String realText = null;
//        realText = queryText.substring(0,originalLength);
//        return realText;
//    }
//    
//    private void printMessage(String message) {
//        setVisibleOfText(true);
//        String content;
//        if(text.getText().length() > 0) {
//            content = text.getText() + "\n" + message;
//        }else {
//            content = message;
//        }
//
//        text.setText(content);
//        edit.setText("");
//        scrollToBottom();
//    }
//    
//    private void updateMessage() {
//        String content;
//        String textContent = edit.getText();
//        
//        if(textContent.length() == 0) {
//            return;
//        }else{
//            setVisibleOfText(true);
//        }
//
//        if(text.getText().length() > 0) {
//            content = text.getText() + "\n" + textContent;
//        }else {
//            content = textContent;
//        }
//
//        text.setText(content);
//        edit.setText("");
//        scrollToBottom();
//    }
//    
//    private void initInterface() {
//        frame = new JFrame("wonderful account system");
//        scrollPane = new JScrollPane();
//        panel = new JPanel();
//        text = new JTextArea();
//        edit = new JTextArea();
//        
//        JTextArea positionHold = new JTextArea();
//        positionHold.setEnabled(false);
//        
//        panel.setLayout(new BorderLayout());
//        panel.add(text,BorderLayout.NORTH);
//        panel.add(edit,BorderLayout.CENTER);
//        panel.add(positionHold,BorderLayout.SOUTH);
//        scrollPane.setViewportView(panel);
//        frame.add(scrollPane);
//        frame.setSize(400,300);
//        frame.setLocation(400, 300);			              
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	//设置虚拟机和界面一同关闭
//        frame.setVisible(true);
//        text.setEditable(false);
//        text.setBorder(null);
//        edit.setBorder(null);
//        setVisibleOfText(false);
//        KeyStroke enter = KeyStroke.getKeyStroke("ENTER");
//        edit.getInputMap().put(enter, "none");
//    }
//    
//    private void systemClose(){
//        printMessage("system is closing...");
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(WonderfulAccountSystem.class.getName()).log(Level.SEVERE, null, ex);
//        }finally{
//            frame.dispose();
//        }
//    }
//    
//    private void setVisibleOfText(boolean visible) {
//        if(visible) {
//            if(!text.isVisible()) {
//                text.setVisible(true);
//            }
//        }else {
//            if(text.isVisible()) {
//                text.setVisible(false);
//            }
//        }
//    }
//    
//    private void initListener() {
//        Listener listener = new Listener();
//        edit.addKeyListener(listener);
//    }
//    
//    class Listener implements KeyListener{
//
//        @Override
//        public void keyPressed(KeyEvent arg0) {
//            // TODO Auto-generated method stub
//            System.out.println("press");
//            SwingUtilities.invokeLater(new Runnable() {
//                @Override
//                public void run() {
//                    if(arg0.getKeyCode() == KeyEvent.VK_CONTROL) {
//                        edit.setText(stringBuilder.toString());
//                        System.out.println("shift");
//                    }
//                    if(arg0.getKeyCode() == KeyEvent.VK_BACK_SPACE){
//                        if(stringBuilder.length()>0) {
//                            stringBuilder.delete(stringBuilder.length()-1, stringBuilder.length());
//                            System.out.println("VK_BACK_SPACE");
//                        }
//                    }            
//                }
//            });
//        }
//
//        @Override
//        public void keyReleased(KeyEvent arg0) {
//            // TODO Auto-generated method stub
//            System.out.println("up");
//            SwingUtilities.invokeLater(new Runnable() {
//                @Override
//                public void run() {
//                    
//                    if(encryption == 0 && arg0.getKeyCode() != KeyEvent.VK_CONTROL) {
//                        String encryptString = encryptString(edit.getText());
//                        edit.setText(encryptString);
//                    }
//
//                    if(arg0.getKeyCode() == KeyEvent.VK_CONTROL) {
//                        String encryptString = encryptString(edit.getText());
//                        edit.setText(encryptString);  
//                    }
//
//                    if(arg0.getKeyCode() == KeyEvent.VK_ENTER) {
//                        String inputString;
//                        switch(MessageType.getByValue(type)){
//                            case DATABASE_PASS:
//                                inputString = stringBuilder.toString();
//                                updateMessage();
//                                input = inputString;
//                                break;
//                            case OPERATION_TYPE:
//                                inputString = edit.getText();
//                                updateMessage();
//                                input = inputString;
//                                break;
//                            case COMMAND:
//                                inputString = edit.getText();
//                                updateMessage();
//                                input = inputString;
//                                break;
//                            case ACCOUNT_TYPE:
//                                inputString = edit.getText();
//                                updateMessage();
//                                input = inputString;
//                                break;
//                            case ACCOUNT:
//                                inputString = edit.getText();
//                                updateMessage();
//                                input = inputString;
//                                break;
//                            case PASSWORD:
//                                inputString = stringBuilder.toString();
//                                updateMessage();
//                                input = inputString;
//                                break;
//                            case PHONE:
//                                inputString = edit.getText();
//                                updateMessage();
//                                input = inputString;
//                                break;
//                            case REMARK:
//                                inputString = edit.getText();
//                                updateMessage();
//                                input = inputString;
//                                break;
//                            case ENCRYPT_KEY:
//                                inputString = edit.getText();
//                                updateMessage();
//                                input = inputString;
//                                break;
//                            case EXECUTE:
//                                inputString = edit.getText();
//                                updateMessage();
//                                input = inputString;
//                                break;
//                        }
//
//                        reset();
//                    }
//                }
//            });
//        }
//
//        @Override
//        public void keyTyped(KeyEvent arg0) {
//            // TODO Auto-generated method stub
//            System.out.println("type");
//            SwingUtilities.invokeLater(new Runnable() {
//                @Override
//                public void run() {
//                    String content = edit.getText();
//                    int start = stringBuilder.length();
//                    int end = content.length();
//                    if(start != -1 && end != -1 && end >= start){
//                        content = content.substring(stringBuilder.length(),content.length());
//                        stringBuilder.append(content);
//                        if(encryption != -1){
//                            String encryptString = encryptString(edit.getText());
//                            edit.setText(encryptString); 
//                        }
//                    }
//                }
//            });
//
//        }
//
//    }
//    
//    private void scrollToBottom(){
//        JScrollBar scrollBar = scrollPane.getVerticalScrollBar(); //得到JScrollPane中的JScrollBar 
//        scrollBar.setValue(scrollBar.getMaximum());               //设置JScrollBar的位置到最后 
//    }
//    
//    private void reset(){
//        encryption = -1;
//        stringBuilder.setLength(0);
//        edit.enableInputMethods(true);//开启输入法
//        scrollToBottom();
//    }
//    
//    private String encryptString(String text) {
//        StringBuilder builder = new StringBuilder();
//        for(int i=0; i<text.length(); i++) {
//            builder.append("*");
//        }
//        return builder.toString();
//    }
//    
//}
