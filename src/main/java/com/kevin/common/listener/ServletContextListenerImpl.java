package com.kevin.common.listener;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.kevin.common.GlobalConstant.GlobalConstant;
import com.kevin.common.core.GeneralEnum;
import com.kevin.common.utils.ClassUtil;
import com.kevin.common.utils.EnumUtil;
import com.kevin.common.utils.RegionUtil;
import com.kevin.enums.sys.DictTypeEnum;
import com.kevin.model.SysCfg;
import com.kevin.model.SysDict;
import com.kevin.model.SysMenu;
import com.kevin.service.sys.ISysCfgService;
import com.kevin.service.sys.ISysDictService;
import com.kevin.service.sys.ISysMenuService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;



/**
 * 监听ServletContext对象的生命周期，实际上就是监听Web应用的生命周期。
 * 当Servlet容器启动或终止Web应用时，会触发ServletContextEvent事件，该事件由 ServletContextListener 来处理。
 * 在 ServletContextListener 接口中定义了处理ServletContextEvent事件的两个方法。
 * 处理Web应用的 servlet上下文(context)的变化的通知。
 * @author Tiger Mo
 * @create 2016.04.13
 */
@WebListener
public class ServletContextListenerImpl implements ServletContextListener {

	private final static Logger logger = LoggerFactory.getLogger(ServletContextListenerImpl.class);
	
	public static boolean licenseed = false;	
	private static ISysDictService dictService;
	private static ISysCfgService cfgService;
	private static ISysMenuService menuService;
	
	@Resource
    public void setService(ISysDictService dictService) {
        this.dictService = dictService;
    }
	@Resource
	public void setService(ISysCfgService cfgService) {
		this.cfgService = cfgService;
	}
	@Resource
	public void setService(ISysMenuService menuService) {
		this.menuService = menuService;
	}
	
	/**
	 * 当Servlet容器终止Web应用时调用该方法。
	 * 在调用该方法之前，容器会先销毁所有的Servlet和Filter过滤器。
	 */
	public void contextDestroyed(ServletContextEvent event) {
		
	}

	/**
	 * 当Servlet容器启动Web应用时调用该方法。
	 * 在调用完该方法之后，容器再对Filter初始化，并且对那些在Web应用启动时就需要被初始化的Servlet进行初始化。
	 */
	public void contextInitialized(ServletContextEvent event) {
		logger.debug("----> 系统初始化。。。。。");		
		ServletContext context = event.getServletContext();
		try {
			//加载字典
			_loadDict(context);
			//加载系统配置
			_loadSysCfg(context);
			//加载系统代码
			_loadEnum(context);
			//加载角色
//			_loadRole(context);
		} catch (Exception e) {
			logger.debug("----> e.getMessage():" + e.getMessage());
		}
	}
	

	/**
	 * 刷新服务器
	 * @param context
	 */
	public static String refresh(ServletContext context){
		logger.debug("----> 刷新服务器。。。。。。");
		try {
			//加载字典
			_loadDict(context);
			//加载系统配置
			_loadSysCfg(context);
			//加载系统代码
			_loadEnum(context);
			//加载角色
//			_loadRole(context);
			return GlobalConstant.OPERATE_SUCCESSED;
		} catch (Exception e) {
			logger.debug("----> e.getMessage():" + e.getMessage());
			return GlobalConstant.OPERATE_FAIL;
		}
	}
	
	
	/**
	 * 加载字典
	 */
	private static Map<String, Map<String, String>> sysDictNameMap;
	private static void _loadDict(ServletContext context) {
		Map<String, List<SysDict>> sysListDictMap = new HashMap<String, List<SysDict>>();
		Map<String, String> sysDictIdMap = new HashMap<String, String>();
		Map<String, Map<String, String>> sysDictNameMap = new HashMap<String, Map<String, String>>();
		List<DictTypeEnum> dictTypeEnumList = (List<DictTypeEnum>) EnumUtil.toList(DictTypeEnum.class);
		for(DictTypeEnum dictTypeEnum : dictTypeEnumList){		
			String dictTypeId = dictTypeEnum.getId();
			Map<String, String> dictNameMap = new HashMap<String, String>();
			sysDictNameMap.put(dictTypeId, dictNameMap);
			//ISysDictService dictService = (ISysDictService) SpringContextUtil.getBean(ISysDictService.class);
			
			SysDict sysDict = new SysDict();
			sysDict.setDictTypeId(dictTypeId);
			sysDict.setRecordState(GlobalConstant.Y);
			
			List<SysDict> sysDictList = dictService.queryList(sysDict, GlobalConstant.SORT_KEY);
			for(SysDict dict : sysDictList){
				String typeId = dict.getDictTypeId()+"."+dict.getDictId();
				sysDictIdMap.put(typeId, dict.getDictValue());
				dictNameMap.put(dict.getDictValue(), dict.getDictKey());
				if(dictTypeEnum.getLevel()>1){
					sysDict.setDictTypeId(typeId);
					List<SysDict> sysDictSecondList = dictService.queryList(sysDict, GlobalConstant.SORT_KEY);
					
					if(sysDictSecondList!=null && sysDictSecondList.size()>0){
						for(SysDict sDict : sysDictSecondList){
							String tTypeId = typeId+"."+sDict.getDictId();
							String tTypeName = dict.getDictValue()+"."+sDict.getDictValue();
							
							sysDictIdMap.put(tTypeId,tTypeName);
							
							if(dictTypeEnum.getLevel()>2){
								sysDict.setDictTypeId(tTypeId);
								List<SysDict> sysDictThirdList = dictService.queryList(sysDict, GlobalConstant.SORT_KEY);
								
								if(sysDictThirdList!=null && sysDictThirdList.size()>0){
									for(SysDict tDict : sysDictThirdList){
										sysDictIdMap.put(tTypeId+"."+tDict.getDictId(), tTypeName+"."+tDict.getDictValue());
									}
								}
								
								sysListDictMap.put(tTypeId, sysDictThirdList);
								context.setAttribute("dictTypeEnum"+tTypeId+"List", sysDictThirdList);
							}
							
						}
					}
					
					sysListDictMap.put(typeId, sysDictSecondList);
					context.setAttribute("dictTypeEnum"+typeId+"List", sysDictSecondList);
				}
			}

			sysListDictMap.put(dictTypeId, sysDictList);
			context.setAttribute("dictTypeEnum"+dictTypeEnum.name()+"List", sysDictList);
		}
//		GlobalContext.sysDictListMap = sysListDictMap;
		ServletContextListenerImpl.sysDictNameMap = sysDictNameMap;
		DictTypeEnum.sysDictIdMap = sysDictIdMap;
		DictTypeEnum.sysListDictMap = sysListDictMap;
	}
	
	/**
	 * 加载枚举
	 */
	private static void _loadEnum(ServletContext context){
		Set<Class<?>> set = ClassUtil.getClasses("com.xinhua.enums");
		for(Class<?> cls : set){		
			@SuppressWarnings({ "unchecked", "rawtypes" })
			List<GeneralEnum> enumList = (List<GeneralEnum>) EnumUtil.toList((Class<? extends GeneralEnum>) cls);
			context.setAttribute(StringUtils.uncapitalize(cls.getSimpleName())+"List", enumList);
			for(@SuppressWarnings("rawtypes") GeneralEnum genum : enumList){	
				context.setAttribute(StringUtils.uncapitalize(cls.getSimpleName()) +genum.name(), genum);			
			}			
		}	
	}
	
	
	
	/**
	 * 加载系统配置
	 */
	private static Map<String,String> sysCfgMap;
	private static Map<String,String> sysCfgDescMap;
	private static void _loadSysCfg(ServletContext context) {
		sysCfgMap = new HashMap<String, String>();
		sysCfgDescMap = new HashMap<String, String>();
		//ISysCfgService cfgService = (ISysCfgService) SpringContextUtil.getBean(ISysCfgService.class);
		List<SysCfg> sysCfgList = cfgService.queryList(new SysCfg(), null);
		for(SysCfg sysCfg : sysCfgList){
			if(StringUtils.isNotBlank(sysCfg.getCfgDesc())){
				sysCfgDescMap.put(sysCfg.getCfgCode(), sysCfg.getCfgDesc());
			}
			sysCfgMap.put(sysCfg.getCfgCode(), sysCfg.getCfgValue());
		}
		context.setAttribute("sysCfgMap",sysCfgMap);
		context.setAttribute("sysCfgDescMap", sysCfgDescMap);
	}
	
	public static String getSysCfg(String key){
		return StringUtils.defaultString(sysCfgMap.get(key));
	}
	
	//**************** 角色菜单  *******************
	
	/**
	 * 加载角色菜单
	 */
//	private static Map<String, List<SysMenu>> roleId_sysMenuListMap ;
//	public static void _loadRole(ServletContext context) {
//		roleId_sysMenuListMap = new HashMap<String, List<SysMenu>>();
//		//从系统配置获取各个身份的角色的ID
//		List<String> roleIdList = new ArrayList<String>();
//		//管理员
//		String roleId_admin = getSysCfg(GlobalConstant.ROLE_ID_ADMIN);
//		roleIdList.add(roleId_admin);
//		//根据角色Id查询菜单列表
//		//ISysMenuService menuService = (ISysMenuService) SpringContextUtil.getBean(ISysMenuService.class);
//		for (String rId : roleIdList) {
//			if(StringUtils.isNotBlank(rId)) {
//				List<SysMenu> menuList = menuService.queryMenuListByRoleId(rId);
//				roleId_sysMenuListMap.put(rId, menuList);
//			}
//		}
//		//--超级管理员
//		SysMenu menu = new SysMenu();
//		menu.setCategoryId(GlobalConstant.N);
//		List<SysMenu> menuList = menuService.queryList(menu, GlobalConstant.SORT_KEY_CREATE_TIME);
//		roleId_sysMenuListMap.put("0", menuList);
//		context.setAttribute("roleId_sysMenuListMap", roleId_sysMenuListMap);
//	}
//
//	public static List<SysMenu> getSysMenuListByRoleId(String roleId){
//		if(StringUtils.isBlank(roleId)){
//			return null;
//		}
//		return roleId_sysMenuListMap.get(roleId);
//	}
}