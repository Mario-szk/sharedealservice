package com.bmi.sharedeal.service.Handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nutz.dao.Cnd;
import org.nutz.dao.pager.Pager;

import com.bmi.sharedeal.service.DAO.Admin;
import com.bmi.sharedeal.service.DAO.Buy;
import com.bmi.sharedeal.service.DAO.Deal;
import com.bmi.sharedeal.service.DAO.DebtInfo;
import com.bmi.sharedeal.service.Server.BaseHandler;
import com.bmi.sharedeal.service.utils.TextUtils;

public class DebtInfoHandler extends BaseHandler{

	//���ծȨת����Ϣ
	public void api_addDebtInfo(){
		String authkey = (String) this.getArgument("authkey");
		if (TextUtils.isEmpty(authkey)) {
			this.writeError("���¼");
			return;
		}
		
		Admin admin = dao.fetch(Admin.class, Cnd.where("authkey", "=", authkey));
		if (admin == null) {
			this.writeError("����Ա��֤ʧ��");
		}
		
		String name = (String) this.getArgument("name");
		String _price = (String) this.getArgument("price");
		String _phone = (String) this.getArgument("phone");
		
		if( TextUtils.isEmpty(name) || TextUtils.isEmpty(_price) || TextUtils.isEmpty(_phone)) {
			this.writeError("��������");
			return;
		}
		
		int price = Integer.parseInt(_price);
		int phone = Integer.parseInt(_phone);
		//���뵽���ݿ�
		DebtInfo debt = new DebtInfo();
		debt.setName(name);
		debt.setPrice(price);
		debt.setPhone(phone);
		
		if (dao.insert(debt) == null) {
			this.writeError("���ݿ����");
			return;
		}
		
		Map res = new HashMap();
		res.put("debt", debt);
		this.writeResult(res);
	}
	
	/**
	 * �޸�ծȨת����Ϣ
	 */
	public void api_modifyDebtInfo(){
		String _debtId = (String) this.getArgument("id");
		String name = (String) this.getArgument("name");
		String _price = (String) this.getArgument("price");
		String _phone = (String) this.getArgument("phone");
		
		if( TextUtils.isEmpty(_debtId) || TextUtils.isEmpty(name) || TextUtils.isEmpty(_price) || TextUtils.isEmpty(_phone)) {
			this.writeError("��������");
			return;
		}
		
		int debtId = Integer.parseInt(_debtId);
		int price = Integer.parseInt(_price);
		int phone = Integer.parseInt(_phone);
		//�������ݿ�
		DebtInfo debt = dao.fetch(DebtInfo.class, Cnd.where("id", "=", debtId));
		if (debt == null) {
			return;
		}
		debt.setName(name);
		debt.setPrice(price);
		debt.setPhone(phone);
		
		if (dao.update(debt) <= 0) {
			this.writeError("���ݿ����");
			return;
		}
		
		Map res = new HashMap();
		res.put("debt", debt);
		this.writeResult(res);
	}
	
	//ɾ��ծȨת����Ϣ
	public void api_delDebtInfo(){
		String _debtId = (String) this.getArgument("id");
		if( TextUtils.isEmpty(_debtId)) {
			this.writeError("��������");
			return;
		}
		int debtId = Integer.parseInt(_debtId);
		DebtInfo debt = dao.fetch(DebtInfo.class, Cnd.where("id", "=", debtId));
		if (debt == null) {
			return;
		}
		
		if (dao.delete(debt) <= 0) {
			this.writeError("���ݿ����");
			return;
		}
		this.writeResult(null);
		
	}
	
	/**
	 * �г�������Ϣ
	 */
	public void api_getAllDebtInfo(){
		String _page = (String) this.getArgument("page");		
		
		if( TextUtils.isEmpty(_page)) {
			this.writeError("��������");
			return;
		}
		
		int page = Integer.parseInt(_page);						
		
		Pager pager = dao.createPager(page, 10);
		List<DebtInfo> debts = dao.query(DebtInfo.class, null, pager);
		int allCnt = dao.count(DebtInfo.class);
		int pages = allCnt / 10 + 1;
		//������
		Map res = new HashMap();
		res.put("debts", debts);
		res.put("pages", pages);
		res.put("cnt", debts.size());
		res.put("allCnt", allCnt);
		
		this.writeResult(res);
	}
}
