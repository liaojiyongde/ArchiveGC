<template>
    <DataLayout>
    <template v-slot:header>
      <el-form :inline="true">
        <el-form-item>
          <el-input
            style="width: 200px"
            v-model="inputValueNum"
            placeholder='请输入编码或标题'
            @keyup.enter.native="search()"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-date-picker
            v-model="startDate"
            type="date"
            placeholder="开始日期-编制日期"
            value-format="yyyy-MM-dd"
          ></el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-date-picker
            v-model="endDate"
            type="date"
            align="right"
            placeholder="结束日期-编制日期"
            value-format="yyyy-MM-dd"
          ></el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search()">{{
            $t("application.SearchData")
          }}</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="primary">发起流程</el-button>
        </el-form-item>
      </el-form>
    </template>
    <template v-slot:main="{ layout }">
      <DataGrid
        ref="InvalidingGrid"
        key="Invaliding"
        dataUrl="/dc/getDocuments"
        v-bind:tableHeight="layout.height-166"
        v-bind:isshowOption="true" v-bind:isshowSelection ="false"
        gridViewName="InvalidingGrid"
        condition="STATUS='已完成'"
        :optionWidth = "2"
        :isshowCustom="false"
        :isEditProperty="false"
        :isShowMoreOption="false"
        :isshowicon="false"
        :isShowChangeList="false"
        :isInitData="false"
        @rowclick="onDataGridRowClick"
      >
      </DataGrid>
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
      inputValueNum:"",
      startDate: "",
      endDate:"",
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
    //文档模糊查询
    search() {
      let _self = this;
      let key="((IS_CURRENT=1 AND STATUS='作废') OR (C_STORE_STATUS<>'已作废' AND IS_CURRENT=0 AND C_INCLUDE_PAPER='有')) AND IS_RELEASED=1 AND TYPE_NAME='设计文件'";
      if(_self.inputValueNum!=''&&_self.inputValueNum!=undefined){
        key+="and (CODING LIKE '%"+_self.inputValueNum+"%' OR TITLE LIKE '%"+_self.inputValueNum+"%')";
      }
      if(_self.startDate!=''&&_self.startDate!=undefined){
          key+=" and C_DRAFT_DATE > '"+_self.startDate+"'";
      }
      if(_self.endDate!=''&&_self.endDate!=undefined){
          key+=" and C_DRAFT_DATE < '"+_self.endDate+"'";
      }
      _self.$refs.InvalidingGrid.condition=key;
      _self.$refs.InvalidingGrid.currentPage = 1;
      _self.$refs.InvalidingGrid.loadGridInfo();
      _self.$refs.InvalidingGrid.loadGridData();
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