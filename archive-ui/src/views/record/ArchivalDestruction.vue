<template>
  <DataLayout>
    <template v-slot:header>
      <el-form :inline="true">
        <el-form-item>
          <el-input
            style="width: 200px"
            v-model="inputValueNum"
            placeholder='请输入编码或标题'
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search()">{{
            $t("application.SearchData")
          }}</el-button>
        </el-form-item>
        <el-form-item>
          <el-select v-model="value" placeholder="新建" v-on:change="changeStatus(val)">
            <el-option
              v-for="item in options"
              :key="item.value"
              :label="item.label"
              :value="item.label"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
            <el-button type="primary" @click="startFlow()">发起流程</el-button>
        </el-form-item>
      </el-form>
    </template>
    <template v-slot:main="{ layout }">
      <div :style="{position:'relative',height: layout.height-startHeight+'px'}">
        <split-pane v-on:resize="onSplitResize" :min-percent='20' :default-percent='topPercent' split="horizontal">
          <template slot="paneL">
                <DataGrid
                  ref="ArchiveDestructionGrid"
                  key="Thematic"
                  dataUrl="/dc/getDocuments"
                  v-bind:tableHeight="(layout.height-startHeight)*topPercent/100-topbarHeight"
                  v-bind:isshowOption="true" v-bind:isshowSelection ="true"
                  gridViewName="ArchiveAppraisalGrid"
                  condition="TYPE_NAME='档案销毁单' and STATUS='新建' and IS_RELEASED=1 "
                  :optionWidth = "2"
                  :isshowCustom="false"
                  :isEditProperty="true"
                  :isInitData="false"
                  showOptions="查看内容"
                  :isShowChangeList="false"
                  @selectchange="selectThChange"
                  @rowclick="onDataGridRowClick"
                >
                </DataGrid>
          </template>
          <template slot="paneR">
            <DataGrid
              ref="DestructionFileGrid"
              key="DestructionFile"
              dataUrl="/dc/getDocuments"
              v-bind:tableHeight="(layout.height-startHeight)*(100-topPercent)/100-bottomHeight"
              v-bind:isshowOption="true" v-bind:isshowSelection ="true"
              gridViewName="AppraisalFileGrid"
              :isInitData="false"
              :optionWidth = "2"
              :isshowCustom="false"
              :isEditProperty="false"
              showOptions="查看内容"
              :isShowChangeList="false"
            >
            </DataGrid>
          </template>
        </split-pane>
      </div>
    </template>
  </DataLayout>
</template>
<script type="text/javascript">
import DataGrid from "@/components/DataGrid";
import DataLayout from "@/components/ecm-data-layout";

export default {
  name: "TC",
  data() {
    return {
      // 本地存储高度名称
      topStorageName: 'ReceivingDC4CnpeHeight',
      // 非split pan 控制区域高度
      startHeight: 135,
      // 顶部百分比*100
      topPercent: 65,
      // 顶部除列表高度
      topbarHeight: 35,
      // 底部除列表高度
      bottomHeight:35,
      selectedDCItems: [],//文档
      selectedThItems:[],//专题
      inputValueNum:"",
      options: [
        {
          value: "新建",
          label: "新建",
        },
        {
          value: "流程中",
          label: "流程中",
        },
        {
          value: "已完成",
          label: "已完成",
        },
      ],
      value:"新建",
    };
  },
  mounted() {
    if (!this.validataPermission()) {
      //跳转至权限提醒页
      let _self = this;
      _self.$nextTick(() => {
        _self.$router.push({ path: "/NoPermission" });
      });
    }
    this.search();
  },
  methods: {
    // 上下分屏事件
    onSplitResize(topPercent){
      // 顶部百分比*100
      this.topPercent = topPercent
      this.setStorageNumber(this.topStorageName, topPercent)
      //console.log(JSON.stringify(topPercent))
    },
    //单击行
    onDataGridRowClick: function (row) {
      var condition1 =
        "SELECT CHILD_ID from ecm_relation where NAME='irel_children'and PARENT_ID ='" +row.ID +"'";
      var key1 = "ID IN (" + condition1 + ") AND IS_RELEASED=1 and STATUS <> '作废'";
      this.$refs.DestructionFileGrid.condition = key1;
      this.$refs.DestructionFileGrid.gridViewName = "GeneralGrid";
      this.$refs.DestructionFileGrid.loadGridInfo();
      this.$refs.DestructionFileGrid.loadGridData();
    },
    //文档模糊查询
    search() {
      let _self = this;
      let key="TYPE_NAME='档案销毁单'";
      if(_self.inputValueNum!=''&&_self.inputValueNum!=undefined){
        key+="and (CODING LIKE '%"+_self.inputValueNum+"%' OR TITLE LIKE '%"+_self.inputValueNum+"%')";
      }
      if(_self.value!=''&&_self.value!=undefined){
        key+=" and STATUS = '"+_self.value+"'";
      }
      _self.$refs.ArchiveDestructionGrid.condition=key;
      _self.$refs.ArchiveDestructionGrid.currentPage = 1;
      _self.$refs.ArchiveDestructionGrid.loadGridInfo();
      _self.$refs.ArchiveDestructionGrid.loadGridData();
    },
    selectThChange(val) {
      this.selectedThItems = val;
    },
    changeStatus:function(){
      this.search()
    },
  },
  props: {},
  components: {
    DataGrid: DataGrid,
    DataLayout: DataLayout,
  },
};
</script>
<style scoped>
.el-form-item {
  margin-bottom: 0px;
}
.el-table td,
.el-table th {
  text-align: center !important;
}
</style>