package com.bmi.sharedeal.service.Handler;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nutz.dao.Cnd;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;

import com.bmi.sharedeal.service.DAO.Buy;
import com.bmi.sharedeal.service.DAO.DaoConst;
import com.bmi.sharedeal.service.DAO.Sell;
import com.bmi.sharedeal.service.DAO.User;
import com.bmi.sharedeal.service.Server.BaseHandler;
import com.bmi.sharedeal.service.utils.TextUtils;

public class SellHandler extends BaseHandler{
	//�ж��Ƿ��¼
	
	//�õ���¼�û�id
	
	//�õ�������
	
	//����t_sell���t_user��
	
	/**
	 * ������Ʊ
	 */
	public void api_sellShares() {
		//�ж��Ƿ��¼
		User user = UserHandler.checkUserAuth(this);
		if(user == null) {
			return;
		}
		
		//��д������Ϣ
		String _price = (String) this.getArgument("price");
		String _shareNum = (String) this.getArgument("sharenum");
		
		if( TextUtils.isEmpty(_shareNum) || TextUtils.isEmpty(_price)) {
			this.writeError("��������");
			return;
		}
		
		float price = Float.parseFloat(_price);
		int shareNum = Integer.parseInt(_shareNum);
				
		if(price <= 0 || shareNum <= 0) {
			return;
		}
		
		int frozenNum = user.getFrozenNum();
		int tradableNum = user.getTradableNum();
		if(shareNum > tradableNum) {
			return;
		}
		
		Sell sell = new Sell();
		sell.setUserId(user.getId());
		sell.setTime(new Date());
		sell.setBuyOrSell(DaoConst.BUYORSELL_SELL);
		sell.setPrice(price);
		sell.setShareNum(shareNum);
		sell.setFinishedShareNum(0);
		sell.setState(DaoConst.STATE_INITIAL);
		
		user.setTradableNum(tradableNum - shareNum);
		user.setFrozenNum(frozenNum + shareNum);
		
		//����t_sell���t_user��
		if(dao.insert(sell) == null || dao.update(user) == 0) {
			this.writeError("���ݿ��������");
            return;
		}
		
		//��Ͻ���
		DealHandler deal = new DealHandler();
		System.out.println("***********************************************sell.getId():" + sell.getId());
		deal.doSell(sell.getId());
				
		//�õ���Ͻ���֮���ί����Ϣ
		sell = dao.fetch(Sell.class, Cnd.where("id", "=", sell.getId()));		
		Map res = new HashMap();
		res.put("sell", sell);
		this.writeResult(res);
	}
	
	/**
	 * ��ѯ���5��ί��������Ϣ
	 */
	
	public void api_getSellInfo() {
		
//		Sql sql = Sqls.queryEntity("SELECT * FROM t_sell where state < " + DaoConst.STATE_FINISHIN + " ORDER BY id DESC LIMIT " + DaoConst.DEALNUM_SELL + "");
//		sql.setCondition(Cnd.orderBy().desc("price"));
		Sql sql = Sqls.queryEntity("SELECT price, SUM(sharenum) AS sharenum FROM t_sell WHERE state < " 
				+ DaoConst.STATE_FINISHIN + " GROUP BY price LIMIT " + DaoConst.DEALNUM_BUY + "");
		sql.setEntity(dao.getEntity(Sell.class));
		dao.execute(sql);
		List<Sell> sells = sql.getList(Sell.class);
		
		Map res = new HashMap();
		res.put("sells", sells);
		
		this.writeResult(res);
	}
}
