package com.ecm.portal.archivegc.controller;

import static org.hamcrest.CoreMatchers.instanceOf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.ecm.common.util.DateUtils;
import com.ecm.common.util.EcmStringUtils;
import com.ecm.common.util.ExcelUtil;
import com.ecm.common.util.FileUtils;
import com.ecm.common.util.JSONUtils;
import com.ecm.core.ActionContext;
import com.ecm.core.cache.manager.CacheManagerOper;
import com.ecm.core.db.SqlUtils;
import com.ecm.core.entity.EcmContent;
import com.ecm.core.entity.EcmDocument;
import com.ecm.core.entity.EcmFolder;
import com.ecm.core.entity.EcmGridView;
import com.ecm.core.entity.EcmGridViewItem;
import com.ecm.core.entity.EcmRelation;
import com.ecm.core.entity.LoginUser;
import com.ecm.core.exception.AccessDeniedException;
import com.ecm.core.exception.EcmException;
import com.ecm.core.service.DocumentService;
import com.ecm.core.service.ExcSynDetailService;
import com.ecm.core.service.FolderPathService;
import com.ecm.core.service.FolderService;
import com.ecm.core.service.GridViewItemService;
import com.ecm.core.service.GridViewService;
import com.ecm.core.service.RelationService;
import com.ecm.portal.archivegc.controller.param.DocParam;
import com.ecm.portal.controller.ControllerAbstract;

@RestController
@RequestMapping("/exchange/doc")
public class DocController  extends ControllerAbstract  {
	
	@Autowired
	private DocumentService documentService;

	@Autowired
	private RelationService relationService;
	
	@Autowired
	private FolderPathService folderPathService;
	
	@Autowired
	private ExcSynDetailService synDetailService;
	
	@Autowired
	private GridViewService gridViewService;
	@Autowired
	private FolderService folderService;
	@Autowired
	private GridViewItemService gridViewItemService;
	private final String queryBase = "SELECT ID,APP_NAME, CREATION_DATE, EXPORT_DATE, IMPORT_DATE, STATUS, ERROR_MESSAGE FROM exc_syn_detail";
	
	@PostMapping("exportTC")
	@ResponseBody
	public void getExportExcelTC(HttpServletRequest request, HttpServletResponse response, @RequestBody String argStr) {
		ExcelUtil excelUtil = new ExcelUtil();
		List<Object[]> datalist = new ArrayList<Object[]>();
		
		Map<String, Object> args = JSONUtils.stringToMap(argStr);
		
		String dataType = this.getStrValue(args, "dataType");
		String startDate = this.getStrValue(args, "startDate");
		String endDate = this.getStrValue(args, "endDate");
		String title = this.getStrValue(args, "titlename");
		List<String> titlename = JSONUtils.stringToArray(title);
		String titlecn = this.getStrValue(args, "titlecnname");
		List<String> titlecnname = JSONUtils.stringToArray(titlecn);
		String filename = this.getStrValue(args, "filename");
		String sheetname = this.getStrValue(args, "sheetname");
		
		String[] titleName = new String[titlename.size()+1];
		String[] titleCNName = new String[titlecnname.size()+1];

		titleName[0]="ID";
		titleCNName[0]="ID";
		for (int i = 1; i < titlename.size() + 1; i++) {
			titleName[i] = titlename.get(i - 1);
			titleCNName[i] = titlecnname.get(i - 1);
		}
		datalist.add(titleCNName);
		
		String timeCheck = new String();
		
		if(!StringUtils.isEmpty(startDate) && !StringUtils.isEmpty(endDate)) {
			timeCheck = setSQLTimeSE(startDate, endDate, dataType);
		}
		if(StringUtils.isEmpty(startDate) && !StringUtils.isEmpty(endDate)) {
			timeCheck = setSQLTimeE(endDate, dataType);
		}
		if(!StringUtils.isEmpty(startDate) && StringUtils.isEmpty(endDate)) {
			timeCheck = setSQLTimeS(startDate, dataType);
		}
		if(StringUtils.isEmpty(startDate) && StringUtils.isEmpty(endDate)) {
			timeCheck = setSQLTimeEmp();
		}
		
		String tcListSQL = new String();
		
		if(dataType.equals("TCSync")) {
			tcListSQL = queryBase+" WHERE APP_NAME = 'TC' AND ACTION_NAME = '接收'"+
					       timeCheck;
		}
		if(dataType.equals("TCPush")){
			tcListSQL = queryBase+" WHERE APP_NAME = 'TC' AND ACTION_NAME = '发送'"+
						   timeCheck;
		}
		if(dataType.equals("NetSync")) {
			tcListSQL = queryBase+" WHERE APP_NAME = 'DOCEX'"+
						   timeCheck;
		}
		String sql="select * from ("+tcListSQL+") t ORDER BY EXPORT_DATE DESC";
		
		try {
			List<Map<String, Object>> getTCList = synDetailService.getExcSynDetails(sql);
			
			Map<String, Object> projMap = new HashMap<String, Object>();
			
			for (Map<String, Object> item : getTCList) {
				projMap = new HashMap<String, Object>();
				projMap.put("ID",item.get("ID").toString());
				projMap.put("typeName", (item.get("APP_NAME")==null)?"":item.get("APP_NAME").toString());				
				
				projMap.put("createdTime", (item.get("CREATION_DATE")==null)?"":item.get("CREATION_DATE").toString());
				
				projMap.put("executedTime", (item.get("EXPORT_DATE")==null)?"":item.get("EXPORT_DATE").toString());
				
				projMap.put("finishedTime", (item.get("IMPORT_DATE")==null)?"":item.get("IMPORT_DATE").toString());
				
				projMap.put("logStatus", (item.get("STATUS")==null)?"":item.get("STATUS").toString());
				
				projMap.put("errorMessage", (item.get("ERROR_MESSAGE")==null)?"":item.get("ERROR_MESSAGE").toString());
				
				Object[] values = new Object[titleName.length];
				for (int i = 0; i < titleName.length; i++) {
					if (projMap.get(titleName[i]) != null) {						
						values[i] = projMap.get(titleName[i]);						
					} else {
						values[i] = "";
					}
				}
				datalist.add(values);
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			excelUtil.makeStreamExcel(filename, sheetname, titleName, datalist, response, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@PostMapping("export")
	@ResponseBody
	public void getExportExcel(HttpServletRequest request, HttpServletResponse response, @RequestBody DocParam params) {
		ExcelUtil excelUtil = new ExcelUtil();
		List<Object[]> datalist = new ArrayList<Object[]>();
		EcmGridView gv = null;
		List<EcmGridViewItem> list = null;
		if(params.getIsCustom() !=null && params.getIsCustom()==true ) {
			try {
				String ctreator = this.getSession().getCurrentUser().getUserName();
				String condition = " NAME='"+ params.getGridName() +"' AND GRID_TYPE=1 and  CREATOR='"+ctreator+"'";
				gv = gridViewService.getObjectByCondition(getToken(), condition);
				gv.getGridViewItems();
				list = gridViewItemService.getEcmCustomGridViewInfo(getToken(), gv.getId());
			} catch (AccessDeniedException e) {
				e.printStackTrace();
			}
		}else {
			gv = CacheManagerOper.getEcmGridViews().get(params.getGridName());
			
			gv.getGridViewItems();
			list = gv.getGridViewItems(params.getLang());
		}
		
		StringBuffer sql = new StringBuffer("select ID,");
	
		
		String[] titleName = new String[list.size() + 1];
		String[] titleCNName = new String[list.size() + 1];
		StringBuffer queryAttr = new StringBuffer();
		titleName[0]="ID";
		titleCNName[0]="ID";
		for (int i = 1; i < list.size() + 1; i++) {
			titleName[i] = list.get(i - 1).getAttrName();
			titleCNName[i] = list.get(i - 1).getLabel();
			queryAttr.append(list.get(i - 1).getAttrName());
			queryAttr.append(",");
		}
		datalist.add(titleCNName);
		String gvCondition=gv.getCondition();
		if(gvCondition==null||"".equals(gvCondition)) {
			gvCondition="";
		}else {
			gvCondition=" and "+gvCondition;
		}
				
		sql.append(queryAttr.deleteCharAt(queryAttr.length() - 1).toString() 
				+ " from ecm_document where 1=1"+gvCondition);
		if (!StringUtils.isEmpty(params.getFolderId())) {
			sql.append(" and folder_id='" + params.getFolderId() + "'");
		}
		
		if (!StringUtils.isEmpty(params.getCondition())) {
			sql.append(" and " + params.getCondition());
		}
		
		if (!StringUtils.isEmpty(params.getOrderBy())) {
			sql.append(" order by " + params.getOrderBy());
		} else {
			sql.append(" order by ID desc");
		}
		try {
			String sqlAfter=SqlUtils.replaceSql(sql.toString(), this.getSession().getCurrentUser());
			List<Map<String,Object>> queryList =documentService.getMapList(getToken(), sqlAfter);
			for (Map<String, Object> map : queryList) {
				Object[] values = new Object[titleName.length];
				for (int i = 0; i < titleName.length; i++) {
					if (map.get(titleName[i]) != null) {
						
						if (titleName[i].equals("C_DOC_DATE")) {
							values[i] = DateUtils.DateToFolderPath((Date) map.get(titleName[i]), "-");
						} else if(titleName[i].toLowerCase().endsWith("_date")) {
							Object dateValue = map.get(titleName[i]);
							if(dateValue instanceof Date) {
								values[i] = (Date)dateValue;
							}
						}else {
							values[i] = map.get(titleName[i]);
						}
					} else {
						values[i] = "";
					}
				}
				datalist.add(values);
			}
		} catch (EcmException | AccessDeniedException e) {
			e.printStackTrace();
		}
		
		try {
			excelUtil.makeStreamExcel(params.getFilename(), params.getSheetname(),titleName, datalist, response, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@RequestMapping(value = "newReplyDoc", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> newReplyDoc(String metaData,String replyDocId, boolean includeRefDoc, MultipartFile uploadFile) throws Exception {
		Map<String, Object> args = JSONUtils.stringToMap(metaData);
		EcmContent en = null;
		EcmDocument doc = new EcmDocument();
		doc.setAttributes(args);
		doc.setStatus("新建");
		if (uploadFile != null) {
			en = new EcmContent();
			en.setName(uploadFile.getOriginalFilename());
			en.setContentSize(uploadFile.getSize());
			en.setFormatName(FileUtils.getExtention(uploadFile.getOriginalFilename()));
			en.setInputStream(uploadFile.getInputStream());
		}
		String folderId = folderPathService.getFolderId(getToken(), doc.getAttributes(), "3");
		if(folderId != null) {
			doc.setFolderId(folderId);
		}else {
			doc.setAclName("acl_all_write");
		}
		String id = documentService.newObject(getToken(), doc, en);
		if(includeRefDoc) {
			createRefDocs(getToken(),replyDocId, id);
		}
		Map<String, Object> mp = new HashMap<String, Object>();
		mp.put("code", ActionContext.SUCESS);
		mp.put("id", id);
		return mp;
	}
	@RequestMapping(value = "deleteRelations", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> remove(@RequestBody List<String> ids) {
		Map<String, Object> mp = new HashMap();
		String formId = ids.get(0);
		for(int i = 1;i < ids.size();i++) {
		try {
			String id = ids.get(i);
			String relationSqlStr = "select id from ecm_relation where parent_id='"+formId+"' and child_id='"+id+"'";
			List<Map<String, Object>> list = relationService.getMapList(getToken(), relationSqlStr);
			relationService.deleteObjectById(getToken(),id);
			Map<String,Object> temprs = list.get(0);
			relationService.deleteObjectById(getToken(),temprs.get("id").toString());			//仅删除当前文件挂载关系
			EcmDocument eu = documentService.getObjectById(getToken(), id);
			if(eu.getTypeName().equals("附件")) {
				documentService.deleteObjectById(getToken(), id);	
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			mp.put("code", ActionContext.SUCESS);
		}
		}
		mp.put("code", ActionContext.SUCESS);
		return mp;
	}
	@RequestMapping(value = "AddRelations", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> adds(@RequestBody List<String> ids) {
		Map<String, Object> mp = new HashMap();
		String formId = ids.get(0);
		for(int i = 1;i < ids.size();i++) {
		try {
			String id = ids.get(i);
			String relationSqlStr = "select id from ecm_relation where parent_id='"+formId+"' and child_id='"+id+"'";
			List<Map<String, Object>> list = relationService.getMapList(getToken(), relationSqlStr);   //先找当前子文件ID是否和表单有关联关系
			if(list!=null) {		//当前结果MAP为空，说明需要创建关系
				EcmRelation relation = new EcmRelation();
				relation.setParentId(formId);
				relation.setChildId(id);
				relation.setName("irel_children");
				relationService.newObject(getToken(), relation);
			}		
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			mp.put("code", 1);
		}
		}
		mp.put("code", 0);
		return mp;
	}
	
	@RequestMapping(value = "addAttachment4Copy", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> add4Attachment(String metaData, MultipartFile[] uploadFile) {
		
		
		Map<String, Object> args = JSONUtils.stringToMap(metaData);
		Map<String, Object> mp = new HashMap<String, Object>();
		try {
			EcmDocument form = new EcmDocument();
			Map<String,Object> temp = new HashMap<String,Object>();
			
			String parentId = args.get("parentDocId").toString();
			if("".equals(parentId)) {
				mp.put("code", ActionContext.FAILURE);
				mp.put("message","没有主文件ID");
				return mp;
			}
			temp = documentService.getObjectMapById(getToken(), parentId);
			form.setId(parentId);
			if(temp==null) {
			form.setTypeName("临时表单");
			documentService.newObject(getToken(), form.getAttributes());		//要是表单没有的话就先创建一个空白表单
			
			}
			if (uploadFile != null&&uploadFile.length>0) {
				for (MultipartFile multipartFile : uploadFile) {
					
					EcmDocument doc = new EcmDocument();
					doc.setAttributes(args);
					String fileName=multipartFile.getOriginalFilename();
					fileName=fileName.substring(0,fileName.lastIndexOf(".")<0
							?fileName.length():fileName.lastIndexOf("."));
					doc.setName(fileName);
					
					Object fid= args.get("folderId");
					String folderId="";
					if(fid==null) {
						folderId= folderPathService.getFolderId(getToken(), doc.getAttributes(), "3");
					}else {
						folderId=fid.toString();
					}
					EcmFolder folder= folderService.getObjectById(getToken(), folderId);
					doc.setFolderId(folderId);
					doc.setAclName(folder.getAclName());
					
					EcmContent en = new EcmContent();
					en.setName(multipartFile.getOriginalFilename());
					en.setContentSize(multipartFile.getSize());
					en.setInputStream(multipartFile.getInputStream());
//					documentService.addRendition(getToken(), parentId, en);
					
					String relationName="irel_children";
					relationName=args.get("relationName")!=null
							&&!"".equals(args.get("relationName").toString())
							?args.get("relationName").toString():"irel_children";
					String id = documentService.newObject(getToken(),doc,en);//创建文件和内容
					//----------------创建关系--------------
					EcmRelation relation=new EcmRelation();
					relation.setParentId(parentId);
					
					relation.setChildId(id);
					relation.setName(relationName);
					try {
						relationService.newObject(getToken(), relation);
					} catch (EcmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						mp.put("code", ActionContext.FAILURE);
						mp.put("message",e.getMessage());
						return mp;
					}
					//----------------end创建关系-------------------
				}
				
				mp.put("code", ActionContext.SUCESS);
			}
		} catch (Exception ex) {
			mp.put("code", ActionContext.FAILURE);
			mp.put("message", ex.getMessage());
		}
		return mp;
	}
	
	
	private void createRefDocs(String token,String fromId, String toId) throws Exception {
		EcmDocument fromDoc = documentService.getObjectById(token, fromId);
		String folderId = null;
		List<Map<String, Object>> refList = null;
		if(fromDoc.getTypeName().equals("文件传递单")) {
			refList = getChildData(token, fromId, "设计文件");
			if(refList != null) {
				int i =1;
				for(Map<String, Object> mp: refList) {
					EcmDocument refDoc = new EcmDocument();
					refDoc.setTypeName("相关文件");
					refDoc.addAttribute("CODING",mp.get("CODING"));
					refDoc.addAttribute("C_IN_CODING",mp.get("C_IN_CODING"));
					refDoc.addAttribute("TITLE",mp.get("TITLE"));
					refDoc.addAttribute("REVISION",mp.get("REVISION"));
					refDoc.setAclName("acl_all_write");
					if(folderId == null) {
						folderId = folderPathService.getFolderId(token, refDoc.getAttributes(), "3");
					}
					refDoc.setFolderId(folderId);
					String childId = documentService.newObject(token, refDoc, null);
					newRelation(token, toId, "相关文件", childId, i,null);
					i++;
				}
			}
		}else {
			refList = getChildData(token, fromId, "相关文件");
			if(refList != null) {
				int i =1;
				for(Map<String, Object> mp: refList) {
					Map<String, Object> attrValues =new HashMap<String,Object>();
					attrValues.put("ACL_NAME", "acl_all_write");
					String childId = documentService.saveAsNew(token, mp.get("ID").toString(), attrValues, false);
					newRelation(token, toId, "相关文件", childId, i,null);
					i++;
				}
			}
		}
		
	}
	
	
	private void newRelation(String token, String parentId, String relationName, String childId, int orderIndex,
			String description) throws EcmException {
		if (!StringUtils.isEmpty(parentId)) {
			EcmRelation rel = new EcmRelation();
			rel.setParentId(parentId);
			rel.setChildId(childId);
			if (relationName == null || "".equals(relationName)) {
				rel.setName("irel_children");
			} else {
				rel.setName(relationName);
			}

			rel.setOrderIndex(orderIndex);
			if (description != null) {
				rel.setDescription(description);
			}
			relationService.newObject(token, rel);
		}
	}
	
	private List<Map<String, Object>> getChildData(String token,String parentId, String relationName) throws EcmException{
		String sql = "select a.ID,a.CODING,a.C_IN_CODING,a.TITLE,a.REVISION from ecm_document a, ecm_relation b where a.ID=b.CHILD_ID "
				+" and b.PARENT_ID='"+parentId+"' AND b.NAME='"+relationName+"'";
		return documentService.getMapList(token, sql);
	}
	
	private String getStrValue(Map<String, Object> args, String key) {
		return (args.containsKey(key) && args.get(key)!=null)?args.get(key).toString():"";
	}
	
	private String setSQLTimeSE(String key1, String key2, String key3) {
		if(key3 == "NetSync") {
			return " and (CREATION_DATE BETWEEN '" + key1 + "' and '" + key2 + "')";
		}else{
			return " and (EXPORT_DATE BETWEEN '" + key1 + "' and '" + key2 + "')";
		}
	}
	
	private String setSQLTimeE(String key1, String key2) {
		if(key2 == "NetSync") {
			return " and (CREATION_DATE < '" + key1 + "')";
		}else{
			return " and (EXPORT_DATE < '" + key1 + "')";
		}
	}
	
	private String setSQLTimeS(String key1, String key2) {
		if(key2 == "NetSync") {
			return " and (CREATION_DATE > '" + key1 + "')";
		}else {
			return " and (EXPORT_DATE > '" + key1 + "')";
		}	
	}
	
	private String setSQLTimeEmp() {
		return "";
	}
	
}
