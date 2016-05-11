package com.bmi.sharedeal.service.Server;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import com.bmi.sharedeal.service.Config;
import com.bmi.sharedeal.service.Handler.UserHandler;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

/**
 * 
 * @author xiaoyu
 * @data : 2015/8/14
 */

public class BmiServer {
	private int port = 80;
	private int threadCnt = 10;
	
	private HttpServer httpServer = null;
	
	public BmiServer (int port, int threadCnt) {
		this.port = port;
		this.threadCnt = threadCnt;
		
		try {
			httpServer = HttpServer.create(new InetSocketAddress(port), threadCnt);
		} catch (IOException e) {
			if (Config.DEBUG){
				e.printStackTrace();
			}
		}
	}
	
	public boolean addHandler (String path, BaseHandler handler) {
		if( httpServer == null){
			return false;
		}
		
		//A filter used to pre- and post-process incoming requests.
		ParameterFilter filter = new ParameterFilter();
		
		try {
			//getClass������������java.lang.Object��̳еõ����ú������ش�Object������ʱ��
			Class<?> c = handler.getClass();	
			Method methods[] = c.getMethods();
			for (Method m : methods) {
				//�� String ��ʽ���ش�Method�����ʾ�ķ�������
				String name = m.getName();
				System.out.println("name��ֵ��" + name);
				if (name.startsWith("api_")) {
					HttpContext context = httpServer.createContext(path + name.substring(4), handler);
					context.getFilters().add(filter);
				}
			}
			
		} catch (Exception e) {
			if (Config.DEBUG){
				e.printStackTrace();
			}
		}
		
		return true;
	}
	
	public boolean start() {
		if (httpServer == null) {
			return false;
		}
		
		httpServer.start();		
		System.out.println("Server Listen port " + port + "...");
		return true;
	}
}
