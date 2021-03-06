/**
 * 
 */
package com.lottery.service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lottery.dao.UserLotteryMapper;
import com.lottery.model.Lottery;
import com.lottery.model.LotteryItem;
import com.lottery.model.UserLottery;
import com.lottery.model.UserLotteryExample;
import com.lottery.model.UserLotteryExample.Criteria;
import com.lottery.utils.StringUtils;

/**
 * @author ws_yu
 *
 */
@Service
public class UserLotteryService {
	
	@Autowired
	private UserLotteryMapper userLotteryMapper;
	
	@Autowired
	private LotteryService lotteryService;
	
	/**
	 * 兑奖超期天数
	 */
	@Value("${exchange.expired}")
	private Integer expired;
	
	private static final Logger logger = LoggerFactory.getLogger(UserLotteryService.class);

	/**
	 * 查询用户参与某抽奖活动的抽奖记录
	 * @param userId
	 * @param lotteryId
	 * @return
	 */
	public List<UserLottery> findUserLotteryListByUserIdAndLotteryId(Integer userId, Integer lotteryId){
		return userLotteryMapper.selectByUserIdAndLotteryId(userId, lotteryId);
	}
	
	/**
	 * 查询用户参与抽奖活动的抽奖记录
	 * @param userId
	 * @return
	 */
	public List<UserLottery> findUserLotteryListByUserId(Integer userId){
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_YEAR, -expired);
		List<UserLottery> selectByExample = userLotteryMapper.selectByUserIdAndExpiredtime(userId, calendar.getTime());
		Map<Integer, LotteryItem> lotteryItemMap = new HashMap<Integer, LotteryItem>();
		for(UserLottery userLottery: selectByExample){
			if(!lotteryItemMap.containsKey(userLottery.getLotteryitemid())){
				lotteryItemMap.put(userLottery.getLotteryitemid(), lotteryService.findLotteryItemById(userLottery.getLotteryitemid()));
			}
			userLottery.setLotteryItem(lotteryItemMap.get(userLottery.getLotteryitemid()));
		}
		return selectByExample;
	}
	
	/**
	 * 中奖
	 * @param userId
	 * @param lotteryItemId
	 * @param shareNum
	 * @return
	 */
	@Transactional
	public String lottery(Integer userId, Integer lotteryItemId, String shareNum){
		String message = "success";
		synchronized(this){
			LotteryItem lotteryItem = lotteryService.findLotteryItemById(lotteryItemId);
			if(lotteryItem==null){
				throw new RuntimeException("奖项不存在");
			}
			Lottery lottery = lotteryService.findLotteryById(lotteryItem.getLotteryid());
			if(lottery==null || lottery.getIsvalid()==0){
				throw new RuntimeException("抽奖活动不存在或已失效");
			}
			if(!StringUtils.isNullOrNone(shareNum)){
				UserLottery shareUserLottery = findUserLotteryByShareNum(shareNum);
				if(shareUserLottery!=null && shareUserLottery.getPrizenum()==null && !shareUserLottery.getUserid().equals(userId) && !hasRelation(lottery.getId(), userId, shareUserLottery.getUserid())){
					shareUserLottery.setPrizenum(generatePrizenum(shareUserLottery.getUserid()));
					shareUserLottery.setOtheruserid(userId);
					userLotteryMapper.updateByPrimaryKey(shareUserLottery);
				}
			}
			if(lotteryItem.getGcount()>=lotteryItem.getMcount()){
				message = "奖项["+lotteryItem.getName()+"]已抽空，抽奖失败";
				logger.info("用户["+userId+"]在参与活动["+lottery.getId()+"]时，"+message);
				return message;
			}
			List<UserLottery> userLotteryList = findUserLotteryListByUserIdAndLotteryId(userId, lottery.getId());
			if(userLotteryList!=null && userLotteryList.size()>=lottery.getMcount()){
				message = "抽奖次数已经用完，抽奖失败";
				logger.info("用户["+userId+"]在活动["+lottery.getId()+"]上的"+message);
				return message;
			}
			lotteryItem.setGcount(lotteryItem.getGcount()+1);
			lotteryService.saveLotteryItem(lotteryItem);
			UserLottery userLottery = new UserLottery();
			userLottery.setLotterydate(new Date());
			userLottery.setUserid(userId);
			userLottery.setLotteryitemid(lotteryItemId);
			userLottery.setLotteryid(lotteryItem.getLotteryid());
			userLottery.setSharenum(generateSharenum(userId));
			if(lottery.getForceshare()==null || lottery.getForceshare()==0){
				userLottery.setPrizenum(generatePrizenum(userId));
			}
			userLotteryMapper.insert(userLottery);
			message = message + ":" + userLottery.getId();
		}
		return message;
	}
	
	/**
	 * 查询中奖信息
	 * @param id
	 * @return
	 */
	public UserLottery findUserLotteryById(Integer id){
		return userLotteryMapper.selectByPrimaryKey(id);
	}
	
	/**
	 * 查询中奖信息
	 * @param prizenum
	 * @return
	 */
	public UserLottery findUserLotteryByPrizenum(String prizenum){
		UserLotteryExample userLotteryExample = new UserLotteryExample();
		Criteria criteria = userLotteryExample.createCriteria();
		criteria.andPrizenumEqualTo(prizenum);
		List<UserLottery> userLotteryList = userLotteryMapper.selectByExample(userLotteryExample);
		if(userLotteryList!=null && userLotteryList.size()>0){
			return userLotteryList.get(0);
		}
		return null;
	}
	
	/**
	 * 兑奖
	 * @param id
	 */
	@Transactional
	public void exchangeUserLottery(Integer id){
		UserLottery userLottery = new UserLottery();
		userLottery.setId(id);
		userLottery.setExchangedate(new Date());
		userLotteryMapper.updateByPrimaryKeySelective(userLottery);
	}
	
	/**
	 * 生成中奖号
	 * @param userId
	 * @return
	 */
	private String generatePrizenum(Integer userId) {
        String date = String.valueOf(new Date().getTime());
        String user = String.valueOf(userId);
        int length = 18 - date.length() - user.length();
        String random = "";
        for(int i = 0; i < length; ++i) {
            Random r = new Random();
            random = random + String.valueOf(r.nextInt(10));
        }
        return date+random+user;
    }
	
	/**
	 * 生成分享编号
	 * @param userId
	 * @return
	 */
	private String generateSharenum(Integer userId) {
        String date = String.valueOf(new Date().getTime());
        String user = String.valueOf(userId);
        return date+"s"+user;
    }
	
	/**
	 * 分享中奖结果
	 * @param userId
	 * @param userLotteryId
	 * @return
	 */
	@Transactional
	public String share(Integer userId, Integer userLotteryId){
		UserLottery userLottery = userLotteryMapper.selectByPrimaryKey(userLotteryId);
		userLottery.setSharenum(generateSharenum(userId));
		userLotteryMapper.updateByPrimaryKeySelective(userLottery);
		return userLottery.getSharenum();
	}
	
	/**
	 * 根据分享号查询中奖信息
	 * @param shareNum
	 * @return
	 */
	public UserLottery findUserLotteryByShareNum(String shareNum){
		UserLotteryExample userLotteryExample = new UserLotteryExample();
		Criteria criteria = userLotteryExample.createCriteria();
		criteria.andSharenumEqualTo(shareNum);
		List<UserLottery> userLotteryList = userLotteryMapper.selectByExample(userLotteryExample);
		if(userLotteryList!=null && userLotteryList.size()>0){
			return userLotteryList.get(0);
		}
		return null;
	}
	
	/**
	 * 在一次活动中两个用户是否存在认证关系
	 * @param lotteryId
	 * @param userId
	 * @param otherUserId
	 * @return
	 */
	private Boolean hasRelation(Integer lotteryId, Integer userId, Integer otherUserId){
		UserLotteryExample userLotteryExample = new UserLotteryExample();
		Criteria criteria = userLotteryExample.createCriteria();
		criteria.andLotteryidEqualTo(lotteryId);
		criteria.andUseridEqualTo(userId);
		criteria.andOtheruseridEqualTo(otherUserId);
		List<UserLottery> userLotteryList = userLotteryMapper.selectByExample(userLotteryExample);
		if(userLotteryList!=null && userLotteryList.size()>0){
			return Boolean.TRUE;
		}
		criteria.andUseridEqualTo(otherUserId);
		criteria.andOtheruseridEqualTo(userId);
		List<UserLottery> userLotteryList1 = userLotteryMapper.selectByExample(userLotteryExample);
		if(userLotteryList1!=null && userLotteryList1.size()>0){
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}
	
}
