package priv.gitonlie.flowable.engine.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

public class Tools {
	
	/**
	 * <p>JSON校验</p>
	 * @param content
	 * @return
	 */
	public static boolean isJson(String content){
	try {
			JSONObject.parseObject(content);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * <p>JSON字符串转换成map集合</p>
	 * @param content
	 * @return
	 */
	public static Map<String,Object> transform(String content) {
		Map<String,Object> res = new HashMap<String, Object>();
		JSONObject obj = JSONObject.parseObject(content);
		Iterator<String> keys = obj.keySet().iterator();
		while(keys.hasNext()) {
			String key = keys.next();
			res.put(key, obj.get(key));
		}
		return res;		
	}
}
