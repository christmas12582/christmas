package com.lottery.service;

import com.github.pagehelper.PageHelper;
import com.lottery.dao.CashMapper;
import com.lottery.dao.ProductMapper;
import com.lottery.dao.UnitMapper;
import com.lottery.dao.UserMapper;
import com.lottery.model.*;
import com.lottery.utils.StringUtils;
import org.apache.ibatis.javassist.expr.Cast;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service
public class OperatorService {

    @Autowired
    ProductMapper productMapper;

    @Autowired
    UnitMapper unitMapper;

    @Autowired
    UserMapper userMapper;

    @Autowired
    CashMapper cashMapper;



    public List<Product>  getProductListbyCondition(Integer id,String name, Integer isvalid){
        ProductExample example = new ProductExample();
        ProductExample.Criteria criteria= example.createCriteria();
        if (id!=null)
            criteria.andIdEqualTo(id);
        if (!StringUtils.isNullOrNone(name))
            criteria.andNameLike("%"+name+"%");
        if (isvalid!=null)
            criteria.andIsvalidEqualTo(isvalid);
        return  productMapper.selectByExample(example);

    }

    public HashMap<String,Object> getProductDetails(Integer id){
        UnitExample example= new UnitExample();
        example.createCriteria().andProductidEqualTo(id);
        List<Unit> unitList=unitMapper.selectByExample(example);
        Product product=productMapper.selectByPrimaryKey(id);
        HashMap<String,Object> result= new HashMap<>();
        result.put("product",product);
        result.put("unit",unitList);
        return result;
    }
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public void removeProduct(Integer id){
        productMapper.deleteByPrimaryKey(id);
        UnitExample example= new UnitExample();
        example.createCriteria().andProductidEqualTo(id);
        unitMapper.deleteByExample(example);
    }

    public  HashMap<String,Integer> addProduct(String name,String icon,String description,Integer isvalid){
        Product product= new Product();
        product.setName(name);
        product.setDescription(description);
        product.setIcon(icon);
        product.setIsvalid(isvalid);
        int count=productMapper.insertSelective(product);
        HashMap<String,Integer> result= new HashMap<>();
        result.put("productid",product.getId());
        result.put("count",count);
        return result;
    }

    public  Integer updateProduct(Integer id,String name,String icon,String description,Integer isvalid){
        Product product= new Product();
        product.setId(id);
        if(isvalid!=null)
            product.setIsvalid(isvalid);
        if(!StringUtils.isNullOrNone(icon))
            product.setIcon(icon);
        if (!StringUtils.isNullOrNone(name))
            product.setName(name);
        if(!StringUtils.isNullOrNone(description))
            product.setDescription(description);
        return  productMapper.updateByPrimaryKeySelective(product);

    }

    public List<Unit> getUnitList(Integer id,Integer productid,Integer isvalid,String name){
        UnitExample example= new UnitExample();
        UnitExample.Criteria criteria= example.createCriteria();
        if (id!=null)
            criteria.andIdEqualTo(id);
        if(productid!=null)
            criteria.andProductidEqualTo(productid);
        if(isvalid!=null)
            criteria.andIsvalidEqualTo(isvalid);
        if(!StringUtils.isNullOrNone(name))
            criteria.andNameEqualTo(name);
        return unitMapper.selectByExample(example);
    }

    public int removeUnit(Integer id){
        return unitMapper.deleteByPrimaryKey(id);
    }

    public HashMap<String,Integer> addUnit(Integer productid,Integer isvalid,Integer price,String name,Integer expired){
        Unit unit= new Unit();
        unit.setName(name);
        unit.setIsvalid(isvalid);
        unit.setProductid(productid);
        unit.setPrice(price);
        unit.setExpired(expired);
        int count=unitMapper.insertSelective(unit);
        Integer unitid=unit.getId();
        HashMap<String,Integer> result= new HashMap<>();
        result.put("unitid",unitid);
        result.put("count",count);
        return result;
    }


    public int updateUnit(Integer unitid,Integer productid,Integer isvalid,Integer price,String name,Integer expired){
        Unit unit= new Unit();
        unit.setId(unitid);
        if (StringUtils.isNullOrNone(name))
            unit.setName(name);
        if (isvalid!=null)
            unit.setIsvalid(isvalid);
        if(productid!=null)
            unit.setProductid(productid);
        if (price!=null)
            unit.setPrice(price);
        if(expired!=null)
            unit.setExpired(expired);
        return unitMapper.updateByPrimaryKeySelective(unit);

    }

    public List<User> getDistributeList(String phone,String openid,Integer isvalid){
        UserExample userExample=new UserExample();
        UserExample.Criteria criteria=userExample.createCriteria();
        criteria.andTypeEqualTo(4);
        if (!StringUtils.isNullOrNone(phone))
            criteria.andPhoneEqualTo(phone);
        if(!StringUtils.isNullOrNone(openid))
            criteria.andOpenidEqualTo(openid);
        if (isvalid!=null)
            criteria.andIsvalidEqualTo(isvalid);
        return userMapper.selectByExample(userExample);
    }

    public  HashMap<String,Object> setRatio(Integer userid,Integer ratio){
        HashMap<String,Object> result= new HashMap<>();
        User user= userMapper.selectByPrimaryKey(userid);
        if (user==null)
            return null;
        if (user.getType()!=4){
            result.put("msg","该用户不是分销商");
            result.put("code",500L);
            return result;
        }
        user.setRatio(ratio);
        Integer count= userMapper.updateByPrimaryKeySelective(user);
        if (count==null||count==0){
            result.put("msg","设置失败，未找到该主键");
            result.put("code",500L);
            return result;
        }
        else{
            result.put("msg","设置成功");
            result.put("code",0L);
            return result;
        }
    }

    public List<Cash> cashList(Integer pagenum,Integer pagesize,String phone, String openid,Integer isexchange,String begintime,String endtime) throws ParseException {
        SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        CashExample cashExample= new CashExample();
        CashExample.Criteria criteria = cashExample.createCriteria();
        if (!StringUtils.isNullOrNone(phone)||!StringUtils.isNullOrNone(openid)) {
            UserExample userExample = new UserExample();
            UserExample.Criteria usercriteria = userExample.createCriteria();
            if (!StringUtils.isNullOrNone(phone))
                usercriteria.andPhoneEqualTo(phone);
            if (!StringUtils.isNullOrNone(openid))
                usercriteria.andOpenidEqualTo(openid);
            List<User> userList = userMapper.selectByExample(userExample);
            if (!userList.isEmpty()) {
                List<Integer> useridlist = new ArrayList<>();
                for (User user : userList) {
                    useridlist.add(user.getId());
                }
                criteria.andCreateidIn(useridlist);
            }else {
              return new ArrayList<>();
            }
        }
        if (isexchange!=null)
            criteria.andIsexchangeEqualTo(isexchange);
        if (!StringUtils.isNullOrNone(begintime)){
            Date begintime_date=sdf.parse(begintime);
            criteria.andCreatetimeGreaterThanOrEqualTo(begintime_date);
        }
        if (!StringUtils.isNullOrNone(endtime)){
            Date endtime_date=sdf.parse(endtime);
            criteria.andCreatetimeLessThanOrEqualTo(endtime_date);
        }
        PageHelper.startPage(pagenum,pagesize);
        PageHelper.orderBy("createtime desc");
        return cashMapper.selectByExample(cashExample);
    }

    @Transactional(rollbackFor = Exception.class)
    public HashMap<String,Object> setCashExchange(Integer cashid,Integer isexchange){
        HashMap<String,Object> result= new HashMap<>();
        Cash cash=cashMapper.selectByPrimaryKey(cashid);
        if (cash==null){
            result.put("code",500L);
            result.put("msg","未找到提现申请");
        }
        cash.setIsexchange(isexchange);
//        Integer money=cash.getMoney();
//        Integer createid=cash.getCreateid();
//        User user=userMapper.selectByPrimaryKey(createid);
//        Integer oldmoney=user.getMoney();
//        Integer newmoney=oldmoney-money;
//        user.setMoney(newmoney);
//        int count1=userMapper.updateByPrimaryKey(user);

        int count=cashMapper.updateByPrimaryKeySelective(cash);
        if (count==0){
            result.put("msg","提现失败，未找到该主键");
            result.put("code",500L);
            return result;
        }
        else{
            result.put("msg","提现申请更新成功");
            result.put("code",0L);
            return result;
        }

    }


}
