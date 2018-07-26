package com.kevin.service.sys.impl;

import com.kevin.common.GlobalConstant.GlobalConstant;
import com.kevin.common.core.GeneralMethod;
import com.kevin.common.shiro.PasswordHelper;
import com.kevin.common.utils.ExcelUtil;
import com.kevin.common.utils.UUIDUtil;
import com.kevin.dao.mapper.SysUserMapper;
import com.kevin.exception.CommonException;
import com.kevin.model.SysUser;
import com.kevin.model.SysUserExample;
import com.kevin.service.sys.ISysUserService;
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
@Service
@Transactional(rollbackFor = Exception.class)
public class SysUserServiceImpl implements ISysUserService {
    private static Logger logger = LoggerFactory.getLogger(SysUserServiceImpl.class);

    @Autowired
    private SysUserMapper sysUserMapper;

//    @Autowired
//    private RedisUtil redisUtil;

    @Override
    @CachePut(value = "currUser",key = "'sysUser_'+#sysUser.getUserId()")
    public int save(SysUser sysUser) {
        if (StringUtils.isBlank(sysUser.getUserId())) {//新增
            sysUser.setUserId(UUIDUtil.getUUID());
            sysUser.setUserPwd(PasswordHelper.encryptPassword(sysUser.getUserId(), GlobalConstant.RESET_PASSWORD));
            GeneralMethod.setRecordInfo(sysUser, true);
            return sysUserMapper.insertSelective(sysUser);
        } else {//修改
            GeneralMethod.setRecordInfo(sysUser, false);
            return sysUserMapper.updateByPrimaryKeySelective(sysUser);
        }
    }

    @Override
    public int logicallyDeleteById(String id) {
        if (StringUtils.isNotBlank(id)) {
            //update
            SysUser user = new SysUser();
            //set
            user.setRecordState(GlobalConstant.N);
            //where
            user.setUserId(id);
            return save(user);
        }
        return GlobalConstant.ZERO;
    }

    @Override
    public int deleteById(String id) {
        if (StringUtils.isNotBlank(id)) {
            return sysUserMapper.deleteByPrimaryKey(id);
        }
        return GlobalConstant.ZERO;
    }

    @Override
//    @Cacheable(value = "currUser",key="'sysUser_'+#sysUser.getUserId()")
    public List<SysUser> queryList(SysUser sysUser, String orderByClause) {
        System.err.println("没有走缓存！"+sysUser.getUserId());
//        SysUser setRedisCurrUser = (SysUser)redisUtil.get("currUser");
//        logger.debug("-------------redis缓存中获取当前用户信息:--------------------" + setRedisCurrUser.getUserName() + setRedisCurrUser.getUserAcc());
        SysUserExample example = new SysUserExample();
        SysUserExample.Criteria criteria = example.createCriteria().andRecordStateEqualTo(GlobalConstant.Y);
        andCrieria(sysUser, criteria);
        /* 排序字段 */
        if (StringUtils.isNotBlank(orderByClause)) {
            example.setOrderByClause(orderByClause);
        }
        return sysUserMapper.selectByExample(example);
    }

    @Override
    public SysUser getById(String id) {
        if (StringUtils.isNotBlank(id)) {
            return sysUserMapper.selectByPrimaryKey(id);
        }
        return null;
    }

    private void andCrieria(SysUser user, SysUserExample.Criteria criteria) {
        if (StringUtils.isNotBlank(user.getUserName())) {
            criteria.andUserNameLike(GlobalConstant.PERCENT + user.getUserName() + GlobalConstant.PERCENT);
        }
        if (StringUtils.isNotBlank(user.getUserPhone())) {
            criteria.andUserPhoneLike(GlobalConstant.PERCENT + user.getUserPhone() + GlobalConstant.PERCENT);
        }
    }

    @Override
    public SysUser insertRoot() {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(GlobalConstant.ROOT_USER_ID);
        sysUser.setUserAcc(GlobalConstant.ROOT_USER_ACC);
        sysUser.setUserName(GlobalConstant.ROOT_USER_NAME);
        String newPwd = PasswordHelper.encryptPassword(GlobalConstant.ROOT_USER_ID, GlobalConstant.RESET_PASSWORD);
        sysUser.setUserPwd(newPwd);
        //sysUser.setStatusId(UserStatusEnum.Activated.getId());
        sysUser.setRecordState(GlobalConstant.Y);
        sysUser.setCreateUserId(GlobalConstant.ROOT_USER_ID);
        sysUser.setCreateTime(Calendar.getInstance().getTime());
        sysUserMapper.insertSelective(sysUser);
        return sysUser;
    }

    @Override
    public SysUser getSysUserByUserAcc(String userAcc) {
        if(StringUtils.isNotBlank(userAcc)){
            SysUserExample example = new SysUserExample();
            example.createCriteria().andUserAccEqualTo(userAcc).andRecordStateEqualTo(GlobalConstant.Y);
            List<SysUser> userList = sysUserMapper.selectByExample(example);
            if (userList != null && !userList.isEmpty()) {
                return userList.get(0);
            }
        }
        return null;
    }

    @Override
    public List<SysUser> checkUnique(SysUser sysUser) {
        SysUserExample example = new SysUserExample();
        SysUserExample.Criteria criteria = example.createCriteria().andRecordStateEqualTo(GlobalConstant.Y);
        if (StringUtils.isNotBlank(sysUser.getUserAcc())) {
            criteria.andUserAccEqualTo(sysUser.getUserAcc());
        }
        if (StringUtils.isNotBlank(sysUser.getUserPhone())) {
            criteria.andUserPhoneEqualTo(sysUser.getUserPhone());
        }
        //非自己！！
        if (sysUser.getUserId() != null) {
            criteria.andUserIdNotEqualTo(sysUser.getUserId());
        }
        List<SysUser> sysUserList = sysUserMapper.selectByExample(example);
        if (sysUserList != null && !sysUserList.isEmpty()) {
            return sysUserList;
        }
        return null;
    }

    @Override
    @CachePut(value = "currUser", key = "#sysUser.userId")
    public void cachePut(SysUser sysUser) {
        logger.debug("为id、key为:" + sysUser.getUserId() + "数据做了缓存");
    }

    @Override
    @CacheEvict(value = "currUser", key = "#userId")
    public void cacheEvict(String userId) {
        logger.debug("删除了id、key为" + userId + "的数据缓存");
    }

    @Override
    @Cacheable(value = "currUser", key = "#userId")
    public SysUser cacheable(String userId) {
        logger.debug("为id、key为:" + userId + "数据做了缓存");
        return getById(userId);
    }

    /**
     * 上传文件解析成工作薄
     * @param excelFile
     * @return
     * @throws Exception
     */
    private Workbook parseFileToWorkbook(MultipartFile excelFile) throws Exception{
        InputStream is  = null;
        try {
            //1、解析Excel
            is = excelFile.getInputStream();
            byte[] fileData = new byte[(int) excelFile.getSize()];
            is.read(fileData);
            Workbook wb = ExcelUtil.createCommonWorkbook(new ByteInputStream(fileData, (int) excelFile.getSize()));
            //Workbook wb = ExcelUtil.createCommonWorkbook(new POIFSFileSystem(excelFile.getInputStream()));
            return wb;
        }catch (Exception e){
            // ******************* 抛出自定义异常 ！！！！！*********************
            throw new CommonException(e.getMessage());
        }finally {
            try {
                if(is != null) {
                    is.close();
                }
            }catch (Exception e2) {
                throw new RuntimeException(e2.getMessage());
            }
        }
    }

    /**
     * 封装成list对象
     * @param wb
     * @return
     * @throws Exception
     */

	/*private List<MngProcessRoute> workbookEncapIntoList(Workbook wb) {
		int sheetNum = wb.getNumberOfSheets();// workbook中的sheet数
		if (sheetNum > 0) {
			List<MngProcessRoute> processRouteList = new ArrayList<>();
			Sheet sheet;
			try {
				sheet = (HSSFSheet) wb.getSheetAt(0);// 得到第一个sheet（以excel的版本来分别）
			} catch (Exception e) {
				sheet = (XSSFSheet) wb.getSheetAt(0);
			}
			int row_num = sheet.getLastRowNum(); // excel的总行数
			for (int i = 1; i <= row_num; i++) {// 读取每行
				// -----单元格begin------
				Row r = sheet.getRow(i);
				int cell_num = 11;// 定死列数 防止读其他空的列
				MngProcessRoute addRoute = new MngProcessRoute();
				MngWorkProcess workProcess = new MngWorkProcess(); // 工序
				MngWorkCenter serchCenter = new MngWorkCenter();

				MngInspectRequire inspectRequire = new MngInspectRequire(); // 检验要求

				MngWorkProcessMaterial processMaterial = new MngWorkProcessMaterial(); // 资源(物料)
				MngMaterial search = new MngMaterial();
				MngMaterialType searchType = new MngMaterialType();
				MngMaterialVersion searchVersion = new MngMaterialVersion();

				MngWorkProcessEquip processEquip = new MngWorkProcessEquip(); // 设备
				MngEquip serchEquip = new MngEquip();

				String key = GlobalConstant.EMPTY;
				for (int j = 0; j < cell_num; j++) {// 遍历一行每列
					String value = "";
					Cell cell = r.getCell((short) j);
					if (cell == null) {
						continue;
					}
					cell.setCellType(HSSFCell.CELL_TYPE_STRING);
					if (cell.getCellType() == HSSFCell.CELL_TYPE_STRING) {
						value = r.getCell((short) j).getStringCellValue();// 将cell中的值付给value
					} else {
						value = ExcelUtil._doubleTrans(r.getCell((short) j).getNumericCellValue());
					}

					// Excel数据封装成java对象
					value = value.trim();
					logger.debug("第" + i + "行" + (j + 1) + "列：" + value);

					if (j == 0) {
					} else if (j == 1) {
						workProcess.setProcessCode(value);
					} else if (j == 2) {
						workProcess.setProcessName(value);
					} else if (j == 3) {
						serchCenter.setWorkCenterName(value); // 工作中心
					} else if (j == 4) {
						workProcess.setPersionQualify(value);
					} else if (j == 5) {
						inspectRequire.setInspectType(Integer.valueOf(value));
					} else if (j == 6) {
						inspectRequire.setRequirement(value);
					} else if (j == 7) {
						inspectRequire.setRequireParam(value);
					} else if (j == 8) {
						search.setMaterialName(value); // 物料名称
					} else if (j == 9) {
						search.setMaterialCode(value); // 物料编码
					} else if (j == 10) {
						searchType.setMaterialTypeName(value); // 物料类型
					} else if (j == 11) {
						searchVersion.setMaterialVersionName(value); // 物料版本
					} else if (j == 11) {
						processMaterial.setMaterialNum((Integer.parseInt(value)));
					} else if (j == 12) {
						serchEquip.setEquipName(value); // 设备名称
					} else if (j == 13) {
						serchEquip.setEquipCode(value); // 设备编码

						// 验证:查询物料、类型及版本
						Map<String, Object> paramMap = new HashMap<String, Object>();
						paramMap.put("material", search);
						paramMap.put("searchType", searchType);
						paramMap.put("materialVersion", searchVersion);
						MngProcessRouteExcelForm searchExtMaterial = processRouteExtMapper.getFromExcelRow1(paramMap);
						MngMaterial existMaterial = null;
						MngMaterialType existType = null;
						MngMaterialVersion existMaterialVersion = null;
						if (searchExtMaterial != null) {
							existMaterial = searchExtMaterial.getMaterial();
							existType = searchExtMaterial.getType();
							existMaterialVersion = searchExtMaterial.getVersion();
						}
						if (existMaterial == null) {
							try {
								throw new CommonException("导入失败，第" + i + "条记录 第" + (j + 1) + "列 ,物料档案不存在！");
							} catch (CommonException e) {
								e.printStackTrace();
							}
						}
						if (existType == null) {
							try {
								throw new CommonException("导入失败，第" + i + "条记录 第" + (j + 1) + "列 ,物料类型不存在！");
							} catch (CommonException e) {
								e.printStackTrace();
							}
						}
						if (existMaterialVersion == null) {
							try {
								throw new CommonException("导入失败，第" + i + "条记录 第" + (j + 1) + "列 ,物料版本不存在！");
							} catch (CommonException e) {
								e.printStackTrace();
							}
						}
						// 验证:查询设备名称及编码
						paramMap.clear();
						paramMap.put("serchEquip", serchEquip);

						MngProcessRouteExcelForm searchExtEquip = processRouteExtMapper.getFromExcelRow2(paramMap);
						MngEquip existEquip = null;
						if (searchExtEquip != null) {
							existEquip = searchExtEquip.getEquip();
						}
						if (existEquip == null) {
							try {
								throw new CommonException("导入失败，第" + i + "条记录 第" + (j + 1) + "列 ,设备不存在！");
							} catch (CommonException e) {
								e.printStackTrace();
							}
						}
						// 验证:查询工作中心
						paramMap.clear();
						paramMap.put("serchCenter", serchCenter);
						MngProcessRouteExcelForm searchExtCenter = processRouteExtMapper.getFromExcelRow3(paramMap);
						MngWorkCenter existWorkCenter = null;
						if (searchExtCenter != null) {
							existWorkCenter = searchExtCenter.getCenter();
						}
						if (existWorkCenter == null) {
							try {
								throw new CommonException("导入失败，第" + i + "条记录 第" + (j + 1) + "列 ,工作中心不存在！");
							} catch (CommonException e) {
								e.printStackTrace();
							}
						}

						String id = UUIDUtil.getUUID();

						addRoute.setProcessRouteId(id);

						processMaterial.setMaterialId(existMaterial.getMaterialId());
						processMaterial.setMaterialId(existMaterial.getMaterialCode());
						processMaterial.setWorkProcessId(id);

						processEquip.setEquipId(serchEquip.getEquipId());
						processEquip.setWorkProcessId(id);

						workProcess.setWorkCenterId(existWorkCenter.getWorkCenterId());
						workProcess.setProcessId(id);

					} // --------读取每行单元格end-----------
						// 读取完一行记录
					processRouteList.add(addRoute);
				} // ----------读取Excel所有行end
				return processRouteList;
			}
		}
		return null;
	}*/

}
