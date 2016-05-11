package com.bmi.sharedeal.service.Handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nutz.dao.Cnd;
import org.nutz.dao.Sqls;
import org.nutz.dao.pager.Pager;
import org.nutz.dao.sql.Sql;

import com.bmi.sharedeal.service.DAO.Buy;
import com.bmi.sharedeal.service.DAO.DaoConst;
import com.bmi.sharedeal.service.DAO.Deal;
import com.bmi.sharedeal.service.DAO.Sell;
import com.bmi.sharedeal.service.DAO.User;
import com.bmi.sharedeal.service.Server.BaseHandler;
import com.bmi.sharedeal.service.utils.TextUtils;

/**
 * 
 * @author xiaoyu
 *
 */

public class EntrustInfoHandler extends BaseHandler{
	/**
	 * ��ȡ�ѵ�¼�û���ί�н�����Ϣ
	 */
	public void api_getEntrustInfo() {
		//�ж��Ƿ��¼
		User user = UserHandler.checkUserAuth(this);
		if(user == null) {
			return;
		}
		
		String _page = (String) this.getArgument("page");
		String _pageSize = (String) this.getArgument("pagesize");
		
		if( TextUtils.isEmpty(_page) || TextUtils.isEmpty(_pageSize)) {
			this.writeError("��������");
			return;
		}
		
		int page = Integer.parseInt(_page);				
		int pageSize = Integer.parseInt(_pageSize);
		
		int userId = user.getId();
		
		//���ƴ��buys��sells������ʵ�ַ�ҳ�Ͱ�ʱ������
		Sql sql = Sqls.queryEntity("SELECT * FROM t_buy WHERE userid = '" + userId 
				+ "' UNION SELECT * FROM t_sell WHERE userid = ' " + userId + "' ORDER BY time DESC");
		sql.setPager(dao.createPager(page, pageSize));
		sql.setEntity(dao.getEntity(Buy.class));
		dao.execute(sql);
		List<Buy> entrusts = sql.getList(Buy.class);

		//�����ѯ������Ĵ�������????(����Ч�ķ���)
		Sql _sql = Sqls.queryEntity("SELECT * FROM t_buy WHERE userid = '" + userId 
				+ "' UNION SELECT * FROM t_sell WHERE userid = ' " + userId + "' ORDER BY time DESC");
		_sql.setEntity(dao.getEntity(Buy.class));
		dao.execute(_sql);
		List<Buy> _entrusts = _sql.getList(Buy.class);
		
		//������
		Map res = new HashMap();
		
		res.put("entrusts", entrusts);
		res.put("Cnt", _entrusts.size());
		
		this.writeResult(res);		
	}
	
	/**
	 * ��ȡ�ѵ�¼�û��ĳɽ���Ϣ
	 */
	public void api_getDealInfo() {
		//�ж��Ƿ��¼
		User user = UserHandler.checkUserAuth(this);
		if(user == null) {
			return;
		}
		
		String _page = (String) this.getArgument("page");
		String _pageSize = (String) this.getArgument("pagesize");
		
		if( TextUtils.isEmpty(_page) || TextUtils.isEmpty(_pageSize)) {
			this.writeError("��������");
			return;
		}
		
		int page = Integer.parseInt(_page);				
		int pageSize = Integer.parseInt(_pageSize);
		
		int userId = user.getId();
		
		Pager pager = dao.createPager(page, pageSize);
		List<Deal> deals = dao.query(Deal.class, Cnd.where("buyerid", "=", userId).or("sellerid", "=", userId).desc("time"), pager);
		int cnt = deals.size();
		int pageCnt = cnt / pageSize + 1;
		//������
		Map res = new HashMap();
		
		res.put("deals", deals);
		res.put("Cnt", cnt);
		res.put("pageCnt", pageCnt);
		
		this.writeResult(res);		
	}

	/**
	 * �ѵ�¼�û�����ί����Ϣ
	 * @param id,buyorsell
	 */
	public void api_repealEntrust() {
		//�ж��Ƿ��¼
		User user = UserHandler.checkUserAuth(this);
		if(user == null) {
			return;
		}
		
		String _entrustId = (String) this.getArgument("entrustid");
		String _buyorsell = (String) this.getArgument("buyorsell");
		
		if( TextUtils.isEmpty(_entrustId) || TextUtils.isEmpty(_buyorsell)) {
			this.writeError("��������");
			return;
		}
		
		int entrustId = Integer.parseInt(_entrustId);
		int buyorsell = Integer.parseInt(_buyorsell);
		float repealFund;
		int repealNum;
					
		if (buyorsell == 0) {
			Buy entrust = dao.fetch(Buy.class, Cnd.where("id", "=", entrustId));
			repealFund = entrust.getPrice() * (entrust.getShareNum() - entrust.getFinishedShareNum());
			
			if (entrust.getState() >= 2) {
				return;
			}
			
			entrust.setState(DaoConst.STATE_REVOCATION);
			user.setFrozenFund(user.getFrozenFund() - repealFund);
			user.setUsableFund(user.getUsableFund() + repealFund);
						
			if (dao.update(user) <= 0 || dao.update(entrust) <= 0) {				
				this.writeError("���ݿ����");
				return;
			}
			
			Map res = new HashMap();			
			res.put("entrust", entrust);
			this.writeResult(res);
		} else if (buyorsell == 1) {
			Sell entrust = dao.fetch(Sell.class, Cnd.where("id", "=", entrustId));
			repealNum = entrust.getShareNum() - entrust.getFinishedShareNum();
			
			if (entrust.getState() >= 3) {
				return;
			}
			
			user.setFrozenNum(user.getFrozenNum() - repealNum);
			user.setTradableNum(user.getTradableNum() + repealNum);
			entrust.setState(DaoConst.STATE_REVOCATION);
			
			if (dao.update(user) <= 0 || dao.update(entrust) <=0) {
				this.writeError("���ݿ����");
				return;
			}
			
			Map res = new HashMap();			
			res.put("entrust", entrust);
			this.writeResult(res);
		}
	}
}
