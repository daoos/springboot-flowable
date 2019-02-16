package priv.gitonlie.flowable.model;

import java.io.Serializable;

/**
 * 返回参数
 */
public class ReturnParam implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private String code = "";
	
	private String msg = "";
	
	private Object data = null;
	
	
	public static ReturnParam getInstance(String code, String msg) {
		ReturnParam returnParam = new ReturnParam();
		returnParam.setCode(code);
		returnParam.setMsg(msg);
		return returnParam;
	}
	
	public static ReturnParam getInstance(String code, String msg, Object data) {
		ReturnParam returnParam = new ReturnParam();
		returnParam.setCode(code);
		returnParam.setMsg(msg);	
		returnParam.setData(data);
		return returnParam;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
	
}
