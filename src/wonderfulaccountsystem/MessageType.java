package wonderfulaccountsystem;

/**
 * @Author wonderful
 * @Description 消息类型枚举器
 * @Date 2019-8-30
 */
public enum MessageType {

    DATABASE_PASS(10000, "DATABASE_PASS"),
    OPERATION_TYPE(10001, "OPERATION_TYPE"),
    COMMAND(10002, "COMMAND"),
    ACCOUNT_TYPE(10003, "ACCOUNT_TYPE"),
    ACCOUNT(10004, "ACCOUNT"),
    PASSWORD(10005, "PASSWORD"),
    PHONE(10006, "PHONE"),
    REMARK(10007, "REMARK"),
    ENCRYPT_KEY(10008, "ENCRYPT_KEY"),
    EXECUTE(10009, "EXECUTE");
    

    private int code;
    private String msg;

    MessageType(int code, String msg) {
            this.code = code;
            this.msg = msg;
    }

    public int getCode() {
            return code;
    }

    public void setCode(int code) {
            this.code = code;
    }

    public String getMsg() {
            return msg;
    }

    public void setMsg(String msg) {
            this.msg = msg;
    }

    // 根据value返回枚举类型,主要在switch中使用
    public static MessageType getByValue(int value) {
            for (MessageType code : values()) {
                    if (code.getCode() == value) {
                            return code;
                    }
            }
            return null;
    }
}
