package com.bmi.sharedeal.service.Handler;

import com.bmi.sharedeal.service.DAO.User;
import com.bmi.sharedeal.service.Server.*;
import com.bmi.sharedeal.service.utils.TextUtils;

import org.nutz.dao.Cnd;
import org.nutz.dao.FieldFilter;
import org.nutz.dao.util.Daos;

import java.util.*;

/**
 * �û�ģ��
 *
 * @author xiaoyu
 */
public class UserHandler extends BaseHandler {
    /**
     * ��¼
     */
    public void api_doLogin() {
        String userName = (String) this.getArgument("username");  //�������û���
        String pwd = (String) this.getArgument("pwd");

        if (TextUtils.isEmpty(userName) || TextUtils.isEmpty(pwd)) {
            this.writeError("�������㣬����д�û���������");
            return;
        }

        User u = dao.fetch(User.class, Cnd.where(Cnd.exps("name", "=", userName)).and("password", "=", pwd));

        if (u == null) {
            this.writeError("�û������������");
            return;
        }
        
        //���״ε�¼
        if (u.getLoginTime() != null){
        	//����authKey
            String seed = u.getName() + new Date().toString() + new Random().nextInt(1000);
            String md5 = TextUtils.MD5(seed);
            if (TextUtils.isEmpty(md5)) {
                this.writeError("�������ڲ�����");
                return;
            }

            u.setAuthKey(md5);            
            if (updateUser(u, "^authKey$") <= 0) {
                this.writeError("���ݿ����");
                return;
            }

            Map res = new HashMap();
            res.put("authKey", u.getAuthKey());
            res.put("user", u.toKVPair());
            this.writeResult(res);
        } else {	//�״ε�¼����Ҫ�޸�����									
        	String newpwd1 = (String) this.getArgument("newpwd1");  //�������û���
            String newpwd2 = (String) this.getArgument("newpwd2");

            if (TextUtils.isEmpty(newpwd1) || TextUtils.isEmpty(newpwd2)) {
                this.writeError("�������㣬�״ε�¼����д������");
                return;
            }
            
            if (!newpwd1.equalsIgnoreCase(newpwd2)) {
            	this.writeError("�������������벻һ��");
            	return;
            }
                             
            //����authKey
            String seed = u.getName() + new Date().toString() + new Random().nextInt(1000);
            String md5 = TextUtils.MD5(seed);
            if (TextUtils.isEmpty(md5)) {
                this.writeError("�������ڲ�����");
                return;
            }

            u.setAuthKey(md5);
            u.setPassword(newpwd1);
            u.setLoginTime(new Date());
            System.out.println("Date:" + new Date());
            if (dao.update(u) <= 0) {	
                this.writeError("���ݿ����");
                return;
            }

            Map res = new HashMap();
            res.put("authKey", u.getAuthKey());
            res.put("user", u.toKVPair());
            this.writeResult(res);
        } 
        
    }

    /**
     * �˳���¼
     */
    public void api_doLogout() {
        User user = checkUserAuth(this);
        if (user == null) {
            return;
        }

        user.setAuthKey(null);

        if (updateUser(user, "^authKey$") <= 0) {
            this.writeError("���ݿ����");
            return;
        }

        this.writeResult(null);
    }

    /**
     * ��ȡ�û���Ϣ(��ָ���û���/id��Ϊ��ȡ������Ϣ),���ȼ���id > name > authKey
     */
    public void api_getInfo(){
        User user = null;

        String id = (String) this.getArgument("id");
        if(!TextUtils.isEmpty(id)){
            user = dao.fetch(User.class, Cnd.where("id", "=", id));
            if (user == null) {
                this.writeError("�û�������");
                return;
            }
        }else {
            String name = (String) this.getArgument("name");
            if (!TextUtils.isEmpty(name)) {
                user = dao.fetch(User.class, Cnd.where("name", "=", name));
                if (user == null) {
                    this.writeError("�û�������");
                    return;
                }
            }else{
                user = checkUserAuth(this);
                if (user == null) {
                    return;
                }
            }
        }

        Map res = new HashMap();
        res.put("user", user.toKVPair());
        this.writeResult(res);
    }

    public void api_getAllUsers(){
        String page = (String) this.getArgument("page");
        if (TextUtils.isEmpty(page)) {
            page = "1";
        }

        List<User> users = dao.query(User.class, Cnd.NEW().asc("id"),
                dao.createPager(Integer.parseInt(page), 10));

        //���û���
        int allCnt = dao.count(User.class, null);

        Map res = new HashMap();
        res.put("allCnt", allCnt);
        res.put("cnt", users.size());
        res.put("users", users);
        res.put("page", page);

        this.writeResult(res);
    }

    /**
     * ͨ�÷����������Ȩ���
     */
    public static User checkUserAuth(BaseHandler h) {
        String authKey = (String) h.getArgument("authkey");
        if (TextUtils.isEmpty(authKey)) {
            h.writeError("��������");
            return null;
        }

        User u = h.dao.fetch(User.class, Cnd.where("authkey", "=", authKey));
        if (u == null) {
            h.writeError("�û���֤ʧ��");
            return null;
        }

        return u;
    }

    /**
     * ֻ������Ҫ���ֶ�
     * @param user
     * @param actived
     * @return
     */
    private int updateUser(User user, String actived){
        return Daos.ext(dao, FieldFilter.create(User.class, actived)).update(user);
    }
}
