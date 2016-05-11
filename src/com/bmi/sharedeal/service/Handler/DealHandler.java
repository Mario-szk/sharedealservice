package com.bmi.sharedeal.service.Handler;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nutz.dao.Cnd;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;

import com.bmi.sharedeal.service.DAO.*;
import com.bmi.sharedeal.service.Server.BaseHandler;

/**
 * ʵ�ֽ��׹���api_doBuy��api_doSell
 * ��ȡƽ̨�ɽ���Ϣ api_getDealInfo
 * @author xiaoyu
 *
 */
public class DealHandler extends BaseHandler {
	/**
	 * ���û��������ʱ���ô˺���ʵ�����빦��
	 * @param buyId
	 */
	public void doBuy(int buyId) {
		//��ȡί��������t_sell�е���Ϣ�����������
		List<Sell> sells = dao.query(Sell.class, Cnd.where("state", "<", DaoConst.STATE_FINISHIN).asc("price"));
		//���t_sells��Ϊ�գ��򷵻�
		if (sells.size() == 0) {
			return;
		}
				
		//�Ƚϴ˴�ί������۸��ί���������еļ۸����ɽ������t_buy��t_sell�Լ�t_user���е���Ϣ
		Buy buy = dao.fetch(Buy.class, Cnd.where("id", "=", buyId));
		float buyPrice = buy.getPrice();
		int buyNum = buy.getShareNum() - buy.getFinishedShareNum();
		int buyerId = buy.getUserId();
		
		int i = 0;
		Sell sell = sells.get(i);		
		int finishedShareNum = sell.getFinishedShareNum();
		int sellNum = sell.getShareNum() - finishedShareNum;
		float sellPrice = sell.getPrice();
		int sellId = sell.getId();	
		int sellerId = sell.getUserId();

		float dealPrice;
		//ί������۸���ڵ���ί�������۸��ֵ����ɽ�		
		while (buyNum > 0 && buyPrice >= sellPrice) {				 				
			//����t_buy,t_sell,t_user,t_deal
			dealPrice = (buyPrice + sellPrice)/2;
			if (buyNum <= sellNum) {
				//t_buy(����)--�ɽ���Ʊ��ΪbuyNum
				buy.setFinishedShareNum(buy.getFinishedShareNum() + buyNum);
				buy.setState(DaoConst.STATE_FINISHIN);
				
				//t_sell(����)
				sell.setFinishedShareNum(finishedShareNum + buyNum);
				if ( buyNum == sellNum) {
					sell.setState(DaoConst.STATE_FINISHIN);
				} else {
					sell.setState(DaoConst.STATE_PARTIAL);
				}
									
				//t_user(����)
				User buyer = dao.fetch(User.class, Cnd.where("id", "=", buyerId));
				buyer.setTradableNum(buyer.getTradableNum() + buyNum);
				//ע��ʵ�ʳɽ��۸��ί�м۸�֮��Զ����ʽ��Ӱ��
				buyer.setFrozenFund(buyer.getFrozenFund() - buyNum * buyPrice);				
				buyer.setUsableFund(buyer.getUsableFund() + buyNum * (buyPrice - dealPrice));
				User seller = dao.fetch(User.class, Cnd.where("id", "=", sellerId));
				seller.setFrozenNum(seller.getFrozenNum() - buyNum);
				seller.setUsableFund(seller.getUsableFund() + buyNum * dealPrice);
				
				//t_deal(����)
				Deal deal = new Deal();
				deal.setBuyId(buyId);
				deal.setSellId(sellId);
				deal.setBuyerId(buyerId);
				deal.setSellerId(sellerId);
				deal.setTime(new Date());
				deal.setPrice(dealPrice);
				deal.setShareNum(buyNum);
				
				//�������ݿ�
				if (dao.update(buy) <= 0 || dao.update(sell) <= 0 || dao.update(buyer) <= 0 
						|| dao.update(seller) <= 0 || dao.insert(deal) == null) {
					this.writeError("���ݿ��������");
		            return;
				}
				
				//����buyNumֵ
				buyNum = buyNum - sellNum;
				
			} else if (buyNum > sellNum) {
				//ƥ��t_sell������һ������
				
				//t_buy
				buy.setFinishedShareNum(buy.getFinishedShareNum() + sellNum);
				buy.setState(DaoConst.STATE_PARTIAL);
				
				//t_sell
				sell.setFinishedShareNum(finishedShareNum + sellNum);
				sell.setState(DaoConst.STATE_FINISHIN);
				
				//t_user
				User buyer = dao.fetch(User.class, Cnd.where("id", "=", buyerId));
				buyer.setTradableNum(buyer.getTradableNum() + sellNum);
//				System.out.println("******************************************************");
//				System.out.println("buyer frozenFund:" + buyer.getFrozenFund());
//				System.out.println("******************************************************");
				buyer.setFrozenFund(buyer.getFrozenFund() - sellNum * buyPrice);
				buyer.setUsableFund(buyer.getUsableFund() + sellNum * (buyPrice - dealPrice));
				User seller = dao.fetch(User.class, Cnd.where("id", "=", sellerId));
				seller.setFrozenNum(seller.getFrozenNum() - sellNum);
				seller.setUsableFund(seller.getUsableFund() + sellNum * dealPrice);
				
				//t_deal
				Deal deal = new Deal();
				deal.setBuyId(buyId);
				deal.setSellId(sellId);
				deal.setBuyerId(buyerId);
				deal.setSellerId(sellerId);
				deal.setTime(new Date());
				deal.setPrice(dealPrice);
				deal.setShareNum(sellNum);
				
				//�������ݿ�
				if (dao.update(buy) <= 0 || dao.update(sell) <= 0 || dao.update(buyer) <= 0 
						|| dao.update(seller) <= 0 || dao.insert(deal) == null) {
					this.writeError("���ݿ��������");
		            return;
				}
				
				//����buyNumֵ
				buyNum = buyNum - sellNum;	
				
				//ƥ��t_sell������һ������
				i = i + 1;	
				sell = sells.get(i);
				finishedShareNum = sell.getFinishedShareNum();
				sellNum = sell.getShareNum() - finishedShareNum;
				sellPrice = sell.getPrice();
				sellId = sell.getId();
				sellerId = sell.getUserId();
			}						
		}
	}
	
	/**
	 * ���û��������ʱ���ô˺���ʵ����������
	 * @param sellId
	 */
	public void doSell(int sellId) {
		//��ȡί�������t_buy�е���Ϣ�����������
		List<Buy> buys = dao.query(Buy.class, Cnd.where("state", "<", DaoConst.STATE_FINISHIN).desc("price"));
		//���t_buys��Ϊ�գ��򷵻�
		if (buys.size() == 0) {
			return;
		}
		
		Sell sell = dao.fetch(Sell.class, Cnd.where("id", "=", sellId));
		float sellPrice = sell.getPrice();
		int sellNum = sell.getShareNum() - sell.getFinishedShareNum();
		int sellerId = sell.getUserId();
	
		int i = 0;
		Buy buy = buys.get(i);
		float buyPrice = buy.getPrice();
		int finishedShareNum = buy.getFinishedShareNum();
		int buyNum = buy.getShareNum() - finishedShareNum;
		int buyerId = buy.getUserId();
		int buyId = buy.getId();
		
		float dealPrice;
		while (sellNum > 0 && sellPrice <= buyPrice) {
			dealPrice = (buyPrice + sellPrice)/2;
			if ( sellNum <= buyNum) {
				//t_sell(����)--�ɽ�����ΪsellNum
				sell.setFinishedShareNum(sell.getFinishedShareNum() + sellNum);
				sell.setState(DaoConst.STATE_FINISHIN);
				
				//t_buy(����)
				buy.setFinishedShareNum(finishedShareNum + sellNum);
				if ( sellNum == buyNum) {
					buy.setState(DaoConst.STATE_FINISHIN);
				} else {
					buy.setState(DaoConst.STATE_PARTIAL);
				}
				
				//t_user(����)
				//���Ҹ���
				User seller = dao.fetch(User.class, Cnd.where("id", "=", sellerId));
				seller.setFrozenNum(seller.getFrozenNum() - sellNum);
				seller.setUsableFund(seller.getUsableFund() + sellNum * dealPrice);
				//�����Ϣ����
				User buyer = dao.fetch(User.class, Cnd.where("id", "=", buyerId));
				buyer.setTradableNum(buyer.getTradableNum() + sellNum);
				//ע��ʵ�ʳɽ��۸��ί�м۸�֮��Զ����ʽ��Ӱ��
				buyer.setFrozenFund(buyer.getFrozenFund() - sellNum * buyPrice);				
				buyer.setUsableFund(buyer.getUsableFund() + sellNum * (buyPrice - dealPrice));
				
				//t_deal(����)				
				Deal deal = new Deal();
				deal.setBuyId(buyId);
				deal.setSellId(sellId);
				deal.setBuyerId(buyerId);
				deal.setSellerId(sellerId);
				deal.setTime(new Date());
				deal.setPrice(dealPrice);
				deal.setShareNum(sellNum);
				
				//�������ݿ�
				if (dao.update(buy) == 0 || dao.update(sell) == 0 || dao.update(buyer) == 0 
						|| dao.update(seller) == 0 || dao.insert(deal) == null) {
					this.writeError("���ݿ��������");
		            return;
				}
				
				//����sellNum��ֵ
				sellNum = sellNum - buyNum;
			} else if (sellNum > buyNum) {				
				//t_sell(����)--�ɽ���Ʊ��ΪbuyNum
				sell.setFinishedShareNum(sell.getFinishedShareNum() + buyNum);
				sell.setState(DaoConst.STATE_PARTIAL);
				
				//t_buy(����)
				buy.setFinishedShareNum(finishedShareNum + buyNum);
				buy.setState(DaoConst.STATE_FINISHIN);
				
				//t_user(����)
				User seller = dao.fetch(User.class, Cnd.where("id", "=", sellerId));
				seller.setFrozenNum(seller.getFrozenNum() - buyNum);
				seller.setUsableFund(seller.getUsableFund() + buyNum * dealPrice);
				User buyer = dao.fetch(User.class, Cnd.where("id", "=", buyerId));
				buyer.setTradableNum(buyer.getTradableNum() + buyNum);
				buyer.setFrozenFund(buyer.getFrozenFund() - buyNum * buyPrice);
				buyer.setUsableFund(buyer.getUsableFund() + buyNum * (buyPrice - dealPrice));
				
				//t_deal(����)				
				Deal deal = new Deal();
				deal.setBuyId(buyId);
				deal.setSellId(sellId);
				deal.setBuyerId(buyerId);
				deal.setSellerId(sellerId);
				deal.setTime(new Date());
				deal.setPrice(dealPrice);
				deal.setShareNum(sellNum);
				
				//�������ݿ�
				if (dao.update(buy) == 0 || dao.update(sell) == 0 || dao.update(buyer) == 0 
						|| dao.update(seller) == 0 || dao.insert(deal) == null) {
					this.writeError("���ݿ��������");
		            return;
				}
				
				//����sellNum��ֵ
				sellNum = sellNum - buyNum;
				
				//��Ҫƥ��t_buy����һ������
				i = i + 1;
				buy = buys.get(i);
				buyPrice = buy.getPrice();
				finishedShareNum = buy.getFinishedShareNum();
				buyNum = buy.getShareNum() - finishedShareNum;
				buyerId = buy.getUserId();
				buyId = buy.getId();			
			}
		}
	}
	
	/**
	 * ��ȡƽ̨����ɽ���Ϣ
	 */
	public void api_getDealInfo() {
		Sql sql = Sqls.queryEntity("SELECT time,price,sharenum FROM `t_deal` ORDER BY time DESC");
		sql.setEntity(dao.getEntity(Deal.class));
		dao.execute(sql);
		List<Deal> deals = sql.getList(Deal.class);
		
		//������
		Map res = new HashMap();
		res.put("deals", deals);
		
		this.writeResult(res);
	}
}
