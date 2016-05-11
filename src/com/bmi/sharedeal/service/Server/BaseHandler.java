package com.bmi.sharedeal.service.Server;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.nutz.dao.Dao;
import org.nutz.json.Json;

import com.bmi.sharedeal.service.Config;
import com.bmi.sharedeal.service.utils.TextUtils;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class BaseHandler implements HttpHandler{

	public Dao dao;
	
	private Headers requestHeaders;
	private Headers responseHeaders;
	private Map arugments;
	
	protected String response;
	
	public BaseHandler() {
        dao = Config.getDao();
    }
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		requestHeaders = exchange.getRequestHeaders();
		responseHeaders = exchange.getResponseHeaders();
		arugments = (Map)exchange.getAttribute("parameters");
		boolean checkApiKey = checkApiKey();
//		System.out.println("checkApiKey��ֵ��" + checkApiKey);
		if(checkApiKey()){
			invokeMethod(exchange.getHttpContext().getPath());
		}		
		
		responseHeaders.set("Content-Type", "application/json; charset=utf-8;");
		responseHeaders.set("Server", Config.ServerName);
		responseHeaders.set("Access-Control-Allow-Origin", "*");
		
		byte[] bytes = response.getBytes("UTF-8");
		exchange.sendResponseHeaders(200, bytes.length);
		OutputStream responseBody = exchange.getResponseBody();
		responseBody.write(bytes);
		responseBody.close();
		
		exchange.close();
	}
	
	/**
	 * 
	 * @param key
	 * @return
	 */
	public Object getArgument (String key) {
		return this.arugments.get(key);
	}

	/**
	 * дResponse(Json�ַ���)
	 * @param isOk
	 * @param errString
	 * @param data
	 */
	private void writeResponse(boolean isOk, String errString, Map data) {
		Map res = new HashMap();
		res.put("invoke", isOk);
		
		if (isOk) {
			res.put("result", data);
		} else {
			res.put("error", errString);
		}
		
		response = Json.toJson(res);
	}
	
	/**
     * (���óɹ�)������
     * @param data
     */
    public void writeResult(Map data) {
        writeResponse(true, "", data);
    }
	
	/**
	 * (����ʧ��)���������Ϣ
	 * @param errString
	 */
	public void writeError(String errStr) {
		writeResponse(false, errStr, null);
	}
	
	/**
	 * ͨ��������ö�Ӧ��api����
	 * @param urlPath
	 */
	private void invokeMethod(String urlPath) {
		String method = urlPath.substring(urlPath.lastIndexOf('/') + 1);
//		System.out.println("method��" + method);
		if (TextUtils.isEmpty(method)) {
			writeError("δָ��api");
		}
		
		boolean isInvokeOk = true;
		long startTime = System.currentTimeMillis();
		
		Class me = this.getClass();
		System.out.println("me:" + me);
		try {
			Method queryMethod = me.getMethod("api_" + method);
			queryMethod.invoke(this);
		} catch (NoSuchMethodException e){
			writeError("δ�ҵ�api" + method);
		} catch (Exception e) {
			if (Config.DEBUG) {
				e.printStackTrace();
			}
			
			if (TextUtils.isEmpty(response)) {
				writeError("�������ڲ�����: " + e.getMessage());
			}
			
			isInvokeOk = false;
		}
		
		System.out.println("test1****method:" + method);
		
		//��¼��������Ӧʱ��
		if (!method.equals("status")) {
			long costTime = System.currentTimeMillis() - startTime;
			ApiMonitor.get().addRecord(urlPath, !isInvokeOk, (int) costTime);
		}
	}

	/**
     * api��Ȩ���
     * @return
     */
    public boolean checkApiKey() {
        String apiKey = (String) getArgument("apikey");
        if (TextUtils.isEmpty(apiKey) || !apiKey.equals(Config.ApiKey)) {
            writeError("δ��Ȩ����");
            return false;
        }

        return true;
    }
}
