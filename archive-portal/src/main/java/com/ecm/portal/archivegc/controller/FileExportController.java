package com.ecm.portal.archivegc.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ecm.common.util.EcmStringUtils;
import com.ecm.common.util.ExcelUtil;
import com.ecm.common.util.JSONUtils;
import com.ecm.core.ActionContext;
import com.ecm.core.cache.manager.CacheManagerOper;
import com.ecm.core.entity.EcmFolder;
import com.ecm.core.entity.EcmGridView;
import com.ecm.core.entity.EcmGridViewItem;
import com.ecm.core.entity.Pager;
import com.ecm.core.exception.AccessDeniedException;
import com.ecm.core.service.DocumentService;
import com.ecm.core.service.FolderPathService;
import com.ecm.core.service.FolderService;
import com.ecm.portal.controller.ControllerAbstract;

@Controller
public class FileExportController extends ControllerAbstract{
	@Autowired
	private DocumentService documentService;
	@Autowired
	private FolderService folderService;
	
	@RequestMapping(value = "/file/exportFolderPath")
	@ResponseBody
	
	public void exportFolderPath(HttpServletRequest request, HttpServletResponse response, @RequestBody String params) throws Exception{
		ExcelUtil excelUtil = new ExcelUtil();
		List<Object[]> datalist = new ArrayList<Object[]>();
		
		Map<String, Object> args = JSONUtils.stringToMap(params);
		
		String folderId = (String) args.get("folderId");
		String gridName = (String) args.get("gridName");
		String lang = (String) args.get("lang");
		int pageSize = Integer.parseInt(args.get("pageSize").toString());
		int pageIndex = Integer.parseInt(args.get("pageIndex").toString());
		Pager pager = new Pager();
		pager.setPageIndex(pageIndex);
		pager.setPageSize(pageSize);
		
		try {
			EcmFolder ecmFolder = folderService.getObjectById(getToken(), folderId);
			String folderPath = ecmFolder.getFolderPath();
			
			String fileName[]=folderPath.split("/");
			Date date = new Date();
	        //设置要获取到什么样的时间
	        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH_mm_ss");
	        //获取String类型的时间
	        String createdate = sdf.format(date);
			
	        String filename = new String();
	        String sheetname = new String();
	        
	        for (int i = 1; i < fileName.length; i++) {
	        	filename += fileName[i] + "-";
	        }
	        filename += createdate + ".xlsx";
			sheetname = fileName[fileName.length-1];
			
			EcmGridView gv = CacheManagerOper.getEcmGridViews().get(gridName);
			List<EcmGridViewItem> list = gv.getGridViewItems(lang);
			
			String[] titleName = new String[list.size()];
			String[] titleCNName = new String[list.size()];
			titleName[0]="ID";
			titleCNName[0]="ID";
			
			for (int i = 1; i < list.size(); i++) {
				titleName[i] = list.get(i - 1).getAttrName();
				titleCNName[i] = list.get(i - 1).getLabel();
			}
			datalist.add(titleCNName);
			
			StringBuffer condition = new StringBuffer("(" + gv.getCondition() + ") ");
			condition.append(" and FOLDER_ID in (SELECT id from ecm_folder where folder_path like '%" + ecmFolder.getFolderPath() + "%')");
			
			List<Map<String, Object>> infList = documentService.getObjectsByConditon(getToken(), gridName, folderId, pager, condition.toString(), args.get("orderBy").toString());
			
			Object[] values = new Object[titleName.length];
			for(int j = 0; j < infList.size(); j++) {
				values = new Object[titleName.length];
				for (int i = 0; i < titleName.length; i++) {
					if (infList.get(j).get(titleName[i]) != null) {						
						values[i] = infList.get(j).get(titleName[i]);						
					} else {
						values[i] = "";
					}
				}
				datalist.add(values);
			}	
		
			excelUtil.makeStreamExcel(filename, sheetname, titleName, datalist, response, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
